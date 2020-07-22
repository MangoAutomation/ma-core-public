/*
 * Copyright (C) 2019 Infinite Automation Software. All rights reserved.
 */
package com.serotonin.m2m2.vo.event;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

import com.infiniteautomation.mango.spring.script.MangoScript;
import com.infiniteautomation.mango.spring.script.StringMangoScript;
import com.infiniteautomation.mango.spring.service.PermissionService;
import com.serotonin.json.spi.JsonProperty;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.dao.RoleDao;
import com.serotonin.m2m2.rt.event.handlers.EventHandlerRT;
import com.serotonin.m2m2.rt.event.handlers.ScriptEventHandlerRT;
import com.serotonin.m2m2.vo.role.Role;
import com.serotonin.m2m2.vo.role.RoleVO;

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

    @Override
    public EventHandlerRT<ScriptEventHandlerVO> createRuntime() {
        PermissionService permissionService = Common.getBean(PermissionService.class);
        return permissionService.runAsSystemAdmin(() -> {
            return new ScriptEventHandlerRT(this);
        });
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
        return scriptRoles.stream().map(r -> r.getXid()).collect(Collectors.toSet());
    }

    private void setScriptRoleXids(Set<String> xids) {
        RoleDao roleDao = Common.getBean(RoleDao.class);
        this.scriptRoles = xids.stream().map(xid -> {
            RoleVO vo = roleDao.getByXid(xid);
            return vo != null ? vo.getRole() : null;
        }).filter(r -> r != null).collect(Collectors.toSet());
    }
}
