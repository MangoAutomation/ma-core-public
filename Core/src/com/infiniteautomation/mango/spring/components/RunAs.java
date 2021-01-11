/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */

package com.infiniteautomation.mango.spring.components;

import java.util.concurrent.Callable;
import java.util.function.Supplier;

import com.serotonin.m2m2.vo.permission.PermissionHolder;

public interface RunAs {
    void runAs(PermissionHolder user, Runnable command);

    <T> T runAs(PermissionHolder user, Supplier<T> command);

    <T> T runAsCallable(PermissionHolder user, Callable<T> command) throws Exception;

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
    <T> T runAsProxy(PermissionHolder user, T instance);
}
