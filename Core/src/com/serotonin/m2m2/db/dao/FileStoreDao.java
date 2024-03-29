/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.db.dao;

import org.jooq.Record;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.infiniteautomation.mango.db.tables.FileStores;
import com.infiniteautomation.mango.db.tables.records.FileStoresRecord;
import com.infiniteautomation.mango.permission.MangoPermission;
import com.infiniteautomation.mango.spring.DaoDependencies;
import com.infiniteautomation.mango.util.LazyInitSupplier;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.vo.FileStore;

/**
 *
 * @author Phillip Dunlap
 * @author Jared Wiltshire
 */
@Repository
public class FileStoreDao extends AbstractVoDao<FileStore, FileStoresRecord, FileStores> {

    private static final LazyInitSupplier<FileStoreDao> springInstance = new LazyInitSupplier<>(() -> Common.getRuntimeContext().getBean(FileStoreDao.class));

    @Autowired
    private FileStoreDao(DaoDependencies dependencies) {
        super(dependencies, FileStore.FileStoreAuditEvent.TYPE_NAME, FileStores.FILE_STORES, new TranslatableMessage("internal.monitor.FILESTORE_COUNT"));
    }

    public static FileStoreDao getInstance() {
        return springInstance.get();
    }

    @Override
    protected String getXidPrefix() {
        return FileStore.XID_PREFIX;
    }

    @Override
    protected Record toRecord(FileStore vo) {
        Record record = table.newRecord();
        record.set(table.xid, vo.getXid());
        record.set(table.name, vo.getName());
        record.set(table.readPermissionId, vo.getReadPermission().getId());
        record.set(table.writePermissionId, vo.getWritePermission().getId());
        return record;
    }

    @Override
    public FileStore mapRecord(Record record) {
        FileStore result = new FileStore();
        result.setId(record.get(table.id));
        result.setXid(record.get(table.xid));
        result.setName(record.get(table.name));
        result.setReadPermission(new MangoPermission(record.get(table.readPermissionId)));
        result.setWritePermission(new MangoPermission(record.get(table.writePermissionId)));
        return result;
    }

    @Override
    public void savePreRelationalData(FileStore existing, FileStore vo) {
        MangoPermission readPermission = permissionService.findOrCreate(vo.getReadPermission());
        vo.setReadPermission(readPermission);

        MangoPermission writePermission = permissionService.findOrCreate(vo.getWritePermission());
        vo.setWritePermission(writePermission);
    }

    @Override
    public void saveRelationalData(FileStore existing, FileStore vo) {
        if(existing != null) {
            if(!existing.getReadPermission().equals(vo.getReadPermission())) {
                permissionService.deletePermissions(existing.getReadPermission());
            }
            if(!existing.getWritePermission().equals(vo.getWritePermission())) {
                permissionService.deletePermissions(existing.getWritePermission());
            }
        }
    }

    @Override
    public void loadRelationalData(FileStore vo) {
        //Populate permissions
        vo.setReadPermission(permissionService.get(vo.getReadPermission().getId()));
        vo.setWritePermission(permissionService.get(vo.getWritePermission().getId()));
    }

    @Override
    public void deletePostRelationalData(FileStore vo) {
        //Clean permissions
        permissionService.deletePermissions(vo.getReadPermission(), vo.getWritePermission());
    }

}
