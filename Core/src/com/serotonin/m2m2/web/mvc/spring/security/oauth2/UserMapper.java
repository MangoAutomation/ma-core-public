/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */

package com.serotonin.m2m2.web.mvc.spring.security.oauth2;

import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.user.OAuth2User;

import com.serotonin.m2m2.vo.User;

public interface UserMapper {
    User mapUser(OAuth2UserRequest userRequest, OAuth2User oAuth2User);
}
