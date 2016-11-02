/**
 * Copyright (C) 2016 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.web.mvc.rest.v1.model.publisher;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.i18n.ProcessResult;
import com.serotonin.m2m2.module.PublisherDefinition;
import com.serotonin.m2m2.vo.publish.PublishedPointVO;
import com.serotonin.m2m2.vo.publish.PublisherVO;
import com.serotonin.m2m2.web.mvc.rest.v1.exception.RestValidationFailedException;
import com.serotonin.m2m2.web.mvc.rest.v1.message.RestProcessResult;
import com.serotonin.m2m2.web.mvc.rest.v1.model.AbstractActionVoModel;

/**
 * @author Terry Packer
 *
 */
public class AbstractPublisherModel<T extends PublisherVO<P>, P extends PublishedPointVO> extends AbstractActionVoModel<PublisherVO<?>> {

	/* Cast of Publisher */
	protected T data;
	protected List<? extends AbstractPublishedPointModel<P>> points;
	
	/**
	 * @param data
	 */
	public AbstractPublisherModel(T data, List<? extends AbstractPublishedPointModel<P>> points) {
		super(data);
		this.data = data;
		this.points = points;
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.serotonin.m2m2.web.mvc.rest.v1.model.AbstractRestModel#validate(com.serotonin.m2m2.web.mvc.rest.v1.message.RestProcessResult)
	 */
	public void validate(RestProcessResult<?> result) throws RestValidationFailedException {
		ProcessResult validation = new ProcessResult();
		this.data.validate(validation);
		
		if(validation.getHasMessages()){
			result.addValidationMessages(validation);
			throw new RestValidationFailedException(this, result);
		}
	}

	@JsonIgnore
	@Override
	public T getData(){
		//Convert point models
		List<P> publishedPoints = new ArrayList<P>();
		for(AbstractPublishedPointModel<P> model : this.points)
			publishedPoints.add(model.getData());
		this.data.setPoints(publishedPoints);
		return this.data;
	}
	
	public List<? extends AbstractPublishedPointModel<P>> getPoints(){
		return this.points;
	}
	public void setPoints(List<? extends AbstractPublishedPointModel<P>> points){
		this.points = points;
	}
	
	public String getPublishType(){
		return PublisherVO.PUBLISH_TYPE_CODES.getCode(this.data.getPublishType());
	}
	public void setPublishType(String type){
		this.data.setPublishType(PublisherVO.PUBLISH_TYPE_CODES.getId(type));
	}
	
	public int getCacheWarningSize() {
        return this.data.getCacheWarningSize();
    }

    public void setCacheWarningSize(int cacheWarningSize) {
        this.data.setCacheWarningSize(cacheWarningSize);
    }

    public int getCacheDiscardSize() {
        return this.data.getCacheDiscardSize();
    }

    public void setCacheDiscardSize(int cacheDiscardSize) {
        this.data.setCacheDiscardSize(cacheDiscardSize);
    }

    public boolean isSendSnapshot() {
        return this.data.isSendSnapshot();
    }

    public void setSendSnapshot(boolean sendSnapshot) {
        this.data.setSendSnapshot(sendSnapshot);
    }

    public String getSnapshotSendPeriodType() {
        return Common.TIME_PERIOD_CODES.getCode(this.data.getSnapshotSendPeriodType());
    }

    public void setSnapshotSendPeriodType(String snapshotSendPeriodType) {
        this.data.setSnapshotSendPeriodType(Common.TIME_PERIOD_CODES.getId(snapshotSendPeriodType));
    }

    public int getSnapshotSendPeriods() {
        return this.data.getSnapshotSendPeriods();
    }

    public void setSnapshotSendPeriods(int snapshotSendPeriods) {
        this.data.setSnapshotSendPeriods(snapshotSendPeriods);
    }
	
	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.web.mvc.rest.v1.model.AbstractBasicVoModel#getModelType()
	 */
	@Override
	public String getModelType() {
		return this.data.getDefinition().getPublisherTypeName();
	}
	
	@JsonIgnore
	public void setDefinition(PublisherDefinition def) {
		this.data.setDefinition(def);
	}

}
