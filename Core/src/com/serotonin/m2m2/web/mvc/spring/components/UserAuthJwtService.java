/**
 * Copyright (C) 2017 Infinite Automation Software. All rights reserved.
 */
package com.serotonin.m2m2.web.mvc.spring.components;

import java.security.KeyPair;
import java.util.Date;

import org.springframework.stereotype.Service;

import com.infiniteautomation.mango.jwt.JwtSignerVerifier;
import com.serotonin.m2m2.db.dao.SystemSettingsDao;
import com.serotonin.m2m2.vo.User;

import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;

/**
 * @author Jared Wiltshire
 */
@Service
public final class UserAuthJwtService extends JwtSignerVerifier {
    public static final String PUBLIC_KEY_SYSTEM_SETTING = "jwt.userAuth.publicKey";
    public static final String PRIVATE_KEY_SYSTEM_SETTING = "jwt.userAuth.privateKey";
    
    public static final String USER_ID_CLAIM = "uId";
    public static final String USER_VERSION_CLAIM = "v";

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
    
    public void invalidateTokens() {
        this.generateNewKeyPair();
    }
    
    public String generateToken(User user, Date expiry) {
        JwtBuilder builder = Jwts.builder()
                .setSubject(user.getUsername())
                .setExpiration(expiry)
                .claim(USER_ID_CLAIM, user.getId())
                .claim(USER_VERSION_CLAIM, 1); // this will be set to a real user version number in the future so we can blacklist old tokens
        return this.sign(builder);
    }
}
