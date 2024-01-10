/*
 * Copyright (C) 2024 Radix IoT LLC. All rights reserved.
 */

package com.serotonin.m2m2.db.dao;


import static com.infiniteautomation.mango.db.tables.Users.USERS;
import static org.assertj.core.api.Assertions.assertThat;

import org.jooq.DSLContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.serotonin.m2m2.MangoTest;
import com.serotonin.m2m2.vo.User;

/**
 * @author Jared Wiltshire
 */
class UserDaoTest extends MangoTest {

    private UserDao userDao;
    private DSLContext context;

    @BeforeEach
    void setUp() {
        this.userDao = getBean(UserDao.class);
        this.context = getBean(DSLContext.class);
    }

    @Test
    void tokenVersionIsPreserved() {
        User user = new User();
        user.setUsername("testuser");
        user.setPassword("testuser");

        userDao.insert(user);
        assertThat(getTokenVersion(user)).isEqualTo(1);

        userDao.revokeTokens(user);
        assertThat(getTokenVersion(user)).isEqualTo(2);

        // any token version manually set on a user should be ignored
        user.setTokenVersion(1);
        userDao.update(user.getId(), user);
        assertThat(getTokenVersion(user)).isEqualTo(2);
    }

    private Integer getTokenVersion(User user) {
        return context.select(USERS.tokenVersion)
                .from(USERS)
                .where(USERS.id.eq(user.getId()))
                .fetchSingle(USERS.tokenVersion);
    }

}
