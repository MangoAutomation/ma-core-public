/*
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.vo.publish;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.apache.commons.lang3.StringUtils;

import com.serotonin.json.JsonException;
import com.serotonin.json.JsonReader;
import com.serotonin.json.ObjectWriter;
import com.serotonin.json.spi.JsonProperty;
import com.serotonin.json.spi.JsonSerializable;
import com.serotonin.json.type.JsonArray;
import com.serotonin.json.type.JsonObject;
import com.serotonin.json.type.JsonValue;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.dao.AbstractDao;
import com.serotonin.m2m2.db.dao.DataPointDao;
import com.serotonin.m2m2.db.dao.PublisherDao;
import com.serotonin.m2m2.i18n.ProcessResult;
import com.serotonin.m2m2.i18n.TranslatableJsonException;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.module.PublisherDefinition;
import com.serotonin.m2m2.rt.event.AlarmLevels;
import com.serotonin.m2m2.rt.event.type.EventType;
import com.serotonin.m2m2.rt.publish.PublisherRT;
import com.serotonin.m2m2.util.ExportCodes;
import com.serotonin.m2m2.vo.AbstractActionVO;
import com.serotonin.m2m2.vo.DataPointVO;
import com.serotonin.m2m2.vo.event.EventTypeVO;
import com.serotonin.m2m2.web.mvc.rest.v1.model.publisher.AbstractPublisherModel;
import com.serotonin.util.SerializationHelper;
import com.serotonin.validation.StringValidation;

/**
 * @author Matthew Lohbihler
 */
abstract public class PublisherVO<T extends PublishedPointVO> extends AbstractActionVO<PublisherVO<?>> implements Serializable, JsonSerializable {
    public static final String XID_PREFIX = "PUB_";

    public interface PublishType{
    	int ALL = 1;
    	int CHANGES_ONLY = 2;
    	int LOGGED_ONLY = 3;
    	int NONE = 4;
    }
    
    public static final ExportCodes PUBLISH_TYPE_CODES = new ExportCodes();
    static{
    	PUBLISH_TYPE_CODES.addElement(PublishType.ALL, "ALL", "publisherEdit.publishType.all");	
    	PUBLISH_TYPE_CODES.addElement(PublishType.CHANGES_ONLY, "CHANGES_ONLY", "publisherEdit.publishType.changesOnly");	
    	PUBLISH_TYPE_CODES.addElement(PublishType.LOGGED_ONLY, "LOGGED_ONLY", "publisherEdit.publishType.loggedOnly");
    	PUBLISH_TYPE_CODES.addElement(PublishType.NONE, "NONE", "publisherEdit.publishType.none");
    }
    
    /**
     * Return the Model Representation of the Publisher
     * @return
     */
    abstract public AbstractPublisherModel<?,?> asModel();

    abstract public TranslatableMessage getConfigDescription();

    abstract protected T createPublishedPointInstance();

    abstract public PublisherRT<T> createPublisherRT();

    public List<EventTypeVO> getEventTypes() {
        List<EventTypeVO> eventTypes = new ArrayList<>();
        eventTypes
                .add(new EventTypeVO(EventType.EventTypeNames.PUBLISHER, null, getId(),
                        PublisherRT.POINT_DISABLED_EVENT, new TranslatableMessage("event.pb.pointMissing"),
                        getAlarmLevel(PublisherRT.POINT_DISABLED_EVENT, AlarmLevels.URGENT)));
        eventTypes
                .add(new EventTypeVO(EventType.EventTypeNames.PUBLISHER, null, getId(),
                        PublisherRT.QUEUE_SIZE_WARNING_EVENT, new TranslatableMessage("event.pb.queueSize"),
                        getAlarmLevel(PublisherRT.QUEUE_SIZE_WARNING_EVENT, AlarmLevels.URGENT)));

        getEventTypesImpl(eventTypes);

        return eventTypes;
    }

    abstract protected void getEventTypesImpl(List<EventTypeVO> eventTypes);

    protected static void addDefaultEventCodes(ExportCodes codes) {
        codes.addElement(PublisherRT.POINT_DISABLED_EVENT, "POINT_DISABLED_EVENT");
        codes.addElement(PublisherRT.QUEUE_SIZE_WARNING_EVENT, "QUEUE_SIZE_WARNING_EVENT");
    }

    abstract public ExportCodes getEventCodes();

    private PublisherDefinition definition;
    
    private Map<Integer, Integer> alarmLevels = new HashMap<>();

    public TranslatableMessage getTypeDescription() {
        return new TranslatableMessage(getDefinition().getDescriptionKey());
    }

    protected List<T> points = new ArrayList<>();
    private int publishType = PublishType.ALL;
    @JsonProperty
    private int cacheWarningSize = 100;
    @JsonProperty
    private int cacheDiscardSize = 1000;
    @JsonProperty
    private boolean sendSnapshot;
    private int snapshotSendPeriodType = Common.TimePeriods.MINUTES;
    @JsonProperty
    private int snapshotSendPeriods = 5;

    public final PublisherDefinition getDefinition() {
        return definition;
    }

    public void setDefinition(PublisherDefinition definition) {
        this.definition = definition;
    }

    public void setAlarmLevel(int eventId, int level) {
        alarmLevels.put(eventId, level);
    }

    public int getAlarmLevel(int eventId, int defaultLevel) {
        Integer level = alarmLevels.get(eventId);
        if (level == null)
            return defaultLevel;
        return level;
    }
    
    public List<T> getPoints() {
        return points;
    }

    public void setPoints(List<T> points) {
        this.points = points;
    }

    public int getPublishType() {
        return publishType;
    }

    public void setPublishType(int publishType) {
        this.publishType = publishType;
    }

    public int getCacheWarningSize() {
        return cacheWarningSize;
    }

    public void setCacheWarningSize(int cacheWarningSize) {
        this.cacheWarningSize = cacheWarningSize;
    }

    public int getCacheDiscardSize() {
        return cacheDiscardSize;
    }

    public void setCacheDiscardSize(int cacheDiscardSize) {
        this.cacheDiscardSize = cacheDiscardSize;
    }

    public boolean isSendSnapshot() {
        return sendSnapshot;
    }

    public void setSendSnapshot(boolean sendSnapshot) {
        this.sendSnapshot = sendSnapshot;
    }

    public int getSnapshotSendPeriodType() {
        return snapshotSendPeriodType;
    }

    public void setSnapshotSendPeriodType(int snapshotSendPeriodType) {
        this.snapshotSendPeriodType = snapshotSendPeriodType;
    }

    public int getSnapshotSendPeriods() {
        return snapshotSendPeriods;
    }

    public void setSnapshotSendPeriods(int snapshotSendPeriods) {
        this.snapshotSendPeriods = snapshotSendPeriods;
    }

    public void validate(ProcessResult response) {
        if (StringUtils.isBlank(name))
            response.addContextualMessage("name", "validate.required");
        if (StringValidation.isLengthGreaterThan(name, 40))
            response.addContextualMessage("name", "validate.nameTooLong");

        if (StringUtils.isBlank(xid))
            response.addContextualMessage("xid", "validate.required");
        else if (!PublisherDao.instance.isXidUnique(xid, id))
            response.addContextualMessage("xid", "validate.xidUsed");
        else if (StringValidation.isLengthGreaterThan(xid, 50))
            response.addContextualMessage("xid", "validate.notLongerThan", 50);

        if (sendSnapshot) {
            if (snapshotSendPeriods <= 0)
                response.addContextualMessage("snapshotSendPeriods", "validate.greaterThanZero");
            if(!Common.TIME_PERIOD_CODES.isValidId(snapshotSendPeriodType, Common.TimePeriods.MILLISECONDS, Common.TimePeriods.DAYS, 
            		Common.TimePeriods.WEEKS, Common.TimePeriods.MONTHS, Common.TimePeriods.YEARS))
            	response.addContextualMessage("snapshotSendPeriodType", "validate.invalidValue");
        }

        if (cacheWarningSize < 1)
            response.addContextualMessage("cacheWarningSize", "validate.greaterThanZero");

        if (cacheDiscardSize <= cacheWarningSize)
            response.addContextualMessage("cacheDiscardSize", "validate.publisher.cacheDiscardSize");

        Set<Integer> set = new HashSet<>();
        ListIterator<T> it = points.listIterator();
        
        while(it.hasNext()) {
        	T point = it.next();
            int pointId = point.getDataPointId();
            //Does this point even exist?
            DataPointVO vo = DataPointDao.instance.getDataPoint(pointId, false);
            if (set.contains(pointId)) {
                response.addGenericMessage("validate.publisher.duplicatePoint", vo.getExtendedName(), vo.getXid());
            }
            else{
                if(vo == null)
                	it.remove();
                else
                	set.add(pointId);
            }
        }
    }

    //
    //
    // Editing customization
    //
    /*
     * Allows the publisher to provide custom context data to its own editing page. Can be used for things like lists
     * of comm ports and such.
     */
    public void addEditContext(Map<String, Object> model) {
        // No op. Override as required.
    }

    //
    //
    // Serialization
    //
    private static final long serialVersionUID = -1;
    private static final int version = 5;

    private void writeObject(ObjectOutputStream out) throws IOException {
        out.writeInt(version);
        out.writeObject(alarmLevels);
        SerializationHelper.writeSafeUTF(out, name);
        out.writeBoolean(enabled);
        out.writeObject(points);
        out.writeInt(publishType);
        out.writeInt(cacheWarningSize);
        out.writeInt(cacheDiscardSize);
        out.writeBoolean(sendSnapshot);
        out.writeInt(snapshotSendPeriodType);
        out.writeInt(snapshotSendPeriods);
    }

    @SuppressWarnings("unchecked")
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        int ver = in.readInt();

        // Switch on the version of the class so that version changes can be elegantly handled.
        if (ver == 1) {
            name = SerializationHelper.readSafeUTF(in);
            enabled = in.readBoolean();
            points = (List<T>) in.readObject();
            //Changes Only
            if(in.readBoolean())
            	this.publishType = PublishType.CHANGES_ONLY;
            else
            	this.publishType = PublishType.ALL;
            cacheWarningSize = in.readInt();
            cacheDiscardSize = cacheWarningSize * 3;
            sendSnapshot = in.readBoolean();
            snapshotSendPeriodType = in.readInt();
            snapshotSendPeriods = in.readInt();
            alarmLevels = new HashMap<Integer,Integer>();
        }
        else if (ver == 2) {
            name = SerializationHelper.readSafeUTF(in);
            enabled = in.readBoolean();
            points = (List<T>) in.readObject();
            //Changes Only
            if(in.readBoolean())
            	this.publishType = PublishType.CHANGES_ONLY;
            else
            	this.publishType = PublishType.ALL;
            cacheWarningSize = in.readInt();
            cacheDiscardSize = in.readInt();
            sendSnapshot = in.readBoolean();
            snapshotSendPeriodType = in.readInt();
            snapshotSendPeriods = in.readInt();
            alarmLevels = new HashMap<Integer,Integer>();
        }else if(ver == 3){
        	alarmLevels = (HashMap<Integer, Integer>) in.readObject();
        	for(Entry<Integer, Integer> item : alarmLevels.entrySet())
            	if(item.getValue() >= 2) //Add warning and important
            		item.setValue(item.getValue()+2);
            name = SerializationHelper.readSafeUTF(in);
            enabled = in.readBoolean();
            points = (List<T>) in.readObject();
            //Changes Only
            if(in.readBoolean())
            	this.publishType = PublishType.CHANGES_ONLY;
            else
            	this.publishType = PublishType.ALL;
            cacheWarningSize = in.readInt();
            cacheDiscardSize = in.readInt();
            sendSnapshot = in.readBoolean();
            snapshotSendPeriodType = in.readInt();
            snapshotSendPeriods = in.readInt();
        }else if(ver == 4){
        	alarmLevels = (HashMap<Integer, Integer>) in.readObject();
        	for(Entry<Integer, Integer> item : alarmLevels.entrySet())
            	if(item.getValue() >= 2) //Add warning and important
            		item.setValue(item.getValue()+2);
            name = SerializationHelper.readSafeUTF(in);
            enabled = in.readBoolean();
            points = (List<T>) in.readObject();
            publishType = in.readInt();
            cacheWarningSize = in.readInt();
            cacheDiscardSize = in.readInt();
            sendSnapshot = in.readBoolean();
            snapshotSendPeriodType = in.readInt();
            snapshotSendPeriods = in.readInt();
        }else if(ver == 5){
        	alarmLevels = (HashMap<Integer, Integer>) in.readObject();
            name = SerializationHelper.readSafeUTF(in);
            enabled = in.readBoolean();
            points = (List<T>) in.readObject();
            publishType = in.readInt();
            cacheWarningSize = in.readInt();
            cacheDiscardSize = in.readInt();
            sendSnapshot = in.readBoolean();
            snapshotSendPeriodType = in.readInt();
            snapshotSendPeriods = in.readInt();
        }
    }

    @Override
    public void jsonWrite(ObjectWriter writer) throws IOException, JsonException {
        super.jsonWrite(writer);
        writer.writeEntry("type", definition.getPublisherTypeName());
        writer.writeEntry("points", points);
        writer.writeEntry("snapshotSendPeriodType", Common.TIME_PERIOD_CODES.getCode(snapshotSendPeriodType));
        writer.writeEntry("publishType", PUBLISH_TYPE_CODES.getCode(publishType));
        ExportCodes eventCodes = getEventCodes();
        if (eventCodes != null && eventCodes.size() > 0) {
            Map<String, String> alarmCodeLevels = new HashMap<>();

            for (int i = 0; i < eventCodes.size(); i++) {
                int eventId = eventCodes.getId(i);
                int level = getAlarmLevel(eventId, AlarmLevels.URGENT);
                alarmCodeLevels.put(eventCodes.getCode(eventId), AlarmLevels.CODES.getCode(level));
            }

            writer.writeEntry("alarmLevels", alarmCodeLevels);
        }
    }

    @Override
    public void jsonRead(JsonReader reader, JsonObject jsonObject) throws JsonException {
        
        //Not reading XID so can't do this: super.jsonRead(reader, jsonObject);
        name = jsonObject.getString("name");
        enabled = jsonObject.getBoolean("enabled");
        
        //Legacy conversion for publishType
        if(jsonObject.containsKey("publishType")) {
        	String publishTypeCode = jsonObject.getString("publishType");
        	int publishTypeId = PUBLISH_TYPE_CODES.getId(publishTypeCode);
        	if(publishTypeId == -1)
        		throw new TranslatableJsonException("emport.error.invalid", "publishType", 
        			publishTypeCode, PUBLISH_TYPE_CODES.getCodeList());
        	publishType = publishTypeId;
        }else if(jsonObject.containsKey("changesOnly")){
        	boolean changesOnly = jsonObject.getBoolean("changesOnly");
        	if(changesOnly){
        		this.publishType = PublishType.CHANGES_ONLY;
        	}else{
        		this.publishType = PublishType.ALL;
        	}
        }
        
        //Could wrap the readInto with a try-catch in case one dataPointId entry is null,
        // however this would be a silent suppression of the issue, so we have elected not to.
        // infiniteautomation/ma-core-public#948
    	JsonArray arr = jsonObject.getJsonArray("points");
        if (arr != null) {
            points.clear();
            for (JsonValue jv : arr) {
                T point = createPublishedPointInstance();
                reader.readInto(point, jv.toJsonObject());
                points.add(point);
            }
        }

        String text = jsonObject.getString("snapshotSendPeriodType");
        if (text != null) {
            snapshotSendPeriodType = Common.TIME_PERIOD_CODES.getId(text);
            if (snapshotSendPeriodType == -1)
                throw new TranslatableJsonException("emport.error.invalid", "snapshotSendPeriodType", text,
                        Common.TIME_PERIOD_CODES.getCodeList());
        }
        
        JsonObject alarmCodeLevels = jsonObject.getJsonObject("alarmLevels");
        if (alarmCodeLevels != null) {
            ExportCodes eventCodes = getEventCodes();
            if (eventCodes != null && eventCodes.size() > 0) {
                for (String code : alarmCodeLevels.keySet()) {
                    int eventId = eventCodes.getId(code);
                    if (!eventCodes.isValidId(eventId))
                        throw new TranslatableJsonException("emport.error.eventCode", code, eventCodes.getCodeList());

                    text = alarmCodeLevels.getString(code);
                    int level = AlarmLevels.CODES.getId(text);
                    if (!AlarmLevels.CODES.isValidId(level))
                        throw new TranslatableJsonException("emport.error.alarmLevel", text, code,
                                AlarmLevels.CODES.getCodeList());

                    setAlarmLevel(eventId, level);
                }
            }
        }
    }
    

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.vo.AbstractVO#getDao()
	 */
	@Override
	protected AbstractDao<PublisherVO<?>> getDao() {
		return PublisherDao.instance;
	}


	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.vo.AbstractVO#getTypeKey()
	 */
	@Override
	public String getTypeKey() {
		return "event.audit.publisher";
	}
}
