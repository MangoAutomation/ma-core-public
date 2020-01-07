/**
 * Copyright (C) 2019  Infinite Automation Software. All rights reserved.
 */
package com.infiniteautomation.mango.spring.service;

import static org.junit.Assert.assertEquals;

import java.util.Set;
import java.util.UUID;

import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.dao.FileStoreDao;
import com.serotonin.m2m2.module.definitions.permissions.UserFileStoreCreatePermissionDefinition;
import com.serotonin.m2m2.vo.FileStore;
import com.serotonin.m2m2.vo.role.Role;

/**
 * @author Terry Packer
 *
 */
public class FileStoreServiceTest extends AbstractBasicVOServiceWithPermissionsTestBase<FileStore, FileStoreDao, FileStoreService> {
    
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

        assertRoles(expected.getReadRoles(), actual.getReadRoles());
        assertRoles(expected.getWriteRoles(), actual.getWriteRoles());
    }

    @Override
    public FileStore newVO() {
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
    void setReadRoles(Set<Role> roles, FileStore vo) {
        vo.setReadRoles(roles);
    }

    @Override
    void setEditRoles(Set<Role> roles, FileStore vo) {
        vo.setWriteRoles(roles);
    }

}
