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

import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.MangoTestBase;
import com.serotonin.m2m2.util.timeout.TimeoutClient;
import com.serotonin.m2m2.util.timeout.TimeoutTask;
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
        Assert.assertEquals(PermissionHolder.SYSTEM_SUPERADMIN, Common.getPermissionHolder());

        // Create task with admin
        TimeoutTask task = new TimeoutTask(0, new TimeoutClient() {
            @Override
            public void scheduleTimeout(long fireTime) {
                Assert.assertEquals(PermissionHolder.SYSTEM_SUPERADMIN, Common.getPermissionHolder());
            }

            @Override
            public String getThreadName() {
                return "Switch user task";
            }
        });

        switchTo(PermissionHolder.ANONYMOUS);
        Assert.assertEquals(PermissionHolder.ANONYMOUS, Common.getPermissionHolder());

        // Run task with anonymous
        task.runTask(0);
    }

    @Test
    public void testLogout() {
        Assert.assertEquals(PermissionHolder.SYSTEM_SUPERADMIN, Common.getPermissionHolder());

        // Create task with admin
        TimeoutTask task = new TimeoutTask(0, new TimeoutClient() {
            @Override
            public void scheduleTimeout(long fireTime) {
                Assert.assertEquals(PermissionHolder.SYSTEM_SUPERADMIN, Common.getPermissionHolder());
            }

            @Override
            public String getThreadName() {
                return "Logout task";
            }
        });

        switchTo(null);
        Assert.assertNull(Common.getPermissionHolder());

        // Run task
        task.runTask(0);
    }

    private void switchTo(PermissionHolder permissionHolder) {
        SecurityContext context = SecurityContextHolder.getContext();
        context.setAuthentication(new PreAuthenticatedAuthenticationToken(permissionHolder, null));
    }
}
