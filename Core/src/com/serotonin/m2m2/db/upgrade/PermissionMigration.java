/**
 * Copyright (C) 2020  Infinite Automation Software. All rights reserved.
 */

package com.serotonin.m2m2.db.upgrade;

import static com.serotonin.m2m2.db.dao.tables.MintermMappingTable.MINTERMS_MAPPING;
import static com.serotonin.m2m2.db.dao.tables.MintermTable.MINTERMS;
import static com.serotonin.m2m2.db.dao.tables.PermissionMappingTable.PERMISSIONS_MAPPING;
import static com.serotonin.m2m2.db.dao.tables.PermissionTable.PERMISSIONS;
import static org.jooq.impl.DSL.count;
import static org.jooq.impl.DSL.default_;
import static org.jooq.impl.DSL.when;

import java.io.OutputStream;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.jooq.DSLContext;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.infiniteautomation.mango.permission.MangoPermission;
import com.infiniteautomation.mango.util.Functions;
import com.serotonin.db.spring.ExtendedJdbcTemplate;
import com.serotonin.m2m2.vo.permission.PermissionHolder;
import com.serotonin.m2m2.vo.role.Role;

/**
 *
 * @author Terry Packer
 */
public interface PermissionMigration {

    /**
     * Get an initalized ejt
     * @return
     */
    public ExtendedJdbcTemplate getEjt();

    public PlatformTransactionManager getTransactionManager();

    public DSLContext getCreate();

    void runScript(Map<String, String[]> scripts, OutputStream out);

    public default TransactionTemplate getTransactionTemplate() {
        return new TransactionTemplate(getTransactionManager());
    }

    /**
     * Get all existing roles so we can ensure we don't create duplicate roles only new mappings
     * @return
     */
    default Map<String, Role> getExistingRoles() {
        return getEjt().query("SELECT id,xid FROM roles", new ResultSetExtractor<Map<String, Role>>() {

            @Override
            public Map<String, Role> extractData(ResultSet rs) throws SQLException, DataAccessException {
                Map<String, Role> mappings = new HashMap<>();
                while(rs.next()) {
                    int id = rs.getInt(1);
                    String xid = rs.getString(2);
                    mappings.put(xid, new Role(id, xid));
                }
                return mappings;
            }
        });
    }

    /**
     * Ensure role exists and insert OR mappings for this permission
     *  this is protected for use in modules for this upgrade
     *
     * @param existingPermissions
     * @param roles
     * @return
     */
    default Integer insertMapping(Set<String> existingPermissions, Map<String, Role> roles) {
        //Ensure each role is only used 1x for this permission
        Set<Set<Role>> permissionOrSet = new HashSet<>();
        for(String permission : existingPermissions) {
            //ensure all roles are lower case and don't have spaces on the ends
            permission = permission.trim();
            String role = permission.toLowerCase();
            roles.compute(role, (k,r) -> {
                if(r == null) {
                    r = new Role(getEjt().doInsert("INSERT INTO roles (xid, name) values (?,?)", new Object[] {role, role}), role);
                }
                //Add an or mapping
                permissionOrSet.add(Collections.singleton(r));
                return r;
            });
        }
        MangoPermission mangoPermission = new MangoPermission(permissionOrSet);
        return permissionId(mangoPermission);
    }


    /**
     * For use by modules in this upgrade
     * @param groups
     * @return
     */
    default Set<String> explodePermissionGroups(String groups) {
        if (groups == null || groups.isEmpty()) {
            return Collections.emptySet();
        }

        Set<String> set = new HashSet<>();
        for (String s : groups.split(",")) {
            s = s.replaceAll(Functions.WHITESPACE_REGEX, "");
            if (!s.isEmpty()) {
                set.add(s);
            }
        }
        return Collections.unmodifiableSet(set);
    }

    default Integer permissionId(MangoPermission permission) {
        if (permission.getRoles().isEmpty()) {
            return getOrInsertPermission(MangoPermission.createOrSet(PermissionHolder.SUPERADMIN_ROLE));
        }

        return getTransactionTemplate().execute(txStatus -> {
            return getOrInsertPermission(permission);
        });
    }

    default Integer getOrInsertPermission(MangoPermission permission) {
        Set<Integer> mintermIds = permission.getRoles().stream()
                .map(this::getOrInsertMinterm)
                .collect(Collectors.toSet());

        Integer permissionId = getCreate().select(PERMISSIONS_MAPPING.permissionId)
                .from(PERMISSIONS_MAPPING)
                .groupBy(PERMISSIONS_MAPPING.permissionId)
                .having(count(when(PERMISSIONS_MAPPING.mintermId.in(mintermIds), 1).else_((Integer) null)).equal(mintermIds.size()))
                .limit(1)
                .fetchOne(0, Integer.class);

        // no matching permission exists already, insert a new one
        if (permissionId == null) {
            permissionId = getCreate().insertInto(PERMISSIONS, PERMISSIONS.id)
                    .values(default_(PERMISSIONS.id))
                    .returningResult(PERMISSIONS.id)
                    .fetchOne().get(PERMISSIONS.id);

            int permissionIdFinal = permissionId;
            getCreate().batch(
                    mintermIds.stream()
                    .map(id -> getCreate().insertInto(PERMISSIONS_MAPPING)
                            .columns(PERMISSIONS_MAPPING.permissionId, PERMISSIONS_MAPPING.mintermId)
                            .values(permissionIdFinal, id))
                    .collect(Collectors.toList()))
            .execute();
        }

        return permissionId;
    }

    default int getOrInsertMinterm(Set<Role> minterm) {
        Set<Integer> roleIds = minterm.stream()
                .map(Role::getId)
                .collect(Collectors.toSet());

        Integer mintermId = getCreate().select(MINTERMS_MAPPING.mintermId)
                .from(MINTERMS_MAPPING)
                .groupBy(MINTERMS_MAPPING.mintermId)
                .having(count(when(MINTERMS_MAPPING.roleId.in(roleIds), 1).else_((Integer) null)).equal(minterm.size()))
                .limit(1)
                .fetchOne(0, Integer.class);

        // no matching minterm exists already, insert a new one
        if (mintermId == null) {
            mintermId = getCreate().insertInto(MINTERMS, MINTERMS.id)
                    .values(default_(MINTERMS.id))
                    .returningResult(MINTERMS.id)
                    .fetchOne().get(PERMISSIONS.id);

            int mintermIdFinal = mintermId;
            getCreate().batch(
                    roleIds.stream()
                    .map(id -> getCreate().insertInto(MINTERMS_MAPPING)
                            .columns(MINTERMS_MAPPING.mintermId, MINTERMS_MAPPING.roleId)
                            .values(mintermIdFinal, id))
                    .collect(Collectors.toList()))
            .execute();
        }

        return mintermId;
    }
}
