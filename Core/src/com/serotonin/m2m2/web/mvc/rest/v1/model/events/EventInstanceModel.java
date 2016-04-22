/**
 * Copyright (C) 2015 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.web.mvc.rest.v1.model.events;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.module.EventTypeDefinition;
import com.serotonin.m2m2.module.ModuleRegistry;
import com.serotonin.m2m2.rt.event.AlarmLevels;
import com.serotonin.m2m2.rt.event.EventInstance;
import com.serotonin.m2m2.rt.event.EventInstance.RtnCauses;
import com.serotonin.m2m2.rt.event.type.EventType;
import com.serotonin.m2m2.vo.UserComment;
import com.serotonin.m2m2.vo.event.EventInstanceVO;
import com.serotonin.m2m2.web.mvc.rest.v1.model.AbstractRestModel;
import com.serotonin.m2m2.web.mvc.rest.v1.model.comment.UserCommentModel;

/**
 * @author Terry Packer
 *
 */
public class EventInstanceModel  extends AbstractRestModel<EventInstanceVO>{

	/***
	 * Current hack to allow this model to hadle event instances and event instance vos.
	 * @param evt
	 */
	public EventInstanceModel(EventInstance evt){
		this();
		this.data.setId(evt.getId());
		this.data.setEventType(evt.getEventType());
		this.data.setActiveTimestamp(evt.getActiveTimestamp());
		this.data.setAcknowledgedByUserId(evt.getAcknowledgedByUserId());
		this.data.setAcknowledgedTimestamp(evt.getAcknowledgedTimestamp());
		this.data.setRtnApplicable(evt.isRtnApplicable());
		this.data.setRtnTimestamp(evt.getRtnTimestamp());
		this.data.setRtnCause(evt.getRtnCause());
		this.data.setAlarmLevel(evt.getAlarmLevel());
		this.data.setMessage(evt.getMessage());
		
		/*TODO When we need them:
		List<UserComment> eventComments
		List<EventHandlerRT> handlers
	    long acknowledgedTimestamp;
	    int acknowledgedByUserId;
	    String acknowledgedByUsername;
	    private TranslatableMessage alternateAckSource;
	    private boolean hasComments;

	    These fields are used only in the context of access by a particular user, providing state filled in from
	    the userEvents table.
	    boolean userNotified;
	    boolean silenced;
	    */
		
	}
	
	/**
	 * @param data
	 */
	public EventInstanceModel(EventInstanceVO data) {
		super(data);
	}

	public EventInstanceModel(){
		super(new EventInstanceVO());
	}
	
	@JsonGetter
	public int getId(){
		return this.data.getId();
	}
	@JsonSetter
	public void setId(int id){
		this.data.setId(id);
	}
	
	@JsonGetter
	public String getAlarmLevel(){
		return AlarmLevels.CODES.getCode(this.data.getAlarmLevel());
	}
	@JsonSetter
	public void setAlarmLevel(String level){
		this.data.setAlarmLevel(AlarmLevels.CODES.getId(level));
	}
	
	@JsonGetter
	public long getActiveTimestamp(){
		return this.data.getActiveTimestamp();
	}
	@JsonSetter
	public void setActiveTimestamp(long activeTimestamp){
		this.data.setActiveTimestamp(activeTimestamp);
	}
	
	@JsonGetter
	public long getReturnToNormalTimestamp(){
		return this.data.getRtnTimestamp();
	}
	@JsonSetter
	public void setReturnToNormalTimestamp(long timestamp){
		this.data.setRtnTimestamp(timestamp);
	}
	
	@JsonGetter
	public String getMessage(){
		if(this.data.getMessage() != null){
			return this.data.getMessage().translate(Common.getTranslations());
		}else{
			return "";
		}
	}
	@JsonSetter void setMessage(String message){
		//TODO this is weird
		this.data.setMessage(new TranslatableMessage("common.default", message));
	}
	
	
	//TODO Probably should break this up and have the UI deal with this stuff
	@JsonGetter
	public String getStatus(){
        TranslatableMessage rtnKey = null;
        if(this.data.isRtnApplicable()){
	        if (!this.data.isActive()) {
	            if (this.data.getRtnCause() == RtnCauses.RETURN_TO_NORMAL)
	                rtnKey = new TranslatableMessage("event.rtn.rtn");
	            else if (this.data.getRtnCause() == RtnCauses.SOURCE_DISABLED) {
	                if (this.data.getEventType().getEventType().equals(EventType.EventTypeNames.DATA_POINT))
	                    rtnKey = new TranslatableMessage("event.rtn.pointDisabled");
	                else if (this.data.getEventType().getEventType().equals(EventType.EventTypeNames.DATA_SOURCE))
	                    rtnKey = new TranslatableMessage("event.rtn.dsDisabled");
	                else if (this.data.getEventType().getEventType().equals(EventType.EventTypeNames.PUBLISHER))
	                    rtnKey = new TranslatableMessage("event.rtn.pubDisabled");
	                else {
	                    EventTypeDefinition def = ModuleRegistry.getEventTypeDefinition(this.data.getEventType().getEventType());
	                    if (def != null)
	                        rtnKey = def.getSourceDisabledMessage();
	                    if (rtnKey == null)
	                        rtnKey = new TranslatableMessage("event.rtn.shutdown");
	                }
	            }
	            else
	                rtnKey = new TranslatableMessage("event.rtn.unknown");
	        }
        }else{
        	rtnKey = new TranslatableMessage("common.nortn");
        }
        if(rtnKey != null)
        	return rtnKey.translate(Common.getTranslations());
        else
        	return "";
    }
	@JsonSetter
	public void setStatus(String status){ }//NoOp for now
	
	@JsonGetter
	public boolean isAcknowledged(){
		return this.data.isAcknowledged();
	}
	@JsonSetter
	public void setAcknowledged(boolean ack){
		this.data.setAcknowledged(ack);
	}
	
	@JsonGetter
	public String getAcknowledgedMessage(){
        if (this.data.isAcknowledged()) {
            if (this.data.getAcknowledgedByUserId() != 0)
                return new TranslatableMessage("events.ackedByUser", this.data.getAcknowledgedByUsername()).translate(Common.getTranslations());
            if (this.data.getAlternateAckSource() != null)
                return this.data.getAlternateAckSource().translate(Common.getTranslations());
        }
        return null;
	}
	@JsonSetter
	public void setAcknowledgedMessage(String message){ } //NoOp for now
	
	@JsonGetter
	public List<UserCommentModel> getComments(){
		List<UserCommentModel> commentModels = new ArrayList<UserCommentModel>();
		List<UserComment> comments = this.data.getEventComments();
		if(comments == null)
			return null;
		for(UserComment comment: comments){
			commentModels.add(new UserCommentModel(comment));
		}
		return commentModels;
	}
	@JsonSetter
	public void setComments(List<UserCommentModel> commentModels){
		List<UserComment> comments = this.data.getEventComments();
		if(comments == null){
			comments = new ArrayList<UserComment>();
			this.data.setEventComments(comments);
		}
		for(UserCommentModel model : commentModels){
			comments.add(model.getDataAsComment());
		}
	}
	
	@JsonGetter
	public EventType getEventType(){
		return this.data.getEventType();
	}
	@JsonSetter
	public void SetEventType(EventType eventType){
		this.data.setEventType(eventType);
	}
	
	@JsonGetter
	public boolean isActive(){
		return this.data.isActive();
	}
	@JsonSetter
	public void setActive(boolean active){
		//No op for now
	}
}
