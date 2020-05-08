/*
 * Copyright (C) 2020 Infinite Automation Software. All rights reserved.
 */
package com.serotonin.m2m2.db.dao;

import static com.serotonin.m2m2.db.dao.tables.MintermMappingTable.*;
import static com.serotonin.m2m2.db.dao.tables.MintermTable.*;
import static com.serotonin.m2m2.db.dao.tables.PermissionMappingTable.*;
import static com.serotonin.m2m2.db.dao.tables.PermissionTable.*;
import static org.jooq.impl.DSL.*;

import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Repository;

import com.infiniteautomation.mango.permission.MangoPermission;
import com.serotonin.m2m2.vo.role.Role;

/**
 * @author Jared Wiltshire
 */
@Repository
public class PermissionDao extends BaseDao {

    public Integer permissionId(MangoPermission permission) {
        if (permission.getRoles().isEmpty()) {
            return null;
        }

        return getTransactionTemplate().execute(txStatus -> {
            return getOrInsertPermission(permission);
        });
    }

    private Integer getOrInsertPermission(MangoPermission permission) {
        Set<Integer> mintermIds = permission.getRoles().stream()
                .map(this::getOrInsertMinterm)
                .collect(Collectors.toSet());

        Integer permissionId = create.select(PERMISSIONS_MAPPING.permissionId)
                .from(PERMISSIONS_MAPPING)
                .groupBy(PERMISSIONS_MAPPING.permissionId)
                .having(count(when(PERMISSIONS_MAPPING.mintermId.in(mintermIds), 1).else_((Integer) null)).equal(mintermIds.size()))
                .limit(1)
                .fetchOne(0, Integer.class);

        // no matching permission exists already, insert a new one
        if (permissionId == null) {
            permissionId = create.insertInto(PERMISSIONS)
                    .columns(PERMISSIONS.id)
                    .values(defaultValue(PERMISSIONS.id))
                    .returning(PERMISSIONS.id)
                    .fetchOne().get(PERMISSIONS.id);

            int permissionIdFinal = permissionId;
            create.batch(
                    mintermIds.stream()
                    .map(id -> create.insertInto(PERMISSIONS_MAPPING)
                            .columns(PERMISSIONS_MAPPING.permissionId, PERMISSIONS_MAPPING.mintermId)
                            .values(permissionIdFinal, id))
                    .collect(Collectors.toList()))
            .execute();
        }

        return permissionId;
    }

    private int getOrInsertMinterm(Set<Role> minterm) {
        Set<Integer> roleIds = minterm.stream()
                .map(Role::getId)
                .collect(Collectors.toSet());

        Integer mintermId = create.select(MINTERMS_MAPPING.mintermId)
                .from(MINTERMS_MAPPING)
                .groupBy(MINTERMS_MAPPING.mintermId)
                .having(count(when(MINTERMS_MAPPING.roleId.in(roleIds), 1).else_((Integer) null)).equal(minterm.size()))
                .limit(1)
                .fetchOne(0, Integer.class);

        // no matching minterm exists already, insert a new one
        if (mintermId == null) {
            mintermId = create.insertInto(MINTERMS)
                    .columns(MINTERMS.id)
                    .values(defaultValue(MINTERMS.id))
                    .returning(MINTERMS.id)
                    .fetchOne().get(MINTERMS.id);

            int mintermIdFinal = mintermId;
            create.batch(
                    roleIds.stream()
                    .map(id -> create.insertInto(MINTERMS_MAPPING)
                            .columns(MINTERMS_MAPPING.mintermId, MINTERMS_MAPPING.roleId)
                            .values(mintermIdFinal, id))
                    .collect(Collectors.toList()))
            .execute();
        }

        return mintermId;
    }

}
