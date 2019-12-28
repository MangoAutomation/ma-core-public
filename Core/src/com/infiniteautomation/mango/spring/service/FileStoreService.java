/**
 * Copyright (C) 2019  Infinite Automation Software. All rights reserved.
 */
package com.infiniteautomation.mango.spring.service;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.serotonin.m2m2.db.dao.FileStoreDao;
import com.serotonin.m2m2.i18n.ProcessResult;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.module.ModuleRegistry;
import com.serotonin.m2m2.module.definitions.permissions.UserFileStoreCreatePermissionDefinition;
import com.serotonin.m2m2.vo.FileStore;
import com.serotonin.m2m2.vo.permission.PermissionHolder;
import com.serotonin.validation.StringValidation;

/**
 * @author Terry Packer
 *
 */
@Service
public class FileStoreService extends AbstractBasicVOService<FileStore, FileStoreDao> {

    /**
     * @param dao
     * @param permissionService
     * @param createPermissionDefinition
     */
    @Autowired
    public FileStoreService(FileStoreDao dao, PermissionService permissionService) {
        super(dao, permissionService, ModuleRegistry.getPermissionDefinition(UserFileStoreCreatePermissionDefinition.TYPE_NAME));
    }

    @Override
    public ProcessResult validate(FileStore vo, PermissionHolder user) {
        ProcessResult result = commonValidation(vo, user);
        permissionService.validateVoRoles(result, "readRoles", user, false, null, vo.getReadRoles());
        permissionService.validateVoRoles(result, "writeRoles", user, false, null, vo.getWriteRoles());
        return result;
    }
    
    @Override
    public ProcessResult validate(FileStore existing, FileStore vo, PermissionHolder user) {
        ProcessResult result = commonValidation(vo, user);
        permissionService.validateVoRoles(result, "readRoles", user, false, existing.getReadRoles(), vo.getReadRoles());
        permissionService.validateVoRoles(result, "writeRoles", user, false, existing.getWriteRoles(), vo.getWriteRoles());
        return result;
    }
    
    protected ProcessResult commonValidation(FileStore vo, PermissionHolder holder) {
        ProcessResult result = new ProcessResult();
        if (StringUtils.isBlank(vo.getStoreName()))
            result.addContextualMessage("storeName", "validate.required");
        else if (StringValidation.isLengthGreaterThan(vo.getStoreName(), 100))
            result.addMessage("storeName", new TranslatableMessage("validate.notLongerThan", 100));
        return result;
    }

    @Override
    public boolean hasEditPermission(PermissionHolder user, FileStore vo) {
        return permissionService.hasAnyRole(user, vo.getWriteRoles());
    }

    @Override
    public boolean hasReadPermission(PermissionHolder user, FileStore vo) {
        return permissionService.hasAnyRole(user, vo.getReadRoles());
    }

}
