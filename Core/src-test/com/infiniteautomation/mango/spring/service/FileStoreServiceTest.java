/**
 * Copyright (C) 2019  Infinite Automation Software. All rights reserved.
 */
package com.infiniteautomation.mango.spring.service;

import static org.junit.Assert.assertEquals;

import java.util.Collections;
import java.util.UUID;

import com.infiniteautomation.mango.db.tables.FileStores;
import com.infiniteautomation.mango.db.tables.records.FileStoresRecord;
import com.infiniteautomation.mango.permission.MangoPermission;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.dao.FileStoreDao;
import com.serotonin.m2m2.module.definitions.permissions.UserFileStoreCreatePermissionDefinition;
import com.serotonin.m2m2.vo.FileStore;
import com.serotonin.m2m2.vo.User;
import com.serotonin.m2m2.vo.role.Role;

/**
 * @author Terry Packer
 *
 */
public class FileStoreServiceTest extends AbstractBasicVOServiceWithPermissionsTestBase<FileStore, FileStoresRecord, FileStores, FileStoreDao, FileStoreService> {

    @Override
    public FileStoreService getService() {
        return Common.getBean(FileStoreService.class);
    }

    @Override
    public FileStoreDao getDao() {
        return FileStoreDao.getInstance();
    }

    @Override
    public void assertVoEqual(FileStore expected, FileStore actual) {
        assertEquals(expected.getId(), actual.getId());
        assertEquals(expected.getName(), actual.getName());
        assertEquals(expected.getXid(), actual.getXid());

        assertPermission(expected.getReadPermission(), actual.getReadPermission());
        assertPermission(expected.getWritePermission(), actual.getWritePermission());
    }

    @Override
    public FileStore newVO(User owner) {
        FileStore vo = new FileStore();
        vo.setXid(UUID.randomUUID().toString());
        vo.setName(vo.getXid());
        return vo;
    }

    @Override
    FileStore updateVO(FileStore existing) {
        FileStore edit = new FileStore();
        edit.setId(existing.getId());
        edit.setXid("new store name");
        edit.setName(edit.getXid());
        return edit;
    }

    @Override
    String getCreatePermissionType() {
        return UserFileStoreCreatePermissionDefinition.TYPE_NAME;
    }

    @Override
    void setReadPermission(MangoPermission permission, FileStore vo) {
        vo.setReadPermission(permission);
    }

    @Override
    void setEditPermission(MangoPermission permission, FileStore vo) {
        vo.setWritePermission(permission);
    }

    @Override
    void addReadRoleToFail(Role role, FileStore vo) {
        vo.getReadPermission().getRoles().add(Collections.singleton(role));
    }

    @Override
    String getReadPermissionContextKey() {
        return "readPermission";
    }

    @Override
    void addEditRoleToFail(Role role, FileStore vo) {
        vo.getWritePermission().getRoles().add(Collections.singleton(role));
    }

    @Override
    String getEditPermissionContextKey() {
        return "writePermission";
    }

}
