/**
 * Copyright (C) 2018 Infinite Automation Software. All rights reserved. 
 *
 */
package com.serotonin.m2m2.db.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.RowMapper;

import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.module.FileStoreDefinition;
import com.serotonin.m2m2.module.ModuleRegistry;
import com.serotonin.m2m2.vo.FileStore;

/**
 *
 * @author Phillip Dunlap
 */
public class FileStoreDao extends BaseDao {
    public static final FileStoreDao instance = new FileStoreDao();
    
    private static final String SELECT_FILE_STORE_DEFINITIONS = "SELECT storeName, readPermission, writePermission FROM fileStores ";
    
    public List<FileStore> getUserFileStores() {
        return ejt.query(SELECT_FILE_STORE_DEFINITIONS, new FileStoreRowMapper());
    }
    
    public FileStore getUserFileStore(String storeName) {
        return ejt.queryForObject(SELECT_FILE_STORE_DEFINITIONS + " where storeName=?", new Object[] {storeName}, new int[] {Types.VARCHAR}, new FileStoreRowMapper(), null);
    }
    
    public FileStore getUserFileStoreById(int storeId) {
        return ejt.queryForObject(SELECT_FILE_STORE_DEFINITIONS + " where id=?", new Object[] {storeId}, new int[] {Types.INTEGER}, new FileStoreRowMapper(), null);
    }
    
    public Map<String, FileStoreDefinition> getFileStoreMap() {
        List<FileStore> fileStores = getUserFileStores();
        Map<String, FileStoreDefinition> definitionsMap = new HashMap<String, FileStoreDefinition>();
        for(FileStore fs : fileStores)
            definitionsMap.put(fs.getStoreName(), fs.toDefinition());
        definitionsMap.putAll(ModuleRegistry.getFileStoreDefinitions());
        return definitionsMap;
    }
    
    public FileStoreDefinition getFileStoreDefinition(String storeName) {
        FileStoreDefinition fsd = ModuleRegistry.getFileStoreDefinition(storeName);
        if(fsd == null) {
            FileStore fs = ejt.queryForObject(SELECT_FILE_STORE_DEFINITIONS + " where storeName=?", new Object[] {storeName}, new int[] {Types.VARCHAR}, new FileStoreRowMapper(), null);
            if(fs == null)
                return null;
            return fs.toDefinition();
        }
        return fsd; 
    }
    
    private class FileStoreRowMapper implements RowMapper<FileStore> {

        /* (non-Javadoc)
         * @see org.springframework.jdbc.core.RowMapper#mapRow(java.sql.ResultSet, int)
         */
        @Override
        public FileStore mapRow(ResultSet rs, int rowNum) throws SQLException {
            int i = 0;
            FileStore result = new FileStore();
            result.setStoreName(rs.getString(++i));
            result.setReadPermission(rs.getString(++i));
            result.setWritePermission(rs.getString(++i));
            return result;
        }
        
    }
    
    private static final String INSERT_FILE_STORE = "INSERT INTO fileStores (storeName, readPermission, writePermission) values (?, ?, ?)";
    private static final String UPDATE_FILE_STORE = "UPDATE fileStores SET storeName=?, readPermission=?, writePermission=? where id=?";
    private static final String DELETE_FILE_STORE = "DELETE FROM fileStores WHERE ";
    
    public int saveFileStore(FileStore fs) {
        if(fs.getId() == Common.NEW_ID)
            insert(fs);
        else
            update(fs);
        return fs.getId();
    }
    
    private void insert(FileStore fs) {
        fs.setId(ejt.doInsert(INSERT_FILE_STORE, new Object[] {fs.getStoreName(), fs.getReadPermission(), fs.getWritePermission()}, 
                new int[] {Types.VARCHAR, Types.VARCHAR, Types.VARCHAR}));
    }
    
    private void update(FileStore fs) {
        ejt.update(UPDATE_FILE_STORE, new Object[] {fs.getStoreName(), fs.getReadPermission(), fs.getWritePermission(), fs.getId()}, 
                new int[] {Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.INTEGER});
    }
    
    public int deleteFileStore(String storeName) {
        return ejt.update(DELETE_FILE_STORE + "storeName='" + storeName + "'");
    }
    
    public int deleteFileStore(int storeId) {
        return ejt.update(DELETE_FILE_STORE + "id='" + storeId + "'");
    }
}
