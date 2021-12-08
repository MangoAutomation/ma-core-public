/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.vo.event;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.serotonin.json.JsonException;
import com.serotonin.json.JsonReader;
import com.serotonin.json.ObjectWriter;
import com.serotonin.json.spi.JsonProperty;
import com.serotonin.json.spi.JsonSerializable;
import com.serotonin.json.type.JsonArray;
import com.serotonin.json.type.JsonBoolean;
import com.serotonin.json.type.JsonNumber;
import com.serotonin.json.type.JsonObject;
import com.serotonin.json.util.TypeDefinition;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.DataType;
import com.serotonin.m2m2.db.dao.DataPointDao;
import com.serotonin.m2m2.i18n.ProcessResult;
import com.serotonin.m2m2.i18n.TranslatableJsonException;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.util.ExportCodes;
import com.serotonin.m2m2.vo.DataPointVO;
import com.serotonin.m2m2.web.dwr.beans.RecipientListEntryBean;
import com.serotonin.util.SerializationHelper;

/**
 * This class is now only used to upgrade old detectors
 * @author Terry Packer
 *
 */
@Deprecated
public class EventHandlerVO implements Serializable, JsonSerializable {
    public static final String XID_PREFIX = "EH_";

    public static final int TYPE_SET_POINT = 1;
    public static final int TYPE_EMAIL = 2;
    public static final int TYPE_PROCESS = 3;

    public static ExportCodes TYPE_CODES = new ExportCodes();
    static {
        TYPE_CODES.addElement(TYPE_SET_POINT, "SET_POINT", "eventHandlers.type.setPoint");
        TYPE_CODES.addElement(TYPE_EMAIL, "EMAIL", "eventHandlers.type.email");
        TYPE_CODES.addElement(TYPE_PROCESS, "PROCESS", "eventHandlers.type.process");
    }

    public static final int RECIPIENT_TYPE_ACTIVE = 1;
    public static final int RECIPIENT_TYPE_ESCALATION = 2;
    public static final int RECIPIENT_TYPE_INACTIVE = 3;

    public static ExportCodes RECIPIENT_TYPE_CODES = new ExportCodes();
    static {
        RECIPIENT_TYPE_CODES.addElement(RECIPIENT_TYPE_ACTIVE, "ACTIVE", "eventHandlers.recipientType.active");
        RECIPIENT_TYPE_CODES.addElement(RECIPIENT_TYPE_ESCALATION, "ESCALATION",
                "eventHandlers.recipientType.escalation");
        RECIPIENT_TYPE_CODES.addElement(RECIPIENT_TYPE_INACTIVE, "INACTIVE", "eventHandlers.recipientType.inactive");
    }

    public static final int SET_ACTION_NONE = 0;
    public static final int SET_ACTION_POINT_VALUE = 1;
    public static final int SET_ACTION_STATIC_VALUE = 2;

    public static ExportCodes SET_ACTION_CODES = new ExportCodes();
    static {
        SET_ACTION_CODES.addElement(SET_ACTION_NONE, "NONE", "eventHandlers.action.none");
        SET_ACTION_CODES.addElement(SET_ACTION_POINT_VALUE, "POINT_VALUE", "eventHandlers.action.point");
        SET_ACTION_CODES.addElement(SET_ACTION_STATIC_VALUE, "STATIC_VALUE", "eventHandlers.action.static");
    }

    // Common fields
    private int id = Common.NEW_ID;
    private String xid;
    @JsonProperty
    private String alias;
    private int handlerType;
    @JsonProperty
    private boolean disabled;

    // Set point handler fields.
    private int targetPointId;
    private int activeAction;
    private String activeValueToSet;
    private int activePointId;
    private int inactiveAction;
    private String inactiveValueToSet;
    private int inactivePointId;

    // Email handler fields.
    private List<RecipientListEntryBean> activeRecipients;
    private boolean sendEscalation;
    private int escalationDelayType;
    private int escalationDelay;
    private List<RecipientListEntryBean> escalationRecipients;
    private boolean sendInactive;
    private boolean inactiveOverride;
    private List<RecipientListEntryBean> inactiveRecipients;
    private boolean includeSystemInfo; //Include Work Items and Service Thread Pool Data
    private int includePointValueCount = 10;
    private boolean includeLogfile;

    // Process handler fields.
    private String activeProcessCommand;
    private int activeProcessTimeout = 15;
    private String inactiveProcessCommand;
    private int inactiveProcessTimeout = 15;


    public TranslatableMessage getMessage() {
        if (!StringUtils.isBlank(alias))
            return new TranslatableMessage("common.default", alias);
        return getTypeMessage(handlerType);
    }

    public static TranslatableMessage getSetActionMessage(int action) {
        switch (action) {
            case SET_ACTION_NONE:
                return new TranslatableMessage("eventHandlers.action.none");
            case SET_ACTION_POINT_VALUE:
                return new TranslatableMessage("eventHandlers.action.point");
            case SET_ACTION_STATIC_VALUE:
                return new TranslatableMessage("eventHandlers.action.static");
        }
        return new TranslatableMessage("common.unknown");
    }

    private static TranslatableMessage getTypeMessage(int handlerType) {
        switch (handlerType) {
            case TYPE_SET_POINT:
                return new TranslatableMessage("eventHandlers.type.setPoint");
            case TYPE_EMAIL:
                return new TranslatableMessage("eventHandlers.type.email");
            case TYPE_PROCESS:
                return new TranslatableMessage("eventHandlers.type.process");
        }
        return new TranslatableMessage("common.unknown");
    }

    public int getTargetPointId() {
        return targetPointId;
    }

    public void setTargetPointId(int targetPointId) {
        this.targetPointId = targetPointId;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getXid() {
        return xid;
    }

    public void setXid(String xid) {
        this.xid = xid;
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public int getHandlerType() {
        return handlerType;
    }

    public void setHandlerType(int handlerType) {
        this.handlerType = handlerType;
    }

    public boolean isDisabled() {
        return disabled;
    }

    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
    }

    public int getActiveAction() {
        return activeAction;
    }

    public void setActiveAction(int activeAction) {
        this.activeAction = activeAction;
    }

    public int getInactiveAction() {
        return inactiveAction;
    }

    public void setInactiveAction(int inactiveAction) {
        this.inactiveAction = inactiveAction;
    }

    public String getActiveValueToSet() {
        return activeValueToSet;
    }

    public void setActiveValueToSet(String activeValueToSet) {
        this.activeValueToSet = activeValueToSet;
    }

    public int getActivePointId() {
        return activePointId;
    }

    public void setActivePointId(int activePointId) {
        this.activePointId = activePointId;
    }

    public String getInactiveValueToSet() {
        return inactiveValueToSet;
    }

    public void setInactiveValueToSet(String inactiveValueToSet) {
        this.inactiveValueToSet = inactiveValueToSet;
    }

    public int getInactivePointId() {
        return inactivePointId;
    }

    public void setInactivePointId(int inactivePointId) {
        this.inactivePointId = inactivePointId;
    }

    public List<RecipientListEntryBean> getActiveRecipients() {
        return activeRecipients;
    }

    public void setActiveRecipients(List<RecipientListEntryBean> activeRecipients) {
        this.activeRecipients = activeRecipients;
    }

    public int getEscalationDelay() {
        return escalationDelay;
    }

    public void setEscalationDelay(int escalationDelay) {
        this.escalationDelay = escalationDelay;
    }

    public int getEscalationDelayType() {
        return escalationDelayType;
    }

    public void setEscalationDelayType(int escalationDelayType) {
        this.escalationDelayType = escalationDelayType;
    }

    public List<RecipientListEntryBean> getEscalationRecipients() {
        return escalationRecipients;
    }

    public void setEscalationRecipients(List<RecipientListEntryBean> escalationRecipients) {
        this.escalationRecipients = escalationRecipients;
    }

    public boolean isSendEscalation() {
        return sendEscalation;
    }

    public void setSendEscalation(boolean sendEscalation) {
        this.sendEscalation = sendEscalation;
    }

    public boolean isSendInactive() {
        return sendInactive;
    }

    public void setSendInactive(boolean sendInactive) {
        this.sendInactive = sendInactive;
    }

    public boolean isInactiveOverride() {
        return inactiveOverride;
    }

    public void setInactiveOverride(boolean inactiveOverride) {
        this.inactiveOverride = inactiveOverride;
    }

    public List<RecipientListEntryBean> getInactiveRecipients() {
        return inactiveRecipients;
    }

    public void setInactiveRecipients(List<RecipientListEntryBean> inactiveRecipients) {
        this.inactiveRecipients = inactiveRecipients;
    }

    public boolean isIncludeSystemInfo(){
        return this.includeSystemInfo;
    }
    public void setIncludeSystemInfo(boolean includeSystemInfo){
        this.includeSystemInfo = includeSystemInfo;
    }

    public int getIncludePointValueCount() {
        return includePointValueCount;
    }

    public void setIncludePointValueCount(int includePointValueCount) {
        this.includePointValueCount = includePointValueCount;
    }

    public boolean isIncludeLogfile() {
        return includeLogfile;
    }

    public void setIncludeLogfile(boolean includeLogfile) {
        this.includeLogfile = includeLogfile;
    }

    public String getActiveProcessCommand() {
        return activeProcessCommand;
    }

    public void setActiveProcessCommand(String activeProcessCommand) {
        this.activeProcessCommand = activeProcessCommand;
    }

    public int getActiveProcessTimeout() {
        return activeProcessTimeout;
    }

    public void setActiveProcessTimeout(int activeProcessTimeout) {
        this.activeProcessTimeout = activeProcessTimeout;
    }

    public String getInactiveProcessCommand() {
        return inactiveProcessCommand;
    }

    public void setInactiveProcessCommand(String inactiveProcessCommand) {
        this.inactiveProcessCommand = inactiveProcessCommand;
    }

    public int getInactiveProcessTimeout() {
        return inactiveProcessTimeout;
    }

    public void setInactiveProcessTimeout(int inactiveProcessTimeout) {
        this.inactiveProcessTimeout = inactiveProcessTimeout;
    }

    public void validate(ProcessResult response) {
        if (handlerType == TYPE_SET_POINT) {
            DataPointVO dp = DataPointDao.getInstance().get(targetPointId);

            if (dp == null)
                response.addGenericMessage("eventHandlers.noTargetPoint");
            else {
                DataType dataType = dp.getPointLocator().getDataType();

                if (activeAction == SET_ACTION_NONE && inactiveAction == SET_ACTION_NONE)
                    response.addGenericMessage("eventHandlers.noSetPointAction");

                // Active
                if (activeAction == SET_ACTION_STATIC_VALUE && dataType == DataType.MULTISTATE) {
                    try {
                        Integer.parseInt(activeValueToSet);
                    }
                    catch (NumberFormatException e) {
                        response.addGenericMessage("eventHandlers.invalidActiveValue");
                    }
                }

                if (activeAction == SET_ACTION_STATIC_VALUE && dataType == DataType.NUMERIC) {
                    try {
                        Double.parseDouble(activeValueToSet);
                    }
                    catch (NumberFormatException e) {
                        response.addGenericMessage("eventHandlers.invalidActiveValue");
                    }
                }

                if (activeAction == SET_ACTION_POINT_VALUE) {
                    DataPointVO dpActive = DataPointDao.getInstance().get(activePointId);

                    if (dpActive == null)
                        response.addGenericMessage("eventHandlers.invalidActiveSource");
                    else if (dataType != dpActive.getPointLocator().getDataType())
                        response.addGenericMessage("eventHandlers.invalidActiveSourceType");
                }

                // Inactive
                if (inactiveAction == SET_ACTION_STATIC_VALUE && dataType == DataType.MULTISTATE) {
                    try {
                        Integer.parseInt(inactiveValueToSet);
                    }
                    catch (NumberFormatException e) {
                        response.addGenericMessage("eventHandlers.invalidInactiveValue");
                    }
                }

                if (inactiveAction == SET_ACTION_STATIC_VALUE && dataType == DataType.NUMERIC) {
                    try {
                        Double.parseDouble(inactiveValueToSet);
                    }
                    catch (NumberFormatException e) {
                        response.addGenericMessage("eventHandlers.invalidInactiveValue");
                    }
                }

                if (inactiveAction == SET_ACTION_POINT_VALUE) {
                    DataPointVO dpInactive = DataPointDao.getInstance().get(inactivePointId);

                    if (dpInactive == null)
                        response.addGenericMessage("eventHandlers.invalidInactiveSource");
                    else if (dataType != dpInactive.getPointLocator().getDataType())
                        response.addGenericMessage("eventHandlers.invalidInactiveSourceType");
                }
            }
        }
        else if (handlerType == TYPE_EMAIL) {
            if (activeRecipients.isEmpty())
                response.addGenericMessage("eventHandlers.noEmailRecips");

            if (sendEscalation) {
                if (escalationDelay <= 0)
                    response.addContextualMessage("escalationDelay", "eventHandlers.escalDelayError");
                if (escalationRecipients.isEmpty())
                    response.addGenericMessage("eventHandlers.noEscalRecips");
            }

            if (sendInactive && inactiveOverride) {
                if (inactiveRecipients.isEmpty())
                    response.addGenericMessage("eventHandlers.noInactiveRecips");
            }
        }
        else if (handlerType == TYPE_PROCESS) {
            if (StringUtils.isBlank(activeProcessCommand) && StringUtils.isBlank(inactiveProcessCommand))
                response.addGenericMessage("eventHandlers.invalidCommands");

            if (!StringUtils.isBlank(activeProcessCommand) && activeProcessTimeout <= 0)
                response.addGenericMessage("validate.greaterThanZero");

            if (!StringUtils.isBlank(inactiveProcessCommand) && inactiveProcessTimeout <= 0)
                response.addGenericMessage("validate.greaterThanZero");
        }
    }

    //
    //
    // Serialization
    //
    private static final long serialVersionUID = -1;
    private static final int version = 5;

    private void writeObject(ObjectOutputStream out) throws IOException {
        out.writeInt(version);
        out.writeInt(handlerType);
        out.writeBoolean(disabled);
        if (handlerType == TYPE_SET_POINT) {
            out.writeInt(targetPointId);
            out.writeInt(activeAction);
            SerializationHelper.writeSafeUTF(out, activeValueToSet);
            out.writeInt(activePointId);
            out.writeInt(inactiveAction);
            SerializationHelper.writeSafeUTF(out, inactiveValueToSet);
            out.writeInt(inactivePointId);
        }
        else if (handlerType == TYPE_EMAIL) {
            out.writeObject(activeRecipients);
            out.writeBoolean(sendEscalation);
            out.writeInt(escalationDelayType);
            out.writeInt(escalationDelay);
            out.writeObject(escalationRecipients);
            out.writeBoolean(sendInactive);
            out.writeBoolean(inactiveOverride);
            out.writeObject(inactiveRecipients);
            out.writeBoolean(includeSystemInfo);
            out.writeInt(includePointValueCount);
            out.writeBoolean(includeLogfile);
        }
        else if (handlerType == TYPE_PROCESS) {
            SerializationHelper.writeSafeUTF(out, activeProcessCommand);
            out.writeInt(activeProcessTimeout);
            SerializationHelper.writeSafeUTF(out, inactiveProcessCommand);
            out.writeInt(inactiveProcessTimeout);
        }
    }

    @SuppressWarnings("unchecked")
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        int ver = in.readInt();
        // Switch on the version of the class so that version changes can be elegantly handled.
        if (ver == 1) {
            handlerType = in.readInt();
            disabled = in.readBoolean();
            if (handlerType == TYPE_SET_POINT) {
                targetPointId = in.readInt();
                activeAction = in.readInt();
                activeValueToSet = SerializationHelper.readSafeUTF(in);
                activePointId = in.readInt();
                inactiveAction = in.readInt();
                inactiveValueToSet = SerializationHelper.readSafeUTF(in);
                inactivePointId = in.readInt();
            }
            else if (handlerType == TYPE_EMAIL) {
                activeRecipients = (List<RecipientListEntryBean>) in.readObject();
                sendEscalation = in.readBoolean();
                escalationDelayType = in.readInt();
                escalationDelay = in.readInt();
                escalationRecipients = (List<RecipientListEntryBean>) in.readObject();
                sendInactive = in.readBoolean();
                inactiveOverride = in.readBoolean();
                inactiveRecipients = (List<RecipientListEntryBean>) in.readObject();
                includePointValueCount = 0;
            }
            else if (handlerType == TYPE_PROCESS) {
                activeProcessCommand = SerializationHelper.readSafeUTF(in);
                activeProcessTimeout = 15;
                inactiveProcessCommand = SerializationHelper.readSafeUTF(in);
                inactiveProcessTimeout = 15;
            }
        }
        else if (ver == 2) {
            handlerType = in.readInt();
            disabled = in.readBoolean();
            if (handlerType == TYPE_SET_POINT) {
                targetPointId = in.readInt();
                activeAction = in.readInt();
                activeValueToSet = SerializationHelper.readSafeUTF(in);
                activePointId = in.readInt();
                inactiveAction = in.readInt();
                inactiveValueToSet = SerializationHelper.readSafeUTF(in);
                inactivePointId = in.readInt();
            }
            else if (handlerType == TYPE_EMAIL) {
                activeRecipients = (List<RecipientListEntryBean>) in.readObject();
                sendEscalation = in.readBoolean();
                escalationDelayType = in.readInt();
                escalationDelay = in.readInt();
                escalationRecipients = (List<RecipientListEntryBean>) in.readObject();
                sendInactive = in.readBoolean();
                inactiveOverride = in.readBoolean();
                inactiveRecipients = (List<RecipientListEntryBean>) in.readObject();
                includePointValueCount = 0;
            }
            else if (handlerType == TYPE_PROCESS) {
                activeProcessCommand = SerializationHelper.readSafeUTF(in);
                activeProcessTimeout = in.readInt();
                inactiveProcessCommand = SerializationHelper.readSafeUTF(in);
                inactiveProcessTimeout = in.readInt();
            }
        }
        else if (ver == 3) {
            handlerType = in.readInt();
            disabled = in.readBoolean();
            if (handlerType == TYPE_SET_POINT) {
                targetPointId = in.readInt();
                activeAction = in.readInt();
                activeValueToSet = SerializationHelper.readSafeUTF(in);
                activePointId = in.readInt();
                inactiveAction = in.readInt();
                inactiveValueToSet = SerializationHelper.readSafeUTF(in);
                inactivePointId = in.readInt();
            }
            else if (handlerType == TYPE_EMAIL) {
                activeRecipients = (List<RecipientListEntryBean>) in.readObject();
                sendEscalation = in.readBoolean();
                escalationDelayType = in.readInt();
                escalationDelay = in.readInt();
                escalationRecipients = (List<RecipientListEntryBean>) in.readObject();
                sendInactive = in.readBoolean();
                inactiveOverride = in.readBoolean();
                inactiveRecipients = (List<RecipientListEntryBean>) in.readObject();
                includeSystemInfo = in.readBoolean();
                includePointValueCount = 0;
            }
            else if (handlerType == TYPE_PROCESS) {
                activeProcessCommand = SerializationHelper.readSafeUTF(in);
                activeProcessTimeout = in.readInt();
                inactiveProcessCommand = SerializationHelper.readSafeUTF(in);
                inactiveProcessTimeout = in.readInt();
            }
        }else if (ver == 4) {
            handlerType = in.readInt();
            disabled = in.readBoolean();
            if (handlerType == TYPE_SET_POINT) {
                targetPointId = in.readInt();
                activeAction = in.readInt();
                activeValueToSet = SerializationHelper.readSafeUTF(in);
                activePointId = in.readInt();
                inactiveAction = in.readInt();
                inactiveValueToSet = SerializationHelper.readSafeUTF(in);
                inactivePointId = in.readInt();
            }
            else if (handlerType == TYPE_EMAIL) {
                activeRecipients = (List<RecipientListEntryBean>) in.readObject();
                sendEscalation = in.readBoolean();
                escalationDelayType = in.readInt();
                escalationDelay = in.readInt();
                escalationRecipients = (List<RecipientListEntryBean>) in.readObject();
                sendInactive = in.readBoolean();
                inactiveOverride = in.readBoolean();
                inactiveRecipients = (List<RecipientListEntryBean>) in.readObject();
                includeSystemInfo = in.readBoolean();
                includePointValueCount = in.readInt();
            }
            else if (handlerType == TYPE_PROCESS) {
                activeProcessCommand = SerializationHelper.readSafeUTF(in);
                activeProcessTimeout = in.readInt();
                inactiveProcessCommand = SerializationHelper.readSafeUTF(in);
                inactiveProcessTimeout = in.readInt();
            }
        }else if (ver == 5) {
            handlerType = in.readInt();
            disabled = in.readBoolean();
            if (handlerType == TYPE_SET_POINT) {
                targetPointId = in.readInt();
                activeAction = in.readInt();
                activeValueToSet = SerializationHelper.readSafeUTF(in);
                activePointId = in.readInt();
                inactiveAction = in.readInt();
                inactiveValueToSet = SerializationHelper.readSafeUTF(in);
                inactivePointId = in.readInt();
            }
            else if (handlerType == TYPE_EMAIL) {
                activeRecipients = (List<RecipientListEntryBean>) in.readObject();
                sendEscalation = in.readBoolean();
                escalationDelayType = in.readInt();
                escalationDelay = in.readInt();
                escalationRecipients = (List<RecipientListEntryBean>) in.readObject();
                sendInactive = in.readBoolean();
                inactiveOverride = in.readBoolean();
                inactiveRecipients = (List<RecipientListEntryBean>) in.readObject();
                includeSystemInfo = in.readBoolean();
                includePointValueCount = in.readInt();
                includeLogfile = in.readBoolean();
            }
            else if (handlerType == TYPE_PROCESS) {
                activeProcessCommand = SerializationHelper.readSafeUTF(in);
                activeProcessTimeout = in.readInt();
                inactiveProcessCommand = SerializationHelper.readSafeUTF(in);
                inactiveProcessTimeout = in.readInt();
            }
        }
    }

    @Override
    public void jsonWrite(ObjectWriter writer) throws IOException, JsonException {
        DataPointDao dataPointDao = DataPointDao.getInstance();

        writer.writeEntry("xid", xid);
        writer.writeEntry("handlerType", TYPE_CODES.getCode(handlerType));

        if (handlerType == TYPE_SET_POINT) {
            String dpXid = dataPointDao.getXidById(targetPointId);
            writer.writeEntry("targetPointId", dpXid);

            // Active
            writer.writeEntry("activeAction", SET_ACTION_CODES.getCode(activeAction));
            if (activeAction == SET_ACTION_POINT_VALUE) {
                dpXid = dataPointDao.getXidById(activePointId);
                writer.writeEntry("activePointId", dpXid);
            }
            else if (activeAction == SET_ACTION_STATIC_VALUE)
                writer.writeEntry("activeValueToSet", activeValueToSet);

            // Inactive
            writer.writeEntry("inactiveAction", SET_ACTION_CODES.getCode(inactiveAction));
            if (inactiveAction == SET_ACTION_POINT_VALUE) {
                dpXid = dataPointDao.getXidById(inactivePointId);
                writer.writeEntry("inactivePointId", dpXid);
            }
            else if (inactiveAction == SET_ACTION_STATIC_VALUE)
                writer.writeEntry("inactiveValueToSet", inactiveValueToSet);
        }
        else if (handlerType == TYPE_EMAIL) {
            writer.writeEntry("activeRecipients", activeRecipients);
            writer.writeEntry("sendEscalation", sendEscalation);
            if (sendEscalation) {
                writer.writeEntry("escalationDelayType", Common.TIME_PERIOD_CODES.getCode(escalationDelayType));
                writer.writeEntry("escalationDelay", escalationDelay);
                writer.writeEntry("escalationRecipients", escalationRecipients);
            }
            writer.writeEntry("sendInactive", sendInactive);
            if (sendInactive) {
                writer.writeEntry("inactiveOverride", inactiveOverride);
                if (inactiveOverride)
                    writer.writeEntry("inactiveRecipients", inactiveRecipients);
            }
            writer.writeEntry("includeSystemInformation", includeSystemInfo);
            writer.writeEntry("includePointValueCount", includePointValueCount);
            writer.writeEntry("includeLogfile", includeLogfile);
        }
        else if (handlerType == TYPE_PROCESS) {
            writer.writeEntry("activeProcessCommand", activeProcessCommand);
            writer.writeEntry("activeProcessTimeout", activeProcessTimeout);
            writer.writeEntry("inactiveProcessCommand", inactiveProcessCommand);
            writer.writeEntry("inactiveProcessTimeout", inactiveProcessTimeout);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void jsonRead(JsonReader reader, JsonObject jsonObject) throws JsonException {
        DataPointDao dataPointDao = DataPointDao.getInstance();

        String text = jsonObject.getString("handlerType");
        if (text != null) {
            handlerType = TYPE_CODES.getId(text);
            if (!TYPE_CODES.isValidId(handlerType))
                throw new TranslatableJsonException("emport.error.eventHandler.invalid", "handlerType", text,
                        TYPE_CODES.getCodeList());
        }

        if (handlerType == TYPE_SET_POINT) {
            String xid = jsonObject.getString("targetPointId");
            if (xid != null) {
                Integer id = dataPointDao.getIdByXid(xid);
                if (id == null)
                    throw new TranslatableJsonException("emport.error.missingPoint", xid);
                targetPointId = id;
            }

            // Active
            text = jsonObject.getString("activeAction");
            if (text != null) {
                activeAction = SET_ACTION_CODES.getId(text);
                if (!SET_ACTION_CODES.isValidId(activeAction))
                    throw new TranslatableJsonException("emport.error.eventHandler.invalid", "activeAction", text,
                            SET_ACTION_CODES.getCodeList());
            }

            if (activeAction == SET_ACTION_POINT_VALUE) {
                xid = jsonObject.getString("activePointId");
                if (xid != null) {
                    Integer id = dataPointDao.getIdByXid(xid);
                    if (id == null)
                        throw new TranslatableJsonException("emport.error.missingPoint", xid);
                    activePointId = id;
                }
            }
            else if (activeAction == SET_ACTION_STATIC_VALUE) {
                text = jsonObject.getString("activeValueToSet");
                if (text != null)
                    activeValueToSet = text;
            }

            // Inactive
            text = jsonObject.getString("inactiveAction");
            if (text != null) {
                inactiveAction = SET_ACTION_CODES.getId(text);
                if (!SET_ACTION_CODES.isValidId(inactiveAction))
                    throw new TranslatableJsonException("emport.error.eventHandler.invalid", "inactiveAction", text,
                            SET_ACTION_CODES.getCodeList());
            }

            if (inactiveAction == SET_ACTION_POINT_VALUE) {
                xid = jsonObject.getString("inactivePointId");
                if (xid != null) {
                    Integer id = dataPointDao.getIdByXid(xid);
                    if (id == null)
                        throw new TranslatableJsonException("emport.error.missingPoint", xid);
                    inactivePointId = id;
                }
            }
            else if (inactiveAction == SET_ACTION_STATIC_VALUE) {
                text = jsonObject.getString("inactiveValueToSet");
                if (text != null)
                    inactiveValueToSet = text;
            }
        }
        else if (handlerType == TYPE_EMAIL) {
            TypeDefinition recipType = new TypeDefinition(List.class, RecipientListEntryBean.class);
            JsonArray jsonActiveRecipients = jsonObject.getJsonArray("activeRecipients");
            if (jsonActiveRecipients != null)
                activeRecipients = (List<RecipientListEntryBean>) reader.read(recipType, jsonActiveRecipients);

            JsonBoolean b = jsonObject.getJsonBoolean("sendEscalation");
            if (b != null)
                sendEscalation = b.booleanValue();

            if (sendEscalation) {
                text = jsonObject.getString("escalationDelayType");
                if (text != null) {
                    escalationDelayType = Common.TIME_PERIOD_CODES.getId(text);
                    if (escalationDelayType == -1)
                        throw new TranslatableJsonException("emport.error.invalid", "escalationDelayType", text,
                                Common.TIME_PERIOD_CODES.getCodeList());
                }

                JsonNumber i = jsonObject.getJsonNumber("escalationDelay");
                if (i != null)
                    escalationDelay = i.intValue();

                JsonArray jsonEscalationRecipients = jsonObject.getJsonArray("escalationRecipients");
                if (jsonEscalationRecipients != null)
                    escalationRecipients = (List<RecipientListEntryBean>) reader.read(recipType,
                            jsonEscalationRecipients);
            }

            b = jsonObject.getJsonBoolean("sendInactive");
            if (b != null)
                sendInactive = b.booleanValue();

            if (sendInactive) {
                b = jsonObject.getJsonBoolean("inactiveOverride");
                if (b != null)
                    inactiveOverride = b.booleanValue();

                if (inactiveOverride) {
                    JsonArray jsonInactiveRecipients = jsonObject.getJsonArray("inactiveRecipients");
                    if (jsonInactiveRecipients != null)
                        inactiveRecipients = (List<RecipientListEntryBean>) reader.read(recipType,
                                jsonInactiveRecipients);
                }
            }
            b = jsonObject.getJsonBoolean("includeSystemInformation");
            if(b != null){
                includeSystemInfo = b.booleanValue();
            }

            includePointValueCount = jsonObject.getInt("includePointValueCount", 0);

            b = jsonObject.getJsonBoolean("includeLogfile");
            if(b != null){
                includeSystemInfo = b.booleanValue();
            }
        }
        else if (handlerType == TYPE_PROCESS) {
            text = jsonObject.getString("activeProcessCommand");
            if (text != null)
                activeProcessCommand = text;

            JsonNumber i = jsonObject.getJsonNumber("activeProcessTimeout");
            if (i != null)
                activeProcessTimeout = i.intValue();

            text = jsonObject.getString("inactiveProcessCommand");
            if (text != null)
                inactiveProcessCommand = text;

            i = jsonObject.getJsonNumber("inactiveProcessTimeout");
            if (i != null)
                inactiveProcessTimeout = i.intValue();
        }
    }
}
