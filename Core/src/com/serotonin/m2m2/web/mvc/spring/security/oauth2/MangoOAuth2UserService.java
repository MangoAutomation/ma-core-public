/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */

package com.serotonin.m2m2.web.mvc.spring.security.oauth2;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;

import com.infiniteautomation.mango.spring.ConditionalOnProperty;
import com.infiniteautomation.mango.spring.components.RunAs;
import com.serotonin.m2m2.vo.User;

@Component
@ConditionalOnProperty("${authentication.oauth2.enabled}")
public class MangoOAuth2UserService extends DefaultOAuth2UserService {

    private final RunAs runAs;
    private final UserMapper mapper;

    @Autowired
    public MangoOAuth2UserService(UserMapper mapper, RunAs runAs) {
        this.runAs = runAs;
        this.mapper = mapper;
    }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User delegate = super.loadUser(userRequest);

        try {
            return runAs.runAs(runAs.systemSuperadmin(), () -> {
                User user = mapper.mapUser(userRequest, delegate);
                return new MangoOAuth2User(delegate, user);
            });
        } catch (Exception e) {
            throw new UserSyncAuthenticationException("Syncing authentication to Mango User failed", e);
        }
    }

}
