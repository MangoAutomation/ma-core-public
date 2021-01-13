/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.infiniteautomation.mango.spring.components;

import java.io.IOException;
import java.net.URI;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.mail.internet.AddressException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import com.infiniteautomation.mango.jwt.JwtSignerVerifier;
import com.infiniteautomation.mango.spring.service.PermissionService;
import com.infiniteautomation.mango.spring.service.UsersService;
import com.infiniteautomation.mango.util.exception.FeatureDisabledException;
import com.infiniteautomation.mango.util.exception.NotFoundException;
import com.infiniteautomation.mango.util.exception.TranslatableRuntimeException;
import com.infiniteautomation.mango.util.exception.ValidationException;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.dao.SystemSettingsDao;
import com.serotonin.m2m2.email.MangoEmailContent;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.i18n.Translations;
import com.serotonin.m2m2.module.DefaultPagesDefinition;
import com.serotonin.m2m2.rt.event.type.SystemEventType;
import com.serotonin.m2m2.rt.maint.work.EmailWorkItem;
import com.serotonin.m2m2.vo.User;
import com.serotonin.m2m2.vo.permission.PermissionException;
import com.serotonin.m2m2.vo.permission.PermissionHolder;

import freemarker.template.TemplateException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtBuilder;

/**
 * Service to verify email addresses belong to a user
 *
 * @author Terry Packer
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
    public static final String USERNAME_CLAIM = "u";

    private final UsersService usersService;
    private final PublicUrlService publicUrlService;
    private final SystemSettingsDao systemSettings;
    private final PermissionService permissionService;
    private final RunAs runAs;

    @Autowired
    public EmailAddressVerificationService(
            UsersService usersService,
            PublicUrlService publicUrlService,
            SystemSettingsDao systemSettings,
            PermissionService permissionService, RunAs runAs) {
        this.usersService = usersService;
        this.publicUrlService = publicUrlService;
        this.systemSettings = systemSettings;
        this.permissionService = permissionService;
        this.runAs = runAs;
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
        PermissionHolder user = Common.getUser();
        if (!permissionService.hasAdminRole(user)) {
            throw new PermissionException(new TranslatableMessage("permission.exception.mustBeAdmin"), user);
        }
        this.generateNewKeyPair();
    }

    /**
     * Verify an email address by sending the address a verification token which must then be submitted back to this service.
     *
     * @param emailAddress
     * @param userToUpdate   Optional, may be null
     * @param expirationDate Optional, may be null
     * @return The generated token
     * @throws TemplateException
     * @throws IOException
     * @throws AddressException
     */
    public String sendVerificationEmail(String emailAddress, User userToUpdate, Date expirationDate) throws TemplateException, IOException, AddressException {
        try {
            String token = this.generateToken(emailAddress, userToUpdate, expirationDate);
            this.doSendVerificationEmail(token, userToUpdate);
            return token;
        } catch (EmailAddressInUseException e) {
            if (permissionService.hasAdminRole(Common.getUser())) {
                // rethrow the exception and notify the administrator
                throw e;
            } else {
                // notify the existing user that someone tried to register/verify their email address
                this.doSendWarningEmail(e.getExistingUser());
            }
        }

        return null;
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
        MangoEmailContent content = new MangoEmailContent("emailVerificationWarning", model, translations, subject.translate(translations), StandardCharsets.UTF_8);
        EmailWorkItem.queueEmail(existingUser.getEmail(), content);
    }

    protected void doSendVerificationEmail(String token, User userToUpdate) throws TemplateException, IOException, AddressException {
        URI uri = null;
        try {
            uri = this.generateEmailVerificationUrl(token);
        } catch (Exception e) {
            // dont care, continue without URI
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
        MangoEmailContent content = new MangoEmailContent("emailVerification", model, translations, subject.translate(translations), StandardCharsets.UTF_8);

        EmailWorkItem.queueEmail(claims.getSubject(), content);
    }

    /**
     * Generate an email verification token
     *
     * @param emailAddress
     * @param userToUpdate   Optional, may be null
     * @param expirationDate Optional, may be null
     * @return
     */
    public String generateToken(String emailAddress, User userToUpdate, Date expirationDate) {
        if (userToUpdate == null) {
            this.ensurePublicRegistrationEnabled();
        }

        long verificationTime = System.currentTimeMillis();

        if (expirationDate == null) {
            int expiryDuration = this.systemSettings.getIntValue(EXPIRY_SYSTEM_SETTING);
            expirationDate = new Date(verificationTime + expiryDuration * 1000L);
        }

        runAs.runAs(PermissionHolder.SYSTEM_SUPERADMIN, () -> {
            try {
                // see if a different user is already using this address
                User existingUser = this.usersService.getUserByEmail(emailAddress);
                if (userToUpdate == null || existingUser.getId() != userToUpdate.getId()) {
                    throw new EmailAddressInUseException(existingUser);
                }
            } catch (NotFoundException e) {
                // no existing user using this email address, proceed
            }
        });

        JwtBuilder builder = this.newToken(emailAddress, expirationDate);
        builder.setIssuedAt(new Date(verificationTime));
        if (userToUpdate != null) {
            this.usersService.ensureEditPermission(Common.getUser(), userToUpdate);
            builder.claim(USER_ID_CLAIM, userToUpdate.getId());
            builder.claim(USERNAME_CLAIM, userToUpdate.getUsername());
        }

        return this.sign(builder);
    }

    /**
     * Generate the URI for email verification
     *
     * @param token
     * @return
     * @throws UnknownHostException
     */
    public URI generateEmailVerificationUrl(String token) throws UnknownHostException {
        UriComponentsBuilder builder = this.publicUrlService.getUriComponentsBuilder();
        String verificationPage = DefaultPagesDefinition.getEmailVerificationUri();
        return builder.path(verificationPage).queryParam(EMAIL_VERIFICATION_PAGE_TOKEN_PARAMETER, token).build().toUri();
    }

    public URI generateRelativeEmailVerificationUrl(String token) {
        String verificationPage = DefaultPagesDefinition.getEmailVerificationUri();
        return UriComponentsBuilder.fromPath(verificationPage).queryParam(EMAIL_VERIFICATION_PAGE_TOKEN_PARAMETER, token).build().toUri();
    }

    /**
     * Verify an email address token for an existing user. Updates the user's email address with the one that was verified.
     *
     * @param tokenString
     * @return
     */
    public User updateUserEmailAddress(String tokenString) {
        Jws<Claims> token = this.parse(tokenString);
        String verifiedEmail = this.verify(token);
        // we could use existing user instead of system superadmin here, but if the admin generates the token we want the user to still
        // be able to change/verify their password from the link/token. The service checks if the user is allowed to edit themselves when
        // generating the token.
        return runAs.runAs(PermissionHolder.SYSTEM_SUPERADMIN, () -> {
            int userId = this.verifyClaimType(token, USER_ID_CLAIM, Number.class).intValue();
            User existing = this.usersService.get(userId);
            this.verifyClaim(token, USERNAME_CLAIM, existing.getUsername());

            User updated = (User) existing.copy();
            updated.setEmail(verifiedEmail);
            updated.setEmailVerified(token.getBody().getIssuedAt());
            return this.usersService.update(existing, updated);
        });
    }

    /**
     * Verify an email address token and create a new disabled user with the verified email address.
     *
     * @param tokenString
     * @param newUser
     * @return
     * @throws ValidationException
     */
    public User publicRegisterNewUser(String tokenString, final User newUser) throws ValidationException {
        this.ensurePublicRegistrationEnabled();

        Jws<Claims> token = this.parse(tokenString);
        String verifiedEmail = this.verify(token);

        this.verifyNoClaim(token, USER_ID_CLAIM);
        this.verifyNoClaim(token, USERNAME_CLAIM);

        //Totally new user
        newUser.setRoles(Collections.emptySet());
        newUser.setEmail(verifiedEmail);
        newUser.setDisabled(true); //Ensure we are disabled
        newUser.setEmailVerified(new Date(Common.timer.currentTimeMillis()));

        return runAs.runAs(PermissionHolder.SYSTEM_SUPERADMIN, () -> {
            User created = this.usersService.insert(newUser);

            //Raise an event upon successful insertion
            SystemEventType eventType = new SystemEventType(SystemEventType.TYPE_NEW_USER_REGISTERED);
            TranslatableMessage message = new TranslatableMessage("event.newUserRegistered", created.getUsername(), created.getEmail());
            SystemEventType.raiseEvent(eventType, Common.timer.currentTimeMillis(), false, message);
            return created;
        });
    }

    private void ensurePublicRegistrationEnabled() {
        if (!systemSettings.getBooleanValue(SystemSettingsDao.USERS_PUBLIC_REGISTRATION_ENABLED)) {
            throw new FeatureDisabledException(new TranslatableMessage("users.publicRegistration.disabled"));
        }
    }

    public static final class EmailAddressInUseException extends TranslatableRuntimeException {
        private static final long serialVersionUID = 1L;
        private final User existingUser;

        private EmailAddressInUseException(User existingUser) {
            super(new TranslatableMessage("users.emailAddressInUse", existingUser.getUsername()));
            this.existingUser = existingUser;
        }

        public User getExistingUser() {
            return existingUser;
        }
    }
}
