/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.infiniteautomation.mango.spring.components;

import freemarker.template.TemplateException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtBuilder;
import java.io.IOException;
import java.net.URI;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import javax.mail.internet.AddressException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import com.infiniteautomation.mango.jwt.JwtSignerVerifier;
import com.infiniteautomation.mango.spring.components.pageresolver.PageResolver;
import com.infiniteautomation.mango.spring.service.PermissionService;
import com.infiniteautomation.mango.spring.service.UsersService;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.dao.SystemSettingsDao;
import com.serotonin.m2m2.email.MangoEmailContent;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.i18n.Translations;
import com.serotonin.m2m2.rt.maint.work.EmailWorkItem;
import com.serotonin.m2m2.vo.User;
import com.serotonin.m2m2.vo.permission.PermissionException;
import com.serotonin.m2m2.vo.permission.PermissionHolder;

/**
 * @author Jared Wiltshire
 */
@Service
public final class PasswordResetService extends JwtSignerVerifier<User> {
    public static final String PUBLIC_KEY_SYSTEM_SETTING = "jwt.passwordReset.publicKey";
    public static final String PRIVATE_KEY_SYSTEM_SETTING = "jwt.passwordReset.privateKey";
    public static final String EXPIRY_SYSTEM_SETTING = "jwt.passwordReset.expiry";

    public static final String PASSWORD_RESET_PAGE_TOKEN_PARAMETER = "resetToken";

    public static final int DEFAULT_EXPIRY_DURATION = 15 * 60; // 15 minutes

    public static final String TOKEN_TYPE_VALUE = "pwreset";
    public static final String USER_ID_CLAIM = "id";
    public static final String USER_PASSWORD_VERSION_CLAIM = "v";

    private final PermissionService permissionService;
    private final UsersService usersService;
    private final PublicUrlService publicUrlService;
    private final RunAs runAs;
    private final PageResolver pageResolver;
    private final SystemSettingsDao systemSettingsDao;

    @Autowired
    public PasswordResetService(
            PermissionService permissionService,
            UsersService usersService,
            PublicUrlService publicUrlService, RunAs runAs, PageResolver pageResolver, SystemSettingsDao systemSettingsDao) {
        this.permissionService = permissionService;
        this.usersService = usersService;
        this.publicUrlService = publicUrlService;
        this.runAs = runAs;
        this.pageResolver = pageResolver;
        this.systemSettingsDao = systemSettingsDao;
    }

    @Override
    protected String tokenType() {
        return TOKEN_TYPE_VALUE;
    }

    @Override
    protected User verifyClaims(Jws<Claims> token) {
        Claims claims = token.getBody();

        String username = claims.getSubject();
        User user = this.runAs.runAs(runAs.systemSuperadmin(), () -> this.usersService.get(username));
        Integer userId = user.getId();
        this.verifyClaim(token, USER_ID_CLAIM, userId);

        Integer pwVersion = user.getPasswordVersion();
        this.verifyClaim(token, USER_PASSWORD_VERSION_CLAIM, pwVersion);
        return user;
    }

    @Override
    protected void saveKeyPair(KeyPair keyPair) {
        systemSettingsDao.setValue(PUBLIC_KEY_SYSTEM_SETTING, keyToString(keyPair.getPublic()));
        systemSettingsDao.setValue(PRIVATE_KEY_SYSTEM_SETTING, keyToString(keyPair.getPrivate()));
    }

    @Override
    protected KeyPair loadKeyPair() {
        String publicKeyStr = systemSettingsDao.getValue(PUBLIC_KEY_SYSTEM_SETTING);
        String privateKeyStr = systemSettingsDao.getValue(PRIVATE_KEY_SYSTEM_SETTING);

        if (publicKeyStr != null && !publicKeyStr.isEmpty() && privateKeyStr != null && !privateKeyStr.isEmpty()) {
            return keysToKeyPair(publicKeyStr, privateKeyStr);
        }
        return null;
    }

    public void resetKeys() {
        PermissionHolder user = Common.getUser();
        if (!permissionService.hasAdminRole(user)) {
            throw new PermissionException(new TranslatableMessage("permission.exception.mustBeAdmin"), user);
        }
        this.generateNewKeyPair();
    }

    public String generateToken(String username, boolean lockPassword, boolean sendEmail) {
        return this.generateToken(username, null, lockPassword, sendEmail);
    }

    public String generateToken(String username, Date expirationDate, boolean lockPassword, boolean sendEmail) {
        PermissionHolder currentUser = Common.getUser();
        User user = usersService.get(username);
        usersService.ensureEditPermission(currentUser, user);

        if (lockPassword) {
            usersService.lockPassword(user.getUsername());
        }

        if (expirationDate == null) {
            int expiryDuration = systemSettingsDao.getIntValue(EXPIRY_SYSTEM_SETTING);
            expirationDate = new Date(System.currentTimeMillis() + expiryDuration * 1000L);
        }

        JwtBuilder builder = this.newToken(user.getUsername(), expirationDate)
                .claim(USER_ID_CLAIM, user.getId())
                .claim(USER_PASSWORD_VERSION_CLAIM, user.getPasswordVersion());

        String token = this.sign(builder);
        if (sendEmail) {
            sendEmail(user, token);
        }
        return token;
    }

    public URI generateResetUrl(String token) throws UnknownHostException {
        UriComponentsBuilder builder = this.publicUrlService.getUriComponentsBuilder();
        String resetPage = pageResolver.getPasswordResetUri();
        return builder.path(resetPage).queryParam(PASSWORD_RESET_PAGE_TOKEN_PARAMETER, token).build().toUri();
    }

    public URI generateRelativeResetUrl(String token) {
        String resetPage = pageResolver.getPasswordResetUri();
        return UriComponentsBuilder.fromPath(resetPage).queryParam(PASSWORD_RESET_PAGE_TOKEN_PARAMETER, token).build().toUri();
    }

    public User resetPassword(String token, String newPassword) {
        User existing = this.verify(token);
        // we copy the user so that when we set the new password it doesn't modify the cached instance
        User updated = (User) existing.copy();
        updated.setPlainTextPassword(newPassword);
        return runAs.runAs(runAs.systemSuperadmin(), () -> usersService.update(existing.getId(), updated));
    }

    public void sendEmail(String username, String email) {
        User user = runAs.runAs(runAs.systemSuperadmin(), () -> usersService.get(username));

        String providedEmail = email.toLowerCase(Locale.ROOT);
        String userEmail = user.getEmail().toLowerCase(Locale.ROOT);
        if (providedEmail.equals(userEmail) && !user.isDisabled()) {
            runAs.runAs(runAs.systemSuperadmin(), () -> generateToken(username, false, true));
        }
    }

    private void sendEmail(User user, String token) {
        URI uri = null;
        try {
            uri = this.generateResetUrl(token);
        } catch (Exception e) {
            // dont care
        }

        Translations translations = Translations.getTranslations(user.getLocaleObject());

        Jws<Claims> parsed = this.parse(token);
        Date expiration = parsed.getBody().getExpiration();

        Map<String, Object> model = new HashMap<>();
        model.put("username", user.getUsername());
        model.put("resetUri", uri != null ? uri : "");
        model.put("token", token);
        model.put("expiration", expiration);

        TranslatableMessage subject = new TranslatableMessage("ftl.passwordReset.subject", user.getUsername());

        try {
            MangoEmailContent content = new MangoEmailContent("passwordReset", model, translations, subject.translate(translations), StandardCharsets.UTF_8);
            EmailWorkItem.queueEmail(user.getEmail(), content);
        } catch (TemplateException | IOException | AddressException e) {
            log.error("Couldn't send password reset email", e);
        }
    }

    public User systemSetup(String password, Map<String, Object> settings) {
        PermissionHolder holder = Common.getUser();

        if (!permissionService.hasAdminRole(holder)) {
            throw new PermissionException(new TranslatableMessage("permission.exception.mustBeAdmin"), holder);
        }

        User user = holder.getUser();
        if (!Common.checkPassword("admin", user.getPassword())) {
            throw new BadCredentialsException(Common.translate("login.validation.invalidLogin"));
        }

        // don't want to change the passed in user in case it comes from the cache (in which case another thread might use it)
        User copy = usersService.get(user.getId());
        copy.setPlainTextPassword(password);
        usersService.ensureValid(copy);

        systemSettingsDao.updateSettings(settings);
        return usersService.update(user.getId(), copy);
    }
}
