/**
 * Copyright (C) 2015 Infinite Automation Software. All rights reserved.
 *
 * @author Terry Packer
 */
package com.serotonin.m2m2.rt.script;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.serotonin.json.spi.JsonProperty;
import com.serotonin.m2m2.i18n.ProcessResult;
import com.serotonin.m2m2.vo.User;
import com.serotonin.m2m2.vo.permission.PermissionHolder;
import com.serotonin.m2m2.vo.permission.Permissions;

/**
 * Container for all scripts that holds the permissions during runtime.
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
    
    public ScriptPermissions(Set<String> permissions) {
        this(permissions == null ? null : Permissions.implodePermissionGroups(permissions));
    }
    
    public ScriptPermissions(String permissions) {
        this.dataPointReadPermissions = permissions;
        this.dataSourcePermissions = permissions;
        this.dataPointSetPermissions = permissions;
        this.customPermissions = permissions;
    }
    
    public ScriptPermissions() {
        dataSourcePermissions = "";
        dataPointSetPermissions = "";
        dataPointReadPermissions = "";
        customPermissions = "";
    }

    public ScriptPermissions(User user) {
        if (user == null) {
            dataSourcePermissions = "";
            dataPointSetPermissions = "";
            dataPointReadPermissions = "";
            customPermissions = "";

        } else {
            dataPointReadPermissions = user.getPermissions();
            dataPointSetPermissions = user.getPermissions();
            dataSourcePermissions = user.getPermissions();
            customPermissions = user.getPermissions();
        }
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

    public void validate(ProcessResult response, User user) {
        if (user == null) {
            response.addContextualMessage(DATA_SOURCE, "validate.invalidPermission", "No User Found");
            response.addContextualMessage(DATA_POINT_SET, "validate.invalidPermission", "No User Found");
            response.addContextualMessage(DATA_POINT_READ, "validate.invalidPermission", "No User Found");
            return;
        }

        if (user.hasAdminPermission())
            return;

        // If superadmin then fine or if not then only allow my groups
        if ((!this.dataSourcePermissions.isEmpty()) && (!Permissions.hasPermission(user, this.dataSourcePermissions))) {
            Set<String> invalid = Permissions.findInvalidPermissions(user, this.dataSourcePermissions);
            String notGranted = Permissions.implodePermissionGroups(invalid);
            response.addContextualMessage(DATA_SOURCE, "validate.invalidPermission", notGranted);
        }
        if ((!this.dataPointSetPermissions.isEmpty()) && (!Permissions.hasPermission(user, this.dataPointSetPermissions))) {
            Set<String> invalid = Permissions.findInvalidPermissions(user, this.dataPointSetPermissions);
            String notGranted = Permissions.implodePermissionGroups(invalid);
            response.addContextualMessage(DATA_POINT_SET, "validate.invalidPermission", notGranted);
        }
        if ((!this.dataPointReadPermissions.isEmpty()) && (!Permissions.hasPermission(user, this.dataPointReadPermissions))) {
            Set<String> invalid = Permissions.findInvalidPermissions(user, this.dataPointReadPermissions);
            String notGranted = Permissions.implodePermissionGroups(invalid);
            response.addContextualMessage(DATA_POINT_READ, "validate.invalidPermission", notGranted);
        }
    }

    public void validate(ProcessResult response, User user, ScriptPermissions oldPermissions) {
        if (user.hasAdminPermission())
            return;

        Set<String> nonUserPre = Permissions.findInvalidPermissions(user, oldPermissions.getDataSourcePermissions());
        Set<String> nonUserPost = Permissions.findInvalidPermissions(user, this.dataSourcePermissions);
        if (nonUserPre.size() != nonUserPost.size())
            response.addContextualMessage(DATA_SOURCE, "validate.invalidPermissionModification", user.getPermissions());
        else {
            for (String s : nonUserPre)
                if (!nonUserPost.contains(s))
                    response.addContextualMessage(DATA_SOURCE, "validate.invalidPermissionModification", user.getPermissions());
        }

        nonUserPre = Permissions.findInvalidPermissions(user, oldPermissions.getDataPointSetPermissions());
        nonUserPost = Permissions.findInvalidPermissions(user, this.dataPointSetPermissions);
        if (nonUserPre.size() != nonUserPost.size())
            response.addContextualMessage(DATA_POINT_SET, "validate.invalidPermissionModification", user.getPermissions());
        else {
            for (String s : nonUserPre)
                if (!nonUserPost.contains(s))
                    response.addContextualMessage(DATA_POINT_SET, "validate.invalidPermissionModification", user.getPermissions());
        }

        nonUserPre = Permissions.findInvalidPermissions(user, oldPermissions.getDataPointReadPermissions());
        nonUserPost = Permissions.findInvalidPermissions(user, this.dataPointReadPermissions);
        if (nonUserPre.size() != nonUserPost.size())
            response.addContextualMessage(DATA_POINT_READ, "validate.invalidPermissionModification", user.getPermissions());
        else {
            for (String s : nonUserPre)
                if (!nonUserPost.contains(s))
                    response.addContextualMessage(DATA_POINT_READ, "validate.invalidPermissionModification", user.getPermissions());
        }
    }

    @Override
    public String getPermissions() {
        return Permissions.implodePermissionGroups(this.getPermissionsSet());
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

    @JsonIgnore //Because Jackson will try to use this and its unmodifiable
    @Override
    public Set<String> getPermissionsSet() {
        //TODO Fix this, due to serialization this is null when read back out of the database
        if(combinedPermissions == null) {
            Set<String> combined = new HashSet<>();
            combined.addAll(Permissions.explodePermissionGroups(this.dataSourcePermissions));
            combined.addAll(Permissions.explodePermissionGroups(this.dataPointSetPermissions));
            combined.addAll(Permissions.explodePermissionGroups(this.dataPointReadPermissions));
            combined.addAll(Permissions.explodePermissionGroups(this.customPermissions));
            return Collections.unmodifiableSet(combined);
        }

        return combinedPermissions.get(() -> {
            Set<String> combined = new HashSet<>();
            combined.addAll(Permissions.explodePermissionGroups(this.dataSourcePermissions));
            combined.addAll(Permissions.explodePermissionGroups(this.dataPointSetPermissions));
            combined.addAll(Permissions.explodePermissionGroups(this.dataPointReadPermissions));
            combined.addAll(Permissions.explodePermissionGroups(this.customPermissions));
            return Collections.unmodifiableSet(combined);
        });
    }
}
