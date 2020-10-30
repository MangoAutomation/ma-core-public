/*
 * Copyright (C) 2020 Infinite Automation Software. All rights reserved.
 */
package com.serotonin.m2m2.db.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.jooq.Field;
import org.jooq.Record;
import org.jooq.SelectSeekStep2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import com.infiniteautomation.mango.permission.MangoPermission;
import com.infiniteautomation.mango.spring.db.RoleTableDefinition;
import com.serotonin.m2m2.vo.role.Role;

import static com.serotonin.m2m2.db.dao.tables.MintermMappingTable.MINTERMS_MAPPING;
import static com.serotonin.m2m2.db.dao.tables.MintermTable.MINTERMS;
import static com.serotonin.m2m2.db.dao.tables.PermissionMappingTable.PERMISSIONS_MAPPING;
import static com.serotonin.m2m2.db.dao.tables.PermissionTable.PERMISSIONS;
import static org.jooq.impl.DSL.*;

/**
 * NOTE: Permissions are cached, usage of tjos dao should be limited to within the PermissionService
 * @author Jared Wiltshire
 */
@Repository
public class PermissionDao extends BaseDao {

    private final RoleTableDefinition roleTable;

    @Autowired
    PermissionDao(RoleTableDefinition roleTable) {
        this.roleTable = roleTable;
    }

    /**
     * Get a MangoPermission by id
     * @param id
     * @return permission if found or null
     */
    public MangoPermission get(Integer id) {
        //TODO Mango 4.0 improve performance
        //Fist check to see if it exists as it may have no minterms
        Integer foundId = create.select(PERMISSIONS.id).from(PERMISSIONS).where(PERMISSIONS.id.equal(id)).fetchOneInto(Integer.class);
        if(foundId == null) {
            return null;
        }

        List<Field<?>> fields = new ArrayList<>();
        fields.add(roleTable.getAlias("id"));
        fields.add(roleTable.getAlias("xid"));
        fields.add(PERMISSIONS_MAPPING.mintermId);

        SelectSeekStep2<Record, Integer, Integer> select = create.select(fields).from(PERMISSIONS_MAPPING)
                .join(MINTERMS_MAPPING).on(PERMISSIONS_MAPPING.mintermId.eq(MINTERMS_MAPPING.mintermId))
                .join(roleTable.getTableAsAlias()).on(roleTable.getAlias("id").eq(MINTERMS_MAPPING.roleId))
                .where(PERMISSIONS_MAPPING.permissionId.eq(id))
                .orderBy(PERMISSIONS_MAPPING.permissionId.asc(), PERMISSIONS_MAPPING.mintermId.asc());

        String sql = select.getSQL();
        List<Object> arguments = select.getBindValues();
        Object[] argumentsArray = arguments.toArray(new Object[arguments.size()]);

        return this.query(sql, argumentsArray, new ResultSetExtractor<MangoPermission>() {

            private int roleIdIndex = 1;
            private int roleXidIndex = 2;
            private int minterIdIndex = 3;
            @Override
            public MangoPermission extractData(ResultSet rs)
                    throws SQLException, DataAccessException {

                if(rs.next()) {
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
    }

    /**
     * Find the Permission id of the combination of these minterms or create one that matches
     *
     * @param minterms
     * @return
     */
    public Integer permissionId(Set<Set<Role>> minterms) {
        // We need to always do this in a new transaction as this ends up in the cache. Otherwise if we are in a
        // nested transaction the permission ID will end up in the cache before the transaction is committed. The
        // caveats of this are:
        // a) if the outer transaction is rolled back we may be left with a dangling permission that is not used
        // b) the permission may get deleted before the outer transaction is committed
        TransactionTemplate txTemplate = new TransactionTemplate(getTransactionManager(),
                new DefaultTransactionDefinition(TransactionDefinition.PROPAGATION_REQUIRES_NEW));
        return txTemplate.execute(txStatus -> getOrInsertPermission(minterms));
    }

    private Integer getOrInsertPermission(Set<Set<Role>> minterms) {
        //TODO Mango 4.0 Optimize this whole method
        Set<Integer> mintermIds = minterms.stream()
                .map(this::getOrInsertMinterm)
                .collect(Collectors.toSet());

        Integer permissionId;
        if(minterms.isEmpty()) {
            permissionId = create.select(PERMISSIONS.id).from(PERMISSIONS)
                    .leftJoin(PERMISSIONS_MAPPING).on(PERMISSIONS.id.eq(PERMISSIONS_MAPPING.permissionId))
                    .where(PERMISSIONS_MAPPING.permissionId.isNull()).limit(1).fetchOne(0, Integer.class);
        }else {
            permissionId = create.select(PERMISSIONS_MAPPING.permissionId)
                    .from(PERMISSIONS_MAPPING)
                    .groupBy(PERMISSIONS_MAPPING.permissionId)
                    .having(count(when(PERMISSIONS_MAPPING.mintermId.in(mintermIds), 1).else_((Integer) null)).equal(mintermIds.size()),
                            count(PERMISSIONS_MAPPING.permissionId).equal(mintermIds.size()))
                    .limit(1)
                    .fetchOne(0, Integer.class);
        }

        // no matching permission exists already, insert a new one
        if (permissionId == null) {
            permissionId = create.insertInto(PERMISSIONS, PERMISSIONS.id)
                    .values(default_(PERMISSIONS.id))
                    .returningResult(PERMISSIONS.id)
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
        if (minterm.isEmpty()) {
            throw new IllegalArgumentException("Minterm should never be empty");
        }

        Set<Integer> roleIds = minterm.stream()
                .map(Role::getId)
                .collect(Collectors.toSet());

        Integer mintermId = create.select(MINTERMS_MAPPING.mintermId)
                .from(MINTERMS_MAPPING)
                .groupBy(MINTERMS_MAPPING.mintermId)
                .having(count(when(MINTERMS_MAPPING.roleId.in(roleIds), 1).else_((Integer) null)).equal(minterm.size()),
                        count(MINTERMS_MAPPING.roleId).equal(roleIds.size()))
                .limit(1)
                .fetchOne(0, Integer.class);

        // no matching minterm exists already, insert a new one
        if (mintermId == null) {
            mintermId = create.insertInto(MINTERMS, MINTERMS.id)
                    .values(default_(MINTERMS.id))
                    .returningResult(MINTERMS.id)
                    .fetchOne().get(PERMISSIONS.id);

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

    public void roleUnlinked() {
        //Clean up minterms that are orphaned (i.e. belong to no role)
        create.deleteFrom(MINTERMS).where(
                MINTERMS.id.in(
                        create.select(MINTERMS.id).from(MINTERMS)
                        .leftJoin(MINTERMS_MAPPING).on(MINTERMS_MAPPING.mintermId.eq(MINTERMS.id))
                        .where(MINTERMS_MAPPING.mintermId.isNull()))).execute();

    }

    /**
     * Clean up references from an unlinked permission
     */
    private void permissionUnlinked() {
        //Clean up minterms that are orphaned (i.e. belong to no permission)
        create.deleteFrom(MINTERMS).where(
                MINTERMS.id.in(
                        create.select(MINTERMS.id).from(MINTERMS)
                        .leftJoin(PERMISSIONS_MAPPING).on(PERMISSIONS_MAPPING.mintermId.eq(MINTERMS.id))
                        .where(PERMISSIONS_MAPPING.permissionId.isNull()))).execute();
    }

    /**
     * Attempt to delete a permission, will not succeed if another object references the permission (via foreign key).
     * @param permissionId
     * @return true if the permission was deleted
     */
    public boolean deletePermission(Integer permissionId) {
        TransactionTemplate txTemplate = new TransactionTemplate(getTransactionManager(),
                new DefaultTransactionDefinition(TransactionDefinition.PROPAGATION_REQUIRES_NEW));

        try {
            txTemplate.execute(txStatus -> {
                if (create.deleteFrom(PERMISSIONS).where(PERMISSIONS.id.eq(permissionId)).execute() > 0) {
                    permissionUnlinked();
                }
                return null;
            });
        } catch (Exception e) {
            return false;
        }

        return true;
    }
}
