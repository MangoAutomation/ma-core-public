/*
 * Copyright (C) 2021 RadixIot LLC. All rights reserved.
 */

package com.infiniteautomation.mango.permission;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.infiniteautomation.mango.spring.components.RunAs;
import com.infiniteautomation.mango.spring.service.DataPointService;
import com.infiniteautomation.mango.spring.service.SystemPermissionService;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.MangoTestBase;
import com.serotonin.m2m2.module.definitions.permissions.DataPointPermissionDefinition;
import com.serotonin.m2m2.vo.DataPointVO;
import com.serotonin.m2m2.vo.User;
import com.serotonin.m2m2.vo.role.RoleVO;

public class ViewDataPointsPermissionTest extends MangoTestBase {
    private RunAs runAs;
    private RoleVO viewDataPointsRole;
    private User userAllowed;
    private User userWithoutPermission;

    private DataPointVO point;
    private DataPointService service;

    @Before
    public void init() {
        this.runAs = Common.getBean(RunAs.class);
        this.service = Common.getBean(DataPointService.class);

        //create Role for this test
        this.viewDataPointsRole = this.createRole("viewDataPointsRole", DataPointPermissionDefinition.PERMISSION);
        this.userAllowed = this.createUser("Jane Doe", "Jane001", "14mJ4n3D03!", "jane.doe@unknown.com", viewDataPointsRole.getRole());
        this.userWithoutPermission = this.createUser("John Doe", "John001", "14mJ0hnD03!", "john.doe@unknown.com");

        //get the permission def
        DataPointPermissionDefinition dpPermDef = Common.getBean(DataPointPermissionDefinition.class);
        SystemPermissionService sysPermServ = Common.getBean(SystemPermissionService.class);
        this.runAs.runAs(this.runAs.systemSuperadmin(), () -> {
            sysPermServ.update(MangoPermission.requireAnyRole(viewDataPointsRole.getRole()), dpPermDef);
        });

        //create a data point that only superadmin can view
        point = (DataPointVO) this.createMockDataPoints(1).get(0);

    }

    @Test
    public void withoutPermission() {

        //confirm that the superadmin can view the data point
        runAs.runAs(runAs.systemSuperadmin(), () -> {
            AtomicInteger count = new AtomicInteger();
            service.buildQuery().equal("id",point.getId()).query(fromDb -> {
                count.incrementAndGet();
                Assert.assertNotNull(fromDb);
                Assert.assertEquals(point, fromDb);
            });
            Assert.assertEquals(1, count.get());
        });

        //test that the user without permission should not view the data point.
        runAs.runAs(userWithoutPermission, () -> {
            AtomicInteger count = new AtomicInteger();
            service.buildQuery().equal("id",point.getId()).query(fromDb -> {
                count.incrementAndGet();

            });
            Assert.assertEquals(0, count.get());
        });
    }

    @Test
    public void withPermission() {

        //confirm that the superadmin can view the data point
        runAs.runAs(runAs.systemSuperadmin(), () -> {
            AtomicInteger count = new AtomicInteger();
            service.buildQuery().equal("id",point.getId()).query(fromDb -> {
                count.incrementAndGet();
                Assert.assertNotNull(fromDb);
                Assert.assertEquals(point, fromDb);
            });
            Assert.assertEquals(1, count.get());
        });

        //test that the user with permission can view the data point.
        runAs.runAs(userAllowed, () -> {
            AtomicInteger count = new AtomicInteger();
            service.buildQuery().equal("id",point.getId()).query(fromDb -> {
                count.incrementAndGet();
                Assert.assertNotNull(fromDb);
                Assert.assertEquals(point, fromDb);
            });
            Assert.assertEquals(1, count.get());

        });
    }


}
