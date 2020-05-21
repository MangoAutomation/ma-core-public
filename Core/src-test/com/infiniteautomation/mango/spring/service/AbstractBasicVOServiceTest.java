/**
 * Copyright (C) 2019  Infinite Automation Software. All rights reserved.
 */
package com.infiniteautomation.mango.spring.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Test;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;

import com.infiniteautomation.mango.permission.MangoPermission;
import com.infiniteautomation.mango.spring.db.AbstractBasicTableDefinition;
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
public abstract class AbstractBasicVOServiceTest<VO extends AbstractBasicVO, TABLE extends AbstractBasicTableDefinition, DAO extends AbstractBasicDao<VO, TABLE>, SERVICE extends AbstractBasicVOService<VO,TABLE,DAO>> extends MangoTestBase {

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
        setupRoles();
        service = getService();
        dao = getDao();
    }

    public AbstractBasicVOServiceTest() {

    }

    public User getEditUser() {
        return editUser;
    }

    public void setContextUser(PermissionHolder holder) {
        SecurityContextImpl sc = new SecurityContextImpl();
        sc.setAuthentication(new PreAuthenticatedAuthenticationToken(holder, holder.getAllInheritedRoles()));
        SecurityContextHolder.setContext(sc);
    }

    @Test
    public void testCreate() {
        runTest(() -> {
            getService().permissionService.runAsSystemAdmin(() -> {
                VO vo = insertNewVO(readUser);
                VO fromDb = service.get(vo.getId());
                assertVoEqual(vo, fromDb);
            });
        });
    }

    @Test
    public void testUpdate() {
        runTest(() -> {
            getService().permissionService.runAsSystemAdmin(() -> {
                VO vo = insertNewVO(readUser);
                VO fromDb = service.get(vo.getId());
                assertVoEqual(vo, fromDb);

                VO updated = updateVO(vo);
                service.update(vo.getId(), updated);
                fromDb = service.get(vo.getId());
                assertVoEqual(updated, fromDb);
            });
        });
    }

    @Test(expected = NotFoundException.class)
    public void testDelete() {
        runTest(() -> {
            getService().permissionService.runAsSystemAdmin(() -> {
                VO vo = insertNewVO(readUser);
                VO fromDb = service.get(vo.getId());
                assertVoEqual(vo, fromDb);
                service.delete(vo.getId());
                service.get(vo.getId());
            });
        });
    }

    @Test
    public void testCount() {
        runTest(() -> {
            getService().permissionService.runAsSystemAdmin(() -> {
                List<VO> all = service.dao.getAll();
                for(VO vo : all) {
                    service.delete(vo.getId());
                }

                List<VO> vos = new ArrayList<>();
                for(int i=0; i<5; i++) {
                    vos.add(insertNewVO(readUser));
                }
                assertEquals(5, service.dao.count());
            });
        });
    }

    @Test
    public void testGetAll() {
        runTest(() -> {
            getService().permissionService.runAsSystemAdmin(() -> {
                List<VO> all = service.dao.getAll();
                for(VO vo : all) {
                    service.delete(vo.getId());
                }
                List<VO> vos = new ArrayList<>();
                for(int i=0; i<5; i++) {
                    vos.add(insertNewVO(readUser));
                }
                all = service.dao.getAll();
                for(VO vo : all) {
                    VO expected = null;
                    for(VO e : vos) {
                        if(e.getId() == vo.getId()) {
                            expected = e;
                        }
                    }
                    assertNotNull("Didn't find expected VO", expected);
                    assertVoEqual(expected, vo);
                }
            });
        });
    }

    VO insertNewVO(User owner) {
        VO vo = newVO(owner);
        return service.insert(vo);
    }

    void assertPermission(MangoPermission expected, MangoPermission actual) {
        assertTrue(expected.equals(actual));
    }

    void setupRoles() {
        roleService = Common.getBean(RoleService.class);

        systemSuperadmin = PermissionHolder.SYSTEM_SUPERADMIN;
        getService().permissionService.runAsSystemAdmin(() -> {
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
        });
    }

    @FunctionalInterface
    public static interface ServiceLayerTest {
        void test();
    }

    /**
     * Run a test and allow providing expected invalid properties
     * @param test
     * @param expectedInvalidProperties - property names (can be null if no invalid properties expected)
     */
    public void runTest(ServiceLayerTest test, String... expectedInvalidProperties) {
        List<String> invalid = new ArrayList<>();
        try {
            test.test();
        } catch(ValidationException e) {
            String failureMessage = "";
            for(ProcessMessage m : e.getValidationResult().getMessages()) {
                if(m.getContextKey() != null) {
                    String messagePart = m.getContextKey() + " -> " + m.getContextualMessage().translate(Common.getTranslations()) + "\n";
                    failureMessage += messagePart;
                    //Were we expecting this failure?
                    if(ArrayUtils.contains(expectedInvalidProperties, m.getContextKey())) {
                        invalid.add(m.getContextKey());
                    }
                }else {
                    failureMessage += m.getContextualMessage().translate(Common.getTranslations()) + "\n";
                }
            }
            if(expectedInvalidProperties.length > 0) {
                if(!Arrays.equals(expectedInvalidProperties, invalid.toArray(new String[invalid.size()]))) {
                    fail("Did not match all invalid properties but found:\n" + failureMessage  + " but expected " + Arrays.asList(expectedInvalidProperties));
                }
            }else if(StringUtils.isNotEmpty(failureMessage)) {
                fail(failureMessage);
            }
        }
    }
    public Role getEditRole() {
        return editRole;
    }
    public RoleService getRoleService() {
        return roleService;
    }

}
