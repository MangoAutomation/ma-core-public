/**
 * Copyright (C) 2017 Infinite Automation Software. All rights reserved.
 */
package com.serotonin.m2m2.web.mvc.spring.components;

import java.net.URI;
import java.security.KeyPair;
import java.util.Date;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import com.infiniteautomation.mango.jwt.JwtSignerVerifier;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.dao.SystemSettingsDao;
import com.serotonin.m2m2.db.dao.UserDao;
import com.serotonin.m2m2.module.DefaultPagesDefinition;
import com.serotonin.m2m2.vo.User;
import com.serotonin.m2m2.vo.exception.NotFoundException;

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
    
    public static final String PASSWORD_RESET_PAGE_TOKEN_PARAMETER = "passwordReset";

    public static final int DEFAULT_EXPIRY_DURATION = 15 * 60; // 15 minutes
    
    public static final String TOKEN_TYPE_VALUE = "pwreset";
    public static final String USER_ID_CLAIM = "id";
    public static final String USER_PASSWORD_VERSION_CLAIM = "v";
    
    UriComponentsBuilder builder;
    
    @Autowired
    public PasswordResetService(UriComponentsBuilder builder) {
        this.builder = builder;
    }

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
        
        // this will be set to a real password version number in the future so we can blacklist old tokens
        Integer pwVersion = 1;
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
        long expiryDuration = SystemSettingsDao.getIntValue(EXPIRY_SYSTEM_SETTING, DEFAULT_EXPIRY_DURATION);
        Date expirationDate = new Date(System.currentTimeMillis() + expiryDuration * 1000);
        
        JwtBuilder builder = this.newToken(user.getUsername(), expirationDate)
            .claim(USER_ID_CLAIM, user.getId())
            .claim(USER_PASSWORD_VERSION_CLAIM, 1); // this will be set to a real password version number in the future so we can blacklist old tokens
        
        return this.sign(builder);
    }
    
    public User resetPassword(String token, String newPassword) {
        User user = this.verify(token);
        user.setPassword(Common.encrypt(newPassword));
        UserDao.instance.saveUser(user);
        return user;
    }
    
    public void sendEmail(User user) {
        String token = this.generateToken(user);
        
        String resetPage = DefaultPagesDefinition.getPasswordResetUri();
        URI uri = builder.path(resetPage).queryParam(PASSWORD_RESET_PAGE_TOKEN_PARAMETER, token).build().toUri();
    }
}
