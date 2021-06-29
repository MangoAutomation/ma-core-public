/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.infiniteautomation.mango.util.script;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Terry Packer
 *
 */
public class MangoJavaScriptResult {

    //Things the script has actioned i.e. setting a point value
    private List<MangoJavaScriptAction> actions;
    //Errors from executing the script
    private List<MangoJavaScriptError> errors; 
    //Script log and console.log messages
    private String scriptOutput;
    //Returned value from script, can be null
    private Object result;
    
    /**
     * @return the actions
     */
    public List<MangoJavaScriptAction> getActions() {
        return actions;
    }

    /**
     * @param actions the actions to set
     */
    public void setActions(List<MangoJavaScriptAction> actions) {
        this.actions = actions;
    }

    /**
     * @return the errors
     */
    public List<MangoJavaScriptError> getErrors() {
        return errors;
    }

    /**
     * @param errors the errors to set
     */
    public void setErrors(List<MangoJavaScriptError> errors) {
        this.errors = errors;
    }

    /**
     * @return the scriptOutput
     */
    public String getScriptOutput() {
        return scriptOutput;
    }

    /**
     * @param scriptOutput the scriptOutput to set
     */
    public void setScriptOutput(String scriptOutput) {
        this.scriptOutput = scriptOutput;
    }

    /**
     * @return the result
     */
    public Object getResult() {
        return result;
    }

    /**
     * @param result the result to set
     */
    public void setResult(Object result) {
        this.result = result;
    }

    public void addAction(MangoJavaScriptAction action) {
        if(this.actions == null)
            this.actions = new ArrayList<>();
        this.actions.add(action);
    }
    
    public void addError(MangoJavaScriptError e) {
        if(this.errors == null)
            this.errors = new ArrayList<>();
        this.errors.add(e);
    }
    
    public boolean hasErrors() {
        return this.errors != null && this.errors.size() > 0;
    }
    
    /**
     * Reset the result for next execution
     */
    public void reset() {
        this.scriptOutput = null;
        this.result = null;
        this.errors = null;
        this.actions = null;
    }
    
}
