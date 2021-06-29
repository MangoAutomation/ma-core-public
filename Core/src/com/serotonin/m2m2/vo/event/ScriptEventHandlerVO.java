/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.vo.event;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import com.infiniteautomation.mango.spring.script.MangoScript;
import com.infiniteautomation.mango.spring.script.StringMangoScript;
import com.infiniteautomation.mango.spring.service.PermissionService;
import com.serotonin.json.spi.JsonProperty;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.vo.role.Role;

/**
 * @author Jared Wiltshire
 */
public class ScriptEventHandlerVO extends AbstractEventHandlerVO {

    @JsonProperty
    String engineName;
    @JsonProperty
    String script;
    @JsonProperty
    Set<Role> scriptRoles = Collections.emptySet();

    private static final long serialVersionUID = -1L;
    private static final int version = 1;

    private void writeObject(ObjectOutputStream out) throws IOException {
        out.writeInt(version);
        out.writeUTF(engineName);
        out.writeUTF(script);
        out.writeObject(getScriptRoleXids());
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        int ver = in.readInt();

        if (ver >= 1) {
            this.engineName = in.readUTF();
            this.script = in.readUTF();
            @SuppressWarnings("unchecked")
            Set<String> roleXids = (Set<String>) in.readObject();
            setScriptRoleXids(roleXids);
        }
    }

    public MangoScript toMangoScript() {
        return new StringMangoScript(engineName, xid, scriptRoles, script);
    }

    public String getScript() {
        return script;
    }

    public void setScript(String script) {
        this.script = script;
    }

    public String getEngineName() {
        return engineName;
    }

    public void setEngineName(String engineName) {
        this.engineName = engineName;
    }

    public Set<Role> getScriptRoles() {
        return scriptRoles;
    }

    public void setScriptRoles(Set<Role> roles) {
        this.scriptRoles = roles;
    }

    private Set<String> getScriptRoleXids() {
        return scriptRoles.stream().map(Role::getXid).collect(Collectors.toSet());
    }

    private void setScriptRoleXids(Set<String> xids) {
        PermissionService permissionService = Common.getBean(PermissionService.class);
        this.scriptRoles = xids.stream().map(xid -> {
            Role r = permissionService.getRole(xid);
            return r != null ? r : null;
        }).filter(Objects::nonNull).collect(Collectors.toSet());
    }
}
