/*
 * Copyright (C) 2019 Infinite Automation Software. All rights reserved.
 */
package com.serotonin.m2m2.vo.event;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import com.infiniteautomation.mango.spring.service.RoleService;
import com.infiniteautomation.mango.util.exception.NotFoundException;
import com.serotonin.json.JsonException;
import com.serotonin.json.JsonReader;
import com.serotonin.json.ObjectWriter;
import com.serotonin.json.spi.JsonProperty;
import com.serotonin.json.type.JsonObject;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.rt.event.handlers.EventHandlerRT;
import com.serotonin.m2m2.rt.event.handlers.ScriptEventHandlerRT;
import com.serotonin.m2m2.vo.role.Role;

/**
 * @author Jared Wiltshire
 */
public class ScriptEventHandlerVO extends AbstractEventHandlerVO {

    @JsonProperty
    String engineName;
    @JsonProperty
    String script;

    Set<Role> scriptRoles = Collections.emptySet();

    @Override
    public EventHandlerRT<ScriptEventHandlerVO> createRuntime() {
        return new ScriptEventHandlerRT(this);
    }

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

    @Override
    public void jsonWrite(ObjectWriter writer) throws IOException, JsonException {
        super.jsonWrite(writer);
        writer.writeEntry("scriptRoles", getScriptRoleXids());
    }


    @Override
    public void jsonRead(JsonReader reader, JsonObject jsonObject) throws JsonException {
        super.jsonRead(reader, jsonObject);

        Set<String> roleXids = new HashSet<>();
        reader.readInto(roleXids, jsonObject.get("scriptRoles"));
        setScriptRoleXids(roleXids);
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
        return scriptRoles.stream().map(r -> r.getXid()).collect(Collectors.toSet());
    }

    private void setScriptRoleXids(Set<String> xids) {
        RoleService roleService = Common.getBean(RoleService.class);
        this.scriptRoles = xids.stream().map(xid -> {
            try {
                return roleService.get(xid).getRole();
            } catch (NotFoundException e) {
                return null;
            }
        }).filter(r -> r != null).collect(Collectors.toSet());
    }
}
