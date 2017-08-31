/**
 * Copyright (C) 2016 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.vo.event.detector;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.serotonin.json.JsonException;
import com.serotonin.json.JsonReader;
import com.serotonin.json.ObjectWriter;
import com.serotonin.json.type.JsonArray;
import com.serotonin.json.type.JsonObject;
import com.serotonin.json.type.JsonValue;
import com.serotonin.m2m2.db.dao.AbstractDao;
import com.serotonin.m2m2.db.dao.EventDetectorDao;
import com.serotonin.m2m2.db.dao.EventHandlerDao;
import com.serotonin.m2m2.i18n.ProcessResult;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.module.EventDetectorDefinition;
import com.serotonin.m2m2.rt.event.detectors.AbstractEventDetectorRT;
import com.serotonin.m2m2.vo.AbstractVO;
import com.serotonin.m2m2.vo.event.EventTypeVO;
import com.serotonin.m2m2.web.mvc.rest.v1.model.events.detectors.AbstractEventDetectorModel;
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
	
	private List<String> addedEventHandlerXids = null;
	
	/**
	 * Our defintion
	 */
	protected EventDetectorDefinition<T> definition;
	
	/**
     * Return the Model Representation of the Event Detector Source
     * @return
     */
    public AbstractEventDetectorModel<T> asModel(){
    	return this.definition.createModel(this);
    }
	
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
	
	public String getAlias() {
		return name;
	}

	public void setAlias(String alias) {
		this.name = alias;
	}
	public int getSourceId(){
		return this.sourceId;
	}
	public void setSourceId(int id){
		sourceId = id;
	}
	public void addEventHandlers(List<String> eventHandlerXids) {
	    if(addedEventHandlerXids == null)
	        addedEventHandlerXids = new ArrayList<String>(eventHandlerXids.size());
	    addedEventHandlerXids.addAll(eventHandlerXids);
	}
	public List<String> getAddedEventHandlers() {
	    return addedEventHandlerXids;
	}
	
	public EventDetectorDefinition<T> getDefinition() {
		return definition;
	}

	@SuppressWarnings("unchecked")
	public void setDefinition(EventDetectorDefinition<?> definition) {
		this.definition = (EventDetectorDefinition<T>) definition;
	}

	@SuppressWarnings("unchecked")
	@Override
	protected AbstractDao<T> getDao(){
		return (AbstractDao<T>) EventDetectorDao.instance;
	}
	
	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.vo.AbstractVO#validate(com.serotonin.m2m2.i18n.ProcessResult)
	 */
	@Override
	public void validate(ProcessResult response) {
		//Can't validate uniqueness of XID so we don't call super
        if (StringUtils.isBlank(xid))
            response.addContextualMessage("xid", "validate.required");
        else if (StringValidation.isLengthGreaterThan(xid, 50))
            response.addMessage("xid", new TranslatableMessage("validate.notLongerThan", 50));
        else if (!isXidUnique(xid, definition.getSourceTypeName(), sourceId))
            response.addContextualMessage("xid", "validate.xidUsed");

        if (StringUtils.isBlank(name))
            response.addContextualMessage("name", "validate.required");
        else if (StringValidation.isLengthGreaterThan(name, 255))
            response.addMessage("name", new TranslatableMessage("validate.notLongerThan", 255));
	}
	
    /**
	 * @param xid
	 * @param sourceTypeName
	 * @param sourceId2
	 * @return
	 */
	protected boolean isXidUnique(String xid, String sourceType, int sourceId) {
		return EventDetectorDao.instance.isXidUnique(xid, id, sourceType, sourceId);
	}

	@Override
    public void jsonWrite(ObjectWriter writer) throws IOException, JsonException {
        writer.writeEntry("type", this.definition.getEventDetectorTypeName());
        writer.writeEntry("sourceType", this.definition.getSourceTypeName());
        writer.writeEntry("xid", xid);
        writer.writeEntry("alias", name);
        
        // The event handler references will now be exported here, 
        // rather than in the handler referencing the detector
        writer.writeEntry("handlers", EventHandlerDao.instance.getEventHandlerXids(getEventType()));
    }

    @Override
    public void jsonRead(JsonReader reader, JsonObject jsonObject) throws JsonException {
        name = jsonObject.getString("alias");
        
        //In keeping with data points, the import can only add mappings
        //  The "handlers" key is removed by the EventDetectorRowMapper
        JsonArray handlers = jsonObject.getJsonArray("handlers");
        if(handlers != null) {
            addedEventHandlerXids = new ArrayList<String>(handlers.size());
            Iterator<JsonValue> iter = handlers.iterator();
            EventTypeVO etvo = getEventType();
            while(iter.hasNext())
                addedEventHandlerXids.add(iter.next().toString());
        }
        
        
    }
}
