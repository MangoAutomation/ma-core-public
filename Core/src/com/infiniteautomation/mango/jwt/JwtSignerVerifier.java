/**
 * Copyright (C) 2017 Infinite Automation Software. All rights reserved.
 */
package com.infiniteautomation.mango.jwt;

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

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.security.crypto.codec.Base64;

import com.serotonin.ShouldNeverHappenException;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.impl.crypto.EllipticCurveProvider;

/**
 * @author Jared Wiltshire
 */
public abstract class JwtSignerVerifier {
    private KeyPair keyPair;
    private JwtParser parser;
    
    protected JwtSignerVerifier() {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
        
        this.parser = Jwts.parser();
        
        this.keyPair = this.loadKeyPair();
        if (this.keyPair == null) {
            this.generateNewKeyPair();
        } else {
            this.parser.setSigningKey(this.keyPair.getPublic());
        }
    }
    
    protected final void generateNewKeyPair() {
        this.keyPair = EllipticCurveProvider.generateKeyPair(SignatureAlgorithm.ES512);
        this.parser.setSigningKey(this.keyPair.getPublic());
        this.saveKeyPair(this.keyPair);
    }

    protected abstract void saveKeyPair(KeyPair keyPair);
    protected abstract KeyPair loadKeyPair();

    protected final String sign(JwtBuilder builder) {
        return builder
            .signWith(SignatureAlgorithm.ES512, keyPair.getPrivate())
            .compact();
    }
    
    public final Jws<Claims> parse(String token) {
        return parser.parseClaimsJws(token);
    }

    public String getPublicKey() {
        return keyToString(keyPair.getPublic());
    }
    
    public boolean isSignedJwt(String token) {
        return parser.isSigned(token);
    }

    public static KeyPair keysToKeyPair(String publicKeyStr, String privateKeyStr) {
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
    
    public static String keyToString(Key key) {
        byte[] publicBase64 = Base64.encode(key.getEncoded());
        return new String(publicBase64, StandardCharsets.ISO_8859_1);
    }
}
