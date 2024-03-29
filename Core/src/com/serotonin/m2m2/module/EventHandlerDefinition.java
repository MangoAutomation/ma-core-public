/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
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
     */
    public void savePreRelationalData(T existing, T vo) {

    }

    /**
     * Save any relational data
     *
     * NOTE: this logic will be executed in a database transaction.
     *
     * @param existing - null on insert
     */
    public void saveRelationalData(T existing, T vo) {

    }

    /**
     * Load in relational data
     *
     */
    public void loadRelationalData(T vo) {

    }

    /**
     * Delete any relational data
     *
     * NOTE: this logic will be executed in a database transaction.
     */
    public void deleteRelationalData(T vo) {

    }

    /**
     * Delete any relational data
     *
     * NOTE: this logic will be executed in a database transaction.
     */
    public void deletePostRelationalData(T vo) {

    }

    /**
     * Handle a role being modified
     */
    @Deprecated
    // TODO We should store the permissions as an array as per PTCP data sources/publishers
    public void handleRoleEvent(T vo, DaoEvent<? extends RoleVO> event) {

    }

    /**
     * Validate a new event handler
     */
    abstract public void validate(ProcessResult response, T vo);

    /**
     * Validate an event handler that is about to be updated
     *  override as necessary
     */
    public void validate(ProcessResult response, T existing, T vo) {
        validate(response, vo);
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
