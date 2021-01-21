/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */

package com.serotonin.m2m2.web.mvc.spring.security.oauth2;

import java.util.Map;

import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

import com.serotonin.m2m2.vo.User;

public class MangoOidcUser extends MangoOAuth2User implements OidcUser {

    private final OidcUser oidcUserDelegate;

    public MangoOidcUser(OidcUser oidcUserDelegate, User user) {
        super(oidcUserDelegate, user);
        this.oidcUserDelegate = oidcUserDelegate;
    }

    @Override
    public Map<String, Object> getClaims() {
        return oidcUserDelegate.getClaims();
    }

    @Override
    public OidcUserInfo getUserInfo() {
        return oidcUserDelegate.getUserInfo();
    }

    @Override
    public OidcIdToken getIdToken() {
        return oidcUserDelegate.getIdToken();
    }
}
