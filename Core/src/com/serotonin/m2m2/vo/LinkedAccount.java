/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */

package com.serotonin.m2m2.vo;

/**
 * Represents a linked OAuth2 / OpenID Connect account. May in the future be used for other authentication mechanisms.
 */
public class LinkedAccount {
    String issuer;
    String subject;

    public LinkedAccount() {
    }

    public LinkedAccount(String issuer, String subject) {
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
}
