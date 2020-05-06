/**
 * Copyright (C) 2016 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.module;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.serotonin.db.pair.IntStringPair;
import com.serotonin.m2m2.db.dao.DataPointDao;
import com.serotonin.m2m2.db.dao.RoleDao.RoleDeletedDaoEvent;
import com.serotonin.m2m2.i18n.ProcessResult;
import com.serotonin.m2m2.util.VarNames;
import com.serotonin.m2m2.vo.event.AbstractEventHandlerVO;
import com.serotonin.m2m2.vo.permission.PermissionHolder;

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
     * Save any relational data to other tables
     * @param vo
     * @param insert (new VO or updating)
     */
    public void saveRelationalData(T vo, boolean insert) {

    }

    /**
     * Load the relational data
     * @param vo
     */
    public void loadRelationalData(T vo) {

    }

    /**
     * Delete any relational data that is no ON DELETE CASCADE
     * @param vo
     */
    public void deleteRelationalData(T vo) {

    }

    /**
     * Handle a role mapping being deleted
     * @param vo
     * @param event
     */
    public void handleRoleDeletedEvent(T vo, RoleDeletedDaoEvent event) {

    }

    /**
     * Validate a new event handler
     * @param response
     * @param ds
     * @param user
     */
    abstract public void validate(ProcessResult response, T ds, PermissionHolder user);

    /**
     * Validate an event handler that is about to be updated
     *  override as necessary
     * @param response
     * @param existing
     * @param ds
     * @param user
     */
    public void validate(ProcessResult response, T existing, T ds, PermissionHolder user) {
        validate(response, ds, user);
    }

    /**
     * Used by MA core code to create a new event handler instances as required. Should not be used by client code.
     */
    public final T baseCreateEventHandlerVO() {
        T handler = createEventHandlerVO();
        handler.setDefinition(this);
        return handler;
    }


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
