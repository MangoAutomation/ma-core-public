/**
 * Copyright (C) 2017 Infinite Automation Software. All rights reserved.
 */
package com.serotonin.m2m2.web.mvc.spring.components;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.security.KeyPair;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.mail.internet.AddressException;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import com.infiniteautomation.mango.jwt.JwtSignerVerifier;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.dao.SystemSettingsDao;
import com.serotonin.m2m2.db.dao.UserDao;
import com.serotonin.m2m2.email.MangoEmailContent;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.i18n.Translations;
import com.serotonin.m2m2.module.DefaultPagesDefinition;
import com.serotonin.m2m2.rt.maint.work.EmailWorkItem;
import com.serotonin.m2m2.vo.User;
import com.serotonin.m2m2.vo.exception.NotFoundException;

import freemarker.template.TemplateException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtBuilder;

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

    @Override
    protected String tokenType() {
        return TOKEN_TYPE_VALUE;
    }

    @Override
    protected User verifyClaims(Jws<Claims> token) {
        Claims claims = token.getBody();
        
        String username = claims.getSubject();
        User user = UserDao.instance.getUser(username);
        if (user == null) {
            throw new NotFoundException();
        }
        
        Integer userId = user.getId();
        this.verifyClaim(token, USER_ID_CLAIM, userId);

        Integer pwVersion = user.getPasswordVersion();
        this.verifyClaim(token, USER_PASSWORD_VERSION_CLAIM, pwVersion);
        
        return user;
    }

    @Override
    protected void saveKeyPair(KeyPair keyPair) {
    	SystemSettingsDao.instance.setValue(PUBLIC_KEY_SYSTEM_SETTING, keyToString(keyPair.getPublic()));
    	SystemSettingsDao.instance.setValue(PRIVATE_KEY_SYSTEM_SETTING, keyToString(keyPair.getPrivate()));
    }
    
    @Override
    protected KeyPair loadKeyPair() {
        String publicKeyStr = SystemSettingsDao.getValue(PUBLIC_KEY_SYSTEM_SETTING);
        String privateKeyStr = SystemSettingsDao.getValue(PRIVATE_KEY_SYSTEM_SETTING);
        
        if (publicKeyStr != null && !publicKeyStr.isEmpty() && privateKeyStr != null && !privateKeyStr.isEmpty()) {
            return keysToKeyPair(publicKeyStr, privateKeyStr);
        }
        return null;
    }
    
    public void resetKeys() {
        this.generateNewKeyPair();
    }
    
    public String generateToken(User user) {
        return this.generateToken(user, null);
    }
    
    public String generateToken(User user, Date expirationDate) {
        if (expirationDate == null) {
            int expiryDuration = SystemSettingsDao.getIntValue(EXPIRY_SYSTEM_SETTING, DEFAULT_EXPIRY_DURATION);
            expirationDate = new Date(System.currentTimeMillis() + expiryDuration * 1000);
        }
        
        JwtBuilder builder = this.newToken(user.getUsername(), expirationDate)
            .claim(USER_ID_CLAIM, user.getId())
            .claim(USER_PASSWORD_VERSION_CLAIM, user.getPasswordVersion());
        
        return this.sign(builder);
    }
    
    public URI generateResetUrl(String token) throws UnknownHostException {
        UriComponentsBuilder builder;
        String baseUrl = SystemSettingsDao.getValue(SystemSettingsDao.PUBLICLY_RESOLVABLE_BASE_URL);
        if (!StringUtils.isEmpty(baseUrl)) {
            builder = UriComponentsBuilder.fromPath(baseUrl);
        } else {
            boolean sslOn = Common.envProps.getBoolean("ssl.on", false);
            int port = sslOn ? Common.envProps.getInt("ssl.port", 443) : Common.envProps.getInt("web.port", 8080);
            
            builder = UriComponentsBuilder.newInstance()
                    .scheme(sslOn ? "https" : "http")
                    .host(InetAddress.getLocalHost().getHostName())
                    .port(port);
        }

        String resetPage = DefaultPagesDefinition.getPasswordResetUri();
        
        return builder.path(resetPage).queryParam(PASSWORD_RESET_PAGE_TOKEN_PARAMETER, token).build().toUri();
    }
    
    public URI generateRelativeResetUrl(String token) {
        String resetPage = DefaultPagesDefinition.getPasswordResetUri();
        return UriComponentsBuilder.fromPath(resetPage).queryParam(PASSWORD_RESET_PAGE_TOKEN_PARAMETER, token).build().toUri();
    }

    public User resetPassword(String token, String newPassword) {
        User user = this.verify(token);
        user.setPassword(Common.encrypt(newPassword));
        UserDao.instance.saveUser(user);
        return user;
    }
    
    public void sendEmail(User user) throws TemplateException, IOException, AddressException {
        String token = this.generateToken(user);
        this.sendEmail(user, token);
    }

    public void sendEmail(User user, String token) throws TemplateException, IOException, AddressException {
        URI uri = null;
        try {
            uri = this.generateResetUrl(token);
        } catch (Exception e) {
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
        MangoEmailContent content = new MangoEmailContent("passwordReset", model, translations, subject.translate(translations), Common.UTF8);
        
        EmailWorkItem.queueEmail(user.getEmail(), content);
    }
}
