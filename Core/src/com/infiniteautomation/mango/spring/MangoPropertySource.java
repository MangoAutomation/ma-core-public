/*
 * Copyright (C) 2018 Infinite Automation Software. All rights reserved.
 */
package com.infiniteautomation.mango.spring;

import org.springframework.core.env.PropertySource;

import com.serotonin.util.properties.MangoProperties;

/**
 * @author Jared Wiltshire
 */
public class MangoPropertySource extends PropertySource<MangoProperties> {

    public MangoPropertySource(String name, MangoProperties source) {
        super(name, source);
    }

    @Override
    public Object getProperty(String name) {
        return this.source.getString(name);
    }

}
