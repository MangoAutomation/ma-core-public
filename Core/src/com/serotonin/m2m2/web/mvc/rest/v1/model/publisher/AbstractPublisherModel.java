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

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.i18n.ProcessMessage;
import com.serotonin.m2m2.i18n.ProcessResult;
import com.serotonin.m2m2.i18n.TranslatableJsonException;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.module.PublisherDefinition;
import com.serotonin.m2m2.rt.event.AlarmLevels;
import com.serotonin.m2m2.util.ExportCodes;
import com.serotonin.m2m2.vo.publish.PublishedPointVO;
import com.serotonin.m2m2.vo.publish.PublisherVO;
import com.serotonin.m2m2.web.mvc.rest.v1.csv.CSVColumnGetter;
import com.serotonin.m2m2.web.mvc.rest.v1.csv.CSVColumnSetter;
import com.serotonin.m2m2.web.mvc.rest.v1.exception.RestValidationFailedException;
import com.serotonin.m2m2.web.mvc.rest.v1.message.RestMessageLevel;
import com.serotonin.m2m2.web.mvc.rest.v1.message.RestProcessResult;
import com.serotonin.m2m2.web.mvc.rest.v1.message.RestValidationMessage;
import com.serotonin.m2m2.web.mvc.rest.v1.model.AbstractActionVoModel;

import io.swagger.annotations.ApiModelProperty;

/**
 * @author Terry Packer
 *
 */
public class AbstractPublisherModel<T extends PublisherVO<P>, P extends PublishedPointVO> {

    @ApiModelProperty(value = "Messages for validation of data", required = false)
    @JsonProperty("validationMessages")
    private List<RestValidationMessage> messages;

    @JsonIgnore
    private List<? extends AbstractPublishedPointModel<P>> modelPoints;

    protected T data;
    
    /**
     * @param data
     */
    public AbstractPublisherModel(T data) {
        this.data = data;
        this.messages = new ArrayList<RestValidationMessage>();
    }
    /**
     * Get the data for the model
     * @return T
     */
    public T getData(){
        return data;
    }

    
    @ApiModelProperty(value ="ID of object in database")
    @JsonGetter("id")
    public int getId(){
        return this.data.getId();
    }
    
    @ApiModelProperty(value = "XID of object", required = false)
    @CSVColumnGetter(order=1, header="xid")
    @JsonGetter("xid")
    public String getXid(){
        return this.data.getXid();
    }
    @CSVColumnSetter(order=1, header="xid")
    @JsonSetter("xid")
    public void setXid(String xid){
        this.data.setXid(xid);
    }
    
    @ApiModelProperty(value = "Name of object", required = false)
    @CSVColumnGetter(order=2, header="name")
    @JsonGetter("name")
    public String getName(){
        return this.data.getName();
    }
    @CSVColumnSetter(order=2, header="name")
    @JsonSetter("name")
    public void setName(String name){
        this.data.setName(name);
    }
    
    public void setMessages(List<RestValidationMessage> messages){
        this.messages = messages;
    }
    public List<RestValidationMessage> getMessages(){
        return this.messages;
    }   
    

    public boolean validate(){
        ProcessResult validation = new ProcessResult();
        this.data.validate(validation);
        
        if(validation.getHasMessages()){
            //Add our messages to the list
            for(ProcessMessage message : validation.getMessages()){
                if(message.getGenericMessage() != null){
                    this.messages.add(new RestValidationMessage(message.getGenericMessage(), RestMessageLevel.ERROR, ""));
                }else{
                    this.messages.add(new RestValidationMessage(
                            message.getContextualMessage(),
                            RestMessageLevel.ERROR,
                            message.getContextKey()
                            ));
                }
            }
            return false;
        }else{
            return true; //Validated ok
        }
    }
    
    /**
     * Helper to add Validation Message
     * @param messageKey
     * @param level
     * @param property
     */
    public void addValidationMessage(String messageKey, RestMessageLevel level, String property){
        this.addValidationMessage(new TranslatableMessage(messageKey), level, property);
    }
    
    /**
     * Helper to add Validation Message
     * @param messageKey
     * @param level
     * @param property
     */
    public void addValidationMessage(TranslatableMessage message, RestMessageLevel level, String property){
        this.messages.add(new RestValidationMessage(message, level, property));
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

    @CSVColumnGetter(order=3, header="enabled")
    @JsonGetter(value="enabled")
    public boolean isEnabled(){
        return this.data.isEnabled();
    }
    @CSVColumnSetter(order=3, header="enabled")
    @JsonSetter(value="enabled")
    public void setEnabled(boolean enabled){
        this.data.setEnabled(enabled);
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


    public String getModelType() {
        return this.data.getDefinition().getPublisherTypeName();
    }

    @JsonIgnore
    public void setDefinition(PublisherDefinition def) {
        this.data.setDefinition(def);
    }

}
