/**
 * Copyright (C) 2019  Infinite Automation Software. All rights reserved.
 */
package com.infiniteautomation.mango.spring.components;

import java.io.IOException;
import java.net.URI;
import java.net.UnknownHostException;
import java.security.KeyPair;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.mail.internet.AddressException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import com.infiniteautomation.mango.jwt.JwtSignerVerifier;
import com.infiniteautomation.mango.spring.MangoRuntimeContextConfiguration;
import com.infiniteautomation.mango.spring.service.UsersService;
import com.infiniteautomation.mango.util.exception.NotFoundException;
import com.infiniteautomation.mango.util.exception.TranslatableRuntimeException;
import com.infiniteautomation.mango.util.exception.ValidationException;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.dao.SystemSettingsDao;
import com.serotonin.m2m2.email.MangoEmailContent;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.i18n.Translations;
import com.serotonin.m2m2.module.DefaultPagesDefinition;
import com.serotonin.m2m2.rt.maint.work.EmailWorkItem;
import com.serotonin.m2m2.vo.User;
import com.serotonin.m2m2.vo.permission.PermissionHolder;
import com.serotonin.m2m2.vo.permission.Permissions;

import freemarker.template.TemplateException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtBuilder;

/**
 * Service to verify email addresses belong to a user
 *
 * @author Terry Packer
 *
 */
@Service
public class EmailAddressVerificationService extends JwtSignerVerifier<String> {

    public static final String PUBLIC_KEY_SYSTEM_SETTING = "jwt.emailAddressVerification.publicKey";
    public static final String PRIVATE_KEY_SYSTEM_SETTING = "jwt.emailAddressVerification.privateKey";
    public static final String EXPIRY_SYSTEM_SETTING = "jwt.emailAddressVerification.expiry";

    public static final String EMAIL_VERIFICATION_PAGE_TOKEN_PARAMETER = "emailAddressVerificationToken";

    public static final int DEFAULT_EXPIRY_DURATION = 15 * 60; // 15 minutes

    public static final String TOKEN_TYPE_VALUE = "emailverify";
    public static final String USER_ID_CLAIM = "id";

    private final PermissionHolder systemSuperadmin;
    private final UsersService usersService;
    private final PublicUrlService publicUrlService;
    private final SystemSettingsDao systemSettings;

    @Autowired
    public EmailAddressVerificationService(
            @Qualifier(MangoRuntimeContextConfiguration.SYSTEM_SUPERADMIN_PERMISSION_HOLDER)
            PermissionHolder systemSuperadmin,
            UsersService usersService,
            PublicUrlService publicUrlService,
            SystemSettingsDao systemSettings) {
        this.systemSuperadmin = systemSuperadmin;
        this.usersService = usersService;
        this.publicUrlService = publicUrlService;
        this.systemSettings = systemSettings;
    }

    @Override
    protected String tokenType() {
        return TOKEN_TYPE_VALUE;
    }

    @Override
    protected String verifyClaims(Jws<Claims> token) {
        Claims claims = token.getBody();
        // subject is the email address
        return claims.getSubject();
    }

    @Override
    protected void saveKeyPair(KeyPair keyPair) {
        this.systemSettings.setValue(PUBLIC_KEY_SYSTEM_SETTING, keyToString(keyPair.getPublic()));
        this.systemSettings.setValue(PRIVATE_KEY_SYSTEM_SETTING, keyToString(keyPair.getPrivate()));
    }

    @Override
    protected KeyPair loadKeyPair() {
        String publicKeyStr = this.systemSettings.getValue(PUBLIC_KEY_SYSTEM_SETTING);
        String privateKeyStr = this.systemSettings.getValue(PRIVATE_KEY_SYSTEM_SETTING);

        if (publicKeyStr != null && !publicKeyStr.isEmpty() && privateKeyStr != null && !privateKeyStr.isEmpty()) {
            return keysToKeyPair(publicKeyStr, privateKeyStr);
        }
        return null;
    }

    /**
     * Reset the keys, invalidating existing tokens
     */
    public void resetKeys() {
        this.generateNewKeyPair();
    }

    /**
     * Verify an email address by sending the address a verification token which must then be submitted back to this service.
     *
     * @param emailAddress
     * @param userToUpdate Optional, may be null
     * @throws TemplateException
     * @throws IOException
     * @throws AddressException
     */
    public void sendVerificationEmail(String emailAddress, User userToUpdate, PermissionHolder permissionHolder) throws TemplateException, IOException, AddressException {
        try {
            String token = this.generateToken(emailAddress, userToUpdate, null, permissionHolder);
            this.doSendVerificationEmail(token, userToUpdate);
        } catch (EmailAddressInUseException e) {
            if (Permissions.hasAdminPermission(permissionHolder)) {
                // rethrow the exception and notify the administrator
                throw e;
            } else {
                // notify the existing user that someone tried to register/verify their email address
                this.doSendWarningEmail(e.getExistingUser());
            }
        }
    }

    /**
     * Warns a user that someone tried to register a new account with their email address.
     *
     * @param existingUser
     * @throws TemplateException
     * @throws IOException
     * @throws AddressException
     */
    protected void doSendWarningEmail(User existingUser) throws TemplateException, IOException, AddressException {
        Translations translations = existingUser.getTranslations();
        Map<String, Object> model = new HashMap<>();
        TranslatableMessage subject = new TranslatableMessage("ftl.emailVerification.warningSubject", this.systemSettings.getValue(SystemSettingsDao.INSTANCE_DESCRIPTION));
        MangoEmailContent content = new MangoEmailContent("emailVerificationWarning", model, translations, subject.translate(translations), Common.UTF8);
        EmailWorkItem.queueEmail(existingUser.getEmail(), content);
    }

    protected void doSendVerificationEmail(String token, User userToUpdate) throws TemplateException, IOException, AddressException {
        URI uri = null;
        try {
            uri = this.generateEmailVerificationUrl(token);
        } catch (Exception e) {
        }

        Translations translations = userToUpdate != null ? userToUpdate.getTranslations() : Common.getTranslations();

        Jws<Claims> parsed = this.parse(token);
        Claims claims = parsed.getBody();
        Date expiration = claims.getExpiration();

        Map<String, Object> model = new HashMap<>();
        model.put("verificationUri", uri != null ? uri : "");
        model.put("token", token);
        model.put("expiration", expiration);
        model.put("username", userToUpdate != null ? userToUpdate.getUsername() : null);

        TranslatableMessage subject = new TranslatableMessage("ftl.emailVerification.subject", this.systemSettings.getValue(SystemSettingsDao.INSTANCE_DESCRIPTION));
        MangoEmailContent content = new MangoEmailContent("emailVerification", model, translations, subject.translate(translations), Common.UTF8);

        EmailWorkItem.queueEmail(claims.getSubject(), content);
    }

    /**
     * Generate an email verification token
     * @param emailAddress
     * @param userToUpdate Optional, may be null
     * @param expirationDate Optional, may be null
     * @return
     * @throws IOException
     * @throws TemplateException
     * @throws AddressException
     */
    public String generateToken(String emailAddress, User userToUpdate, Date expirationDate, PermissionHolder permissionHolder) {
        if (userToUpdate == null) {
            this.ensurePublicRegistrationEnabled();
        }

        if (expirationDate == null) {
            int expiryDuration = this.systemSettings.getIntValue(EXPIRY_SYSTEM_SETTING);
            expirationDate = new Date(System.currentTimeMillis() + expiryDuration * 1000);
        }

        try {
            // see if a different user is already using this address
            User existingUser = this.usersService.getUserByEmail(emailAddress, this.systemSuperadmin);
            if (userToUpdate == null || existingUser.getId() != userToUpdate.getId()) {
                throw new EmailAddressInUseException(existingUser);
            }
        } catch(NotFoundException e) {
            // no existing user using this email address, proceed
        }

        JwtBuilder builder = this.newToken(emailAddress, expirationDate);
        if (userToUpdate != null) {
            this.usersService.ensureEditPermission(permissionHolder, userToUpdate);
            builder.claim(USER_ID_CLAIM, userToUpdate.getId());
        }

        return this.sign(builder);
    }

    /**
     * Generate the URI for email verification
     * @param token
     * @return
     * @throws UnknownHostException
     */
    public URI generateEmailVerificationUrl(String token) throws UnknownHostException {
        UriComponentsBuilder builder = this.publicUrlService.getUriComponentsBuilder();
        String verificationPage = DefaultPagesDefinition.getEmailVerificationUri();
        return builder.path(verificationPage).queryParam(EMAIL_VERIFICATION_PAGE_TOKEN_PARAMETER, token).build().toUri();
    }

    /**
     * Verify an email address token for an existing user. Updates the user's email address with the one that was verified.
     * @param tokenString
     * @return
     */
    public User verifyUserEmail(String tokenString) {
        Jws<Claims> token = this.parse(tokenString);
        String verifiedEmail = this.verify(token);

        int userId = this.verifyClaimType(token, USER_ID_CLAIM, Integer.class);

        User existing = this.usersService.get(userId, this.systemSuperadmin);

        User updated = existing.copy();
        updated.setEmail(verifiedEmail);
        updated.setEmailVerifiedTs(Common.timer.currentTimeMillis());

        // we could use existing user instead of system superadmin here, but if the admin generates the token we want the user to still
        // be able to change/verify their password from the link/token. The service checks if the user is allowed to edit themselves when
        // generating the token.
        return this.usersService.update(existing, updated, this.systemSuperadmin);
    }

    /**
     * Verify an email address token and create a new disabled user with the verified email address.
     * @param tokenString
     * @param newUser
     * @param permissionHolder
     * @return
     * @throws ValidationException
     */
    public User publicCreateNewUser(String tokenString, User newUser) throws ValidationException {
        this.ensurePublicRegistrationEnabled();

        Jws<Claims> token = this.parse(tokenString);
        String verifiedEmail = this.verify(token);

        this.verifyNoClaim(token, USER_ID_CLAIM);

        //Totally new user
        newUser.setEmail(verifiedEmail);
        newUser.setDisabled(true); //Ensure we are disabled
        newUser.setEmailVerifiedTs(Common.timer.currentTimeMillis());
        return this.usersService.insert(newUser, this.systemSuperadmin);
    }

    public void ensurePublicRegistrationEnabled() {
        if (!systemSettings.getBooleanValue(SystemSettingsDao.USERS_PUBLIC_REGISTRATION_ENABLED)) {
            // TODO PermissionException deal with null permissionHolder
            throw new TranslatableRuntimeException(new TranslatableMessage("users.publicRegistration.disabled"));
        }
    }

    public static class EmailAddressInUseException extends TranslatableRuntimeException {
        private static final long serialVersionUID = 1L;
        private final User existingUser;

        public EmailAddressInUseException(User existingUser) {
            super(new TranslatableMessage("users.emailAddressInUse", existingUser.getUsername()));
            this.existingUser = existingUser;
        }

        public User getExistingUser() {
            return existingUser;
        }
    }
}
