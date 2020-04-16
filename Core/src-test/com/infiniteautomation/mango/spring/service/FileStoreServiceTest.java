/**
 * Copyright (C) 2019  Infinite Automation Software. All rights reserved.
 */
package com.infiniteautomation.mango.spring.service;

import static org.junit.Assert.assertEquals;

import java.util.Collections;
import java.util.UUID;

import com.infiniteautomation.mango.permission.MangoPermission;
import com.infiniteautomation.mango.spring.db.FileStoreTableDefinition;
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
public class FileStoreServiceTest extends AbstractBasicVOServiceWithPermissionsTestBase<FileStore, FileStoreTableDefinition, FileStoreDao, FileStoreService> {

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
        assertEquals(expected.getStoreName(), actual.getStoreName());

        assertPermission(expected.getReadPermission(), actual.getReadPermission());
        assertPermission(expected.getWritePermission(), actual.getWritePermission());
    }

    @Override
    public FileStore newVO(User owner) {
        FileStore vo = new FileStore();
        vo.setStoreName(UUID.randomUUID().toString());
        return vo;
    }

    @Override
    FileStore updateVO(FileStore existing) {
        FileStore edit = new FileStore();
        edit.setId(existing.getId());
        edit.setStoreName("new store name");
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
    String getReadRolesContextKey() {
        return "readPermission";
    }

    @Override
    void addEditRoleToFail(Role role, FileStore vo) {
        vo.getWritePermission().getRoles().add(Collections.singleton(role));
    }

    @Override
    String getEditRolesContextKey() {
        return "writePermission";
    }

}
