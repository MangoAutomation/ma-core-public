/**
 * Copyright (C) 2020  Infinite Automation Software. All rights reserved.
 */

package com.infiniteautomation.mango.emport;

import java.util.HashSet;
import java.util.Set;

import com.infiniteautomation.mango.util.exception.ValidationException;
import com.serotonin.json.type.JsonArray;
import com.serotonin.json.type.JsonObject;
import com.serotonin.json.type.JsonValue;
import com.serotonin.m2m2.module.ModuleRegistry;
import com.serotonin.m2m2.module.PermissionDefinition;

/**
 *
 * @author Terry Packer
 */
public class PermissionImporter extends Importer {

    public PermissionImporter(JsonObject json) {
        super(json);
    }

    @Override
    protected void importImpl() {
        String permissionType = json.getString("permissionType");
        PermissionDefinition def = ModuleRegistry.getPermissionDefinition(permissionType);

        if(def != null) {
            try {
                JsonValue v = json.get("roles");
                if(v != null) {
                    Set<String> xids = new HashSet<>();
                    JsonArray roles = v.toJsonArray();
                    for(JsonValue jv : roles) {
                        xids.add(jv.toString());
                    }
                    def.setRoles(xids);
                }
                addSuccessMessage(false, "emport.permission.prefix", permissionType);
            }catch(ValidationException e) {
                setValidationMessages(e.getValidationResult(), "emport.permission.prefix", permissionType);
            }
        }

    }
}
