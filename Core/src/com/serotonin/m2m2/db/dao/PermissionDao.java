/*
 * Copyright (C) 2020 Infinite Automation Software. All rights reserved.
 */
package com.serotonin.m2m2.db.dao;

import static com.serotonin.m2m2.db.dao.tables.MintermTable.*;
import static com.serotonin.m2m2.db.dao.tables.PermissionTable.*;
import static org.jooq.impl.DSL.*;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.jooq.exception.NoDataFoundException;
import org.springframework.stereotype.Repository;

import com.infiniteautomation.mango.permission.MangoPermission;
import com.serotonin.m2m2.vo.role.Role;

/**
 * @author Jared Wiltshire
 */
@Repository
public class PermissionDao extends BaseDao {

    public Integer getOrInsertPermission(MangoPermission permission) {
        Set<Set<Role>> minterms = permission.getRoles();
        if (minterms.isEmpty()) {
            return null;
        }

        return getTransactionTemplate().execute(txStatus -> {
            Set<Integer> mintermIds = new HashSet<>();

            for (Set<Role> minterm : minterms) {
                int id = getOrInsertMinterm(minterm);
                mintermIds.add(id);
            }

            try {
                return create.select(PERMISSIONS.id)
                        .from(PERMISSIONS)
                        .groupBy(PERMISSIONS.id)
                        .having(count(when(PERMISSIONS.minTermId.in(mintermIds), 1).else_((Integer) null)).equal(mintermIds.size()))
                        .limit(1)
                        .fetchSingle(0, Integer.class);
            } catch (NoDataFoundException e) {
                // fall through
            }

            Integer maxId = create.select(max(PERMISSIONS.id)).from(PERMISSIONS).fetchOne(0, Integer.class);
            if (maxId == null) {
                maxId = 0;
            }

            final int newId = maxId + 1;
            create.batch(
                    mintermIds.stream()
                    .map(id -> create.insertInto(PERMISSIONS)
                            .columns(PERMISSIONS.id, PERMISSIONS.minTermId)
                            .values(newId, id))
                    .collect(Collectors.toList()))
            .execute();

            return newId;
        });
    }

    private int getOrInsertMinterm(Set<Role> minterm) {
        List<Integer> roleIds = minterm.stream()
                .map(Role::getId)
                .collect(Collectors.toList());

        try {
            return create.select(MINTERMS.id)
                    .from(MINTERMS)
                    .groupBy(MINTERMS.id)
                    .having(count(when(MINTERMS.roleId.in(roleIds), 1).else_((Integer) null)).equal(minterm.size()))
                    .limit(1)
                    .fetchSingle(0, Integer.class);
        } catch (NoDataFoundException e) {
            // fall through
        }

        Integer maxId = create.select(max(MINTERMS.id)).from(MINTERMS).fetchOne(0, Integer.class);
        if (maxId == null) {
            maxId = 0;
        }

        final int newIdFinal = maxId + 1;
        create.batch(
                roleIds.stream()
                .map(id -> create.insertInto(MINTERMS)
                        .columns(MINTERMS.id, MINTERMS.roleId)
                        .values(newIdFinal, id))
                .collect(Collectors.toList()))
        .execute();

        return newIdFinal;
    }

}
