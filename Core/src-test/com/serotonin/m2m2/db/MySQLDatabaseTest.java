/*
 * Copyright (C) 2022 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.db;

import static org.junit.Assert.assertEquals;
import static org.junit.Assume.assumeTrue;

import org.jooq.DSLContext;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import com.infiniteautomation.mango.db.tables.Roles;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.MangoTestBase;
import com.serotonin.m2m2.db.dao.RoleDao;
import com.serotonin.m2m2.vo.role.RoleVO;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class MySQLDatabaseTest extends MangoTestBase {

    public MySQLDatabaseTest() {
        String currentDb = properties.getProperty("db.type");
        assumeTrue(currentDb.equals("mysql"));
    }

    @Test
    public void test1AutoIncrement() {
        DSLContext context = Common.getBean(DatabaseProxy.class).getContext();
        Roles r = Roles.ROLES;
        context.insertInto(r, r.id, r.xid, r.name).values(10, "xid", "name").execute();
        context.insertInto(r, r.xid, r.name).values("test", "test").execute();
        RoleVO role = Common.getBean(RoleDao.class).getByXid("test");
        assertEquals(11, role.getId());
    }
}
