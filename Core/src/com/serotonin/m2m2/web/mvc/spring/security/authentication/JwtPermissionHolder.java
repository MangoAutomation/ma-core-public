/*
 * Copyright (C) 2024 Radix IoT LLC. All rights reserved.
 */

package com.serotonin.m2m2.web.mvc.spring.security.authentication;

import java.util.Collections;
import java.util.Set;

import com.serotonin.m2m2.vo.permission.PermissionHolder;
import com.serotonin.m2m2.vo.role.Role;

/**
 * @author Jared Wiltshire
 */
public class JwtPermissionHolder implements PermissionHolder {
  private final String permissionHolderName;
  private final Set<Role> roles;


  public JwtPermissionHolder(String permissionHolderName, Set<Role> roles) {
    this.permissionHolderName = permissionHolderName;
    this.roles = Collections.unmodifiableSet(roles);
  }

  @Override
  public String getPermissionHolderName() {
    return permissionHolderName;
  }

  @Override
  public boolean isPermissionHolderDisabled() {
    return false;
  }

  @Override
  public Set<Role> getRoles() {
    return roles;
  }
}
