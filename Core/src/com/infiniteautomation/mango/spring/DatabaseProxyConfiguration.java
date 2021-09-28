/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */

package com.infiniteautomation.mango.spring;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import com.serotonin.m2m2.db.DatabaseProxy;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class DatabaseProxyConfiguration {

    private final Environment env;
    private final ClassLoader classLoader;
    private final List<DatabaseProxyListener> listeners;

    @Autowired
    public DatabaseProxyConfiguration(Environment env, ApplicationContext applicationContext, List<DatabaseProxyListener> listeners) {
        this(env, applicationContext.getClassLoader(), listeners);
    }

    public DatabaseProxyConfiguration(Environment env, ClassLoader classLoader, List<DatabaseProxyListener> listeners) {
        this.env = env;
        this.classLoader = classLoader;
        this.listeners = listeners;
    }

    public Environment getEnv() {
        return env;
    }

    public ClassLoader getClassLoader() {
        return classLoader;
    }

    public List<DatabaseProxyListener> getListeners() {
        return listeners;
    }

    @FunctionalInterface
    public interface DatabaseProxyListener {
        void onInitialize(DatabaseProxy databaseProxy);

        default void onTerminate(DatabaseProxy databaseProxy) {
            // noop
        }
    }
}
