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
import com.infiniteautomation.mango.util.exception.ValidationException;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.dao.SystemSettingsDao;
import com.serotonin.m2m2.email.MangoEmailContent;
import com.serotonin.m2m2.i18n.ProcessResult;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.i18n.Translations;
import com.serotonin.m2m2.module.DefaultPagesDefinition;
import com.serotonin.m2m2.rt.maint.work.EmailWorkItem;
import com.serotonin.m2m2.vo.User;
import com.serotonin.m2m2.vo.permission.PermissionHolder;

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
public class EmailAddressVerificationService extends JwtSignerVerifier<User> {

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

    @Autowired
    public EmailAddressVerificationService(
            @Qualifier(MangoRuntimeContextConfiguration.SYSTEM_SUPERADMIN_PERMISSION_HOLDER)
            PermissionHolder systemSuperadmin,
            UsersService usersService,
            PublicUrlService publicUrlService) {
        this.systemSuperadmin = systemSuperadmin;
        this.usersService = usersService;
        this.publicUrlService = publicUrlService;
    }

    @Override
    protected String tokenType() {
        return TOKEN_TYPE_VALUE;
    }

    @Override
    protected User verifyClaims(Jws<Claims> token) {
        Claims claims = token.getBody();

        User user = null;
        String emailAddress = claims.getSubject();
        //This could be for a new uncreated user or an existing one, let's check
        try {
            user = this.usersService.getUserByEmail(emailAddress, this.systemSuperadmin);
            this.verifyClaim(token, USER_ID_CLAIM, user.getId());
        } catch(NotFoundException e) {
            this.verifyNoClaim(token, USER_ID_CLAIM);
        }

        return user;
    }

    @Override
    protected void saveKeyPair(KeyPair keyPair) {
        SystemSettingsDao.instance.setValue(PUBLIC_KEY_SYSTEM_SETTING, keyToString(keyPair.getPublic()));
        SystemSettingsDao.instance.setValue(PRIVATE_KEY_SYSTEM_SETTING, keyToString(keyPair.getPrivate()));
    }

    @Override
    protected KeyPair loadKeyPair() {
        String publicKeyStr = SystemSettingsDao.instance.getValue(PUBLIC_KEY_SYSTEM_SETTING);
        String privateKeyStr = SystemSettingsDao.instance.getValue(PRIVATE_KEY_SYSTEM_SETTING);

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
     * Verify
     * @param userToVerify
     * @param expirationDate
     * @throws TemplateException
     * @throws IOException
     * @throws AddressException
     */
    public void sendEmail(User userToVerify, Date expirationDate) throws TemplateException, IOException, AddressException {
        String token = this.generateToken(userToVerify.getEmail(), userToVerify, expirationDate);
        sendEmail(userToVerify.getEmail(), token, expirationDate, userToVerify.getTranslations());
    }

    /**
     * Send an email with an email verification token to verify email addresses
     *  this will fail if the email address is already used
     * @param emailAddress
     * @param expirationDate
     * @throws TemplateException
     * @throws IOException
     * @throws AddressException
     */
    public void sendEmail(String emailAddress, Date expirationDate) throws TemplateException, IOException, AddressException {

        try {
            //See if the user exists
            User existingUser = this.usersService.getUserByEmail(emailAddress, this.systemSuperadmin);
            //Warn this user
            Map<String, Object> model = new HashMap<>();
            TranslatableMessage subject = new TranslatableMessage("ftl.emailVerification.warningSubject", SystemSettingsDao.instance.getValue(SystemSettingsDao.INSTANCE_DESCRIPTION));
            MangoEmailContent content = new MangoEmailContent("emailVerificationWarning", model, existingUser.getTranslations(), subject.translate(existingUser.getTranslations()), Common.UTF8);
            EmailWorkItem.queueEmail(emailAddress, content);
        }catch(NotFoundException e) {
            //All good, we don't want this user to already exist
            String token = this.generateToken(emailAddress, null, expirationDate);
            sendEmail(emailAddress, token, expirationDate, Common.getTranslations());
        }
    }

    protected void sendEmail(String emailAddress, String token, Date expirationDate, Translations translations) throws TemplateException, IOException, AddressException {
        URI uri = null;
        try {
            uri = this.generateEmailVerificationUrl(token);
        } catch (Exception e) {
            throw new IOException(e);
        }

        Jws<Claims> parsed = this.parse(token);
        Date expiration = parsed.getBody().getExpiration();

        Map<String, Object> model = new HashMap<>();
        model.put("verificationUri", uri != null ? uri : "");
        model.put("token", token);
        model.put("expiration", expiration);

        TranslatableMessage subject = new TranslatableMessage("ftl.emailVerification.subject", SystemSettingsDao.instance.getValue(SystemSettingsDao.INSTANCE_DESCRIPTION));
        MangoEmailContent content = new MangoEmailContent("emailVerification", model, translations, subject.translate(translations), Common.UTF8);

        EmailWorkItem.queueEmail(emailAddress, content);
    }

    /**
     * Generate an email verification token with optional expiry
     * @param emailAddress
     * @param user
     * @param expirationDate
     * @return
     */
    public String generateToken(String emailAddress, User user, Date expirationDate) {
        if (expirationDate == null) {
            int expiryDuration = SystemSettingsDao.instance.getIntValue(EXPIRY_SYSTEM_SETTING);
            expirationDate = new Date(System.currentTimeMillis() + expiryDuration * 1000);
        }

        JwtBuilder builder = this.newToken(emailAddress, expirationDate);
        if(user != null) {
            builder.claim(USER_ID_CLAIM, user.getId());
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
     * Verify an existing user's email
     * @param tokenString
     * @return
     */
    public User verifyEmail(String tokenString) {
        Jws<Claims> token = this.parse(tokenString);
        User existing = this.verify(token);
        if (existing == null) {
            throw new NotFoundException();
        }
        User updated = existing.copy();
        updated.setEmailVerifiedTs(Common.timer.currentTimeMillis());
        this.usersService.update(existing, updated, existing);
        return updated;
    }

    /**
     * Verify an email address to create a new disabled user, this token
     *  can only be for a new non-existing user
     * @param tokenString
     * @param newUser
     * @return
     * @throws ValidationException
     */
    public User verifyEmail(String tokenString, User newUser) throws ValidationException {
        Jws<Claims> token = this.parse(tokenString);
        User user = this.verify(token);
        if(user != null) {
            ProcessResult result = new ProcessResult();
            result.addContextualMessage("token", "validate.invalidValue");
            throw new ValidationException(result);
        }else {
            //Totally new user
            newUser.setEmail(token.getBody().getSubject());
            newUser.setDisabled(true); //Ensure we are disabled
            newUser.setEmailVerifiedTs(Common.timer.currentTimeMillis());
            this.usersService.insert(newUser, this.systemSuperadmin);
            return newUser;
        }
    }

}
