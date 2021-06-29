/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */

package com.serotonin.m2m2.db.upgrade;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.infiniteautomation.mango.permission.MangoPermission;
import com.infiniteautomation.mango.spring.service.PermissionService;
import com.serotonin.db.spring.ExtendedJdbcTemplate;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.vo.role.Role;

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

    Map<MangoPermission, MangoPermission> permissionCache();
    Map<Role, Role> roleCache();

    /**
     * Get an existing permission, will be null if not found
     * @param id
     * @return
     */
    default MangoPermission getExistingPermission(int id) {

        ExtendedJdbcTemplate ejt = getJdbcTemplate();
        return getTransactionTemplate().execute(tx -> {
            //Make sure it exists at all (if no minterms exist we won't be able to tell it apart from the superadmin permission
            int foundId = ejt.queryForInt("SELECT permissions.id FROM permissions WHERE permissions.id = ?", new Object[] {id}, -1);
            if(foundId == -1) {
                return null;
            }

            return ejt.query(
                    "SELECT roles.id, roles.xid, permissionsMinterms.mintermId FROM permissionsMinterms " +
                            "JOIN mintermsRoles ON permissionsMinterms.mintermId = mintermsRoles.mintermId " +
                            "JOIN roles ON roles.id = mintermsRoles.roleId " +
                            "WHERE permissionsMinterms.permissionId = ? " +
                            "ORDER BY permissionsMinterms.permissionId ASC, permissionsMinterms.mintermId ASC", new Object[] {id}, new ResultSetExtractor<MangoPermission>() {

                        int roleIdIndex = 1;
                        int roleXidIndex = 2;
                        int minterIdIndex = 3;
                        @Override
                        public MangoPermission extractData(ResultSet rs)
                                throws SQLException, DataAccessException {
                            if(rs.next()){
                                Set<Set<Role>> roleSet = new HashSet<>();
                                Set<Role> minTerm = new HashSet<>();
                                roleSet.add(minTerm);
                                minTerm.add(new Role(rs.getInt(roleIdIndex), rs.getString(roleXidIndex)));

                                int mintermId = rs.getInt(minterIdIndex);
                                while(rs.next()) {
                                    if(rs.getInt(minterIdIndex) == mintermId) {
                                        //Add to current minterm
                                        minTerm.add(new Role(rs.getInt(roleIdIndex), rs.getString(roleXidIndex)));
                                    }else {
                                        //Add to next minterm
                                        minTerm = new HashSet<>();
                                        roleSet.add(minTerm);
                                        minTerm.add(new Role(rs.getInt(roleIdIndex), rs.getString(roleXidIndex)));
                                        mintermId = rs.getInt(minterIdIndex);
                                    }
                                }
                                MangoPermission permission = new MangoPermission(id, roleSet);
                                return permission;
                            }else {
                                return new MangoPermission(id);
                            }
                        }

                    });
        });
    }
    default MangoPermission getOrCreatePermission(MangoPermission permission) {
        return permissionCache().computeIfAbsent(permission, this::getOrCreatePermissionNoCache);
    }

    default MangoPermission getOrCreatePermissionNoCache(MangoPermission permission) {
        if (permission.getId() != null) {
            return permission;
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
            Integer id = getOrCreatePermission(mintermIds);
            MangoPermission saved = new MangoPermission(id, minterms);
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
        return roleCache().computeIfAbsent(role, this::getOrCreateRoleNoCache);
    }

    default Role getOrCreateRoleNoCache(Role role) {
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
                    new Object[]{xid, xid},
                    new int[]{Types.VARCHAR, Types.VARCHAR});
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

    /**
     * Convert an array of minterms (array of role xids) to an unsaved Mango permission.
     * The roles contained within the permission may not exist and will have an id of -1.
     * Used when deserializing an object from a data column in the database.
     *
     * @param permissionArray array of minterms
     * @return an unsaved mango permission without
     */
    static MangoPermission fromArray(String[][] permissionArray) {
        Set<Set<Role>> minterms = Arrays.stream(permissionArray).map(mt -> Arrays.stream(mt)
                .map(xid -> new Role(Common.NEW_ID, xid))
                .collect(Collectors.toSet())
        ).collect(Collectors.toSet());
        return new MangoPermission(minterms);
    }
}
