/**
 * Copyright (C) 2018 Infinite Automation Software. All rights reserved.
 */
package com.infiniteautomation.mango.util.script;

import java.lang.reflect.Method;
import java.util.List;

import javax.script.Bindings;
import javax.script.ScriptEngine;

import com.serotonin.m2m2.rt.script.JsonImportExclusion;
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
    
    public void setPermissions(PermissionHolder holder) {
        this.permissions = holder;
    }
    
    public PermissionHolder getPermissions() {
        return permissions;
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
