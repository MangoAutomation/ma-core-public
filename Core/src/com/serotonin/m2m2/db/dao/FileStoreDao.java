/**
 * Copyright (C) 2018 Infinite Automation Software. All rights reserved.
 *
 */
package com.serotonin.m2m2.db.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import com.infiniteautomation.mango.spring.service.PermissionService;
import com.infiniteautomation.mango.util.LazyInitSupplier;
import com.serotonin.db.pair.IntStringPair;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.module.FileStoreDefinition;
import com.serotonin.m2m2.module.ModuleRegistry;
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

    private FileStoreDao() {
        super("fs", new String[] {}, false,  new TranslatableMessage("internal.monitor.filestoreCount"));
    }

    public static FileStoreDao getInstance() {
        return springInstance.get();
    }

    public Map<String, FileStoreDefinition> getFileStoreMap() {
        List<FileStore> fileStores = getAll(true);
        Map<String, FileStoreDefinition> definitionsMap = new HashMap<String, FileStoreDefinition>();
        for(FileStore fs : fileStores)
            definitionsMap.put(fs.getStoreName(), fs.toDefinition());
        definitionsMap.putAll(ModuleRegistry.getFileStoreDefinitions());
        return definitionsMap;
    }

    public FileStoreDefinition getFileStoreDefinition(String storeName) {
        FileStoreDefinition fsd = ModuleRegistry.getFileStoreDefinition(storeName);
        if(fsd == null) {
            FileStore fs = ejt.queryForObject(SELECT_ALL + " WHERE storeName=?", new Object[] {storeName}, new int[] {Types.VARCHAR}, new FileStoreRowMapper(), null);
            if(fs == null)
                return null;
            return fs.toDefinition();
        }
        return fsd;
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
    protected String getTableName() {
        return SchemaDefinition.FILE_STORES_TABLE;
    }

    @Override
    protected Object[] voToObjectArray(FileStore vo) {
        return new Object[] {
                vo.getStoreName(),
        };
    }

    @Override
    protected LinkedHashMap<String, Integer> getPropertyTypeMap() {
        LinkedHashMap<String, Integer> map = new LinkedHashMap<String, Integer>();
        map.put("id", Types.INTEGER);
        map.put("storeName", Types.VARCHAR);
        return map;
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

}
