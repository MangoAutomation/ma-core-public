/**
 * Copyright (C) 2018 Infinite Automation Software. All rights reserved.
 *
 */
package com.serotonin.m2m2.db.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.List;
import java.util.stream.Collectors;

import org.jooq.Condition;
import org.jooq.Record;
import org.jooq.SelectJoinStep;
import org.jooq.Table;
import org.jooq.impl.DSL;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.infiniteautomation.mango.db.query.ConditionSortLimit;
import com.infiniteautomation.mango.permission.MangoPermission;
import com.infiniteautomation.mango.spring.MangoRuntimeContextConfiguration;
import com.infiniteautomation.mango.spring.db.FileStoreTableDefinition;
import com.infiniteautomation.mango.spring.db.RoleTableDefinition;
import com.infiniteautomation.mango.spring.service.PermissionService;
import com.infiniteautomation.mango.util.LazyInitSupplier;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.dao.tables.MintermMappingTable;
import com.serotonin.m2m2.db.dao.tables.PermissionMappingTable;
import com.serotonin.m2m2.vo.FileStore;
import com.serotonin.m2m2.vo.permission.PermissionHolder;

/**
 *
 * @author Phillip Dunlap
 */
@Repository
public class FileStoreDao extends AbstractBasicDao<FileStore, FileStoreTableDefinition> {

    private static final LazyInitSupplier<FileStoreDao> springInstance = new LazyInitSupplier<>(() -> {
        return Common.getRuntimeContext().getBean(FileStoreDao.class);
    });

    private final PermissionDao permissionDao;
    private final PermissionService permissionService;

    @Autowired
    private FileStoreDao(FileStoreTableDefinition table,
            @Qualifier(MangoRuntimeContextConfiguration.DAO_OBJECT_MAPPER_NAME)ObjectMapper mapper,
            ApplicationEventPublisher publisher,
            PermissionDao permissionDao,
            PermissionService permissionService) {
        super(table, mapper, publisher);
        this.permissionDao = permissionDao;
        this.permissionService = permissionService;
    }

    public static FileStoreDao getInstance() {
        return springInstance.get();
    }

    /**
     * Get a store by name
     * @param storeName
     * @return
     */
    public FileStore getByName(String storeName) {
        return ejt.queryForObject(getJoinedSelectQuery().getSQL() + " WHERE storeName=?", new Object[] {storeName}, new int[] {Types.VARCHAR}, new FileStoreRowMapper(), null);
    }

    private class FileStoreRowMapper implements RowMapper<FileStore> {

        @Override
        public FileStore mapRow(ResultSet rs, int rowNum) throws SQLException {
            int i = 0;
            FileStore result = new FileStore();
            result.setId(rs.getInt(++i));
            result.setStoreName(rs.getString(++i));
            result.setReadPermission(new MangoPermission(rs.getInt(++i)));
            result.setWritePermission(new MangoPermission(rs.getInt(++i)));
            return result;
        }

    }

    @Override
    protected Object[] voToObjectArray(FileStore vo) {
        return new Object[] {
                vo.getStoreName(),
                vo.getReadPermission().getId(),
                vo.getWritePermission().getId()
        };
    }

    @Override
    public RowMapper<FileStore> getRowMapper() {
        return new FileStoreRowMapper();
    }

    @Override
    public void savePreRelationalData(FileStore existing, FileStore vo) {
        permissionDao.permissionId(vo.getReadPermission());
        permissionDao.permissionId(vo.getWritePermission());
    }

    @Override
    public void saveRelationalData(FileStore existing, FileStore vo) {
        if(existing != null) {
            if(!existing.getReadPermission().equals(vo.getReadPermission())) {
                permissionDao.permissionDeleted(existing.getReadPermission());
            }
            if(!existing.getWritePermission().equals(vo.getWritePermission())) {
                permissionDao.permissionDeleted(existing.getWritePermission());
            }
        }
    }

    @Override
    public void loadRelationalData(FileStore vo) {
        //Populate permissions
        vo.setReadPermission(permissionDao.get(vo.getReadPermission().getId()));
        vo.setWritePermission(permissionDao.get(vo.getWritePermission().getId()));
    }

    @Override
    public void deletePostRelationalData(FileStore vo) {
        //Clean permissions
        permissionDao.permissionDeleted(vo.getReadPermission(), vo.getWritePermission());
    }

    @Override
    public <R extends Record> SelectJoinStep<R> joinPermissions(SelectJoinStep<R> select, ConditionSortLimit conditions,
            PermissionHolder user) {
        if(!permissionService.hasAdminRole(user)) {
            List<Integer> roleIds = user.getAllInheritedRoles().stream().map(r -> r.getId()).collect(Collectors.toList());

            Condition roleIdsIn = RoleTableDefinition.roleIdField.in(roleIds);

            Table<?> mintermsGranted = this.create.select(MintermMappingTable.MINTERMS_MAPPING.mintermId)
                    .from(MintermMappingTable.MINTERMS_MAPPING)
                    .groupBy(MintermMappingTable.MINTERMS_MAPPING.mintermId)
                    .having(DSL.count().eq(DSL.count(
                            DSL.case_().when(roleIdsIn, DSL.inline(1))
                            .else_(DSL.inline((Integer)null))))).asTable("mintermsGranted");

            Table<?> permissionsGranted = this.create.selectDistinct(PermissionMappingTable.PERMISSIONS_MAPPING.permissionId)
                    .from(PermissionMappingTable.PERMISSIONS_MAPPING)
                    .join(mintermsGranted).on(mintermsGranted.field(MintermMappingTable.MINTERMS_MAPPING.mintermId).eq(PermissionMappingTable.PERMISSIONS_MAPPING.mintermId))
                    .asTable("permissionsGranted");

            select = select.join(permissionsGranted).on(
                    permissionsGranted.field(PermissionMappingTable.PERMISSIONS_MAPPING.permissionId).in(
                            FileStoreTableDefinition.READ_PERMISSION_ALIAS, FileStoreTableDefinition.WRITE_PERMISSION_ALIAS));

        }
        return select;
    }

}
