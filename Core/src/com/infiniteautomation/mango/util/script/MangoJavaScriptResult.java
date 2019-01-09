/**
 * Copyright (C) 2019  Infinite Automation Software. All rights reserved.
 */
package com.infiniteautomation.mango.util.script;

import java.util.ArrayList;
import java.util.List;

import com.serotonin.m2m2.i18n.TranslatableMessage;

/**
 * @author Terry Packer
 *
 */
public class MangoJavaScriptResult {

    //Things the script has actioned i.e. setting a point value
    private List<TranslatableMessage> actions;
    //Errors from executing the script
    private List<TranslatableMessage> errors;
    //Script log and console.log messages
    private String scriptOutput;
    //Returned value from script, can be null
    private Object result;
    
    
    /**
     * @return the actions
     */
    public List<TranslatableMessage> getActions() {
        return actions;
    }

    /**
     * @param actions the actions to set
     */
    public void setActions(List<TranslatableMessage> actions) {
        this.actions = actions;
    }

    /**
     * @return the errors
     */
    public List<TranslatableMessage> getErrors() {
        return errors;
    }

    /**
     * @param errors the errors to set
     */
    public void setErrors(List<TranslatableMessage> errors) {
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

    public void addAction(TranslatableMessage action) {
        if(this.actions == null)
            this.actions = new ArrayList<>();
        this.actions.add(action);
    }
    
    public void addError(TranslatableMessage action) {
        if(this.errors == null)
            this.errors = new ArrayList<>();
        this.errors.add(action);
    }
    
    public boolean hasErrors() {
        return this.errors != null && this.errors.size() > 0;
    }
    
}
