/*
 * Copyright (C) 2022 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.timer;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.test.annotation.Timed;

import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.MangoTestBase;
import com.serotonin.m2m2.util.timeout.TimeoutClient;
import com.serotonin.m2m2.util.timeout.TimeoutTask;
import com.serotonin.m2m2.vo.permission.PermissionException;
import com.serotonin.m2m2.vo.permission.PermissionHolder;

/**
 * @author Mert Cingöz
 */
public class TimerTaskPermissionHolderTest extends MangoTestBase {

    @After
    public void restoreAdmin() {
        // To be able to clean up test data
        switchTo(PermissionHolder.SYSTEM_SUPERADMIN);
    }

    @Test
    public void testSwitchUser() {
        Assert.assertEquals(PermissionHolder.SYSTEM_SUPERADMIN, Common.getUser());

        // Create task with admin
        TimeoutTask task = new TimeoutTask(0, new TimeoutClient() {
            @Override
            public void scheduleTimeout(long fireTime) {
                Assert.assertEquals(PermissionHolder.SYSTEM_SUPERADMIN, Common.getUser());
            }

            @Override
            public String getThreadName() {
                return "Switch user task";
            }
        });

        var newUser = createUsers(1, PermissionHolder.USER_ROLE).get(0);
        switchTo(newUser);
        Assert.assertEquals(newUser, Common.getUser());

        // Run task new user
        task.runTask(0);
    }

    @Test
    public void testLogout() {
        Assert.assertEquals(PermissionHolder.SYSTEM_SUPERADMIN, Common.getUser());

        // Create task with admin
        TimeoutTask task = new TimeoutTask(0, new TimeoutClient() {
            @Override
            public void scheduleTimeout(long fireTime) {
                Assert.assertEquals(PermissionHolder.SYSTEM_SUPERADMIN, Common.getUser());
            }

            @Override
            public String getThreadName() {
                return "Logout task";
            }
        });

        switchTo(PermissionHolder.ANONYMOUS);
        Assert.assertEquals(PermissionHolder.ANONYMOUS, Common.getUser());

        // Run task with anonymous
        task.runTask(0);
    }

    @Test(timeout = 1000 * 5, expected = PermissionException.class)
    public void timeoutTestNoContext() {
        Common.getUser();
    }

    @Test
    @Timed(millis = 1000 * 5)
    public void timedTestHasContext() {
        Assert.assertEquals(PermissionHolder.SYSTEM_SUPERADMIN, Common.getUser());
    }

    private void switchTo(PermissionHolder permissionHolder) {
        SecurityContext context = SecurityContextHolder.getContext();
        context.setAuthentication(new PreAuthenticatedAuthenticationToken(permissionHolder, null));
    }
}
