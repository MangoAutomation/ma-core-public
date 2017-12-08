/**
 * Copyright (C) 2017 Infinite Automation Software. All rights reserved.
 */
package com.serotonin.m2m2.web.mvc.spring.components;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Security;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Date;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.security.crypto.codec.Base64;
import org.springframework.stereotype.Service;

import com.serotonin.ShouldNeverHappenException;
import com.serotonin.m2m2.db.dao.SystemSettingsDao;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.impl.crypto.EllipticCurveProvider;

/**
 * @author Jared Wiltshire
 */
@Service
public class JwtService {
    public static final String PUBLIC_KEY_SYSTEM_SETTING = "jwt.publicKey";
    public static final String PRIVATE_KEY_SYSTEM_SETTING = "jwt.privateKey";
    
    private KeyPair keyPair;
    
    public JwtService() {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
        
        loadKeyPair();
    }
    
    private void generateNewKeyPair() {
        keyPair = EllipticCurveProvider.generateKeyPair(SignatureAlgorithm.ES512);
    }
    
    private static KeyPair readKeyPair(String publicKeyStr, String privateKeyStr) {
        byte[] publicBase64 = Base64.decode(publicKeyStr.getBytes(StandardCharsets.ISO_8859_1));
        byte[] privateBase64 = Base64.decode(privateKeyStr.getBytes(StandardCharsets.ISO_8859_1));

        try {
            KeyFactory kf = KeyFactory.getInstance("ECDSA", BouncyCastleProvider.PROVIDER_NAME);
            PublicKey publicKey = kf.generatePublic(new X509EncodedKeySpec(publicBase64));
            PrivateKey privateKey = kf.generatePrivate(new PKCS8EncodedKeySpec(privateBase64));
            return new KeyPair(publicKey, privateKey);
        } catch (NoSuchAlgorithmException | NoSuchProviderException | InvalidKeySpecException e) {
            throw new ShouldNeverHappenException(e);
        }
    }
    
    private static String writeKey(Key key) {
        byte[] publicBase64 = Base64.encode(key.getEncoded());
        return new String(publicBase64, StandardCharsets.ISO_8859_1);
    }

    private void saveKeyPair() {
    	SystemSettingsDao.instance.setValue(PUBLIC_KEY_SYSTEM_SETTING, writeKey(keyPair.getPublic()));
    	SystemSettingsDao.instance.setValue(PRIVATE_KEY_SYSTEM_SETTING, writeKey(keyPair.getPrivate()));
    }
    
    private void loadKeyPair() {
        String publicKeyStr = SystemSettingsDao.getValue(PUBLIC_KEY_SYSTEM_SETTING);
        String privateKeyStr = SystemSettingsDao.getValue(PRIVATE_KEY_SYSTEM_SETTING);
        
        if (publicKeyStr == null || publicKeyStr.isEmpty() || privateKeyStr == null || privateKeyStr.isEmpty()) {
            generateNewKeyPair();
            saveKeyPair();
        } else {
            keyPair = readKeyPair(publicKeyStr, privateKeyStr);
        }
    }
    
    public String generateToken(String username, Date expiry) {
        return Jwts.builder()
            .setSubject(username)
            .setExpiration(expiry)
            .signWith(SignatureAlgorithm.ES512, keyPair.getPrivate()).compact();
    }
    
    public Jws<Claims> parseToken(String token) {
        return Jwts.parser().setSigningKey(keyPair.getPublic()).parseClaimsJws(token);
    }
    
    public boolean isSignedJwt(String token) {
        return Jwts.parser().isSigned(token);
    }

    public String getPublicKey() {
        return writeKey(keyPair.getPublic());
    }
}
