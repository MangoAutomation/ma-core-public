/**
 * Copyright (C) 2019  Infinite Automation Software. All rights reserved.
 */
package com.infiniteautomation.mango.spring.service;

import java.util.Set;

import org.springframework.stereotype.Service;

import com.infiniteautomation.mango.spring.db.JsonDataTableDefinition;
import com.infiniteautomation.mango.util.exception.NotFoundException;
import com.serotonin.m2m2.db.dao.JsonDataDao;
import com.serotonin.m2m2.i18n.ProcessResult;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.module.ModuleRegistry;
import com.serotonin.m2m2.module.definitions.permissions.JsonDataCreatePermissionDefinition;
import com.serotonin.m2m2.vo.json.JsonDataVO;
import com.serotonin.m2m2.vo.permission.PermissionHolder;
import com.serotonin.m2m2.vo.role.Role;

/**
 * @author Terry Packer
 *
 */
@Service
public class JsonDataService extends AbstractVOService<JsonDataVO, JsonDataTableDefinition, JsonDataDao> {

    public JsonDataService(JsonDataDao dao, PermissionService permissionService) {
        super(dao, permissionService);
    }

    @Override
    public Set<Role> getCreatePermissionRoles() {
        return ModuleRegistry.getPermissionDefinition(JsonDataCreatePermissionDefinition.TYPE_NAME).getRoles();
    }

    @Override
    public boolean hasEditPermission(PermissionHolder user, JsonDataVO vo) {
        return permissionService.hasAnyRole(user, vo.getEditRoles());
    }

    @Override
    public boolean hasReadPermission(PermissionHolder user, JsonDataVO vo) {
        return permissionService.hasAnyRole(user, vo.getReadRoles());
    }

    /**
     * Get data and ensure it is public,
     * @param xid
     * @return
     * @throws NotFoundException if it doesn't exist or if it exists and is not public
     */
    public JsonDataVO getPublicData(String xid) throws NotFoundException {
        JsonDataVO vo = this.getPermissionService().runAsSystemAdmin(() -> {
            return super.get(xid);
        });
        if(!vo.isPublicData()) {
            throw new NotFoundException();
        }else {
            return vo;
        }
    }

    @Override
    public ProcessResult validate(JsonDataVO vo, PermissionHolder user) {
        ProcessResult result = commonValidation(vo, user);

        permissionService.validateVoRoles(result, "readRoles", user, false, null, vo.getReadRoles());
        permissionService.validateVoRoles(result, "editRoles", user, false, null, vo.getEditRoles());

        return result;
    }

    @Override
    public ProcessResult validate(JsonDataVO existing, JsonDataVO vo, PermissionHolder user) {
        ProcessResult result = commonValidation(vo, user);
        //Additional checks for existing list
        permissionService.validateVoRoles(result, "readRoles", user, false, existing.getReadRoles(), vo.getReadRoles());
        permissionService.validateVoRoles(result, "editRoles", user, false, existing.getEditRoles(), vo.getEditRoles());

        return result;
    }

    private ProcessResult commonValidation(JsonDataVO vo, PermissionHolder user) {
        ProcessResult result = super.validate(vo, user);
        try{
            JsonDataDao.getInstance().writeValueAsString(vo.getJsonData());
        }catch(Exception e){
            result.addMessage("jsonData", new TranslatableMessage("common.default", e.getMessage()));
        }
        return result;
    }

}
