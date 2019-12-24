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
import com.serotonin.m2m2.db.dao.AbstractDao;
import com.serotonin.m2m2.i18n.ProcessMessage;
import com.serotonin.m2m2.vo.AbstractVO;
import com.serotonin.m2m2.vo.RoleVO;
import com.serotonin.m2m2.vo.User;
import com.serotonin.m2m2.vo.permission.PermissionHolder;

/**
 * Base class to test the service layer implementations
 * 
 * @author Terry Packer
 *
 */
public abstract class ServiceTestBase<VO extends AbstractVO<?>, DAO extends AbstractDao<VO>, SERVICE extends AbstractVOService<VO,DAO>> extends MangoTestBase {

    public ServiceTestBase() {
        
    }
    
    public ServiceTestBase(boolean enableWebDb, int webDbPort) {
        super(enableWebDb, webDbPort);
    }
    
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
    
    protected SERVICE service;
    protected DAO dao;

    abstract SERVICE getService();
    abstract DAO getDao();
    abstract void assertVoEqual(VO expected, VO actual);
    
    abstract VO newVO();
    abstract VO updateVO(VO existing);
    
    @Before
    public void setupServiceTest() {
        setupRoles();
        service = getService();
        dao = getDao();
    }
    
    @Test
    public void testCreate() {
        runTest(() -> {
            VO vo = insertNewVO();
            VO fromDb = service.getFull(vo.getId(), systemSuperadmin);
            assertVoEqual(vo, fromDb);            
        });
    }
    
    @Test
    public void testUpdate() {
        runTest(() -> {
            VO vo = insertNewVO();
            VO fromDb = service.getFull(vo.getId(), systemSuperadmin);
            assertVoEqual(vo, fromDb);
            
            VO updated = updateVO(vo);
            service.updateFull(vo.getId(), updated, systemSuperadmin);
            fromDb = service.getFull(vo.getId(), systemSuperadmin);
            assertVoEqual(updated, fromDb);            
        });
    }
    
    @Test(expected = NotFoundException.class)
    public void testDelete() {
        runTest(() -> {
            VO vo = insertNewVO();
            VO fromDb = service.getFull(vo.getId(), systemSuperadmin);
            assertVoEqual(vo, fromDb);
            service.delete(vo.getId(), systemSuperadmin);
            service.getFull(vo.getId(), systemSuperadmin);            
        });
    }
    
    VO insertNewVO() {
        VO vo = newVO();
        service.insertFull(vo, systemSuperadmin);
        return vo;
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
        readRole = new RoleVO();
        readRole.setXid("read-role");
        readRole.setName("Role to allow reading.");
        roleService.insert(readRole, systemSuperadmin);
        
        editRole = new RoleVO();
        editRole.setXid("edit-role");
        editRole.setName("Role to allow editing.");
        roleService.insert(editRole, systemSuperadmin);
        
        setRole = new RoleVO();
        setRole.setXid("set-role");
        setRole.setName("Role to allow setting.");
        roleService.insert(setRole, systemSuperadmin);
        
        deleteRole = new RoleVO();
        deleteRole.setXid("delete-role");
        deleteRole.setName("Role to allow deleting.");
        roleService.insert(deleteRole, systemSuperadmin);

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
    
}
