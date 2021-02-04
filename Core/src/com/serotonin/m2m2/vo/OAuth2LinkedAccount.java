/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */

package com.serotonin.m2m2.vo;

/**
 * Represents a linked OAuth2 / OpenID Connect account.
 */
public class OAuth2LinkedAccount implements LinkedAccount {
    public static final String OAUTH2_LINKED_ACCOUNT = "OAUTH2";

    String issuer;
    String subject;

    public OAuth2LinkedAccount() {
    }

    public OAuth2LinkedAccount(String issuer, String subject) {
        this.issuer = issuer;
        this.subject = subject;
    }

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    @Override
    public String getType() {
        return OAUTH2_LINKED_ACCOUNT;
    }
}
