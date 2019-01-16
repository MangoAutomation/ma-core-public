/**
 * Copyright (C) 2018 Infinite Automation Software. All rights reserved. 
 * @author Phillip Dunlap
 */
package com.serotonin.m2m2.module.definitions.script;

import com.infiniteautomation.mango.util.script.ScriptUtility;
import com.serotonin.m2m2.module.MangoJavascriptContextObjectDefinition;
import com.serotonin.m2m2.rt.script.DataPointQuery;

public class DataPointQueryScriptUtilityDefinition extends MangoJavascriptContextObjectDefinition {

    @Override
    protected Class<? extends ScriptUtility> getUtilityClass() {
        return DataPointQuery.class;
    }

    @Override
    protected Class<? extends ScriptUtility> getTestUtilityClass() {
        return DataPointQuery.class;
    }

}
