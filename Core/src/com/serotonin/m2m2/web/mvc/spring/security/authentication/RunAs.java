/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */

package com.serotonin.m2m2.web.mvc.spring.security.authentication;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.util.concurrent.Callable;
import java.util.function.Supplier;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.concurrent.DelegatingSecurityContextRunnable;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.stereotype.Component;

import com.infiniteautomation.mango.spring.service.PermissionService;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.vo.permission.PermissionHolder;

/**
 * See also {@link DelegatingSecurityContextRunnable}
 */
@Component
public class RunAs {

    private final PermissionService permissionService;

    @Autowired
    public RunAs(PermissionService permissionService) {
        this.permissionService = permissionService;
    }

    public void runAs(PermissionHolder user, Runnable command) {
        SecurityContext original = getOriginalContext();
        try {
            SecurityContextHolder.setContext(newSecurityContext(user));
            command.run();
        } finally {
            restoreSecurityContext(original);
        }
    }

    public <T> T runAs(PermissionHolder user, Supplier<T> command) {
        SecurityContext original = getOriginalContext();
        try {
            SecurityContextHolder.setContext(newSecurityContext(user));
            return command.get();
        } finally {
            restoreSecurityContext(original);
        }
    }

    public <T> T runAsCallable(PermissionHolder user, Callable<T> command) throws Exception {
        SecurityContext original = getOriginalContext();
        try {
            SecurityContextHolder.setContext(newSecurityContext(user));
            return command.call();
        } finally {
            restoreSecurityContext(original);
        }
    }

    /**
     * Creates a proxy object for the supplied instance where every method invoked is run as the supplied user.
     * The returned proxy object will implement all interfaces of the supplied instance.
     *
     * @param <T> must be an interface
     * @param user
     * @param instance
     * @return
     */
    @SuppressWarnings("unchecked")
    public <T> T runAsProxy(PermissionHolder user, T instance) {
        Class<?> clazz = instance.getClass();
        SecurityContext runAsContext = newSecurityContext(user);

        return (T) Proxy.newProxyInstance(clazz.getClassLoader(), clazz.getInterfaces(), (proxy, method, args) -> {
            SecurityContext original = getOriginalContext();
            try {
                SecurityContextHolder.setContext(runAsContext);
                return method.invoke(instance, args);
            } catch (InvocationTargetException e) {
                throw e.getCause();
            } finally {
                restoreSecurityContext(original);
            }
        });
    }

    /**
     * Creates a new security context for the supplied user
     * @param user
     * @return
     */
    private SecurityContext newSecurityContext(PermissionHolder user) {
        SecurityContext newContext = SecurityContextHolder.createEmptyContext();
        newContext.setAuthentication(new PreAuthenticatedAuthenticationToken(user, null));
        return newContext;
    }

    private SecurityContext getOriginalContext() {
        SecurityContext original = SecurityContextHolder.getContext();
        if (original.getAuthentication() == null) {
            return original;
        }
        permissionService.ensureAdminRole(Common.getUser());
        return original;
    }

    private void restoreSecurityContext(SecurityContext original) {
        SecurityContext emptyContext = SecurityContextHolder.createEmptyContext();
        if (emptyContext.equals(original)) {
            SecurityContextHolder.clearContext();
        } else {
            SecurityContextHolder.setContext(original);
        }
    }
}
