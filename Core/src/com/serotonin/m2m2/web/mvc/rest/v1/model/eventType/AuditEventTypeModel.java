/**
 * Copyright (C) 2017 Infinite Automation Software. All rights reserved.
 *
 */
package com.serotonin.m2m2.web.mvc.rest.v1.model.eventType;

import com.serotonin.m2m2.db.dao.UserDao;
import com.serotonin.m2m2.rt.event.type.AuditEventType;
import com.serotonin.m2m2.rt.event.type.EventType;
import com.serotonin.m2m2.rt.event.type.EventType.DuplicateHandling;
import com.serotonin.m2m2.vo.User;

/**
 * 
 * @author Terry Packer
 */
public class AuditEventTypeModel extends EventTypeModel{

    private String auditEventType;
    private int referenceId;
    private String raisingUsername;
    private int changeType;
    private int auditEventId;
    
    public AuditEventTypeModel(){ }
    
    public AuditEventTypeModel(AuditEventType type){
    	this.auditEventType = type.getAuditEventType();
    	this.referenceId = type.getReferenceId1();
    	if(type.getRaisingUser() != null)
    		this.raisingUsername = type.getRaisingUser().getUsername();
    	this.changeType = type.getChangeType();
    	this.auditEventId = type.getReferenceId2();
    }

	public String getAuditEventType() {
		return auditEventType;
	}

	public void setAuditEventType(String auditEventType) {
		this.auditEventType = auditEventType;
	}

	public int getReferenceId() {
		return referenceId;
	}

	public void setReferenceId(int referenceId) {
		this.referenceId = referenceId;
	}

	public String getRaisingUsername() {
		return raisingUsername;
	}

	public void setRaisingUsername(String raisingUsername) {
		this.raisingUsername = raisingUsername;
	}

	public int getChangeType() {
		return changeType;
	}

	public void setChangeType(int changeType) {
		this.changeType = changeType;
	}

	public int getAuditEventId() {
		return auditEventId;
	}

	public void setAuditEventId(int auditEventId) {
		this.auditEventId = auditEventId;
	}

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.web.mvc.rest.v1.model.eventType.EventTypeModel#getTypeName()
	 */
	@Override
	public String getTypeName() {
		return EventType.EventTypeNames.AUDIT;
	}

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.web.mvc.rest.v1.model.eventType.EventTypeModel#isRateLimited()
	 */
	@Override
	public boolean isRateLimited() {
		return false;
	}

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.web.mvc.rest.v1.model.eventType.EventTypeModel#getDuplicateHandling()
	 */
	@Override
	public String getDuplicateHandling() {
		return EventType.DUPLICATE_HANDLING_CODES.getCode(DuplicateHandling.ALLOW);
	}

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.web.mvc.rest.v1.model.eventType.EventTypeModel#getEventTypeInstance()
	 */
	@Override
	public EventType getEventTypeInstance() {
		User user = null;
		if(this.raisingUsername != null)
			user = UserDao.instance.getUser(this.raisingUsername);
		return new AuditEventType(auditEventType, changeType, referenceId, user, auditEventId);
	}
	
    
}
