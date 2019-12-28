/**
 * Copyright (C) 2016 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.vo.event.detector;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.serotonin.json.JsonException;
import com.serotonin.json.JsonReader;
import com.serotonin.json.ObjectWriter;
import com.serotonin.json.type.JsonObject;
import com.serotonin.m2m2.db.dao.AbstractDao;
import com.serotonin.m2m2.db.dao.EventDetectorDao;
import com.serotonin.m2m2.db.dao.EventHandlerDao;
import com.serotonin.m2m2.i18n.ProcessResult;
import com.serotonin.m2m2.i18n.TranslatableJsonException;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.module.EventDetectorDefinition;
import com.serotonin.m2m2.rt.event.AlarmLevels;
import com.serotonin.m2m2.rt.event.detectors.AbstractEventDetectorRT;
import com.serotonin.m2m2.vo.AbstractVO;
import com.serotonin.m2m2.vo.event.AbstractEventHandlerVO;
import com.serotonin.m2m2.vo.event.EventTypeVO;
import com.serotonin.validation.StringValidation;

/**
 * @author Terry Packer
 *
 */
public abstract class AbstractEventDetectorVO<T extends AbstractEventDetectorVO<T>> extends AbstractVO<T>{

	private static final long serialVersionUID = 1L;

	public static final String XID_PREFIX = "ED_";
	
	/* Source of the detector */
	protected int sourceId;
	
    protected AlarmLevels alarmLevel = AlarmLevels.NONE;

	
	/**
	 * Handlers that will be added to this detector upon save.
	 * Used for JSON Imports
	 */
	private List<AbstractEventHandlerVO<?>> addedEventHandlers = null;
	
	/**
	 * All event handlers that map to this detector.
	 * When non-null this will replace all mappings for this detector <--> handlers
	 */
	private List<String> eventHandlerXids;
	
	/**
	 * Our defintion
	 */
	protected EventDetectorDefinition<T> definition;
	
	/**
	 * What event type do we generate
	 * @return
	 */
	public abstract EventTypeVO getEventType();
	

	/**
	 * Create the runtime 
	 * @return
	 */
	public abstract AbstractEventDetectorRT<T> createRuntime();

	/**
	 * Is our event Rtn Applicable?
	 * @return
	 */
	public abstract boolean isRtnApplicable();
	
	/**
	 * Return the configuration description for this handlers
	 * @return
	 */
	protected abstract TranslatableMessage getConfigurationDescription(); 
	
    public AlarmLevels getAlarmLevel() {
        return alarmLevel;
    }
    
    public void setAlarmLevel(AlarmLevels alarmLevel) {
        this.alarmLevel = alarmLevel;
    }
	
	/**
	 * Our type name defintion
	 * @return
	 */
	public String getDetectorType(){
		return this.definition.getEventDetectorTypeName();
	}
	
	/**
	 * Our source type name
	 * @return
	 */
	public String getDetectorSourceType(){
		return this.definition.getSourceTypeName();
	}
	
    public TranslatableMessage getDescription() {
        if (!StringUtils.isBlank(name))
            return new TranslatableMessage("common.default", name);
        return getConfigurationDescription();
    }
	
	@Override
	public String getTypeKey(){
		return "event.audit.pointEventDetector";
	}
	
    /**
     * Deprecated as we should just use the name. Leaving here as I believe these are probably accessed on the legacy page via DWR.
     * @param alias
     */
	@Deprecated
	public String getAlias() {
		return name;
	}

	/**
	 * Deprecated as we should just use the name. Leaving here as I believe these are probably accessed on the legacy page via DWR.
	 * @param alias
	 */
	@Deprecated
	public void setAlias(String alias) {
		this.name = alias;
	}
	
	public int getSourceId(){
		return this.sourceId;
	}
	public void setSourceId(int id){
		sourceId = id;
	}
	public void addAddedEventHandler(AbstractEventHandlerVO<?> eventHandler) {
	    if(addedEventHandlers == null)
	        addedEventHandlers = new ArrayList<>();
	    addedEventHandlers.add(eventHandler);
	}
	public List<AbstractEventHandlerVO<?>> getAddedEventHandlers() {
	    return addedEventHandlers;
	}
	
	public EventDetectorDefinition<T> getDefinition() {
		return definition;
	}
	
	/**
     * @return the eventHandlerXids
     */
    public List<String> getEventHandlerXids() {
        return eventHandlerXids;
    }
    
    /**
     * @param eventHandlerXids the eventHandlerXids to set
     */
    public void setEventHandlerXids(List<String> eventHandlerXids) {
        this.eventHandlerXids = eventHandlerXids;
    }
    

	@SuppressWarnings("unchecked")
	public void setDefinition(EventDetectorDefinition<?> definition) {
		this.definition = (EventDetectorDefinition<T>) definition;
	}

	@SuppressWarnings("unchecked")
	@Override
	protected AbstractDao<T> getDao(){
		return (AbstractDao<T>) EventDetectorDao.getInstance();
	}

	/*
	 * Override so we can allow for blank names
	 */
	@Override
	public void validate(ProcessResult response) {
	    if (StringUtils.isBlank(xid))
            response.addContextualMessage("xid", "validate.required");
        else if (StringValidation.isLengthGreaterThan(xid, 100))
            response.addMessage("xid", new TranslatableMessage("validate.notLongerThan", 100));
        else if (!isXidUnique(xid, id))
            response.addContextualMessage("xid", "validate.xidUsed");

	    // allow blank names
        if (!StringUtils.isBlank(name)) {
            if (StringValidation.isLengthGreaterThan(name, 255))
                response.addMessage("name", new TranslatableMessage("validate.notLongerThan", 255));
        }
        
        //Verify that they each exist as we will create a mapping when we save
        if(addedEventHandlers != null)
            for(AbstractEventHandlerVO<?> eh : addedEventHandlers) {
                if(EventHandlerDao.getInstance().getXidById(eh.getId()) == null)
                    response.addMessage("handlers", new TranslatableMessage("emport.eventHandler.missing", eh.getXid()));
            }
        if(eventHandlerXids != null) {
            for(String ehXid : eventHandlerXids) {
                if(EventHandlerDao.getInstance().getIdByXid(ehXid) == null)
                    response.addMessage("eventHandlerXids", new TranslatableMessage("emport.eventHandler.missing", ehXid));
            }
        }
	}

	@Override
    public void jsonWrite(ObjectWriter writer) throws IOException, JsonException {
        writer.writeEntry("type", this.definition.getEventDetectorTypeName());
        writer.writeEntry("sourceType", this.definition.getSourceTypeName());
        writer.writeEntry("xid", xid);
        writer.writeEntry("name", name);
        writer.writeEntry("alarmLevel", alarmLevel.name());

        /* Event handler references are not exported here because there would be a circular dependency
        *  with the eventTypes array in the handler, and since there are other event types that was deemed
        *  the more versatile. One can create handler mappings through this array, but you cannot have both 
        *  items refer to one another in the JSON if both are new, so this is not exported.
        */
        //writer.writeEntry("handlers", EventHandlerDao.getInstance().getEventHandlerXids(getEventType()));
    }

    @Override
    public void jsonRead(JsonReader reader, JsonObject jsonObject) throws JsonException {
        String text = jsonObject.getString("name");
        if(text != null)
            name = text;
        else {
            text = jsonObject.getString("alias");
            if(text != null)
                name = text;
        }
        
        text = jsonObject.getString("alarmLevel");
        if (text != null) {
            try {
                alarmLevel = AlarmLevels.fromName(text);
            } catch (IllegalArgumentException e) {
                throw new TranslatableJsonException("emport.error.ped.invalid", "alarmLevel", text,
                        Arrays.asList(AlarmLevels.values()));
            }
        }
    }
}
