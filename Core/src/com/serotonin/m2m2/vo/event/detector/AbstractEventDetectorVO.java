/**
 * Copyright (C) 2016 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.vo.event.detector;

import java.io.IOException;

import org.apache.commons.lang3.StringUtils;

import com.serotonin.json.JsonException;
import com.serotonin.json.JsonReader;
import com.serotonin.json.ObjectWriter;
import com.serotonin.json.type.JsonObject;
import com.serotonin.m2m2.db.dao.EventDetectorDao;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.module.EventDetectorDefinition;
import com.serotonin.m2m2.rt.event.detectors.AbstractEventDetectorRT;
import com.serotonin.m2m2.vo.AbstractVO;
import com.serotonin.m2m2.vo.event.EventTypeVO;
import com.serotonin.m2m2.web.mvc.rest.v1.model.events.detectors.AbstractEventDetectorModel;

/**
 * @author Terry Packer
 *
 */
public abstract class AbstractEventDetectorVO<T extends AbstractEventDetectorVO<?>> extends AbstractVO<AbstractEventDetectorVO<?>>{

	private static final long serialVersionUID = 1L;

	public static final String XID_PREFIX = "ED_";
	
	/* Source of the detector */
	private int sourceId;
	
	/**
	 * Our defintion
	 */
	protected EventDetectorDefinition definition;
	
	/**
     * Return the Model Representation of the Event Detector Source
     * @return
     */
    public AbstractEventDetectorModel<?> asModel(){
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
	public EventDetectorDefinition getDefinition() {
		return definition;
	}

	public void setDefinition(EventDetectorDefinition definition) {
		this.definition = definition;
	}

	@Override
	protected EventDetectorDao getDao(){
		return EventDetectorDao.instance;
	}
	
    @Override
    public void jsonWrite(ObjectWriter writer) throws IOException, JsonException {
        writer.writeEntry("type", this.definition.getEventDetectorTypeName());
        writer.writeEntry("xid", xid);
        writer.writeEntry("alias", name);
    }

    @Override
    public void jsonRead(JsonReader reader, JsonObject jsonObject) throws JsonException {
        name = jsonObject.getString("alias");
    }

}
