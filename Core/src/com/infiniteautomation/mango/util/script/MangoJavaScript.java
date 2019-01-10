/**
 * Copyright (C) 2019  Infinite Automation Software. All rights reserved.
 */
package com.infiniteautomation.mango.util.script;

import java.util.List;

import com.serotonin.m2m2.rt.script.ScriptContextVariable;
import com.serotonin.m2m2.rt.script.ScriptPermissions;

/**
 * Container for script execution/validation/testing
 * @author Terry Packer
 *
 */
public class MangoJavaScript {
    private boolean wrapInFunction;  //Should this be wrapped and executed as a function i.e. meta points do this
    private String script;
    private List<ScriptContextVariable> context;
    private String permissions;
    private ScriptLogLevels logLevel;
    //If non-null coerce the result into a PointValueTime with this data type
    private Integer resultDataTypeId; 
    
    
    /**
     * @return the wrapInFunction
     */
    public boolean isWrapInFunction() {
        return wrapInFunction;
    }
    /**
     * @param wrapInFunction the wrapInFunction to set
     */
    public void setWrapInFunction(boolean wrapInFunction) {
        this.wrapInFunction = wrapInFunction;
    }
    /**
     * @return the script
     */
    public String getScript() {
        return script;
    }
    /**
     * @param script the script to set
     */
    public void setScript(String script) {
        this.script = script;
    }
    /**
     * @return the context
     */
    public List<ScriptContextVariable> getContext() {
        return context;
    }
    /**
     * @param context the context to set
     */
    public void setContext(List<ScriptContextVariable> context) {
        this.context = context;
    }
    /**
     * @return the permissions
     */
    public String getPermissions() {
        return permissions;
    }
    /**
     * @param permissions the permissions to set
     */
    public void setPermissions(String permissions) {
        this.permissions = permissions;
    }
    /**
     * @return the logLevel
     */
    public ScriptLogLevels getLogLevel() {
        return logLevel;
    }
    /**
     * @param logLevel the logLevel to set
     */
    public void setLogLevel(ScriptLogLevels logLevel) {
        this.logLevel = logLevel;
    }
    
    /**
     * @return the resultDataTypeId
     */
    public Integer getResultDataTypeId() {
        return resultDataTypeId;
    }
    
    /**
     * @param resultDataTypeId the resultDataTypeId to set
     */
    public void setResultDataTypeId(Integer resultDataTypeId) {
        this.resultDataTypeId = resultDataTypeId;
    }
    
    public ScriptPermissions createScriptPermissions() {
        //TODO Clean up 
        ScriptPermissions permissions = new ScriptPermissions();
        permissions.setDataPointReadPermissions(this.permissions);
        return permissions;
    }
}
