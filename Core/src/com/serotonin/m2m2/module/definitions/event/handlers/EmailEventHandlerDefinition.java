/**
 * Copyright (C) 2016 Infinite Automation Software. All rights reserved.
 * 
 * @author Terry Packer
 */
package com.serotonin.m2m2.module.definitions.event.handlers;

import com.infiniteautomation.mango.spring.service.PermissionService;
import com.infiniteautomation.mango.util.script.ScriptPermissions;
import com.serotonin.m2m2.db.dao.RoleDao;
import com.serotonin.m2m2.module.EventHandlerDefinition;
import com.serotonin.m2m2.vo.event.AbstractEventHandlerVO;
import com.serotonin.m2m2.vo.event.EmailEventHandlerVO;

/**
 * @author Terry Packer
 *
 */
public class EmailEventHandlerDefinition extends EventHandlerDefinition<EmailEventHandlerVO> {

    public static final String TYPE_NAME = "EMAIL";
    public static final String DESC_KEY = "eventHandlers.type.email";
    public static final int EMAIL_SCRIPT_TYPE = 2;

    @Override
    public String getEventHandlerTypeName() {
        return TYPE_NAME;
    }

    @Override
    public String getDescriptionKey() {
        return DESC_KEY;
    }

    @Override
    protected EmailEventHandlerVO createEventHandlerVO() {
        return new EmailEventHandlerVO();
    }

    @Override
    public void saveRelationalData(AbstractEventHandlerVO<?> vo, boolean insert) {
        EmailEventHandlerVO eh = (EmailEventHandlerVO) vo;
        if (eh.getScriptRoles() != null) {
            RoleDao.getInstance().replaceRolesOnVoPermission(eh.getScriptRoles().getRoles(), eh,
                    PermissionService.SCRIPT, insert);
        }
    }

    @Override
    public void loadRelationalData(AbstractEventHandlerVO<?> vo) {
        EmailEventHandlerVO eh = (EmailEventHandlerVO) vo;
        eh.setScriptRoles(new ScriptPermissions(
                RoleDao.getInstance().getRoles(eh, PermissionService.SCRIPT)));
    }
    
    @Override
    public void deleteRelationalData(AbstractEventHandlerVO<?> vo) {
        RoleDao.getInstance().deleteRolesForVoPermission(vo, PermissionService.SCRIPT);
    }
}
