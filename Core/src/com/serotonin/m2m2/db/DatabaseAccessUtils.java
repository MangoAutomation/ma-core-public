package com.serotonin.m2m2.db;

import com.serotonin.util.StringEncrypter;

public class DatabaseAccessUtils {
    private static final String KEY = "TR58yrqPswXJubYGiRdARw==";

    public String decrypt(String input) {
        int colon = input.indexOf(":");
        if (colon == -1)
            return input;

        String alg = input.substring(0, colon);
        String value = input.substring(colon + 1);

        StringEncrypter se = new StringEncrypter(KEY, alg);
        return se.decodeToString(value);
    }
    //    
    //    public static void main(String[] args) {
    //        StringEncrypter se = new StringEncrypter(KEY, "Blowfish");
    //        System.out.println(se.encodeString("mango2"));
    //    }
}
