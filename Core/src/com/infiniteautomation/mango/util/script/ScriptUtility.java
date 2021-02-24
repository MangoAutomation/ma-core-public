/**
 * Copyright (C) 2018 Infinite Automation Software. All rights reserved.
 */
package com.infiniteautomation.mango.util.script;

import java.lang.reflect.Method;
import java.util.List;

import javax.script.Bindings;
import javax.script.ScriptEngine;

import org.springframework.beans.factory.annotation.Autowired;

import com.infiniteautomation.mango.spring.service.MangoJavaScriptService;
import com.infiniteautomation.mango.spring.service.PermissionService;
import com.serotonin.m2m2.rt.script.JsonImportExclusion;
import com.serotonin.m2m2.rt.script.ScriptLog;
import com.serotonin.m2m2.rt.script.ScriptPointValueSetter;
import com.serotonin.m2m2.vo.permission.PermissionHolder;

/**
 * Base class for Mango Javascript Utilities
 * 
 * @author Terry Packer
 */
public abstract class ScriptUtility {

    protected final static String NEWLINE = "\n";
    protected PermissionHolder permissions;
    protected ScriptEngine scriptEngine;
    protected final MangoJavaScriptService service;
    protected final PermissionService permissionService;
    protected ScriptLog log;
    //Used to track any actions performed (usually during testing when mocking a set/modify call)
    protected MangoJavaScriptResult result;
    
    @Autowired
    public ScriptUtility(MangoJavaScriptService service, PermissionService permissionService) {
        this.service = service;
        this.permissionService = permissionService;
    }
    
    /**
     * The key to use in the context for the utility
     * @return
     */
    public abstract String getContextKey();
    
    public void setPermissions(PermissionHolder holder) {
        this.permissions = holder;
    }
    
    public PermissionHolder getPermissions() {
        return permissions;
    }

    public ScriptEngine getScriptEngine() {
        return scriptEngine;
    }

    public void setScriptEngine(ScriptEngine scriptEngine) {
        this.scriptEngine = scriptEngine;
    }

    public void setScriptLog(ScriptLog log) {
        this.log = log;
    }
    
    public MangoJavaScriptResult getResult() {
        return result;
    }
    public void setResult(MangoJavaScriptResult result) {
        this.result = result;
    }
    
    public String getHelp() {
        StringBuilder builder = new StringBuilder();
        builder.append("{");
        builder.append(NEWLINE);
        for(Method method : this.getClass().getMethods()) {
            builder.append(method.getReturnType().getName());
            builder.append(" ");
            builder.append(method.getName());
            builder.append("(");
            Class<?>[] types =  method.getParameterTypes();
            int count = 0;
            for(Class<?> type : types) {
                builder.append(type.getName());
                count++;
                if(count != types.length)
                    builder.append(",");
            }
            builder.append(")");
            types =  method.getExceptionTypes();
            if(types.length > 0) {
                builder.append(" throws ");
                count = 0;
                for(Class<?> type : types) {
                    builder.append(type.getName());
                    count++;
                    if(count != types.length)
                        builder.append(",");
                }
            }
            builder.append(NEWLINE);
        }
        builder.append("}");
        return builder.toString();
    }

    @Override
    public String toString() {
        return getHelp();
    }
    
    public void takeContext(ScriptEngine engine, Bindings engineScope, 
            ScriptPointValueSetter setter, List<JsonImportExclusion> importExclusions, boolean testRun) {
        //because some utilities had other things in their constructor, offer this method to grab these al-a-carte
    }

}
