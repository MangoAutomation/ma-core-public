/*
 * Copyright (C) 2020 Infinite Automation Software. All rights reserved.
 */
package com.serotonin.m2m2.db.dao;

import static com.serotonin.m2m2.db.dao.tables.MintermMappingTable.MINTERMS_MAPPING;
import static com.serotonin.m2m2.db.dao.tables.MintermTable.MINTERMS;
import static com.serotonin.m2m2.db.dao.tables.PermissionMappingTable.PERMISSIONS_MAPPING;
import static com.serotonin.m2m2.db.dao.tables.PermissionTable.PERMISSIONS;
import static org.jooq.impl.DSL.count;
import static org.jooq.impl.DSL.default_;
import static org.jooq.impl.DSL.when;

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

import com.infiniteautomation.mango.permission.MangoPermission;
import com.serotonin.m2m2.vo.role.Role;

/**
 * @author Jared Wiltshire
 */
@Repository
public class PermissionDao extends BaseDao {

    private final RoleDao roleDao;

    @Autowired
    PermissionDao(RoleDao roleDao) {
        this.roleDao = roleDao;
    }

    /**
     * Get a MangoPermission by id
     * @param id
     * @return
     */
    public MangoPermission get(Integer id) {
        List<Field<?>> fields = new ArrayList<>();
        fields.add(roleDao.getTable().getAlias("id"));
        fields.add(roleDao.getTable().getAlias("xid"));
        fields.add(PERMISSIONS_MAPPING.mintermId);

        SelectSeekStep2<Record, Integer, Integer> select = create.select(fields).from(PERMISSIONS_MAPPING)
                .join(MINTERMS_MAPPING).on(PERMISSIONS_MAPPING.mintermId.eq(MINTERMS_MAPPING.mintermId))
                .join(roleDao.getTable().getTableAsAlias()).on(roleDao.getTable().getAlias("id").eq(MINTERMS_MAPPING.roleId))
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
                    MangoPermission permission = new MangoPermission(roleSet);
                    permission.setId(id);
                    return permission;
                }else {
                    return null;
                }
            }

        });
    }

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

}