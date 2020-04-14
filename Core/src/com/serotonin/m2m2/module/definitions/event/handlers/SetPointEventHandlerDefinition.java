/**
 * Copyright (C) 2016 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.module.definitions.event.handlers;

import java.util.ArrayList;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;

import com.infiniteautomation.mango.permission.MangoPermission;
import com.infiniteautomation.mango.spring.service.MangoJavaScriptService;
import com.infiniteautomation.mango.spring.service.PermissionService;
import com.infiniteautomation.mango.util.script.ScriptPermissions;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.DataTypes;
import com.serotonin.m2m2.db.dao.DataPointDao;
import com.serotonin.m2m2.db.dao.RoleDao;
import com.serotonin.m2m2.i18n.ProcessResult;
import com.serotonin.m2m2.module.EventHandlerDefinition;
import com.serotonin.m2m2.rt.script.ScriptError;
import com.serotonin.m2m2.vo.DataPointVO;
import com.serotonin.m2m2.vo.event.SetPointEventHandlerVO;
import com.serotonin.m2m2.vo.permission.PermissionHolder;

/**
 * @author Terry Packer
 *
 */
public class SetPointEventHandlerDefinition extends EventHandlerDefinition<SetPointEventHandlerVO>{

    public static final String TYPE_NAME = "SET_POINT";
    public static final String DESC_KEY = "eventHandlers.type.setPoint";
    public static final int ACTIVE_SCRIPT_TYPE = 0;
    public static final int INACTIVE_SCRIPT_TYPE = 1;

    @Autowired
    PermissionService service;

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
    public void saveRelationalData(SetPointEventHandlerVO eh, boolean insert) {
        if(eh.getScriptRoles() != null) {
            RoleDao.getInstance().replaceRolesOnVoPermission(eh.getScriptRoles().getPermission(), eh, PermissionService.SCRIPT, insert);
        }
    }

    @Override
    public void loadRelationalData(SetPointEventHandlerVO eh) {
        eh.setScriptRoles(new ScriptPermissions(RoleDao.getInstance().getPermission(eh, PermissionService.SCRIPT)));
    }

    @Override
    public void deleteRelationalData(SetPointEventHandlerVO eh) {
        RoleDao.getInstance().deleteRolesForVoPermission(eh, PermissionService.SCRIPT);
    }

    @Override
    public void validate(ProcessResult result, SetPointEventHandlerVO vo, PermissionHolder savingUser) {
        commonValidation(result, vo, savingUser);
        if(vo.getScriptRoles() != null) {
            service.validateVoRoles(result, "scriptRoles", savingUser, false, null, vo.getScriptRoles().getPermission());
        }
    }

    @Override
    public void validate(ProcessResult result, SetPointEventHandlerVO existing, SetPointEventHandlerVO vo, PermissionHolder savingUser) {
        commonValidation(result, vo, savingUser);
        if (vo.getScriptRoles() == null) {
            result.addContextualMessage("scriptRoles", "validate.permission.null");
        }else {
            MangoPermission existingPermission = existing.getScriptRoles() == null ? null : existing.getScriptRoles().getPermission();
            service.validateVoRoles(result, "scriptRoles", savingUser, false,
                    existingPermission, vo.getScriptRoles().getPermission());
        }
    }

    private void commonValidation(ProcessResult response, SetPointEventHandlerVO vo, PermissionHolder savingUser) {
        DataPointVO dp = DataPointDao.getInstance().get(vo.getTargetPointId());

        int dataType = DataTypes.UNKNOWN;
        if (dp == null)
            response.addContextualMessage("targetPointId", "eventHandlers.noTargetPoint");
        else {
            dataType = dp.getPointLocator().getDataTypeId();
            if(!dp.getPointLocator().isSettable())
                response.addContextualMessage("targetPointId", "event.setPoint.targetNotSettable");
        }

        if (vo.getActiveAction() == SetPointEventHandlerVO.SET_ACTION_NONE && vo.getInactiveAction() == SetPointEventHandlerVO.SET_ACTION_NONE) {
            response.addContextualMessage("activeAction", "eventHandlers.noSetPointAction");
            response.addContextualMessage("inactiveAction", "eventHandlers.noSetPointAction");
        }
        MangoJavaScriptService javaScriptService = Common.getBean(MangoJavaScriptService.class);
        // Active
        if (vo.getActiveAction() == SetPointEventHandlerVO.SET_ACTION_STATIC_VALUE && dataType == DataTypes.MULTISTATE) {
            try {
                Integer.parseInt(vo.getActiveValueToSet());
            }
            catch (NumberFormatException e) {
                response.addContextualMessage("activeValueToSet", "eventHandlers.invalidActiveValue");
            }
        }
        else if (vo.getActiveAction() == SetPointEventHandlerVO.SET_ACTION_STATIC_VALUE && dataType == DataTypes.NUMERIC) {
            try {
                Double.parseDouble(vo.getActiveValueToSet());
            }
            catch (NumberFormatException e) {
                response.addContextualMessage("activeValueToSet", "eventHandlers.invalidActiveValue");
            }
        }
        else if (vo.getActiveAction() == SetPointEventHandlerVO.SET_ACTION_POINT_VALUE) {
            DataPointVO dpActive = DataPointDao.getInstance().get(vo.getActivePointId());

            if (dpActive == null)
                response.addContextualMessage("activePointId", "eventHandlers.invalidActiveSource");
            else if (dataType != dpActive.getPointLocator().getDataTypeId())
                response.addContextualMessage("activeDataPointId", "eventHandlers.invalidActiveSourceType");
        }
        else if (vo.getActiveAction() == SetPointEventHandlerVO.SET_ACTION_SCRIPT_VALUE) {
            if(StringUtils.isEmpty(vo.getActiveScript())) {
                response.addContextualMessage("activeScript", "eventHandlers.invalidActiveScript");
            }else {
                javaScriptService.getPermissionService().runAs(vo.getScriptRoles(), () -> {
                    try {
                        javaScriptService.compile(vo.getActiveScript(), true);
                    } catch(ScriptError e) {
                        response.addContextualMessage("activeScript", "eventHandlers.invalidActiveScriptError", e.getTranslatableMessage());
                    }
                });
            }
        }

        // Inactive
        if (vo.getInactiveAction() == SetPointEventHandlerVO.SET_ACTION_STATIC_VALUE && dataType == DataTypes.MULTISTATE) {
            try {
                Integer.parseInt(vo.getInactiveValueToSet());
            }
            catch (NumberFormatException e) {
                response.addContextualMessage("inactiveValueToSet", "eventHandlers.invalidInactiveValue");
            }
        }
        else if (vo.getInactiveAction() == SetPointEventHandlerVO.SET_ACTION_STATIC_VALUE && dataType == DataTypes.NUMERIC) {
            try {
                Double.parseDouble(vo.getInactiveValueToSet());
            }
            catch (NumberFormatException e) {
                response.addContextualMessage("inactiveValueToSet", "eventHandlers.invalidInactiveValue");
            }
        }
        else if (vo.getInactiveAction() == SetPointEventHandlerVO.SET_ACTION_POINT_VALUE) {
            DataPointVO dpInactive = DataPointDao.getInstance().get(vo.getInactivePointId());

            if (dpInactive == null)
                response.addContextualMessage("inactivePointId", "eventHandlers.invalidInactiveSource");
            else if (dataType != dpInactive.getPointLocator().getDataTypeId())
                response.addContextualMessage("inactivePointId", "eventHandlers.invalidInactiveSourceType");
        }
        else if (vo.getInactiveAction() == SetPointEventHandlerVO.SET_ACTION_SCRIPT_VALUE) {
            if(StringUtils.isEmpty(vo.getInactiveScript())) {
                response.addContextualMessage("inactiveScript", "eventHandlers.invalidInactiveScript");
            }else {
                javaScriptService.getPermissionService().runAs(vo.getScriptRoles(), () -> {
                    try {
                        javaScriptService.compile(vo.getInactiveScript(), true);
                    } catch(ScriptError e) {
                        response.addContextualMessage("inactiveScript", "eventHandlers.invalidActiveScriptError", e.getTranslatableMessage());
                    }
                });
            }
        }

        if(vo.getAdditionalContext() != null)
            validateScriptContext(vo.getAdditionalContext(), response);
        else
            vo.setAdditionalContext(new ArrayList<>());
    }

}
