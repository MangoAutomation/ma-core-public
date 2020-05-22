/**
 * Copyright (C) 2020  Infinite Automation Software. All rights reserved.
 */

package com.infiniteautomation.mango.emport;

import java.util.Iterator;

import com.infiniteautomation.mango.permission.MangoPermission;
import com.infiniteautomation.mango.spring.service.SystemPermissionService;
import com.infiniteautomation.mango.util.exception.ValidationException;
import com.serotonin.json.JsonException;
import com.serotonin.json.type.JsonObject;
import com.serotonin.json.type.JsonValue;
import com.serotonin.m2m2.module.ModuleRegistry;
import com.serotonin.m2m2.module.PermissionDefinition;

/**
 *
 * @author Terry Packer
 */
public class PermissionImporter extends Importer {

    private final SystemPermissionService service;

    public PermissionImporter(JsonObject json, SystemPermissionService service) {
        super(json);
        this.service = service;
    }

    @Override
    protected void importImpl() {
        Iterator<String> it = json.keySet().iterator();
        if(it.hasNext()) {
            String permissionType = it.next();
            PermissionDefinition def = ModuleRegistry.getPermissionDefinition(permissionType);

            if(def != null) {
                try {
                    JsonValue v = json.get(permissionType);
                    if(v != null) {
                        MangoPermission permission = ctx.getReader().read(MangoPermission.class, v);
                        service.update(permission, def);
                    }
                    addSuccessMessage(false, "emport.permission.prefix", permissionType);
                }catch(ValidationException e) {
                    setValidationMessages(e.getValidationResult(), "emport.permission.prefix", permissionType);
                } catch (JsonException e) {
                    addFailureMessage("emport.permission.prefix", permissionType, getJsonExceptionMessage(e));
                }
            }
        }

    }
}
