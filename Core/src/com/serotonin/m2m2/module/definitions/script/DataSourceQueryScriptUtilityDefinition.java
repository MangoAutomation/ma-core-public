/**
 * Copyright (C) 2018 Infinite Automation Software. All rights reserved. 
 * @author Phillip Dunlap
 */
package com.serotonin.m2m2.module.definitions.script;

import com.infiniteautomation.mango.util.script.ScriptUtility;
import com.serotonin.m2m2.module.MangoJavascriptContextObjectDefinition;
import com.serotonin.m2m2.rt.script.DataSourceQuery;

public class DataSourceQueryScriptUtilityDefinition extends MangoJavascriptContextObjectDefinition {

    @Override
    public String getContextKey() {
        return DataSourceQuery.CONTEXT_KEY;
    }

    @Override
    protected Class<? extends ScriptUtility> getUtilityClass() {
        return DataSourceQuery.class;
    }

    @Override
    protected Class<? extends ScriptUtility> getTestUtilityClass() {
        return DataSourceQuery.class;
    }

}
