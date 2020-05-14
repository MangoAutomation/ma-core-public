/**
 * Copyright (C) 2020  Infinite Automation Software. All rights reserved.
 */

package com.infiniteautomation.mango.permission;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.Test;

import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.MangoTestBase;
import com.serotonin.m2m2.db.dao.PermissionDao;
import com.serotonin.m2m2.vo.role.Role;

/**
 *
 * @author Terry Packer
 */
public class PermissionPersistenceTest extends MangoTestBase {

    @Test
    public void testAndPermission() {

        PermissionDao dao = Common.getBean(PermissionDao.class);

        //Create some roles
        Set<Role> roles = this.createRoles(2).stream().map(r -> r.getRole()).collect(Collectors.toSet());
        //insert the permission
        Integer permissionId = dao.permissionId(MangoPermission.createAndSet(roles), true);
        MangoPermission read = dao.get(permissionId);

        assertEquals(1, read.getRoles().size());
        Iterator<Set<Role>> it = read.getRoles().iterator();
        assertEquals(2, it.next().size());
    }

    @Test
    public void testOrPermission() {

        PermissionDao dao = Common.getBean(PermissionDao.class);

        //Create some roles
        Set<Role> roles = this.createRoles(2).stream().map(r -> r.getRole()).collect(Collectors.toSet());
        //insert the permission
        Integer permissionId = dao.permissionId(MangoPermission.createOrSet(roles), true);
        MangoPermission read = dao.get(permissionId);

        assertEquals(2, read.getRoles().size());
        Iterator<Set<Role>> it = read.getRoles().iterator();
        assertEquals(1, it.next().size());
        assertEquals(1, it.next().size());
    }


    @Test
    public void testComplexPermission() {

        PermissionDao dao = Common.getBean(PermissionDao.class);

        //Create some roles
        Set<Role> minterm1 = this.createRoles(1).stream().map(r -> r.getRole()).collect(Collectors.toSet());
        Set<Role> minterm2 = this.createRoles(2).stream().map(r -> r.getRole()).collect(Collectors.toSet());
        Set<Role> minterm3 = this.createRoles(3).stream().map(r -> r.getRole()).collect(Collectors.toSet());

        //insert the permission
        Set<Set<Role>> permission = new HashSet<>();
        permission.add(minterm1);
        permission.add(minterm2);
        permission.add(minterm3);

        Integer permissionId = dao.permissionId(new MangoPermission(permission), true);
        MangoPermission read = dao.get(permissionId);

        assertEquals(3, read.getRoles().size());
        Iterator<Set<Role>> it = read.getRoles().iterator();

        while(it.hasNext()) {
            Set<Role> minterm = it.next();
            switch(minterm.size()) {
                case 1:
                    assertEquals(1, minterm.size());
                    Role minterm1Role1 = minterm.iterator().next();
                    assertTrue(minterm1.contains(minterm1Role1));
                    break;
                case 2:
                    assertEquals(2, minterm.size());
                    Iterator<Role> it2 = minterm.iterator();
                    Role minterm2Role1 = it2.next();
                    Role minterm2Role2 = it2.next();
                    assertTrue(minterm2.contains(minterm2Role1));
                    assertTrue(minterm2.contains(minterm2Role2));
                    break;
                case 3:
                    assertEquals(3, minterm.size());
                    Iterator<Role> it3 = minterm.iterator();
                    Role minterm3Role1 = it3.next();
                    Role minterm3Role2 = it3.next();
                    Role minterm3Role3 = it3.next();
                    assertTrue(minterm3.contains(minterm3Role1));
                    assertTrue(minterm3.contains(minterm3Role2));
                    assertTrue(minterm3.contains(minterm3Role3));
                    break;
            }
        }
    }

    @Test
    public void testOrphanedPermission() {
        //Create permission with a single role
        //Delete all roles in minterm
    }

    //Test when removing a role we also delete any orphaned permission (has no roles in minterms)
    // Delete role then check each used minterms to see if empty

    //Test when removing/modifying a permission ensure
    // change permissionId mappings from ON DELETE CASCADE to ON DELETE RESTRICT
    // then if deleted then check for any orphaned minterms (not mapped to permission)
    @Test
    public void testModifyPermission() {

        PermissionDao dao = Common.getBean(PermissionDao.class);

        //Create some roles
        Set<Role> roles = this.createRoles(2).stream().map(r -> r.getRole()).collect(Collectors.toSet());
        //insert the permission
        Integer permissionId = dao.permissionId(MangoPermission.createOrSet(roles), true);
        MangoPermission read = dao.get(permissionId);

        assertEquals(2, read.getRoles().size());
        Iterator<Set<Role>> it = read.getRoles().iterator();
        Role toKeep = it.next().iterator().next();

        permissionId = dao.permissionId(MangoPermission.createOrSet(toKeep), false);
        read = dao.get(permissionId);
        assertEquals(1, read.getRoles().size());

    }

}
