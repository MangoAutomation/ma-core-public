/*
    Copyright (C) 2006-2007 Serotonin Software Technologies Inc.
 	@author Matthew Lohbihler
 */
package com.serotonin.util;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import java.security.Security;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Base64;

import com.serotonin.ShouldNeverHappenException;

/**
 * @author Matthew Lohbihler
 */
public class StringEncrypter {
    private static final String DEFAULT_ALGORITHM = "AES";
    private static final Charset ASCII = Charset.forName("ASCII");
    private static final Charset UTF8 = Charset.forName("UTF-8");

    private static final String ALGO_MD5 = "MD5";
    private static final String ALGO_SHA = "SHA";
    private static final String ALGO_SHA256 = "SHA-256";
    private static final String ALGO_SHA384 = "SHA-384";
    private static final String ALGO_SHA512 = "SHA-512";

    private final String algorithm;
    private SecretKey keySpec;
    private Cipher cipher;
    private final Base64 encoder = new Base64();

    public StringEncrypter() {
        algorithm = DEFAULT_ALGORITHM;
        // Use this for Base64 encoding only.
    }

    public StringEncrypter(String base64EncryptionKey) {
        this(base64EncryptionKey, true, DEFAULT_ALGORITHM);
    }

    public StringEncrypter(String base64EncryptionKey, String algorithm) {
        this(base64EncryptionKey, true, algorithm);
    }

    public StringEncrypter(String encryptionKey, boolean base64Encoded) {
        this(encryptionKey, base64Encoded, DEFAULT_ALGORITHM);
    }

    public StringEncrypter(String encryptionKey, boolean base64Encoded, String algorithm) {
        this.algorithm = algorithm;
        byte[] bytes = encryptionKey.getBytes(UTF8);
        if (base64Encoded)
            bytes = encoder.decode(bytes);
        createCipher(bytes);
    }

    public StringEncrypter(byte[] encryptionKey) {
        this(encryptionKey, DEFAULT_ALGORITHM);
    }

    public StringEncrypter(byte[] encryptionKey, String algorithm) {
        this.algorithm = algorithm;
        createCipher(encryptionKey);
    }

    private void createCipher(byte[] encryptionKey) {
        try {
            keySpec = new SecretKeySpec(encryptionKey, algorithm);
            cipher = Cipher.getInstance(algorithm);
        }
        catch (NoSuchAlgorithmException e) {
            throw new EncryptionException(e);
        }
        catch (NoSuchPaddingException e) {
            throw new EncryptionException(e);
        }
    }

    public synchronized String encrypt(String unencryptedString) {
        return encryptBytes(unencryptedString.getBytes(UTF8));
    }

    public synchronized String encryptBytes(byte[] unencryptedData) {
        try {
            cipher.init(Cipher.ENCRYPT_MODE, keySpec);
            byte[] encrypted = cipher.doFinal(unencryptedData);
            return new String(encoder.encode(encrypted));
        }
        catch (InvalidKeyException e) {
            throw new EncryptionException(e);
        }
        catch (IllegalBlockSizeException e) {
            throw new EncryptionException(e);
        }
        catch (BadPaddingException e) {
            throw new EncryptionException(e);
        }
    }

    public synchronized String decrypt(String encryptedString) {
        return new String(decryptBytes(encryptedString));
    }

    public synchronized byte[] decryptBytes(String encryptedString) {
        try {
            byte[] encrypted = encoder.decode(encryptedString.getBytes(ASCII));
            cipher.init(Cipher.DECRYPT_MODE, keySpec);
            return cipher.doFinal(encrypted);
        }
        catch (InvalidKeyException e) {
            throw new EncryptionException(e);
        }
        catch (IllegalBlockSizeException e) {
            throw new EncryptionException(e);
        }
        catch (BadPaddingException e) {
            throw new EncryptionException(e);
        }
    }

    public static class EncryptionException extends RuntimeException {
        private static final long serialVersionUID = -1;

        public EncryptionException(Throwable t) {
            super(t);
        }
    }

    public String encodeString(String s) {
        return encodeBytes(s.getBytes(UTF8));
    }

    public String encodeBytes(byte[] bytes) {
        return new String(encoder.encode(bytes), ASCII);
    }

    public String decodeToString(String s) {
        return new String(decodeToBytes(s), UTF8);
    }

    public byte[] decodeToBytes(String s) {
        return encoder.decode(s.getBytes(ASCII));
    }

    public static String generateKey() {
        return generateKey(DEFAULT_ALGORITHM);
    }

    public static String generateKey(String algorithm) {
        try {
            KeyGenerator kgen = KeyGenerator.getInstance(algorithm);
            kgen.init(128);
            SecretKey skey = kgen.generateKey();
            byte[] raw = skey.getEncoded();
            return new String(new Base64().encode(raw));
        }
        catch (NoSuchAlgorithmException e) {
            throw new EncryptionException(e);
        }
    }

    public synchronized static String hashSHAToBase64(String plaintext) {
        return hashEncryptToBase64(plaintext, ALGO_SHA);
    }

    public synchronized static String hashSHA256ToBase64(String plaintext) {
        return hashEncryptToBase64(plaintext, ALGO_SHA256);
    }

    public synchronized static String hashSHA384ToBase64(String plaintext) {
        return hashEncryptToBase64(plaintext, ALGO_SHA384);
    }

    public synchronized static String hashSHA512ToBase64(String plaintext) {
        return hashEncryptToBase64(plaintext, ALGO_SHA512);
    }

    public synchronized static String hashMD5ToBase64(String plaintext) {
        return hashEncryptToBase64(plaintext, ALGO_MD5);
    }

    public synchronized static String hashEncryptToBase64(String plaintext, String algorithm) {
        byte[] raw = hashEncrypt(plaintext, algorithm);
        String hash = new String(Base64.encodeBase64(raw));
        return hash;
    }

    public synchronized static byte[] hashSHA(String plaintext) {
        return hashEncrypt(plaintext, ALGO_SHA);
    }

    public synchronized static byte[] hashSHA256(String plaintext) {
        return hashEncrypt(plaintext, ALGO_SHA256);
    }

    public synchronized static byte[] hashSHA384(String plaintext) {
        return hashEncrypt(plaintext, ALGO_SHA384);
    }

    public synchronized static byte[] hashSHA512(String plaintext) {
        return hashEncrypt(plaintext, ALGO_SHA512);
    }

    public synchronized static byte[] hashMD5(byte[] bytes) {
        return hashEncrypt(bytes, ALGO_MD5);
    }

    public synchronized static byte[] hashMD5(String plaintext) {
        return hashEncrypt(plaintext, ALGO_MD5);
    }

    public synchronized static byte[] hashEncrypt(String plaintext, String algorithm) {
        try {
            return hashEncrypt(plaintext.getBytes("UTF-8"), algorithm);
        }
        catch (UnsupportedEncodingException e) {
            // Should never happen, so just wrap in a runtime exception and rethrow
            throw new ShouldNeverHappenException(e);
        }
    }

    public synchronized static byte[] hashEncrypt(byte[] bytes, String algorithm) {
        try {
            MessageDigest md = MessageDigest.getInstance(algorithm);
            md.update(bytes);
            return md.digest();
        }
        catch (NoSuchAlgorithmException e) {
            // Should never happen, so just wrap in a runtime exception and rethrow
            throw new ShouldNeverHappenException(e);
        }
    }

    public static void main(String[] args) throws Exception {
        // String key = "MyFr1ends0Fea,Inc!";
        // // String key = generateKey();
        // // System.out.println(key);
        //
        // String s = "The string to encrypt";
        // StringEncrypter se = new StringEncrypter(key, false);
        // String enc = se.encrypt(s);
        // System.out.println("Encrypted string: " + enc);
        //
        // String dec = se.decrypt(enc);
        // System.out.println("Decrypted string: " + dec);

        //        String s = "Oh Canadian cable, where art thou?";
        //
        //        MessageDigest md = MessageDigest.getInstance("MD5");
        //        md.update(s.getBytes("UTF-8"));
        //        byte raw[] = md.digest();
        //        System.out.println(StreamUtils.dumpHex(raw));
        //        String hash = new String(Base64.encodeBase64(raw));
        //        System.out.println(hash);

        for (Provider p : Security.getProviders()) {
            System.out.println(p.getServices());
        }
    }
}
