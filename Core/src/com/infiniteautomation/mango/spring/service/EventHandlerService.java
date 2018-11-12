/**
 * Copyright (C) 2018  Infinite Automation Software. All rights reserved.
 */
package com.infiniteautomation.mango.spring.service;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import javax.script.ScriptException;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.serotonin.ShouldNeverHappenException;
import com.serotonin.db.pair.IntStringPair;
import com.serotonin.m2m2.Common;
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
import com.serotonin.m2m2.vo.mailingList.EmailRecipient;
import com.serotonin.m2m2.vo.permission.PermissionHolder;
import com.serotonin.m2m2.web.dwr.beans.RecipientListEntryBean;

import freemarker.template.Template;

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
     * @param vo
     * @param user
     * @param response
     */
    protected void validateEmailHandlerImpl(EmailEventHandlerVO vo, User user, ProcessResult response) {
        if(vo.getActiveRecipients() != null) {
            int pos = 0;
            for(RecipientListEntryBean b : vo.getActiveRecipients()) {
                validateRecipient("activeRecipients[" + pos + "]", b, user, response);
                pos++;
            }
        }
        
        if (vo.isSendEscalation()) {
            if (vo.getEscalationDelay() <= 0)
                response.addContextualMessage("escalationDelay", "eventHandlers.escalDelayError");
            if(!Common.TIME_PERIOD_CODES.isValidId(vo.getEscalationDelayType()))
                response.addContextualMessage("escalationDelayType", "validate.invalidValue");
            if(vo.getEscalationRecipients() != null) {
                int pos = 0;
                for(RecipientListEntryBean b : vo.getEscalationRecipients()) {
                    validateRecipient("escalationRecipients[" + pos + "]", b, user, response);
                    pos++;
                }
            }
        } else if(vo.isRepeatEscalations())
            vo.setRepeatEscalations(false);
        
        try {
            new Template("customTemplate", new StringReader(vo.getCustomTemplate()), Common.freemarkerConfiguration);
        }catch(Exception e) {
            response.addContextualMessage("customTemplate", "common.default", e.getMessage());
        }
        
        if(vo.getAdditionalContext() != null)
            validateScriptContext(vo.getAdditionalContext(), user, response);
        else
            vo.setAdditionalContext(new ArrayList<>());
        
        if(!StringUtils.isEmpty(vo.getScript())) {
            try {
                CompiledScriptExecutor.compile(vo.getScript());
            } catch(ScriptException e) {
                response.addGenericMessage("eventHandlers.invalidActiveScriptError", e.getMessage() == null ? e.getCause().getMessage() : e.getMessage());
            }
        }
        //TODO Review this as per adding permissions
        if(vo.getScriptPermissions() != null) {
            vo.getScriptPermissions().validate(response, user);
        }
    }

    /**
     * @param b
     * @param user
     * @param response
     */
    private void validateRecipient(String prefix, RecipientListEntryBean b, User user, ProcessResult response) {
        switch(b.getRecipientType()) {
            case EmailRecipient.TYPE_MAILING_LIST:
                if(b.getReferenceId() < 1)
                    response.addContextualMessage(prefix, "validate.invalidValue");
                break;
            case EmailRecipient.TYPE_USER:
                if(b.getReferenceId() < 1)
                    response.addContextualMessage(prefix, "validate.invalidValue");
                break;
            case EmailRecipient.TYPE_ADDRESS:
                //TODO Validate email format?
                break;
        }        
    }

    /**
     * @param vo
     * @param user
     * @param response
     */
    protected void validateSetPointHandler(SetPointEventHandlerVO vo, User user, ProcessResult response) {
        DataPointVO dp = DataPointDao.getInstance().getDataPoint(vo.getTargetPointId(), false);

        if (dp == null)
            response.addContextualMessage("targetPointXid", "eventHandlers.noTargetPoint");

        int dataType = dp == null ? DataTypes.UNKNOWN : dp.getPointLocator().getDataTypeId();

        if (vo.getActiveAction() == SetPointEventHandlerVO.SET_ACTION_NONE && vo.getInactiveAction() == SetPointEventHandlerVO.SET_ACTION_NONE) {
            response.addContextualMessage("activeAction", "eventHandlers.noSetPointAction");
            response.addContextualMessage("inactiveAction", "eventHandlers.noSetPointAction");
        }

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
                response.addContextualMessage("activeValue", "eventHandlers.invalidActiveValue");
            }
        }
        else if (vo.getActiveAction() == SetPointEventHandlerVO.SET_ACTION_POINT_VALUE) {
            DataPointVO dpActive = DataPointDao.getInstance().getDataPoint(vo.getActivePointId(), false);

            if (dpActive == null)
                response.addContextualMessage("activePointXid", "eventHandlers.invalidActiveSource");
            else if (dataType != dpActive.getPointLocator().getDataTypeId())
                response.addContextualMessage("activeDataPointXid", "eventHandlers.invalidActiveSourceType");
        }
        else if (vo.getActiveAction() == SetPointEventHandlerVO.SET_ACTION_SCRIPT_VALUE) {
            if(StringUtils.isEmpty(vo.getActiveScript()))
                response.addContextualMessage("activeScript", "eventHandlers.invalidActiveScript");
            try {
                CompiledScriptExecutor.compile(vo.getActiveScript());
            } catch(ScriptException e) {
                response.addContextualMessage("activeScript", "eventHandlers.invalidActiveScriptError", e.getMessage() == null ? e.getCause().getMessage() : e.getMessage());
            }
        }

        // Inactive
        if (vo.getInactiveAction() == SetPointEventHandlerVO.SET_ACTION_STATIC_VALUE && dataType == DataTypes.MULTISTATE) {
            try {
                Integer.parseInt(vo.getInactiveValueToSet());
            }
            catch (NumberFormatException e) {
                response.addContextualMessage("inactiveAction", "eventHandlers.invalidInactiveValue");
            }
        }
        else if (vo.getInactiveAction() == SetPointEventHandlerVO.SET_ACTION_STATIC_VALUE && dataType == DataTypes.NUMERIC) {
            try {
                Double.parseDouble(vo.getInactiveValueToSet());
            }
            catch (NumberFormatException e) {
                response.addContextualMessage("inactiveValue", "eventHandlers.invalidInactiveValue");
            }
        }
        else if (vo.getInactiveAction() == SetPointEventHandlerVO.SET_ACTION_POINT_VALUE) {
            DataPointVO dpInactive = DataPointDao.getInstance().getDataPoint(vo.getInactivePointId(), false);

            if (dpInactive == null)
                response.addContextualMessage("inactivePointXid", "eventHandlers.invalidInactiveSource");
            else if (dataType != dpInactive.getPointLocator().getDataTypeId())
                response.addContextualMessage("inactivePointXid", "eventHandlers.invalidInactiveSourceType");
        }
        else if (vo.getInactiveAction() == SetPointEventHandlerVO.SET_ACTION_SCRIPT_VALUE) {
            if(StringUtils.isEmpty(vo.getInactiveScript()))
                response.addContextualMessage("inactiveScript", "eventHandlers.invalidInactiveScript");
            try {
                CompiledScriptExecutor.compile(vo.getInactiveScript());
            } catch(ScriptException e) {
                response.addContextualMessage("inactiveScript", "eventHandlers.invalidInactiveScriptError", e.getMessage() == null ? e.getCause().getMessage() : e.getMessage());
            }
        }
        
        if(vo.getAdditionalContext() != null)
            validateScriptContext(vo.getAdditionalContext(), user, response);
        else
            vo.setAdditionalContext(new ArrayList<>());


    }
    
    /**
     * @param additionalContext
     * @param user
     * @param response
     */
    private void validateScriptContext(List<IntStringPair> additionalContext, User user,
            ProcessResult response) {
        List<String> varNameSpace = new ArrayList<String>();
        
        int pos = 0;
        for(IntStringPair cxt : additionalContext) {
            if(DataPointDao.getInstance().get(cxt.getKey()) == null)
                response.addContextualMessage("scriptContext[" + pos + "].xid", "event.script.contextPointMissing", cxt.getValue(), cxt.getKey());
            
            String varName = cxt.getValue();
            if (StringUtils.isBlank(varName)) {
                response.addContextualMessage("scriptContext[" + pos + "].varaibleName", "validate.allVarNames");
                break;
            }

            if (!VarNames.validateVarName(varName)) {
                response.addContextualMessage("scriptContext[" + pos + "].varaibleName","validate.invalidVarName", varName);
                break;
            }

            if (varNameSpace.contains(varName)) {
                response.addContextualMessage("scriptContext[" + pos + "].variableName", "validate.duplicateVarName", varName);
                break;
            }

            varNameSpace.add(varName);
            pos++;
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
