/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.infiniteautomation.mango.emport;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.infiniteautomation.mango.permission.MangoPermission;
import com.infiniteautomation.mango.spring.service.PermissionService;
import com.infiniteautomation.mango.spring.service.RoleService;
import com.infiniteautomation.mango.spring.service.SystemPermissionService;
import com.infiniteautomation.mango.util.exception.ValidationException;
import com.serotonin.json.type.JsonObject;
import com.serotonin.json.type.JsonValue;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.dao.SystemSettingsDao;
import com.serotonin.m2m2.i18n.ProcessResult;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.module.ModuleRegistry;
import com.serotonin.m2m2.module.PermissionDefinition;
import com.serotonin.m2m2.vo.permission.PermissionHolder;
import com.serotonin.m2m2.vo.role.Role;
import com.serotonin.m2m2.vo.role.RoleVO;

/**
 * @author Terry Packer
 *
 */
public class SystemSettingsImporter extends Importer {

    private final PermissionHolder user;
    private final SystemPermissionService permissionService;
    private final RoleService roleService;

    /**
     */
    public SystemSettingsImporter(JsonObject json, PermissionHolder user, SystemPermissionService permissionService, RoleService roleService) {
        super(json);
        this.user = user;
        this.permissionService = permissionService;
        this.roleService = roleService;
    }

    @Override
    protected void importImpl() {

        try {
            Map<String, Object> settings = new HashMap<String, Object>();

            // Finish reading it in.
            for (String key : json.keySet()) {
                JsonValue value = json.get(key);
                // Handle legacy permissions
                // Don't import null values or database schemas
                if ((value != null)
                        && (!key.startsWith(SystemSettingsDao.DATABASE_SCHEMA_VERSION))) {
                    Object o = value.toNative();
                    if (o instanceof String) {
                        PermissionDefinition def = ModuleRegistry.getPermissionDefinition(key);
                        if (def != null) {
                            //Legacy permission import
                            try{
                                Set<String> xids = PermissionService.explodeLegacyPermissionGroups((String)o);
                                Set<Set<Role>> roles = new HashSet<>();
                                for(String xid : xids) {
                                    RoleVO role = roleService.get(xid);
                                    if(role != null) {
                                        roles.add(Collections.singleton(role.getRole()));
                                    }else {
                                        roles.add(Collections.singleton(new Role(Common.NEW_ID, xid)));
                                    }
                                }
                                permissionService.update(new MangoPermission(roles), def);
                                addSuccessMessage(false, "emport.permission.prefix", key);
                            }catch(ValidationException e) {
                                setValidationMessages(e.getValidationResult(), "emport.permission.prefix", key);
                                return;
                            }
                        }else {
                            // Could be an export code so try and convert it
                            Integer id =
                                    SystemSettingsDao.getInstance().convertToValueFromCode(key, (String) o);
                            if (id != null)
                                settings.put(key, id);
                            else
                                settings.put(key, o);
                        }
                    } else {
                        settings.put(key, o);
                    }
                }
            }

            // Now validate it. Use a new response object so we can distinguish errors in this vo
            // from
            // other errors.
            ProcessResult voResponse = new ProcessResult();
            SystemSettingsDao.getInstance().validate(settings, voResponse, user);
            if (voResponse.getHasMessages())
                setValidationMessages(voResponse, "emport.systemSettings.prefix",
                        new TranslatableMessage("header.systemSettings")
                        .translate(Common.getTranslations()));
            else {
                SystemSettingsDao.getInstance().updateSettings(settings);
                addSuccessMessage(false, "emport.systemSettings.prefix",
                        new TranslatableMessage("header.systemSettings")
                        .translate(Common.getTranslations()));
            }
        } catch (Exception e) {
            addFailureMessage("emport.systemSettings.prefix",
                    new TranslatableMessage("header.systemSettings")
                    .translate(Common.getTranslations()),
                    e.getMessage());
        }
    }

}
