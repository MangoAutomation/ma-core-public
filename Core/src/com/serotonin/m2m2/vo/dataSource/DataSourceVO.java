/*
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.vo.dataSource;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.infiniteautomation.mango.util.exception.ValidationException;
import com.serotonin.ShouldNeverHappenException;
import com.serotonin.json.JsonException;
import com.serotonin.json.JsonReader;
import com.serotonin.json.ObjectWriter;
import com.serotonin.json.spi.JsonProperty;
import com.serotonin.json.type.JsonObject;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.dao.DataSourceDao;
import com.serotonin.m2m2.i18n.ProcessResult;
import com.serotonin.m2m2.i18n.TranslatableJsonException;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.i18n.Translations;
import com.serotonin.m2m2.module.DataSourceDefinition;
import com.serotonin.m2m2.rt.dataSource.DataSourceRT;
import com.serotonin.m2m2.rt.event.AlarmLevels;
import com.serotonin.m2m2.rt.event.type.DataSourceEventType;
import com.serotonin.m2m2.rt.event.type.DuplicateHandling;
import com.serotonin.m2m2.util.ExportCodes;
import com.serotonin.m2m2.vo.AbstractActionVO;
import com.serotonin.m2m2.vo.DataPointVO.PurgeTypes;
import com.serotonin.m2m2.vo.event.EventTypeVO;

abstract public class DataSourceVO<T extends DataSourceVO<T>> extends AbstractActionVO<T> {
    public static final String XID_PREFIX = "DS_";

    abstract public TranslatableMessage getConnectionDescription();

    abstract public PointLocatorVO<?> createPointLocator();

    abstract public DataSourceRT<? extends DataSourceVO<?>> createDataSourceRT();

    abstract public ExportCodes getEventCodes();

    final public List<EventTypeVO> getEventTypes() {
        List<EventTypeVO> eventTypes = new ArrayList<>();
        addEventTypes(eventTypes);
        return eventTypes;
    }

    abstract protected void addEventTypes(List<EventTypeVO> eventTypes);

    @Override
    public boolean isNew() {
        return id == Common.NEW_ID;
    }

    private DataSourceDefinition definition;

    private Map<Integer, AlarmLevels> alarmLevels = new HashMap<>();

    @JsonProperty
    private boolean purgeOverride = false;
    private int purgeType = PurgeTypes.YEARS;
    @JsonProperty
    private int purgePeriod = 1;
    @JsonProperty
    private String editPermission;

    public final DataSourceDefinition getDefinition() {
        return definition;
    }

    public void setDefinition(DataSourceDefinition definition) {
        this.definition = definition;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void setAlarmLevel(int eventId, AlarmLevels level) {
        alarmLevels.put(eventId, level);
    }
    
    /**
     * Set an alarm level based on the sub-type of the data source event type
     * which MUST (and already is) one of the codes in getEventCodes()
     * @param subType
     * @param level
     */
    public void setAlarmLevel(String subType, AlarmLevels level) throws ValidationException {
        ExportCodes codes = getEventCodes();
        int eventId = codes.getId(subType);
        if(eventId == -1) {
            ProcessResult result = new ProcessResult();
            result.addContextualMessage("alarmLevel", "emport.error.eventCode", subType, codes.getCodeList());
            throw new ValidationException(result);
        }
        alarmLevels.put(eventId, level);
    }

    public AlarmLevels getAlarmLevel(int eventId, AlarmLevels defaultLevel) {
        AlarmLevels level = alarmLevels.get(eventId);
        if (level == null)
            return defaultLevel;
        return level;
    }

    public boolean isPurgeOverride() {
        return purgeOverride;
    }

    public void setPurgeOverride(boolean purgeOverride) {
        this.purgeOverride = purgeOverride;
    }

    public int getPurgeType() {
        return purgeType;
    }

    public void setPurgeType(int purgeType) {
        this.purgeType = purgeType;
    }

    public int getPurgePeriod() {
        return purgePeriod;
    }

    public void setPurgePeriod(int purgePeriod) {
        this.purgePeriod = purgePeriod;
    }

    public String getEditPermission() {
        return editPermission;
    }

    public void setEditPermission(String editPermission) {
        this.editPermission = editPermission;
    }

    /**
     * Helper to get description on Page
     *
     * @return
     */
    public String getConnectionDescriptionString() {
        return getConnectionDescription().translate(Common.getTranslations());
    }

    public void setConnectionDescriptionString(String str) {
        //No-op
    }

    /**
     * @param dsSpecificEventTypeId corresponds to typeRef2
     * @param message
     * @return
     */
    protected EventTypeVO createEventType(int dsSpecificEventTypeId, TranslatableMessage message) {
        return createEventType(dsSpecificEventTypeId, message, DuplicateHandling.IGNORE, AlarmLevels.URGENT);
    }

    /**
     * @param dsSpecificEventTypeId corresponds to typeRef2
     * @param message
     * @param duplicateHandling
     * @param defaultAlarmLevel
     * @return
     */
    protected EventTypeVO createEventType(int dsSpecificEventTypeId, TranslatableMessage message, DuplicateHandling duplicateHandling,
            AlarmLevels defaultAlarmLevel) {
        AlarmLevels alarmLevel = getAlarmLevel(dsSpecificEventTypeId, defaultAlarmLevel);
        return new EventTypeVO(
                new DataSourceEventType(getId(), dsSpecificEventTypeId, alarmLevel, duplicateHandling),
                message, alarmLevel);
    }

    public TranslatableMessage getTypeDescription() {
        return new TranslatableMessage(getDefinition().getDescriptionKey());
    }

    /**
     *
     * @return
     */
    public String getTypeDescriptionString() {
        return Common.translate(getDefinition().getDescriptionKey());
    }

    public void setTypeDescriptionString(String m) {
        //no op
    }

    @Override
    public void validate(ProcessResult response) {
        super.validate(response);
        if (purgeOverride) {
            if (purgeType != PurgeTypes.DAYS && purgeType != PurgeTypes.MONTHS && purgeType != PurgeTypes.WEEKS
                    && purgeType != PurgeTypes.YEARS)
                response.addContextualMessage("purgeType", "validate.invalidValue");
            if (purgePeriod <= 0)
                response.addContextualMessage("purgePeriod", "validate.greaterThanZero");
        }
    }

    protected String getMessage(Translations translations, String key, Object... args) {
        return new TranslatableMessage(key, args).translate(translations);
    }

    @Override
    public String getTypeKey() {
        return "event.audit.dataSource";
    }

    //
    //
    // Editing customization
    //
    /*
     * Allows the data source to provide custom context data to its own editing page. Can be used for things like lists
     * of comm ports and such. See DataSourceEditController.
     */
    public void addEditContext(Map<String, Object> model) {
        // No op. Override as required.
    }

    //
    //
    // Serialization
    //
    private static final long serialVersionUID = -1;
    private static final int version = 4;

    private void writeObject(ObjectOutputStream out) throws IOException {
        out.writeInt(version);
        out.writeBoolean(enabled);
        out.writeObject(alarmLevels);
        out.writeBoolean(purgeOverride);
        out.writeInt(purgeType);
        out.writeInt(purgePeriod);
    }

    @SuppressWarnings("unchecked")
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        int ver = in.readInt();

        // Switch on the version of the class so that version changes can be elegantly handled.
        if (ver == 1) {
            enabled = in.readBoolean();
            HashMap<Integer, Integer> alarmLevelsInt = (HashMap<Integer, Integer>) in.readObject();
            for(Entry<Integer, Integer> item : alarmLevelsInt.entrySet())
                if(item.getValue() >= 2) //Add warning and important
                    item.setValue(item.getValue()+2);
            alarmLevels = AlarmLevels.convertMap(alarmLevelsInt);
            purgeOverride = false;
            purgeType = PurgeTypes.YEARS;
            purgePeriod = 1;
        }
        else if (ver == 2) {
            enabled = in.readBoolean();
            HashMap<Integer, Integer> alarmLevelsInt = (HashMap<Integer, Integer>) in.readObject();
            for(Entry<Integer, Integer> item : alarmLevelsInt.entrySet())
                if(item.getValue() >= 2) //Add warning and important
                    item.setValue(item.getValue()+2);
            alarmLevels = AlarmLevels.convertMap(alarmLevelsInt);
            purgeOverride = in.readBoolean();
            purgeType = in.readInt();
            purgePeriod = in.readInt();
        }
        else if (ver == 3) {
            enabled = in.readBoolean();
            HashMap<Integer, Integer> alarmLevelsInt = (HashMap<Integer, Integer>) in.readObject();
            alarmLevels = AlarmLevels.convertMap(alarmLevelsInt);
            purgeOverride = in.readBoolean();
            purgeType = in.readInt();
            purgePeriod = in.readInt();
        }
        else if (ver == 4) {
            enabled = in.readBoolean();
            alarmLevels = (HashMap<Integer, AlarmLevels>) in.readObject();
            purgeOverride = in.readBoolean();
            purgeType = in.readInt();
            purgePeriod = in.readInt();
        }
        else {
            throw new ShouldNeverHappenException("Unknown serialization version.");
        }
    }

    @Override
    public void jsonWrite(ObjectWriter writer) throws IOException, JsonException {
        super.jsonWrite(writer);

        //Write the type
        writer.writeEntry("type", this.definition.getDataSourceTypeName());

        ExportCodes eventCodes = getEventCodes();
        if (eventCodes != null && eventCodes.size() > 0) {
            Map<String, String> alarmCodeLevels = new HashMap<>();

            for (int i = 0; i < eventCodes.size(); i++) {
                int eventId = eventCodes.getId(i);
                AlarmLevels level = getAlarmLevel(eventId, AlarmLevels.URGENT);
                alarmCodeLevels.put(eventCodes.getCode(eventId), level.name());
            }

            writer.writeEntry("alarmLevels", alarmCodeLevels);
        }

        writer.writeEntry("purgeType", Common.TIME_PERIOD_CODES.getCode(purgeType));
    }

    @Override
    public void jsonRead(JsonReader reader, JsonObject jsonObject) throws JsonException {

        //Not reading XID so can't do this: super.jsonRead(reader, jsonObject);
        if(jsonObject.containsKey("name"))
            name = getString(jsonObject, "name");
        if(jsonObject.containsKey("enabled"))
            enabled = getBoolean(jsonObject, "enabled");

        JsonObject alarmCodeLevels = jsonObject.getJsonObject("alarmLevels");
        if (alarmCodeLevels != null) {
            ExportCodes eventCodes = getEventCodes();
            if (eventCodes != null && eventCodes.size() > 0) {
                for (String code : alarmCodeLevels.keySet()) {
                    int eventId = eventCodes.getId(code);
                    if (!eventCodes.isValidId(eventId))
                        throw new TranslatableJsonException("emport.error.eventCode", code, eventCodes.getCodeList());

                    String text = alarmCodeLevels.getString(code);
                    try {
                        setAlarmLevel(eventId, AlarmLevels.fromName(text));
                    } catch (IllegalArgumentException | NullPointerException e) {
                        throw new TranslatableJsonException("emport.error.alarmLevel", text, code,
                                Arrays.asList(AlarmLevels.values()));
                    }
                }
            }
        }

        String text = jsonObject.getString("purgeType");
        if (text != null) {
            purgeType = Common.TIME_PERIOD_CODES.getId(text);
            if (purgeType == -1)
                throw new TranslatableJsonException("emport.error.invalid", "purgeType", text,
                        Common.TIME_PERIOD_CODES.getCodeList());
        }
    }

    protected void writeUpdatePeriodType(ObjectWriter writer, int updatePeriodType) throws IOException, JsonException {
        writer.writeEntry("updatePeriodType", Common.TIME_PERIOD_CODES.getCode(updatePeriodType));
    }

    protected Integer readUpdatePeriodType(JsonObject json) throws JsonException {
        return readPeriodType("updatePeriodType", json);
    }
    
    protected Integer readPeriodType(String field, JsonObject json) throws JsonException {
        try {
            String text = getString(json, field);
            if (text == null)
                return null;
    
            int value = Common.TIME_PERIOD_CODES.getId(text);
            if (value == -1)
                throw new TranslatableJsonException("emport.error.invalid", field, text,
                        Common.TIME_PERIOD_CODES.getCodeList());
            return value;
        } catch(JsonException e) {
            //For legacy data sources who accidentally used an integer instead of the code
            try {
                int testInt = getInt(json, field);
                if(Common.TIME_PERIOD_CODES.getCode(testInt) == null) //Tell them to use text instead...
                    throw new TranslatableJsonException("emport.error.invalid", field, testInt, Common.TIME_PERIOD_CODES.getCodeList());
                return testInt;
            }catch(JsonException e2) {
                //Wasn't an int 
                throw new TranslatableJsonException("emport.error.missing", field, Common.TIME_PERIOD_CODES.getCodeList());
            }
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    protected DataSourceDao<T> getDao() {
        return (DataSourceDao<T>) DataSourceDao.getInstance();
    }
}
