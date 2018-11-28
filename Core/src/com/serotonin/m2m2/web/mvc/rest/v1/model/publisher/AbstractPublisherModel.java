/**
 * Copyright (C) 2016 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.web.mvc.rest.v1.model.publisher;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.i18n.ProcessResult;
import com.serotonin.m2m2.i18n.TranslatableJsonException;
import com.serotonin.m2m2.module.PublisherDefinition;
import com.serotonin.m2m2.rt.event.AlarmLevels;
import com.serotonin.m2m2.util.ExportCodes;
import com.serotonin.m2m2.vo.publish.PublishedPointVO;
import com.serotonin.m2m2.vo.publish.PublisherVO;
import com.serotonin.m2m2.web.mvc.rest.v1.exception.RestValidationFailedException;
import com.serotonin.m2m2.web.mvc.rest.v1.message.RestProcessResult;
import com.serotonin.m2m2.web.mvc.rest.v1.model.AbstractActionVoModel;

/**
 * @author Terry Packer
 *
 */
public class AbstractPublisherModel<T extends PublisherVO<P>, P extends PublishedPointVO> extends AbstractActionVoModel<T> {

    @JsonIgnore
    private List<? extends AbstractPublishedPointModel<P>> modelPoints;

    /**
     * @param data
     */
    public AbstractPublisherModel(T data) {
        super(data);
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

    @SuppressWarnings("unchecked")
    public List<? extends AbstractPublishedPointModel<P>> getPoints(){
        List<AbstractPublishedPointModel<P>> publishedPoints = new ArrayList<>();
        for(P point : this.data.getPoints())
            publishedPoints.add((AbstractPublishedPointModel<P>) point.asModel());
        return publishedPoints;
    }

    public Map<String,String> getAlarmLevels(){
        ExportCodes eventCodes = this.data.getEventCodes();
        Map<String, String> alarmCodeLevels = new HashMap<>();

        if (eventCodes != null && eventCodes.size() > 0) {

            for (int i = 0; i < eventCodes.size(); i++) {
                int eventId = eventCodes.getId(i);
                AlarmLevels level = this.data.getAlarmLevel(eventId, AlarmLevels.URGENT);
                alarmCodeLevels.put(eventCodes.getCode(eventId), level.name());
            }
        }
        return alarmCodeLevels;
    }

    public void setAlarmLevels(Map<String,String> alarmCodeLevels) throws TranslatableJsonException{
        if (alarmCodeLevels != null) {
            ExportCodes eventCodes = this.data.getEventCodes();
            if (eventCodes != null && eventCodes.size() > 0) {
                for (String code : alarmCodeLevels.keySet()) {
                    int eventId = eventCodes.getId(code);
                    if (!eventCodes.isValidId(eventId))
                        throw new TranslatableJsonException("emport.error.eventCode", code, eventCodes.getCodeList());

                    String text = alarmCodeLevels.get(code);
                    try {
                        this.data.setAlarmLevel(eventId, AlarmLevels.fromName(text));
                    } catch (IllegalArgumentException | NullPointerException e) {
                        throw new TranslatableJsonException("emport.error.alarmLevel", text, code,
                                Arrays.asList(AlarmLevels.values()));
                    }
                }
            }
        }
    }

    public void setPoints(List<? extends AbstractPublishedPointModel<P>> points){
        List<P> publishedPoints = new ArrayList<>();
        for(AbstractPublishedPointModel<P> model : points)
            publishedPoints.add(model.getData());
        this.data.setPoints(publishedPoints);
        modelPoints = points;
    }

    public List<? extends AbstractPublishedPointModel<P>> getIncomingModelPoints() {
        return modelPoints;
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
