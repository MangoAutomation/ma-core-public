/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.infiniteautomation.mango.spring.service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.jooq.Record;
import org.jooq.Table;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import com.infiniteautomation.mango.rules.ExpectValidationExceptionRule;
import com.infiniteautomation.mango.spring.components.RunAs;
import com.infiniteautomation.mango.util.AbstractRoleBasedTest;
import com.infiniteautomation.mango.util.AssertionUtils;
import com.infiniteautomation.mango.util.exception.NotFoundException;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.dao.AbstractBasicDao;
import com.serotonin.m2m2.vo.AbstractBasicVO;
import com.serotonin.m2m2.vo.User;
import com.serotonin.m2m2.vo.role.Role;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author Terry Packer
 *
 */
public abstract class AbstractBasicVOServiceTest<
        VO extends AbstractBasicVO, R extends Record, TABLE extends Table<R>, DAO extends AbstractBasicDao<VO, R, TABLE>,
        SERVICE extends AbstractBasicVOService<VO, DAO>> extends AbstractRoleBasedTest implements AssertionUtils {

    @Rule
    public ExpectValidationExceptionRule validation = new ExpectValidationExceptionRule();

    protected RunAs runAs;
    protected SERVICE service;
    protected DAO dao;

    abstract SERVICE getService();
    abstract DAO getDao();
    abstract void assertVoEqual(VO expected, VO actual);


    protected SystemPermissionService systemPermissionService;

    /**
     * Create a new VO with all fields populated except any roles
     *
     * @param owner - owner of VO if necessary
     */
    abstract VO newVO(User owner);
    /**
     * Update every field with a new value
     */
    abstract VO updateVO(VO existing);

    @Before
    public void setupServiceTest() {
        this.runAs = Common.getBean(RunAs.class);
        this.systemPermissionService = Common.getBean(SystemPermissionService.class);
        setupRoles();
        service = getService();
        dao = getDao();
    }

    public AbstractBasicVOServiceTest() {

    }

    public User getEditUser() {
        return editUser;
    }

    @Test
    public void testCreate() {
        VO vo = insertNewVO(readUser);
        VO fromDb = service.get(vo.getId());
        assertVoEqual(vo, fromDb);
    }

    @Test
    public void testUpdate() {
        VO vo = insertNewVO(readUser);
        VO fromDb = service.get(vo.getId());
        assertVoEqual(vo, fromDb);

        VO updated = updateVO(vo);
        service.update(vo.getId(), updated);
        fromDb = service.get(vo.getId());
        assertVoEqual(updated, fromDb);
    }

    @Test(expected = NotFoundException.class)
    public void testDelete() {
        VO vo = insertNewVO(readUser);
        VO fromDb = service.get(vo.getId());
        assertVoEqual(vo, fromDb);
        service.delete(vo.getId());
        service.get(vo.getId());
    }

    @Test
    public void testCount() {
        List<VO> all = service.dao.getAll();
        for (VO vo : all) {
            service.delete(vo.getId());
        }

        List<VO> vos = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            vos.add(insertNewVO(readUser));
        }
        assertEquals(5, service.dao.count());
    }

    @Test
    public void testGetAll() {
        List<VO> all = service.dao.getAll();
        for (VO vo : all) {
            service.delete(vo.getId());
        }
        List<VO> vos = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            vos.add(insertNewVO(readUser));
        }
        all = service.dao.getAll();
        for (VO vo : all) {
            VO expected = null;
            for (VO e : vos) {
                if (e.getId() == vo.getId()) {
                    expected = e;
                }
            }
            assertNotNull("Didn't find expected VO", expected);
            assertVoEqual(expected, vo);
        }
    }

    VO insertNewVO(User owner) {
        VO vo = newVO(owner);
        return service.insert(vo);
    }

    public Role getEditRole() {
        return editRole;
    }

    public RoleService getRoleService() {
        return roleService;
    }

    public String randomXid() {
        return UUID.randomUUID().toString();
    }

}
