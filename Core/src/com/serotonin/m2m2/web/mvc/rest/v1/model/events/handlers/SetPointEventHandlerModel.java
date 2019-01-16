/**
 * Copyright (C) 2017 Infinite Automation Software. All rights reserved.
 *
 */
package com.serotonin.m2m2.web.mvc.rest.v1.model.events.handlers;

import java.util.List;
import java.util.Set;

import com.infiniteautomation.mango.util.script.ScriptPermissions;
import com.serotonin.db.pair.IntStringPair;
import com.serotonin.m2m2.vo.event.SetPointEventHandlerVO;

/**
 * 
 * @author Terry Packer
 */
public class SetPointEventHandlerModel extends AbstractEventHandlerModel<SetPointEventHandlerVO>{
	
	/**
	 * @param data
	 */
	public SetPointEventHandlerModel(SetPointEventHandlerVO data) {
		super(data);
	}
	
	public SetPointEventHandlerModel(){
		super(new SetPointEventHandlerVO());
	}
	
    public int getTargetPointId() {
        return this.data.getTargetPointId();
    }

    public void setTargetPointId(int targetPointId) {
        this.data.setTargetPointId(targetPointId);;
    }

    public String getActiveAction() {
        return SetPointEventHandlerVO.SET_ACTION_CODES.getCode(this.data.getActiveAction());
    }

    public void setActiveAction(String activeAction) {
        this.data.setActiveAction(SetPointEventHandlerVO.SET_ACTION_CODES.getId(activeAction));
    }

    public String getInactiveAction() {
        return SetPointEventHandlerVO.SET_ACTION_CODES.getCode(this.data.getInactiveAction());
    }

    public void setInactiveAction(String inactiveAction) {
        this.data.setInactiveAction(SetPointEventHandlerVO.SET_ACTION_CODES.getId(inactiveAction));
    }
    
    public String getActiveValueToSet() {
        return this.data.getActiveValueToSet();
    }

    public void setActiveValueToSet(String activeValueToSet) {
        this.data.setActiveValueToSet(activeValueToSet);
    }

    public int getActivePointId() {
        return this.data.getActivePointId();
    }

    public void setActivePointId(int activePointId) {
        this.data.setActivePointId(activePointId);
    }

    public String getInactiveValueToSet() {
        return this.data.getInactiveValueToSet();
    }

    public void setInactiveValueToSet(String inactiveValueToSet) {
        this.data.setInactiveValueToSet(inactiveValueToSet);
    }

    public int getInactivePointId() {
        return this.data.getInactivePointId();
    }

    public void setInactivePointId(int inactivePointId) {
        this.data.setInactivePointId(inactivePointId);
    }
    
    public String getActiveScript(){
    	return this.data.getActiveScript();
    }
    
    public void setActiveScript(String activeScript){
    	this.data.setActiveScript(activeScript);
    }
    
    public String getInactiveScript(){
    	return this.data.getInactiveScript();
    }
    
    public void setInactiveScript(String inactiveScript){
    	this.data.setInactiveScript(inactiveScript);
    }
    
    public List<IntStringPair> getAdditionalContext() {
    	return this.data.getAdditionalContext();
    }
    
    public void setAdditionalContext(List<IntStringPair> additionalContext) {
    	this.data.setAdditionalContext(additionalContext);
    }
    
    public Set<String> getScriptPermissions() {
        if(this.data.getScriptPermissions() != null)
            return this.data.getScriptPermissions().getPermissionsSet();
        else
            return null;
    }
    
    public void setScriptPermissions(Set<String> scriptPermissions) {
        if(scriptPermissions != null)
            this.data.setScriptPermissions(new ScriptPermissions(scriptPermissions));
    }
}
