/**
 * Copyright (C) 2016 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.module.definitions.event.handlers;

import com.infiniteautomation.mango.spring.service.PermissionService;
import com.infiniteautomation.mango.util.script.ScriptPermissions;
import com.serotonin.m2m2.db.dao.RoleDao;
import com.serotonin.m2m2.module.EventHandlerDefinition;
import com.serotonin.m2m2.vo.event.AbstractEventHandlerVO;
import com.serotonin.m2m2.vo.event.SetPointEventHandlerVO;

/**
 * @author Terry Packer
 *
 */
public class SetPointEventHandlerDefinition extends EventHandlerDefinition<SetPointEventHandlerVO>{
	
	public static final String TYPE_NAME = "SET_POINT";
	public static final String DESC_KEY = "eventHandlers.type.setPoint";
	public static final int ACTIVE_SCRIPT_TYPE = 0;
	public static final int INACTIVE_SCRIPT_TYPE = 1;
	
	@Override
	public String getEventHandlerTypeName() {
		return TYPE_NAME;
	}

	@Override
	public String getDescriptionKey() {
		return DESC_KEY;
	}

	@Override
	protected SetPointEventHandlerVO createEventHandlerVO() {
		return new SetPointEventHandlerVO();
	}

	@Override
	public void saveRelationalData(AbstractEventHandlerVO<?> vo, boolean insert) {
	    SetPointEventHandlerVO eh = (SetPointEventHandlerVO)vo;
	    if(eh.getScriptRoles() != null) {
	        RoleDao.getInstance().replaceRolesOnVoPermission(eh.getScriptRoles().getRoles(), eh, PermissionService.SCRIPT, insert);
	    }
	}
	
	@Override
	public void loadRelationalData(AbstractEventHandlerVO<?> vo) {
	    SetPointEventHandlerVO eh = (SetPointEventHandlerVO)vo;
        eh.setScriptRoles(new ScriptPermissions(RoleDao.getInstance().getRoles(eh, PermissionService.SCRIPT)));
	}
}
