/**
 * Copyright (C) 2019  Infinite Automation Software. All rights reserved.
 */
package com.infiniteautomation.mango.spring.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Test;

import com.infiniteautomation.mango.util.exception.NotFoundException;
import com.infiniteautomation.mango.util.exception.ValidationException;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.MangoTestBase;
import com.serotonin.m2m2.db.dao.AbstractBasicDao;
import com.serotonin.m2m2.i18n.ProcessMessage;
import com.serotonin.m2m2.vo.AbstractBasicVO;
import com.serotonin.m2m2.vo.User;
import com.serotonin.m2m2.vo.permission.PermissionHolder;
import com.serotonin.m2m2.vo.role.RoleVO;

/**
 * @author Terry Packer
 *
 */
public abstract class AbstractBasicVOServiceTest<VO extends AbstractBasicVO, DAO extends AbstractBasicDao<VO>, SERVICE extends AbstractBasicVOService<VO,DAO>> extends MangoTestBase {

    protected SERVICE service;
    protected DAO dao;

    abstract SERVICE getService();
    abstract DAO getDao();
    abstract void assertVoEqual(VO expected, VO actual);
    
    protected RoleService roleService;
    
    protected PermissionHolder systemSuperadmin;
    protected PermissionHolder systemUser;
    
    protected User readUser;
    protected User editUser;
    protected User setUser;
    protected User deleteUser;
    protected User allUser;
    
    protected RoleVO readRole;
    protected RoleVO editRole;
    protected RoleVO deleteRole;
    protected RoleVO setRole;
    
    /**
     * Create a new VO with all fields populated except any roles
     * @return
     */
    abstract VO newVO();
    /**
     * Update every field with a new value
     * @param existing
     * @return
     */
    abstract VO updateVO(VO existing);
    
    @Before
    public void setupServiceTest() {
        setupRoles();
        service = getService();
        dao = getDao();
    }
    
    public AbstractBasicVOServiceTest() {
        
    }
    
    public AbstractBasicVOServiceTest(boolean enableWebDb, int webDbPort) {
        super(enableWebDb, webDbPort);
    }
    
    public User getEditUser() {
        return editUser;
    }
    
    @Test
    public void testCreate() {
        runTest(() -> {
            VO vo = insertNewVO();
            VO fromDb = service.get(vo.getId(), true, systemSuperadmin);
            assertVoEqual(vo, fromDb);            
        });
    }
    
    @Test
    public void testUpdate() {
        runTest(() -> {
            VO vo = insertNewVO();
            VO fromDb = service.get(vo.getId(), true, systemSuperadmin);
            assertVoEqual(vo, fromDb);
            
            VO updated = updateVO(vo);
            service.update(vo.getId(), updated, true, systemSuperadmin);
            fromDb = service.get(vo.getId(), true, systemSuperadmin);
            assertVoEqual(updated, fromDb);            
        });
    }
    
    @Test(expected = NotFoundException.class)
    public void testDelete() {
        runTest(() -> {
            VO vo = insertNewVO();
            VO fromDb = service.get(vo.getId(), true, systemSuperadmin);
            assertVoEqual(vo, fromDb);
            service.delete(vo.getId(), systemSuperadmin);
            service.get(vo.getId(), true, systemSuperadmin);
        });
    }
    
    VO insertNewVO() {
        VO vo = newVO();
        return service.insert(vo, true, systemSuperadmin);
    }
    
    void assertRoles(Set<RoleVO> expected, Set<RoleVO> actual) {
        assertEquals(expected.size(), actual.size());
        Set<RoleVO> missing = new HashSet<>();
        for(RoleVO expectedRole : expected) {
            boolean found = false;
            for(RoleVO actualRole : actual) {
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
            for(RoleVO missingRole : missing) {
                missingRoles += "< " + missingRole.getId() + " - " + missingRole.getName() + "> ";
            }
            fail("Not all roles matched, missing: " + missingRoles);
        }
    }

    void setupRoles() {
        roleService = Common.getBean(RoleService.class);
        
        systemSuperadmin = PermissionHolder.SYSTEM_SUPERADMIN;

        //Add some roles
        readRole = new RoleVO("read-role", "Role to allow reading.");
        roleService.insert(readRole, true, systemSuperadmin);
        
        editRole = new RoleVO("edit-role", "Role to allow editing.");
        roleService.insert(editRole, true, systemSuperadmin);
        
        setRole = new RoleVO("set-role", "Role to allow setting.");
        roleService.insert(setRole, true, systemSuperadmin);
        
        deleteRole = new RoleVO("delete-role", "Role to allow deleting.");
        roleService.insert(deleteRole, true, systemSuperadmin);

        readUser = createUser("readUser", "readUser", "password", "readUser@example.com", readRole);
        editUser = createUser("editUser", "editUser", "password", "editUser@example.com", editRole);
        setUser = createUser("setUser", "setUser", "password", "setUser@example.com", setRole);
        deleteUser = createUser("deleteUser", "deleteUser", "password", "deleteUser@example.com", deleteRole);
        allUser = createUser("allUser", "allUser", "password", "allUser@example.com", readRole, editRole, setRole, deleteRole);   
    }
    
    @FunctionalInterface
    public static interface ServiceLayerTest {
        void test();
    }
    
    /**
     * Run a test and provide nice validation failure messages
     * @param test
     */
    public void runTest(ServiceLayerTest test) {
        try {
            test.test();
        } catch(ValidationException e) {
            String failureMessage = "";
            for(ProcessMessage m : e.getValidationResult().getMessages()){
                String messagePart = m.getContextKey() + " -> " + m.getContextualMessage().translate(Common.getTranslations()) + "\n";
                failureMessage += messagePart;
            }
            fail(failureMessage);
        }
    }
    public RoleVO getEditRole() {
        return editRole;
    }
    public RoleService getRoleService() {
        return roleService;
    }
    
}
