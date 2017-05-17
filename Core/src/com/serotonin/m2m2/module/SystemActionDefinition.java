/*
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.module;

import com.fasterxml.jackson.databind.JsonNode;
import com.serotonin.m2m2.util.timeout.SystemActionTask;

/**
 * This class proaction that can be actived via the REST system-action endpoint.
 * 
 * @author Terry Packer
 */
abstract public class SystemActionDefinition extends ModuleElementDefinition {

    /**
     * The reference key to the action.  Should be unique across all Modules and Mango Core
     * 
     * @return the reference key
     */
    abstract public String getKey();

    /**
     * Create the Task with input that will be scheduled and run
     * @param input
     * @return
     */
    abstract public SystemActionTask getTask(final JsonNode input);

}
