/**
 * Copyright (C) 2020  Infinite Automation Software. All rights reserved.
 */

package com.infiniteautomation.mango.permission;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.Test;
import org.springframework.jdbc.core.RowMapper;

import com.serotonin.db.spring.ExtendedJdbcTemplate;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.MangoTestBase;
import com.serotonin.m2m2.db.dao.PermissionDao;
import com.serotonin.m2m2.db.dao.RoleDao;
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
        Integer permissionId = dao.permissionId(MangoPermission.createAndSet(roles));
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
        Integer permissionId = dao.permissionId(MangoPermission.createOrSet(roles));
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

        Integer permissionId = dao.permissionId(new MangoPermission(permission));
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

    /**
     * Modify a permission and ensure that it is retrieved correctly from the database after
     */
    @Test
    public void testModifyPermission() {

        PermissionDao dao = Common.getBean(PermissionDao.class);

        //insert some roles
        Set<Role> roles = this.createRoles(2).stream().map(r -> r.getRole()).collect(Collectors.toSet());
        //insert the permission
        Integer permissionId = dao.permissionId(MangoPermission.createOrSet(roles));
        MangoPermission read = dao.get(permissionId);

        assertEquals(2, read.getRoles().size());
        Iterator<Set<Role>> it = read.getRoles().iterator();
        Role toKeep = it.next().iterator().next();

        permissionId = dao.permissionId(MangoPermission.createOrSet(toKeep));
        read = dao.get(permissionId);
        assertEquals(1, read.getRoles().size());

    }

    /**
     *  TODO we cannot remove permissions with empty minterms as of the FK RESTRICT on VOs
     *  Test when removing a role we also delete any orphaned permission (has no roles in minterms)
     *  Delete role then check each used minterms to see if empty
     */
    @Test
    public void testOrphanedPermission() {
        PermissionDao dao = Common.getBean(PermissionDao.class);

        //insert some roles
        Set<Role> roles = this.createRoles(2).stream().map(r -> r.getRole()).collect(Collectors.toSet());

        //insert the permission
        Integer permissionId = dao.permissionId(MangoPermission.createOrSet(roles));
        MangoPermission read = dao.get(permissionId);
        assertEquals(2, read.getRoles().size());

        //Delete all roles in minterm
        Iterator<Set<Role>> it = read.getRoles().iterator();
        Set<Role> term1 = it.next();
        Iterator<Role> term1It = term1.iterator();
        Set<Role> term2 = it.next();
        Iterator<Role> term2It = term2.iterator();
        RoleDao.getInstance().delete(term1It.next().getId());
        RoleDao.getInstance().delete(term2It.next().getId());

        //Confirm that the permission has no roles
        read = dao.get(permissionId);
        assertEquals(0, read.getRoles().size());

        //Check for orphaned minterm mappings
        ExtendedJdbcTemplate ejt = new ExtendedJdbcTemplate();
        ejt.setDataSource(Common.databaseProxy.getDataSource());
        List<Integer> mintermIds = ejt.query("SELECT mintermId from permissionsMinterms WHERE permissionId=" + permissionId, new RowMapper<Integer>() {

            @Override
            public Integer mapRow(ResultSet rs, int rowNum) throws SQLException {
                return rs.getInt(1);
            }

        });
        assertEquals(0, mintermIds.size());
    }

}
