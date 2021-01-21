/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */

package com.serotonin.m2m2.web.mvc.spring.security.oauth2;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

import com.serotonin.m2m2.vo.User;
import com.serotonin.m2m2.vo.permission.PermissionHolder;
import com.serotonin.m2m2.vo.role.Role;

public class MangoOAuth2User implements OAuth2User, PermissionHolder {

    private final OAuth2User delegate;
    private final User user;

    public MangoOAuth2User(OAuth2User delegate, User user) {
        this.delegate = delegate;
        this.user = user;
    }

    @Override
    public User getUser() {
        return user;
    }

    @Override
    public String getPermissionHolderName() {
        return user.getUsername();
    }

    @Override
    public boolean isPermissionHolderDisabled() {
        return user.isDisabled();
    }

    @Override
    public Set<Role> getRoles() {
        return user.getRoles();
    }

    @Override
    public Map<String, Object> getAttributes() {
        return delegate.getAttributes();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return user.getAuthorities();
        // the delegate authorities contain scopes
//            return delegate.getAuthorities();
    }

    @Override
    public String getName() {
        return user.getUsername();
    }
}
