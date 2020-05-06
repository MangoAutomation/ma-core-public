/*
 * Copyright (C) 2020 Infinite Automation Software. All rights reserved.
 */
package com.serotonin.m2m2.db.dao;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.jooq.Field;
import org.jooq.Record;
import org.jooq.Table;
import org.jooq.exception.NoDataFoundException;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;
import org.springframework.stereotype.Repository;

import com.infiniteautomation.mango.permission.MangoPermission;
import com.serotonin.m2m2.vo.role.Role;

/**
 * @author Jared Wiltshire
 */
@Repository
public class PermissionDao extends BaseDao {

    public static final Table<Record> PERMISSIONS = DSL.table(DSL.name("permissions"));
    public static final Field<Integer> PERMISSION_ID = DSL.field(PERMISSIONS.getQualifiedName().append("id"), SQLDataType.INTEGER.nullable(false));
    public static final Field<Integer> PERMISSION_MINTERM_ID = DSL.field(PERMISSIONS.getQualifiedName().append("min_term_id"), SQLDataType.INTEGER.nullable(false));

    public static final Table<Record> MINTERMS = DSL.table(DSL.name("permissions_minterms"));
    public static final Field<Integer> MINTERM_ID = DSL.field(MINTERMS.getQualifiedName().append("id"), SQLDataType.INTEGER.nullable(false));
    public static final Field<Integer> MINTERM_ROLE_ID = DSL.field(MINTERMS.getQualifiedName().append("role_id"), SQLDataType.INTEGER.nullable(false));

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
                return create.select(PERMISSION_ID)
                        .from(PERMISSIONS)
                        .groupBy(PERMISSION_ID)
                        .having(DSL.count(DSL.when(PERMISSION_MINTERM_ID.in(mintermIds), 1).else_((Integer) null)).equal(mintermIds.size()))
                        .limit(1)
                        .fetchSingle(0, Integer.class);
            } catch (NoDataFoundException e) {
                // fall through
            }

            Integer maxId = create.select(DSL.max(PERMISSION_ID)).from(PERMISSIONS).fetchOne(0, Integer.class);
            if (maxId == null) {
                maxId = 0;
            }

            final int newId = maxId + 1;
            create.batch(
                    mintermIds.stream()
                    .map(id -> create.insertInto(PERMISSIONS).columns(PERMISSION_ID, PERMISSION_MINTERM_ID).values(newId, id))
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
            return create.select(MINTERM_ID)
                    .from(MINTERMS)
                    .groupBy(MINTERM_ID)
                    .having(DSL.count(DSL.when(MINTERM_ROLE_ID.in(roleIds), 1).else_((Integer) null)).equal(minterm.size()))
                    .limit(1)
                    .fetchSingle(0, Integer.class);
        } catch (NoDataFoundException e) {
            // fall through
        }

        Integer maxId = create.select(DSL.max(MINTERM_ID)).from(MINTERMS).fetchOne(0, Integer.class);
        if (maxId == null) {
            maxId = 0;
        }

        final int newIdFinal = maxId + 1;
        create.batch(
                roleIds.stream()
                .map(id -> create.insertInto(MINTERMS).columns(MINTERM_ID, MINTERM_ROLE_ID).values(newIdFinal, id))
                .collect(Collectors.toList()))
        .execute();

        return newIdFinal;
    }

}
