/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */

package com.serotonin.m2m2.web.mvc.spring.security.oauth2;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Component;

import com.infiniteautomation.mango.spring.ConditionalOnProperty;
import com.infiniteautomation.mango.spring.components.RunAs;
import com.serotonin.m2m2.vo.User;

@Component
@ConditionalOnProperty("${authentication.oauth2.enabled}")
public class MangoOidcUserService extends OidcUserService {

    private final RunAs runAs;
    private final UserMapper mapper;

    @Autowired
    public MangoOidcUserService(UserMapper mapper, RunAs runAs) {
        this.runAs = runAs;
        this.mapper = mapper;
    }

    @Override
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        OidcUser delegate = super.loadUser(userRequest);

        try {
            return runAs.runAs(runAs.systemSuperadmin(), () -> {
                User user = mapper.mapUser(userRequest, delegate);
                return new MangoOidcUser(delegate, user);
            });
        } catch (Exception e) {
            throw new UserSyncAuthenticationException("Syncing authentication to Mango User failed", e);
        }
    }

}
