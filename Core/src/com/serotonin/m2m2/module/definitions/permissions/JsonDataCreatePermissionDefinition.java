/**
 * Copyright (C) 2017 Infinite Automation Software. All rights reserved.
 *
 */
package com.serotonin.m2m2.module.definitions.permissions;

import java.util.Arrays;
import java.util.List;

import com.serotonin.m2m2.module.PermissionDefinition;

/**
 *
 * @author Terry Packer
 */
public class JsonDataCreatePermissionDefinition extends PermissionDefinition{

    public static final String TYPE_NAME = "jsonData.create";

    @Override
    public String getPermissionKey() {
        return "jsonData.permission.create";
    }

    @Override
    public String getPermissionTypeName() {
        return TYPE_NAME;
    }

    @Override
    public List<String> getDefaultGroups() {
        return Arrays.asList(new String[] {SuperadminPermissionDefinition.GROUP_NAME, "edit-ui-pages"});
    }

}
