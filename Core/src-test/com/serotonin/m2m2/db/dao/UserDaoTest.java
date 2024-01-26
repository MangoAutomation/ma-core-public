/*
 * Copyright (C) 2024 Radix IoT LLC. All rights reserved.
 */

package com.serotonin.m2m2.db.dao;


import com.serotonin.m2m2.MangoTestBase;
import com.serotonin.m2m2.vo.User;
import org.jooq.DSLContext;
import org.junit.Test;
import org.springframework.context.ApplicationContext;

import static com.infiniteautomation.mango.db.tables.Users.USERS;
import static org.junit.Assert.assertEquals;

/**
 * @author Jared Wiltshire
 */
public class UserDaoTest extends MangoTestBase {

    private UserDao userDao;
    private DSLContext context;

    @Override
    public void before() {
        super.before();

        ApplicationContext context = MangoTestBase.lifecycle.getRuntimeContext();
        this.userDao = context.getBean(UserDao.class);
        this.context = context.getBean(DSLContext.class);
    }

    @Test
    public void tokenVersionIsPreserved() {
        User user = new User();
        user.setUsername("testuser");
        user.setPassword("testuser");

        userDao.insert(user);
        assertEquals((Object) getTokenVersion(user), 1);

        userDao.revokeTokens(user);
        assertEquals((Object) getTokenVersion(user), 2);

        // any token version manually set on a user should be ignored
        user.setTokenVersion(1);
        userDao.update(user.getId(), user);
        assertEquals((Object) getTokenVersion(user), 2);
    }

    private Integer getTokenVersion(User user) {
        return context.select(USERS.tokenVersion)
                .from(USERS)
                .where(USERS.id.eq(user.getId()))
                .fetchSingle(USERS.tokenVersion);
    }
}