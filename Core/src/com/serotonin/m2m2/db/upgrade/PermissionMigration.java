/*
 * Copyright (C) 2020  Infinite Automation Software. All rights reserved.
 */

package com.serotonin.m2m2.db.upgrade;

import com.infiniteautomation.mango.permission.MangoPermission;
import com.infiniteautomation.mango.spring.service.PermissionService;
import com.serotonin.db.spring.ExtendedJdbcTemplate;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.vo.role.Role;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.sql.Types;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Terry Packer
 * @author Jared Wiltshire
 */
public interface PermissionMigration {

    ExtendedJdbcTemplate getJdbcTemplate();
    PlatformTransactionManager getTransactionManager();
    default TransactionTemplate getTransactionTemplate() {
        return new TransactionTemplate(getTransactionManager());
    }

    default MangoPermission getOrCreatePermission(MangoPermission permission) {
        if (permission.getId() != null) {
            throw new IllegalArgumentException("Permission is already saved: " + permission);
        }

        return getTransactionTemplate().execute(tx -> {
            Set<Integer> mintermIds = new HashSet<>();
            Set<Set<Role>> minterms = new HashSet<>();
            for (Set<Role> minterm : permission.getRoles()) {
                Set<Role> savedRoles = new HashSet<>();
                for (Role role : minterm) {
                    savedRoles.add(getOrCreateRole(role));
                }
                mintermIds.add(getOrCreateMinterm(savedRoles));
            }

            MangoPermission saved = new MangoPermission(minterms);
            saved.setId(getOrCreatePermission(mintermIds));
            return saved;
        });
    }

    default Integer getOrCreatePermission(Set<Integer> mintermIdsSet) {
        ExtendedJdbcTemplate ejt = getJdbcTemplate();
        Integer[] mintermIds = mintermIdsSet.toArray(new Integer[0]);
        try {
            if (mintermIdsSet.isEmpty()) {
                return ejt.queryForObject("SELECT permissions.id FROM permissions LEFT JOIN permissionsMinterms ON permissions.id = permissionsMinterms.permissionId WHERE permissionsMinterms.permissionId IS NULL LIMIT 1",
                        Integer.class);
            }
            return ejt.queryForObject("SELECT permissionId FROM permissionsMinterms GROUP BY permissionId HAVING (COUNT(CASE WHEN mintermId IN (?) THEN 1 ELSE NULL END) = ? AND COUNT(permissionId) = ?) LIMIT 1",
                    Integer.class, mintermIds, mintermIds.length, mintermIds.length);
        } catch (EmptyResultDataAccessException e) {
            int permissionId = ejt.doInsert("INSERT INTO permissions () VALUES ()", new Object[] {}, new int[] {});
            for (Integer mintermId : mintermIds) {
                ejt.doInsert("INSERT INTO permissionsMinterms (permissionId, mintermId) VALUES (?, ?)",
                        new Object[] {permissionId, mintermId},
                        new int[] {Types.INTEGER, Types.INTEGER});
            }
            return permissionId;
        }
    }

    default Integer getOrCreateMinterm(Set<Role> minterm) {
        if (minterm.isEmpty()) {
            throw new IllegalArgumentException("Minterm should never be empty");
        }

        ExtendedJdbcTemplate ejt = getJdbcTemplate();
        Integer[] roleIds = minterm.stream().map(Role::getId).toArray(Integer[]::new);
        try {
            return ejt.queryForObject("SELECT mintermId FROM mintermsRoles GROUP BY mintermId HAVING (COUNT(CASE WHEN roleId IN (?) THEN 1 ELSE NULL END) = ? AND COUNT(roleId) = ?) LIMIT 1",
                    Integer.class, roleIds, roleIds.length, roleIds.length);
        } catch (EmptyResultDataAccessException e) {
            int mintermId = ejt.doInsert("INSERT INTO minterms () VALUES ()", new Object[] {}, new int[] {});
            for (int roleId : roleIds) {
                ejt.doInsert("INSERT INTO mintermsRoles (mintermId, roleId) VALUES (?, ?)",
                        new Object[] {mintermId, roleId},
                        new int[] {Types.INTEGER, Types.INTEGER});
            }
            return mintermId;
        }
    }

    default Role getOrCreateRole(Role role) {
        if (role.getId() > 0) {
            return role;
        }

        ExtendedJdbcTemplate ejt = getJdbcTemplate();
        String xid = role.getXid();
        int roleId;
        try {
            roleId = Objects.requireNonNull(ejt.queryForObject("SELECT id FROM roles WHERE xid = ?", Integer.class, xid));
        } catch (EmptyResultDataAccessException e) {
            roleId = ejt.doInsert("INSERT INTO roles (xid, name) VALUES (?, ?)",
                    new Object[] {xid, xid},
                    new int[] {Types.VARCHAR, Types.VARCHAR});
        }

        return new Role(roleId, xid);
    }

    /**
     * Returns an unsaved MangoPermission with roles that are also not saved (i.e. their id is -1)
     * @param permissions legacy permission string to upgrade
     * @return unsaved MangoPermission
     */
    static MangoPermission parseLegacyPermission(String permissions) {
        Set<String> xids = PermissionService.explodeLegacyPermissionGroups(permissions);
        Set<Set<Role>> minterms = xids.stream()
                .map(xid -> new Role(Common.NEW_ID, xid))
                .map(Collections::singleton)
                .collect(Collectors.toSet());
        return new MangoPermission(minterms);
    }

}
