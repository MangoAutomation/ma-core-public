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
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;

import com.infiniteautomation.mango.util.exception.NotFoundException;
import com.infiniteautomation.mango.util.exception.ValidationException;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.MangoTestBase;
import com.serotonin.m2m2.db.dao.AbstractBasicDao;
import com.serotonin.m2m2.i18n.ProcessMessage;
import com.serotonin.m2m2.vo.AbstractBasicVO;
import com.serotonin.m2m2.vo.User;
import com.serotonin.m2m2.vo.permission.PermissionHolder;
import com.serotonin.m2m2.vo.role.Role;
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
    
    protected Role readRole;
    protected Role editRole;
    protected Role deleteRole;
    protected Role setRole;
    
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
    
    /**
     * 
     * @param holder
     */
    public void setContextUser(PermissionHolder holder) {
        SecurityContextImpl sc = new SecurityContextImpl();
        sc.setAuthentication(new PreAuthenticatedAuthenticationToken(holder, holder.getRoles()));
        SecurityContextHolder.setContext(sc);
    }
    
    @Test
    public void testCreate() {
        runTest(() -> {
            Common.setUser(systemSuperadmin);
            try {
                VO vo = insertNewVO();
                VO fromDb = service.get(vo.getId());
                assertVoEqual(vo, fromDb);     
            }finally {
                Common.removeUser();
            }
        });
    }
    
    @Test
    public void testUpdate() {
        runTest(() -> {
            Common.setUser(systemSuperadmin);
            try {
                VO vo = insertNewVO();
                VO fromDb = service.get(vo.getId());
                assertVoEqual(vo, fromDb);
                
                VO updated = updateVO(vo);
                service.update(vo.getId(), updated);
                fromDb = service.get(vo.getId());
                assertVoEqual(updated, fromDb);
            }finally {
                Common.removeUser();
            }
        });
    }
    
    @Test(expected = NotFoundException.class)
    public void testDelete() {
        runTest(() -> {
            Common.setUser(systemSuperadmin);
            try {
                VO vo = insertNewVO();
                VO fromDb = service.get(vo.getId());
                assertVoEqual(vo, fromDb);
                service.delete(vo.getId());
                service.get(vo.getId());
            }finally {
                Common.removeUser();
            }
        });
    }
    
    VO insertNewVO() {
        VO vo = newVO();
        return service.insert(vo);
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
        
        systemSuperadmin = PermissionHolder.SYSTEM_SUPERADMIN;
        Common.setUser(systemSuperadmin);
        try {
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
        }finally {
            Common.removeUser();
        }
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
    public Role getEditRole() {
        return editRole;
    }
    public RoleService getRoleService() {
        return roleService;
    }
    
}
