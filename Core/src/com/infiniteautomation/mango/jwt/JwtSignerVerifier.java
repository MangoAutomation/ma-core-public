/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
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
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Base64.Decoder;
import java.util.Date;

import javax.annotation.PostConstruct;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.serotonin.ShouldNeverHappenException;

import io.jsonwebtoken.ClaimJwtException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.IncorrectClaimException;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwsHeader;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MissingClaimException;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

/**
 * @author Jared Wiltshire
 */
public abstract class JwtSignerVerifier<T> {
    public static final String TOKEN_TYPE_CLAIM = "typ";

    public static final String INCORRECT_TYPE_EXPECTED_CLAIM_MESSAGE_TEMPLATE = "Expected %s claim to be of type: %s, but was: %s.";
    public static final String MISSING_TYPE_EXPECTED_CLAIM_MESSAGE_TEMPLATE = "Expected %s claim to be of type: %s, but was not present.";

    private KeyPair keyPair;
    private JwtParser parser;

    protected final Logger log;

    protected JwtSignerVerifier() {
        this.log = LoggerFactory.getLogger(this.getClass());
    }

    @PostConstruct
    private synchronized void postConstruct() {
        KeyPair keyPair = this.loadKeyPair();
        if (keyPair == null) {
            this.generateNewKeyPair();
        } else {
            this.parser = Jwts.parserBuilder()
                    .require(TOKEN_TYPE_CLAIM, this.tokenType())
                    .setSigningKey(keyPair.getPublic())
                    .build();
            this.keyPair = keyPair;
        }
    }

    protected synchronized final void generateNewKeyPair() {
        KeyPair keyPair = Keys.keyPairFor(SignatureAlgorithm.ES512);
        this.parser = Jwts.parserBuilder()
                .require(TOKEN_TYPE_CLAIM, this.tokenType())
                .setSigningKey(keyPair.getPublic())
                .build();
        this.keyPair = keyPair;
        this.saveKeyPair(this.keyPair);
    }

    protected abstract String tokenType();
    protected abstract T verifyClaims(Jws<Claims> token);
    protected abstract void saveKeyPair(KeyPair keyPair);
    protected abstract KeyPair loadKeyPair();

    protected final JwtBuilder newToken(String subject, Date expiration) {
        return Jwts.builder()
                .setSubject(subject)
                .setExpiration(expiration);
    }

    protected final String sign(JwtBuilder builder) {
        String token = builder.claim(TOKEN_TYPE_CLAIM, this.tokenType())
                .signWith(SignatureAlgorithm.ES512, keyPair.getPrivate())
                .compact();

        if (log.isDebugEnabled()) {
            log.debug("Created JWT token: " + printToken(token));
        }

        return token;
    }

    /**
     * Parses the token and verifies it's signature and expiration. Does NOT verify any other claims!
     */
    public final Jws<Claims> parse(String token) {
        return parser.parseClaimsJws(token);
    }

    /**
     * Parses the token and verifies it's signature, expiration and claims.
     */
    public final T verify(String token) {
        return this.verify(this.parse(token));
    }

    /**
     * Verify a parsed token's claims (NOT expiration).
     */
    public final T verify(Jws<Claims> token) {
        return this.verifyClaims(token);
    }

    /**
     * Throws IncorrectClaimException if token contains a claim with the specified name
     *
     */
    protected void verifyNoClaim(Jws<Claims> token, String claimName) {
        JwsHeader<?> header = token.getHeader();
        Claims claims = token.getBody();

        Object actualClaimValue = claims.get(claimName);
        if (actualClaimValue != null) {
            String msg = String.format(
                    ClaimJwtException.INCORRECT_EXPECTED_CLAIM_MESSAGE_TEMPLATE,
                    claimName, null, actualClaimValue);
            throw new IncorrectClaimException(header, claims, msg);
        }
    }

    @SuppressWarnings("unchecked")
    protected <V> V verifyClaimType(Jws<Claims> token, String claimName, Class<V> claimType) {
        JwsHeader<?> header = token.getHeader();
        Claims claims = token.getBody();

        Object value = claims.get(claimName);
        if (value == null) {
            throw new MissingClaimException(header, claims, String.format(MISSING_TYPE_EXPECTED_CLAIM_MESSAGE_TEMPLATE, claimName, claimType));
        }
        if (!claimType.isAssignableFrom(value.getClass())) {
            throw new IncorrectClaimException(header, claims, String.format(INCORRECT_TYPE_EXPECTED_CLAIM_MESSAGE_TEMPLATE, claimName, claimType, value.getClass()));
        }
        return (V) value;
    }

    /**
     * Verifies that the token contains the specified claim and that it matches the expected value.
     *
     */
    protected void verifyClaim(Jws<Claims> token, String expectedClaimName, Object expectedClaimValue) {
        JwsHeader<?> header = token.getHeader();
        Claims claims = token.getBody();

        Object actualClaimValue = claims.get(expectedClaimName);
        if (actualClaimValue == null) {
            String msg = String.format(
                    ClaimJwtException.MISSING_EXPECTED_CLAIM_MESSAGE_TEMPLATE,
                    expectedClaimName, expectedClaimValue);
            throw new MissingClaimException(header, claims, msg);
        } else if (!expectedClaimValue.equals(actualClaimValue)) {
            String msg = String.format(
                    ClaimJwtException.INCORRECT_EXPECTED_CLAIM_MESSAGE_TEMPLATE,
                    expectedClaimName, expectedClaimValue, actualClaimValue);
            throw new IncorrectClaimException(header, claims, msg);
        }
    }

    public String getPublicKey() {
        return keyToString(keyPair.getPublic());
    }

    public boolean isSignedJwt(String token) {
        return parser.isSigned(token);
    }

    public static KeyPair keysToKeyPair(String publicKeyStr, String privateKeyStr) {
        Decoder base64Decoder = Base64.getDecoder();

        byte[] publicBase64 = base64Decoder.decode(publicKeyStr);
        byte[] privateBase64 = base64Decoder.decode(privateKeyStr);

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
        return Base64.getEncoder().encodeToString(key.getEncoded());
    }

    public static String printToken(String token) {
        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            return token;
        }

        Decoder base64Decoder = Base64.getDecoder();

        byte[] headerBytes = base64Decoder.decode(parts[0]);
        String header = new String(headerBytes, StandardCharsets.UTF_8);

        byte[] bodyBytes = base64Decoder.decode(parts[1]);
        String body = new String(bodyBytes, StandardCharsets.UTF_8);

        return String.format("{header: %s, body: %s}", header, body);
    }
}
