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
import org.jooq.Field;
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
import com.infiniteautomation.mango.spring.MangoRuntimeContextConfiguration;
import com.infiniteautomation.mango.spring.db.FileStoreTableDefinition;
import com.infiniteautomation.mango.spring.db.RoleTableDefinition;
import com.infiniteautomation.mango.spring.db.RoleTableDefinition.GrantedAccess;
import com.infiniteautomation.mango.spring.service.PermissionService;
import com.infiniteautomation.mango.util.LazyInitSupplier;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.vo.FileStore;
import com.serotonin.m2m2.vo.permission.PermissionHolder;

/**
 *
 * @author Phillip Dunlap
 */
@Repository
public class FileStoreDao extends AbstractBasicDao<FileStore, FileStoreTableDefinition> {

    private static final LazyInitSupplier<FileStoreDao> springInstance = new LazyInitSupplier<>(() -> {
        FileStoreDao dao = Common.getRuntimeContext().getBean(FileStoreDao.class);
        if (dao == null)
            throw new IllegalStateException("DAO not initialized in Spring Runtime Context");
        return dao;
    });

    private final PermissionService permissionService;

    @Autowired
    private FileStoreDao(FileStoreTableDefinition table,
            @Qualifier(MangoRuntimeContextConfiguration.DAO_OBJECT_MAPPER_NAME)ObjectMapper mapper,
            ApplicationEventPublisher publisher,
            PermissionService permissionService) {
        super(table, mapper, publisher);
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
            return result;
        }

    }

    @Override
    protected Object[] voToObjectArray(FileStore vo) {
        return new Object[] {
                vo.getStoreName(),
        };
    }

    @Override
    public RowMapper<FileStore> getRowMapper() {
        return new FileStoreRowMapper();
    }

    @Override
    public void saveRelationalData(FileStore vo, boolean insert) {
        //Replace the role mappings
        RoleDao.getInstance().replaceRolesOnVoPermission(vo.getReadPermission(), vo, PermissionService.READ, insert);
        RoleDao.getInstance().replaceRolesOnVoPermission(vo.getWritePermission(), vo, PermissionService.WRITE, insert);

    }

    @Override
    public void loadRelationalData(FileStore vo) {
        //Populate permissions
        vo.setReadPermission(RoleDao.getInstance().getPermission(vo, PermissionService.READ));
        vo.setWritePermission(RoleDao.getInstance().getPermission(vo, PermissionService.WRITE));
    }

    @Override
    public void deleteRelationalData(FileStore vo) {
        RoleDao.getInstance().deleteRolesForVoPermission(vo, PermissionService.READ);
        RoleDao.getInstance().deleteRolesForVoPermission(vo, PermissionService.WRITE);
    }

    @Override
    public <R extends Record> SelectJoinStep<R> joinPermissions(SelectJoinStep<R> select, ConditionSortLimit conditions,
            PermissionHolder user) {
        //Join on permissions
        if(!permissionService.hasAdminRole(user)) {
            List<Integer> roleIds = user.getAllInheritedRoles().stream().map(r -> r.getId()).collect(Collectors.toList());

            Condition roleIdsIn = RoleTableDefinition.roleIdField.in(roleIds);
            Field<Boolean> granted = new GrantedAccess(RoleTableDefinition.maskField, roleIdsIn);

            Table<?> readSubselect = this.create.select(
                    RoleTableDefinition.voIdField,
                    DSL.inline(1).as("granted"))
                    .from(RoleTableDefinition.ROLE_MAPPING_TABLE)
                    .where(RoleTableDefinition.voTypeField.eq(FileStore.class.getSimpleName()),
                            RoleTableDefinition.permissionTypeField.eq(PermissionService.READ))
                    .groupBy(RoleTableDefinition.voIdField)
                    .having(granted)
                    .asTable("fileStoreRead");

            select = select.leftJoin(readSubselect).on(this.table.getIdAlias().eq(readSubselect.field(RoleTableDefinition.voIdField)));

            Table<?> writeSubselect = this.create.select(
                    RoleTableDefinition.voIdField,
                    DSL.inline(1).as("granted"))
                    .from(RoleTableDefinition.ROLE_MAPPING_TABLE)
                    .where(RoleTableDefinition.voTypeField.eq(FileStore.class.getSimpleName()),
                            RoleTableDefinition.permissionTypeField.eq(PermissionService.WRITE))
                    .groupBy(RoleTableDefinition.voIdField)
                    .having(granted)
                    .asTable("fileStoreWrite");

            select = select.leftJoin(writeSubselect).on(this.table.getIdAlias().eq(writeSubselect.field(RoleTableDefinition.voIdField)));

            conditions.addCondition(DSL.or(
                    readSubselect.field("granted").isTrue(),
                    writeSubselect.field("granted").isTrue()));
        }
        return select;
    }

}
