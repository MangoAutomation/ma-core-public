/**
 * Copyright (C) 2019  Infinite Automation Software. All rights reserved.
 */
package com.infiniteautomation.mango.spring.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.jooq.Record;
import org.jooq.Table;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import com.infiniteautomation.mango.permission.MangoPermission;
import com.infiniteautomation.mango.spring.components.RunAs;
import com.infiniteautomation.mango.util.exception.NotFoundException;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.MangoTestBase;
import com.serotonin.m2m2.db.dao.AbstractBasicDao;
import com.serotonin.m2m2.vo.AbstractBasicVO;
import com.serotonin.m2m2.vo.User;
import com.serotonin.m2m2.vo.role.Role;
import com.serotonin.m2m2.vo.role.RoleVO;

/**
 * @author Terry Packer
 *
 */
public abstract class AbstractBasicVOServiceTest<VO extends AbstractBasicVO, R extends Record, TABLE extends Table<R>, DAO extends AbstractBasicDao<VO, R, TABLE>, SERVICE extends AbstractBasicVOService<VO, DAO>> extends MangoTestBase {

    @Rule
    public ExpectValidationErrorRule validation = new ExpectValidationErrorRule();

    protected RunAs runAs;
    protected SERVICE service;
    protected DAO dao;

    abstract SERVICE getService();
    abstract DAO getDao();
    abstract void assertVoEqual(VO expected, VO actual);

    protected RoleService roleService;

    protected User readUser;
    protected User editUser;
    protected User setUser;
    protected User deleteUser;
    protected User allUser;

    protected Role readRole;
    protected Role editRole;
    protected Role deleteRole;
    protected Role setRole;

    /**
     * Create a new VO with all fields populated except any roles
     *
     * @param owner - owner of VO if necessary
     * @return
     */
    abstract VO newVO(User owner);
    /**
     * Update every field with a new value
     * @param existing
     * @return
     */
    abstract VO updateVO(VO existing);

    @Before
    public void setupServiceTest() {
        this.runAs = Common.getBean(RunAs.class);
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

    void assertPermission(MangoPermission expected, MangoPermission actual) {
        assertTrue(expected.equals(actual));
    }

    void assertRoles(Set<Role> expected, Set<Role> actual) {
        assertEquals(expected.size(), actual.size());
        Set<Role> missing = new HashSet<>();
        for(Role expectedRole : expected) {
            boolean found = false;
            for(Role actualRole : actual) {
                if(StringUtils.equals(expectedRole.getXid(), actualRole.getXid())) {
                    found = true;
                    break;
                }
            }
            if(!found) {
                missing.add(expectedRole);
            }
        }
        if(missing.size() > 0) {
            String missingRoles = "";
            for(Role missingRole : missing) {
                missingRoles += "< " + missingRole.getId() + " - " + missingRole.getXid() + "> ";
            }
            fail("Not all roles matched, missing: " + missingRoles);
        }
    }

    void setupRoles() {
        roleService = Common.getBean(RoleService.class);

        //Add some roles
        RoleVO temp = new RoleVO(Common.NEW_ID, "read-role", "Role to allow reading.");
        roleService.insert(temp);
        readRole = new Role(temp);

        temp = new RoleVO(Common.NEW_ID, "edit-role", "Role to allow editing.");
        roleService.insert(temp);
        editRole = new Role(temp);

        temp = new RoleVO(Common.NEW_ID, "set-role", "Role to allow setting.");
        roleService.insert(temp);
        setRole = new Role(temp);

        temp = new RoleVO(Common.NEW_ID, "delete-role", "Role to allow deleting.");
        roleService.insert(temp);
        deleteRole = new Role(temp);

        readUser = createUser("readUser", "readUser", "password", "readUser@example.com", readRole);
        editUser = createUser("editUser", "editUser", "password", "editUser@example.com", editRole);
        setUser = createUser("setUser", "setUser", "password", "setUser@example.com", setRole);
        deleteUser = createUser("deleteUser", "deleteUser", "password", "deleteUser@example.com", deleteRole);
        allUser = createUser("allUser", "allUser", "password", "allUser@example.com", readRole, editRole, setRole, deleteRole);
    }

    public Role getEditRole() {
        return editRole;
    }

    public RoleService getRoleService() {
        return roleService;
    }

}
