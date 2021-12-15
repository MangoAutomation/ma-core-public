/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */

package com.infiniteautomation.mango.spring.components;

import static com.infiniteautomation.mango.spring.MangoRuntimeContextConfiguration.SYSTEM_SUPERADMIN_PERMISSION_HOLDER;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Supplier;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.concurrent.DelegatingSecurityContextCallable;
import org.springframework.security.concurrent.DelegatingSecurityContextExecutorService;
import org.springframework.security.concurrent.DelegatingSecurityContextRunnable;
import org.springframework.security.concurrent.DelegatingSecurityContextScheduledExecutorService;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.stereotype.Component;

import com.infiniteautomation.mango.spring.service.PermissionService;
import com.serotonin.m2m2.vo.permission.PermissionHolder;

/**
 * Based on {@link DelegatingSecurityContextRunnable} and {@link DelegatingSecurityContextCallable}.
 */
@Component
public class RunAsImpl implements RunAs {

    private final PermissionService permissionService;
    private final PermissionHolder superadmin;

    @Autowired
    public RunAsImpl(PermissionService permissionService,
                     @Qualifier(SYSTEM_SUPERADMIN_PERMISSION_HOLDER) PermissionHolder superadmin) {
        this.permissionService = permissionService;
        this.superadmin = superadmin;
    }

    @Override
    public void runAs(PermissionHolder user, Runnable command) {
        SecurityContext original = getOriginalContext();
        try {
            SecurityContextHolder.setContext(newSecurityContext(user));
            command.run();
        } finally {
            restoreSecurityContext(original);
        }
    }

    @Override
    public <T> T runAs(PermissionHolder user, Supplier<T> command) {
        SecurityContext original = getOriginalContext();
        try {
            SecurityContextHolder.setContext(newSecurityContext(user));
            return command.get();
        } finally {
            restoreSecurityContext(original);
        }
    }

    @Override
    public <T> T runAsCallable(PermissionHolder user, Callable<T> command) throws Exception {
        SecurityContext original = getOriginalContext();
        try {
            SecurityContextHolder.setContext(newSecurityContext(user));
            return command.call();
        } finally {
            restoreSecurityContext(original);
        }
    }

    @Override
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

    @Override
    public ExecutorService executorService(PermissionHolder user, ExecutorService executorService) {
        return new DelegatingSecurityContextExecutorService(executorService, newSecurityContext(user));
    }

    @Override
    public ScheduledExecutorService scheduledExecutorService(PermissionHolder user, ScheduledExecutorService executorService) {
        return new DelegatingSecurityContextScheduledExecutorService(executorService, newSecurityContext(user));
    }

    @Override
    public PermissionHolder systemSuperadmin() {
        return superadmin;
    }

    /**
     * Creates a new security context for the supplied user
     *
     */
    private SecurityContext newSecurityContext(PermissionHolder user) {
        SecurityContext newContext = SecurityContextHolder.createEmptyContext();
        newContext.setAuthentication(new PreAuthenticatedAuthenticationToken(user, null));
        return newContext;
    }

    private SecurityContext getOriginalContext() {
        return SecurityContextHolder.getContext();
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
