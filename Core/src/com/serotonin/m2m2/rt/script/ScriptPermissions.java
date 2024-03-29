/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.rt.script;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.infiniteautomation.mango.spring.service.PermissionService;
import com.serotonin.ShouldNeverHappenException;
import com.serotonin.json.spi.JsonProperty;
import com.serotonin.m2m2.vo.permission.PermissionHolder;
import com.serotonin.m2m2.vo.role.Role;

/**
 * This is a legacy class left here for deserialization puposes
 *
 * @author Terry Packer
 * @deprecated Use {@link com.infiniteautomation.mango.util.script.ScriptPermissions}
 */
@Deprecated
public class ScriptPermissions extends ScriptPermissionParent implements Serializable, PermissionHolder {

    public static final String DATA_SOURCE = "scriptDataSourcePermission";
    public static final String DATA_POINT_READ = "scriptDataPointReadPermission";
    public static final String DATA_POINT_SET = "scriptDataPointSetPermission";
    /**
     *
     */
    private static final long serialVersionUID = 1L;

    @JsonProperty
    private String dataSourcePermissions = "";
    @JsonProperty
    private String dataPointSetPermissions = "";
    @JsonProperty
    private String dataPointReadPermissions = "";
    @JsonProperty
    private String customPermissions = "";

    public ScriptPermissions() {
        dataSourcePermissions = "";
        dataPointSetPermissions = "";
        dataPointReadPermissions = "";
        customPermissions = "";
    }

    @Deprecated
    public String getDataSourcePermissions() {
        return dataSourcePermissions;
    }

    public void setDataSourcePermissions(String dataSourcePermissions) {
        this.dataSourcePermissions = dataSourcePermissions;
        this.combinedPermissions.reset();
    }

    @Deprecated
    public String getDataPointSetPermissions() {
        return dataPointSetPermissions;
    }

    public void setDataPointSetPermissions(String dataPointSetPermissions) {
        this.dataPointSetPermissions = dataPointSetPermissions;
        this.combinedPermissions.reset();
    }

    @Deprecated
    public String getDataPointReadPermissions() {
        return dataPointReadPermissions;
    }

    public void setDataPointReadPermissions(String dataPointReadPermissions) {
        this.dataPointReadPermissions = dataPointReadPermissions;
        this.combinedPermissions.reset();
    }

    @Deprecated
    public String getCustomPermissions() {
        return customPermissions;
    }

    public void setCustomPermissions(String customPermissions) {
        this.customPermissions = customPermissions;
        this.combinedPermissions.reset();
    }

    @Override
    public String getPermissionHolderName() {
        // TODO return parent object's name or something, this is used in messages in exceptions
        return "script";
    }

    @Override
    public boolean isPermissionHolderDisabled() {
        return false;
    }

    public Set<String> getAllLegacyPermissions() {
        Set<String> combined = new HashSet<>();
        combined.addAll(PermissionService.explodeLegacyPermissionGroups(this.dataSourcePermissions));
        combined.addAll(PermissionService.explodeLegacyPermissionGroups(this.dataPointSetPermissions));
        combined.addAll(PermissionService.explodeLegacyPermissionGroups(this.dataPointReadPermissions));
        combined.addAll(PermissionService.explodeLegacyPermissionGroups(this.customPermissions));
        return combined;
    }

    @JsonIgnore
    @Override
    public Set<Role> getRoles() {
        throw new ShouldNeverHappenException("deprecated and should never be used, convert to com.infiniteautomation.mango.util.script.ScriptPermissions via getAllLegacyPermissions()");
    }
}
