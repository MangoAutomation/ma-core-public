/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */

package com.serotonin.m2m2.web.mvc.spring.security.oauth2;

import com.infiniteautomation.mango.util.EnvironmentPropertyMapper;

@FunctionalInterface
public interface RegistrationPropertyMapperFactory {
    EnvironmentPropertyMapper forRegistrationId(String id, String suffix);

    default EnvironmentPropertyMapper forRegistrationId(String id) {
        return forRegistrationId(id, "");
    }
}
