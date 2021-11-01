/*
 * Copyright (C) 2021 RadixIot LLC. All rights reserved.
 */

package com.infiniteautomation.mango.spring.service;

import org.junit.BeforeClass;

public class UserServiceNoCacheTest extends UsersServiceTest {

    @BeforeClass
    public static void setupProperties() {
        properties.setProperty("cache.users.enabled", "false");
    }

}
