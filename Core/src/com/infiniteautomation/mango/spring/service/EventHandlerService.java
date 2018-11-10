/**
 * Copyright (C) 2018  Infinite Automation Software. All rights reserved.
 */
package com.infiniteautomation.mango.spring.service;

import java.util.ArrayList;
import java.util.List;

import javax.script.ScriptException;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.serotonin.ShouldNeverHappenException;
import com.serotonin.db.pair.IntStringPair;
import com.serotonin.m2m2.DataTypes;
import com.serotonin.m2m2.db.dao.AbstractDao;
import com.serotonin.m2m2.db.dao.DataPointDao;
import com.serotonin.m2m2.i18n.ProcessResult;
import com.serotonin.m2m2.rt.script.CompiledScriptExecutor;
import com.serotonin.m2m2.util.VarNames;
import com.serotonin.m2m2.vo.DataPointVO;
import com.serotonin.m2m2.vo.User;
import com.serotonin.m2m2.vo.event.AbstractEventHandlerVO;
import com.serotonin.m2m2.vo.event.EmailEventHandlerVO;
import com.serotonin.m2m2.vo.event.ProcessEventHandlerVO;
import com.serotonin.m2m2.vo.event.SetPointEventHandlerVO;
import com.serotonin.m2m2.vo.permission.PermissionHolder;

/**
 * @author Terry Packer
 *
 */
@Service
public class EventHandlerService extends AbstractVOService<AbstractEventHandlerVO<?>> {

    @Autowired
    public EventHandlerService(AbstractDao<AbstractEventHandlerVO<?>> dao) {
        super(dao);
    }

    @Override
    public boolean hasCreatePermission(PermissionHolder user) {
        // TODO Auto-generated method stub
        return user.hasAdminPermission();
    }

    @Override
    public boolean hasEditPermission(PermissionHolder user, AbstractEventHandlerVO<?> vo) {
        // TODO Auto-generated method stub
        return user.hasAdminPermission();
    }

    @Override
    public boolean hasReadPermission(PermissionHolder user, AbstractEventHandlerVO<?> vo) {
        // TODO Auto-generated method stub
        return user.hasAdminPermission();
    }

    /**
     * TODO make modular
     */
    @Override
    protected void validateImpl(AbstractEventHandlerVO<?> vo, User user, ProcessResult result) {
        if(vo instanceof EmailEventHandlerVO) {
            validateEmailHandlerImpl((EmailEventHandlerVO) vo, user, result);
        }else if(vo instanceof ProcessEventHandlerVO) {
            validateProcessHandler((ProcessEventHandlerVO)vo, user, result);
        }else if(vo instanceof SetPointEventHandlerVO) {
            validateSetPointHandler((SetPointEventHandlerVO)vo, user, result);
        }else {
            throw new ShouldNeverHappenException("Un-implemented model for " + vo.getClass().getName());
        }
    }
    
    /**
     * TODO - Review and cleanup generic messages
     * @param vo
     * @param user
     * @param response
     */
    protected void validateEmailHandlerImpl(EmailEventHandlerVO vo, User user, ProcessResult response) {
        if (vo.isSendEscalation()) {
            if (vo.getEscalationDelay() <= 0)
                response.addContextualMessage("escalationDelay", "eventHandlers.escalDelayError");
        } else if(vo.isRepeatEscalations())
            vo.setRepeatEscalations(false);
        
        List<String> varNameSpace = new ArrayList<String>();
        if(vo.getAdditionalContext() != null){
            for(IntStringPair cxt : vo.getAdditionalContext()) {
                if(DataPointDao.getInstance().get(cxt.getKey()) == null)
                    response.addGenericMessage("event.script.contextPointMissing", cxt.getKey(), cxt.getValue());
                
                String varName = cxt.getValue();
                if (StringUtils.isBlank(varName)) {
                    response.addGenericMessage("validate.allVarNames");
                    break;
                }
    
                if (!VarNames.validateVarName(varName)) {
                    response.addGenericMessage("validate.invalidVarName", varName);
                    break;
                }
    
                if (varNameSpace.contains(varName)) {
                    response.addGenericMessage("validate.duplicateVarName", varName);
                    break;
                }
    
                varNameSpace.add(varName);
            }
        }else{
            vo.setAdditionalContext(new ArrayList<>());
        }
        
        if(!StringUtils.isEmpty(vo.getScript())) {
            try {
                CompiledScriptExecutor.compile(vo.getScript());
            } catch(ScriptException e) {
                response.addGenericMessage("eventHandlers.invalidActiveScriptError", e.getMessage() == null ? e.getCause().getMessage() : e.getMessage());
            }
        }
    }

    /**
     * TODO Review and cleanup
     * @param vo
     * @param user
     * @param response
     */
    protected void validateSetPointHandler(SetPointEventHandlerVO vo, User user, ProcessResult response) {
        DataPointVO dp = DataPointDao.getInstance().getDataPoint(vo.getTargetPointId(), false);

        if (dp == null)
            response.addGenericMessage("eventHandlers.noTargetPoint");
        else {
            int dataType = dp.getPointLocator().getDataTypeId();

            if (vo.getActiveAction() == SetPointEventHandlerVO.SET_ACTION_NONE && vo.getInactiveAction() == SetPointEventHandlerVO.SET_ACTION_NONE)
                response.addGenericMessage("eventHandlers.noSetPointAction");

            // Active
            if (vo.getActiveAction() == SetPointEventHandlerVO.SET_ACTION_STATIC_VALUE && dataType == DataTypes.MULTISTATE) {
                try {
                    Integer.parseInt(vo.getActiveValueToSet());
                }
                catch (NumberFormatException e) {
                    response.addGenericMessage("eventHandlers.invalidActiveValue");
                }
            }
            else if (vo.getActiveAction() == SetPointEventHandlerVO.SET_ACTION_STATIC_VALUE && dataType == DataTypes.NUMERIC) {
                try {
                    Double.parseDouble(vo.getActiveValueToSet());
                }
                catch (NumberFormatException e) {
                    response.addGenericMessage("eventHandlers.invalidActiveValue");
                }
            }
            else if (vo.getActiveAction() == SetPointEventHandlerVO.SET_ACTION_POINT_VALUE) {
                DataPointVO dpActive = DataPointDao.getInstance().getDataPoint(vo.getActivePointId(), false);

                if (dpActive == null)
                    response.addGenericMessage("eventHandlers.invalidActiveSource");
                else if (dataType != dpActive.getPointLocator().getDataTypeId())
                    response.addGenericMessage("eventHandlers.invalidActiveSourceType");
            }
            else if (vo.getActiveAction() == SetPointEventHandlerVO.SET_ACTION_SCRIPT_VALUE) {
                if(StringUtils.isEmpty(vo.getActiveScript()))
                    response.addGenericMessage("eventHandlers.invalidActiveScript");
                try {
                    CompiledScriptExecutor.compile(vo.getActiveScript());
                } catch(ScriptException e) {
                    response.addGenericMessage("eventHandlers.invalidActiveScriptError", e.getMessage() == null ? e.getCause().getMessage() : e.getMessage());
                }
            }

            // Inactive
            if (vo.getInactiveAction() == SetPointEventHandlerVO.SET_ACTION_STATIC_VALUE && dataType == DataTypes.MULTISTATE) {
                try {
                    Integer.parseInt(vo.getInactiveValueToSet());
                }
                catch (NumberFormatException e) {
                    response.addGenericMessage("eventHandlers.invalidInactiveValue");
                }
            }
            else if (vo.getInactiveAction() == SetPointEventHandlerVO.SET_ACTION_STATIC_VALUE && dataType == DataTypes.NUMERIC) {
                try {
                    Double.parseDouble(vo.getInactiveValueToSet());
                }
                catch (NumberFormatException e) {
                    response.addGenericMessage("eventHandlers.invalidInactiveValue");
                }
            }
            else if (vo.getInactiveAction() == SetPointEventHandlerVO.SET_ACTION_POINT_VALUE) {
                DataPointVO dpInactive = DataPointDao.getInstance().getDataPoint(vo.getInactivePointId(), false);

                if (dpInactive == null)
                    response.addGenericMessage("eventHandlers.invalidInactiveSource");
                else if (dataType != dpInactive.getPointLocator().getDataTypeId())
                    response.addGenericMessage("eventHandlers.invalidInactiveSourceType");
            }
            else if (vo.getInactiveAction() == SetPointEventHandlerVO.SET_ACTION_SCRIPT_VALUE) {
                if(StringUtils.isEmpty(vo.getInactiveScript()))
                    response.addGenericMessage("eventHandlers.invalidInactiveScript");
                try {
                    CompiledScriptExecutor.compile(vo.getInactiveScript());
                } catch(ScriptException e) {
                    response.addGenericMessage("eventHandlers.invalidInactiveScriptError", e.getMessage() == null ? e.getCause().getMessage() : e.getMessage());
                }
            }
            
            List<String> varNameSpace = new ArrayList<String>();
            varNameSpace.add(SetPointEventHandlerVO.TARGET_CONTEXT_KEY);
            for(IntStringPair cxt : vo.getAdditionalContext()) {
                if(DataPointDao.getInstance().getDataPoint(cxt.getKey(), false) == null)
                    response.addGenericMessage("event.script.contextPointMissing", cxt.getKey(), cxt.getValue());
                
                String varName = cxt.getValue();
                if (StringUtils.isBlank(varName)) {
                    response.addGenericMessage("validate.allVarNames");
                    break;
                }

                if (!VarNames.validateVarName(varName)) {
                    response.addGenericMessage("validate.invalidVarName", varName);
                    break;
                }

                if (varNameSpace.contains(varName)) {
                    response.addGenericMessage("validate.duplicateVarName", varName);
                    break;
                }

                varNameSpace.add(varName);
            }
        }
    }
    
    /**
     * TODO Review and cleanup remove generic message
     * @param vo
     * @param user
     * @param response
     */
    protected void validateProcessHandler(ProcessEventHandlerVO vo, User user, ProcessResult response) {
        if (StringUtils.isBlank(vo.getActiveProcessCommand()) && StringUtils.isBlank(vo.getInactiveProcessCommand()))
            response.addGenericMessage("eventHandlers.invalidCommands");

        if (!StringUtils.isBlank(vo.getActiveProcessCommand()) && vo.getActiveProcessTimeout() <= 0)
            response.addGenericMessage("validate.greaterThanZero");

        if (!StringUtils.isBlank(vo.getInactiveProcessCommand()) && vo.getInactiveProcessTimeout() <= 0)
            response.addGenericMessage("validate.greaterThanZero");
    }
}
