/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */

package com.infiniteautomation.mango.spring;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class DatabaseProxyConfiguration {

    private final Environment env;
    private final ClassLoader classLoader;

    @Autowired
    public DatabaseProxyConfiguration(Environment env, ApplicationContext applicationContext) {
        this(env, applicationContext.getClassLoader());
    }

    public DatabaseProxyConfiguration(Environment env, ClassLoader classLoader) {
        this.env = env;
        this.classLoader = classLoader;
    }

    public Environment getEnv() {
        return env;
    }

    public ClassLoader getClassLoader() {
        return classLoader;
    }
}
