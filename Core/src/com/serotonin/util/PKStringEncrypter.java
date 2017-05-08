/*
    Copyright (C) 2006-2007 Serotonin Software Technologies Inc.
 	@author Matthew Lohbihler
 */
package com.serotonin.util;

import java.io.FileOutputStream;
import java.nio.charset.Charset;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.KeySpec;
import java.security.spec.X509EncodedKeySpec;

import javax.crypto.Cipher;

import org.apache.commons.codec.binary.Base64;

import com.serotonin.io.StreamUtils;

/**
 * @author Matthew Lohbihler
 */
public class PKStringEncrypter {
    // public static void main(String[] args) throws Exception {
    // InputStream inStream = new FileInputStream("EApubcert.dat");
    // CertificateFactory cf = CertificateFactory.getInstance("X.509");
    // X509Certificate cert = (X509Certificate) cf.generateCertificate(inStream);
    // inStream.close();
    //        
    //
    // cert.checkValidity();
    // System.out.println(cert);
    // System.out.println(cert.getIssuerAlternativeNames());
    // System.out.println(cert.getSigAlgName());
    // System.out.println(cert.getType());
    // System.out.println(cert.getVersion());
    // System.out.println(cert.getIssuerDN());
    //
    // Cipher cipher = Cipher.getInstance("RSA");
    // cipher.init(Cipher.ENCRYPT_MODE, cert.getPublicKey());
    // System.out.println("alg: " + cipher.getAlgorithm());
    // System.out.println(cipher.getIV());
    //
    // // KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
    // // generator.initialize(2048);
    // // KeyPair keyPair = generator.generateKeyPair();
    // //
    // // System.out.println(keyPair.getPrivate().getFormat());
    // // byte[] encodedPrivate = keyPair.getPrivate().getEncoded();
    // // Base64 encoder = new Base64();
    // // System.out.println(new String(encoder.encode(encodedPrivate)));
    // //
    //
    // // encrypt("1111222233334444", cipher, encoder);
    // // encrypt("4520987654321098", cipher, encoder);
    // // encrypt("6474050599905534", cipher, encoder);
    // // encrypt("1021647405059999", cipher, encoder);
    //
    // FileOutputStream out = new FileOutputStream("cc.raw");
    // encrypt("1111222233334444", cipher, out);
    // encrypt("4520987654321098", cipher, out);
    // encrypt("6474050599905534", cipher, out);
    // encrypt("1021647405059999", cipher, out);
    // out.close();
    // }

    public static void encrypt(String s, Cipher cipher, Base64 encoder) throws Exception {
        byte[] bs = s.getBytes(Charset.forName("UTF-8"));
        byte[] benc = cipher.doFinal(bs);
        System.out.println(benc.length);
        System.out.println(new String(encoder.encode(benc)));
    }

    public static void encrypt(String s, Cipher cipher, FileOutputStream out) throws Exception {
        byte[] bs = s.getBytes(Charset.forName("UTF-8"));
        byte[] benc = cipher.doFinal(bs);
        out.write(benc);
        out.write(new byte[] { (byte) '\r', (byte) '\n' });
    }

    public static void main(String[] args) throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        // generator.initialize(2048);
        generator.initialize(512);
        KeyPair keyPair = generator.generateKeyPair();
        System.out.println(keyPair.getPublic());
        System.out.println(keyPair.getPrivate());

        // System.out.println(keyPair.getPublic().getFormat());
        byte[] encodedPublic = keyPair.getPublic().getEncoded();
        System.out.println("public: " + StreamUtils.dumpHex(encodedPublic));

        KeySpec keySpec = new X509EncodedKeySpec(encodedPublic);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        RSAPublicKey publicKey = (RSAPublicKey) keyFactory.generatePublic(keySpec);
        System.out.println("mod: " + publicKey.getModulus().toString(16));
        System.out.println("exp: " + publicKey.getPublicExponent().toString(16));
        // System.out.println(publicKey);

        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);

        String s = "Oh Canadian cable, where art thou?";
        byte[] bs = s.getBytes(Charset.forName("UTF-8"));
        byte[] benc = cipher.doFinal(bs);
        System.out.println("encrypted: " + StreamUtils.dumpHex(benc));

        cipher.init(Cipher.DECRYPT_MODE, keyPair.getPrivate());
        byte[] bdec = cipher.doFinal(benc);

        String d = new String(bdec, Charset.forName("UTF-8"));

        System.out.println(d);

    }
}
