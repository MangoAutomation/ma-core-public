/**
 * Copyright (C) 2019  Infinite Automation Software. All rights reserved.
 */
package com.infiniteautomation.mango.util.script;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import com.infiniteautomation.mango.spring.service.PermissionService;
import com.serotonin.ShouldNeverHappenException;
import com.serotonin.json.JsonException;
import com.serotonin.json.JsonReader;
import com.serotonin.json.ObjectWriter;
import com.serotonin.json.spi.JsonProperty;
import com.serotonin.json.spi.JsonSerializable;
import com.serotonin.json.type.JsonArray;
import com.serotonin.json.type.JsonObject;
import com.serotonin.json.type.JsonValue;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.dao.RoleDao;
import com.serotonin.m2m2.i18n.TranslatableJsonException;
import com.serotonin.m2m2.vo.RoleVO;
import com.serotonin.m2m2.vo.User;
import com.serotonin.m2m2.vo.permission.PermissionHolder;

/**
 * Script Permissions Container, to replace com.serotonin.m2m2.rt.script.ScriptPermissions
 * @author Terry Packer
 *
 */
public class ScriptPermissions implements JsonSerializable, Serializable, PermissionHolder {

    @JsonProperty
    private final Set<RoleVO> roles;
    private final String permissionHolderName; //Name for exception messages

    public ScriptPermissions() {
        this(Collections.emptySet());
    }

    public ScriptPermissions(Set<RoleVO> permissionsSet) {
        this(permissionsSet, "script");
    }

    public ScriptPermissions(User user) {
        this(user.getRoles(), user.getPermissionHolderName());
    }

    public ScriptPermissions(Set<RoleVO> roles, String permissionHolderName) {
        if (roles != null) {
            this.roles = roles;
        } else {
            this.roles = Collections.unmodifiableSet(Collections.emptySet());
        }
        this.permissionHolderName = permissionHolderName;
    }

    @Override
    public String getPermissionHolderName() {
        if(StringUtils.isEmpty(permissionHolderName))
            return "JavaScript";
        else
            return "JavaScript saved by: " + permissionHolderName;
    }

    @Override
    public boolean isPermissionHolderDisabled() {
        return false;
    }
    
    @Override
    public Set<RoleVO> getRoles() {
        return roles;
    }

    private static final int version = 2;
    private static final long serialVersionUID = 1L;

    private void writeObject(ObjectOutputStream out) throws IOException {
        out.writeInt(version);
    }

    @SuppressWarnings("unchecked")
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        int ver = in.readInt();
        if(ver == 1) {
            Set<String> permissionsSet = (Set<String>) in.readObject();
            roles.clear();
            for(String permission : permissionsSet) {
                RoleVO role = RoleDao.getInstance().getByXid(permission);
                if(role != null) {
                    roles.add(role);
                }
            }
        }else if(ver == 2) {
            //Nada
        }
    }

    @Override
    public void jsonWrite(ObjectWriter writer) throws IOException, JsonException {
        throw new ShouldNeverHappenException("Permissions should be written out in the parent class as a String.");
    }

    @Override
    public void jsonRead(JsonReader reader, JsonObject jsonObject) throws JsonException {
        throw new ShouldNeverHappenException("Permissions should be read in the parent class as a String.");
    }

    /**
     * Safely read legacy and super-legacy ScriptPermissions current permissions are JSON exportable
     * @param jsonObject
     * @return
     * @throws TranslatableJsonException 
     */
    public static ScriptPermissions readJsonSafely(JsonObject jsonObject) throws TranslatableJsonException {
        if(jsonObject.containsKey("scriptPermissions")) {
            try{
                PermissionService service = Common.getBean(PermissionService.class);
                JsonObject o = jsonObject.getJsonObject("scriptPermissions");
                Set<RoleVO> roles = new HashSet<>();
                Set<String> permissions = new HashSet<>();
                permissions.addAll(service.explodeLegacyPermissionGroups(o.getString("dataSourcePermissions")));
                permissions.addAll(service.explodeLegacyPermissionGroups(o.getString("dataPointSetPermissions")));
                permissions.addAll(service.explodeLegacyPermissionGroups(o.getString("dataPointReadPermissions")));
                permissions.addAll(service.explodeLegacyPermissionGroups(o.getString("customPermissions")));
                
                for(String permission : permissions) {
                    RoleVO role = RoleDao.getInstance().getByXid(permission);
                    if(role != null) {
                        roles.add(role);
                    } else {
                        throw new TranslatableJsonException("emport.error.missingRole", permission, "scriptPermissions");
                    }
                }
                
                return new ScriptPermissions(roles);
            }catch(ClassCastException e) {
                //Munchy munch, not a legacy script permissions object
            }

            Set<RoleVO> roles = new HashSet<>();
            JsonArray permissions = jsonObject.getJsonArray("scriptPermissions");
            for(JsonValue jv : permissions) {
                RoleVO role = RoleDao.getInstance().getByXid(jv.toString());
                if(role != null) {
                    roles.add(role);
                } else {
                    throw new TranslatableJsonException("emport.error.missingRole", jv.toString(), "scriptPermissions");
                }
            }
            return new ScriptPermissions(roles);
        }else {
            return new ScriptPermissions();
        }
    }
}
