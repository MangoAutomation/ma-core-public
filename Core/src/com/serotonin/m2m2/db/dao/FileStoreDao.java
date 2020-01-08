/**
 * Copyright (C) 2018 Infinite Automation Software. All rights reserved.
 *
 */
package com.serotonin.m2m2.db.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.infiniteautomation.mango.spring.MangoRuntimeContextConfiguration;
import com.infiniteautomation.mango.spring.db.FileStoreTableDefinition;
import com.infiniteautomation.mango.spring.service.PermissionService;
import com.infiniteautomation.mango.util.LazyInitSupplier;
import com.serotonin.db.pair.IntStringPair;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.vo.FileStore;

/**
 *
 * @author Phillip Dunlap
 */
@Repository
public class FileStoreDao extends AbstractBasicDao<FileStore> {

    private static final LazyInitSupplier<FileStoreDao> springInstance = new LazyInitSupplier<>(() -> {
        FileStoreDao dao = Common.getRuntimeContext().getBean(FileStoreDao.class);
        if (dao == null)
            throw new IllegalStateException("DAO not initialized in Spring Runtime Context");
        return dao;
    });

    @Autowired
    private FileStoreDao(FileStoreTableDefinition table,
            @Qualifier(MangoRuntimeContextConfiguration.DAO_OBJECT_MAPPER_NAME)ObjectMapper mapper,
            ApplicationEventPublisher publisher) {
        super(table, mapper, publisher);
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
        return ejt.queryForObject(getSelectQuery().getSQL() + " WHERE storeName=?", new Object[] {storeName}, new int[] {Types.VARCHAR}, new FileStoreRowMapper(), null);
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
    protected Map<String, IntStringPair> getPropertiesMap() {
        return new HashMap<String, IntStringPair>();
    }

    @Override
    public RowMapper<FileStore> getRowMapper() {
        return new FileStoreRowMapper();
    }

    @Override
    public void saveRelationalData(FileStore vo, boolean insert) {
        //Replace the role mappings
        RoleDao.getInstance().replaceRolesOnVoPermission(vo.getReadRoles(), vo, PermissionService.READ, insert);
        RoleDao.getInstance().replaceRolesOnVoPermission(vo.getWriteRoles(), vo, PermissionService.WRITE, insert);

    }

    @Override
    public void loadRelationalData(FileStore vo) {
        //Populate permissions
        vo.setReadRoles(RoleDao.getInstance().getRoles(vo, PermissionService.READ));
        vo.setWriteRoles(RoleDao.getInstance().getRoles(vo, PermissionService.WRITE));
    }

    @Override
    public void deleteRelationalData(FileStore vo) {
        RoleDao.getInstance().deleteRolesForVoPermission(vo, PermissionService.READ);
        RoleDao.getInstance().deleteRolesForVoPermission(vo, PermissionService.EDIT);
    }

}
