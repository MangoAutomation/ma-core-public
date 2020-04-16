/**
 * Copyright (C) 2013 Infinite Automation Software. All rights reserved.
 *
 * @author Terry Packer
 */
package com.infiniteautomation.mango.emport;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.infiniteautomation.mango.spring.service.PermissionService;
import com.infiniteautomation.mango.util.exception.ValidationException;
import com.serotonin.json.type.JsonObject;
import com.serotonin.json.type.JsonValue;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.dao.RoleDao;
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

    /**
     * @param json
     */
    public SystemSettingsImporter(JsonObject json, PermissionHolder user) {
        super(json);
        this.user = user;
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
                                    RoleVO role = RoleDao.getInstance().getByXid(xid);
                                    if(role != null) {
                                        roles.add(Collections.singleton(role.getRole()));
                                    }else {
                                        roles.add(Collections.singleton(new Role(Common.NEW_ID, xid)));
                                    }
                                }
                                def.update(roles);
                                addSuccessMessage(false, "emport.permission.prefix", key);
                            }catch(ValidationException e) {
                                setValidationMessages(e.getValidationResult(), "emport.permission.prefix", key);
                                return;
                            }
                        }else {
                            // Could be an export code so try and convert it
                            Integer id =
                                    SystemSettingsDao.instance.convertToValueFromCode(key, (String) o);
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
            SystemSettingsDao.instance.validate(settings, voResponse, user);
            if (voResponse.getHasMessages())
                setValidationMessages(voResponse, "emport.systemSettings.prefix",
                        new TranslatableMessage("header.systemSettings")
                        .translate(Common.getTranslations()));
            else {
                SystemSettingsDao.instance.updateSettings(settings);
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
