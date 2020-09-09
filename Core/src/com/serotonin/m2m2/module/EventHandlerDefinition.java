/**
 * Copyright (C) 2016 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.module;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.infiniteautomation.mango.spring.events.DaoEvent;
import com.serotonin.db.pair.IntStringPair;
import com.serotonin.m2m2.db.dao.DataPointDao;
import com.serotonin.m2m2.i18n.ProcessResult;
import com.serotonin.m2m2.rt.event.handlers.EventHandlerRT;
import com.serotonin.m2m2.util.VarNames;
import com.serotonin.m2m2.vo.event.AbstractEventHandlerVO;
import com.serotonin.m2m2.vo.permission.PermissionHolder;
import com.serotonin.m2m2.vo.role.RoleVO;

/**
 * Provides modules with the ability to register additional event handlers
 *
 * @author Terry Packer
 *
 */
public abstract class EventHandlerDefinition<T extends AbstractEventHandlerVO> extends ModuleElementDefinition{

    /**
     * An internal identifier for this type of Event Handler. Must be unique within an MA instance, and is recommended
     * to have the form "&lt;moduleType&gt;.&lt;modelName&gt;" so as to be unique across all modules.
     *
     * @return the event handler type name.
     */
    abstract public String getEventHandlerTypeName();

    /**
     * A reference to a human readable and translatable brief description of the handler. Key reference values in
     * i18n.properties files. Descriptions are used in drop down select boxes, and so should be as brief as possible.
     *
     * @return the reference key to the handler's short description.
     */
    abstract public String getDescriptionKey();

    /**
     * Create and return an instance of the event handler
     *
     * @return a new instance of the event handler.
     */
    abstract protected T createEventHandlerVO();

    /**
     * Save any relational data for prior
     * to the VO being saved
     *
     * NOTE: this logic will be executed in a database transaction.
     *
     * @param existing - null on insert
     * @param vo
     */
    public void savePreRelationalData(T existing, T vo) {

    }

    /**
     * Save any relational data
     *
     * NOTE: this logic will be executed in a database transaction.
     *
     * @param existing - null on insert
     * @param vo
     */
    public void saveRelationalData(T existing, T vo) {

    }

    /**
     * Load in relational data
     *
     * @param vo
     */
    public void loadRelationalData(T vo) {

    }

    /**
     * Delete any relational data
     *
     * NOTE: this logic will be executed in a database transaction.
     * @param vo
     */
    public void deleteRelationalData(T vo) {

    }

    /**
     * Delete any relational data
     *
     * NOTE: this logic will be executed in a database transaction.
     * @param vo
     */
    public void deletePostRelationalData(T vo) {

    }

    /**
     * Handle a role being modified
     * @param vo
     * @param event
     */
    public void handleRoleEvent(T vo, DaoEvent<? extends RoleVO> event) {

    }

    /**
     * Validate a new event handler
     * @param response
     * @param ds
     * @param user
     */
    abstract public void validate(ProcessResult response, T vo, PermissionHolder user);

    /**
     * Validate an event handler that is about to be updated
     *  override as necessary
     * @param response
     * @param existing
     * @param ds
     * @param user
     */
    public void validate(ProcessResult response, T existing, T vo, PermissionHolder user) {
        validate(response, vo, user);
    }

    /**
     * Used by MA core code to create a new event handler instances as required. Should not be used by client code.
     */
    public final T baseCreateEventHandlerVO() {
        T handler = createEventHandlerVO();
        handler.setDefinition(this);
        return handler;
    }

    /**
     * Create the runtime handler
     * @return
     */
    public abstract EventHandlerRT<T> createRuntime(T vo);

    protected void validateScriptContext(List<IntStringPair> additionalContext, ProcessResult response) {
        List<String> varNameSpace = new ArrayList<String>();

        int pos = 0;
        for(IntStringPair cxt : additionalContext) {
            if(DataPointDao.getInstance().getXidById(cxt.getKey()) == null)
                response.addContextualMessage("scriptContext[" + pos + "].id", "event.script.contextPointMissing", cxt.getValue(), cxt.getKey());

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
}
