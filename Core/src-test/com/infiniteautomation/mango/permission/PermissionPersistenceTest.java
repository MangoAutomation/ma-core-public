/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */

package com.infiniteautomation.mango.permission;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.Test;
import org.springframework.jdbc.core.RowMapper;

import com.infiniteautomation.mango.spring.service.PermissionService;
import com.serotonin.db.spring.ExtendedJdbcTemplate;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.MangoTestBase;
import com.serotonin.m2m2.db.DatabaseProxy;
import com.serotonin.m2m2.db.dao.RoleDao;
import com.serotonin.m2m2.vo.role.Role;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 *
 * @author Terry Packer
 */
public class PermissionPersistenceTest extends MangoTestBase {

    @Test
    public void testAndPermission() {

        PermissionService service = Common.getBean(PermissionService.class);

        //Create some roles
        Set<Role> roles = this.createRoles(2).stream().map(r -> r.getRole()).collect(Collectors.toSet());
        //insert the permission
        MangoPermission permission = service.findOrCreate(MangoPermission.requireAllRoles(roles));
        MangoPermission read = service.get(permission.getId());

        assertEquals(1, read.getRoles().size());
        Iterator<Set<Role>> it = read.getRoles().iterator();
        assertEquals(2, it.next().size());
    }

    @Test
    public void testOrPermission() {

        PermissionService service = Common.getBean(PermissionService.class);

        //Create some roles
        Set<Role> roles = this.createRoles(2).stream().map(r -> r.getRole()).collect(Collectors.toSet());
        //insert the permission
        MangoPermission permission = service.findOrCreate(MangoPermission.requireAnyRole(roles));
        MangoPermission read = service.get(permission.getId());

        assertEquals(2, read.getRoles().size());
        Iterator<Set<Role>> it = read.getRoles().iterator();
        assertEquals(1, it.next().size());
        assertEquals(1, it.next().size());
    }


    @Test
    public void testComplexPermission() {

        PermissionService service = Common.getBean(PermissionService.class);

        //Create some roles
        Set<Role> minterm1 = this.createRoles(1).stream().map(r -> r.getRole()).collect(Collectors.toSet());
        Set<Role> minterm2 = this.createRoles(2).stream().map(r -> r.getRole()).collect(Collectors.toSet());
        Set<Role> minterm3 = this.createRoles(3).stream().map(r -> r.getRole()).collect(Collectors.toSet());

        //insert the permission
        Set<Set<Role>> minterms = new HashSet<>();
        minterms.add(minterm1);
        minterms.add(minterm2);
        minterms.add(minterm3);

        MangoPermission permission = service.findOrCreate(new MangoPermission(minterms));
        MangoPermission read = service.get(permission.getId());


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

    /**
     * Modify a permission and ensure that it is retrieved correctly from the database after
     */
    @Test
    public void testModifyPermission() {

        PermissionService service = Common.getBean(PermissionService.class);

        //insert some roles
        Set<Role> roles = this.createRoles(2).stream().map(r -> r.getRole()).collect(Collectors.toSet());
        //insert the permission
        MangoPermission permission = service.findOrCreate(MangoPermission.requireAnyRole(roles));
        MangoPermission read = service.get(permission.getId());

        assertEquals(2, read.getRoles().size());
        Iterator<Set<Role>> it = read.getRoles().iterator();
        Role toKeep = it.next().iterator().next();

        MangoPermission keep = service.findOrCreate(MangoPermission.requireAnyRole(toKeep));
        read = service.get(keep.getId());
        assertEquals(1, read.getRoles().size());

    }

    /**
     *  TODO we cannot remove permissions with empty minterms as of the FK RESTRICT on VOs
     *  Test when removing a role we also delete any orphaned permission (has no roles in minterms)
     *  Delete role then check each used minterms to see if empty
     */
    @Test
    public void testOrphanedPermission() {
        PermissionService service = Common.getBean(PermissionService.class);

        //insert some roles
        Set<Role> roles = this.createRoles(2).stream().map(r -> r.getRole()).collect(Collectors.toSet());

        //insert the permission
        MangoPermission permission = service.findOrCreate(MangoPermission.requireAnyRole(roles));
        MangoPermission read = service.get(permission.getId());
        assertEquals(2, read.getRoles().size());

        //Delete all roles in minterm
        Iterator<Set<Role>> it = read.getRoles().iterator();
        Set<Role> term1 = it.next();
        Iterator<Role> term1It = term1.iterator();
        Set<Role> term2 = it.next();
        Iterator<Role> term2It = term2.iterator();
        Common.getBean(RoleDao.class).delete(term1It.next().getId());
        Common.getBean(RoleDao.class).delete(term2It.next().getId());

        //Confirm that the permission has no roles
        read = service.get(permission.getId());
        assertEquals(0, read.getRoles().size());

        //Check for orphaned minterm mappings
        ExtendedJdbcTemplate ejt = new ExtendedJdbcTemplate();
        ejt.setDataSource(Common.getBean(DatabaseProxy.class).getDataSource());
        List<Integer> mintermIds = ejt.query("SELECT mintermId from permissionsMinterms WHERE permissionId=" + permission.getId(), new RowMapper<Integer>() {

            @Override
            public Integer mapRow(ResultSet rs, int rowNum) throws SQLException {
                return rs.getInt(1);
            }

        });
        assertEquals(0, mintermIds.size());
    }

}
