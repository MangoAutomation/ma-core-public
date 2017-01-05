/**
 * Copyright (C) 2016 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.vo.event;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import com.serotonin.ShouldNeverHappenException;
import com.serotonin.json.JsonException;
import com.serotonin.json.JsonReader;
import com.serotonin.json.ObjectWriter;
import com.serotonin.json.type.JsonObject;
import com.serotonin.m2m2.DataTypes;
import com.serotonin.m2m2.db.dao.DataPointDao;
import com.serotonin.m2m2.i18n.ProcessResult;
import com.serotonin.m2m2.i18n.TranslatableJsonException;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.rt.event.handlers.EventHandlerRT;
import com.serotonin.m2m2.rt.event.handlers.SetPointHandlerRT;
import com.serotonin.m2m2.util.ExportCodes;
import com.serotonin.m2m2.vo.DataPointVO;
import com.serotonin.m2m2.web.mvc.rest.v1.model.events.handlers.AbstractEventHandlerModel;
import com.serotonin.util.SerializationHelper;

/**
 * @author Terry Packer
 *
 */
public class SetPointEventHandlerVO extends AbstractEventHandlerVO<SetPointEventHandlerVO>{

    public static final int SET_ACTION_NONE = 0;
    public static final int SET_ACTION_POINT_VALUE = 1;
    public static final int SET_ACTION_STATIC_VALUE = 2;

    public static ExportCodes SET_ACTION_CODES = new ExportCodes();
    static {
        SET_ACTION_CODES.addElement(SET_ACTION_NONE, "NONE", "eventHandlers.action.none");
        SET_ACTION_CODES.addElement(SET_ACTION_POINT_VALUE, "POINT_VALUE", "eventHandlers.action.point");
        SET_ACTION_CODES.addElement(SET_ACTION_STATIC_VALUE, "STATIC_VALUE", "eventHandlers.action.static");
    }
	
    private int targetPointId;
    private int activeAction;
    private String activeValueToSet;
    private int activePointId;
    private int inactiveAction;
    private String inactiveValueToSet;
    private int inactivePointId;
    
    public int getTargetPointId() {
        return targetPointId;
    }

    public void setTargetPointId(int targetPointId) {
        this.targetPointId = targetPointId;
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
    
    public void validate(ProcessResult response) {
    	super.validate(response);
        DataPointVO dp = DataPointDao.instance.getDataPoint(targetPointId);

        if (dp == null)
            response.addGenericMessage("eventHandlers.noTargetPoint");
        else {
            int dataType = dp.getPointLocator().getDataTypeId();

            if (activeAction == SET_ACTION_NONE && inactiveAction == SET_ACTION_NONE)
                response.addGenericMessage("eventHandlers.noSetPointAction");

            // Active
            if (activeAction == SET_ACTION_STATIC_VALUE && dataType == DataTypes.MULTISTATE) {
                try {
                    Integer.parseInt(activeValueToSet);
                }
                catch (NumberFormatException e) {
                    response.addGenericMessage("eventHandlers.invalidActiveValue");
                }
            }

            if (activeAction == SET_ACTION_STATIC_VALUE && dataType == DataTypes.NUMERIC) {
                try {
                    Double.parseDouble(activeValueToSet);
                }
                catch (NumberFormatException e) {
                    response.addGenericMessage("eventHandlers.invalidActiveValue");
                }
            }

            if (activeAction == SET_ACTION_POINT_VALUE) {
                DataPointVO dpActive = DataPointDao.instance.getDataPoint(activePointId);

                if (dpActive == null)
                    response.addGenericMessage("eventHandlers.invalidActiveSource");
                else if (dataType != dpActive.getPointLocator().getDataTypeId())
                    response.addGenericMessage("eventHandlers.invalidActiveSourceType");
            }

            // Inactive
            if (inactiveAction == SET_ACTION_STATIC_VALUE && dataType == DataTypes.MULTISTATE) {
                try {
                    Integer.parseInt(inactiveValueToSet);
                }
                catch (NumberFormatException e) {
                    response.addGenericMessage("eventHandlers.invalidInactiveValue");
                }
            }

            if (inactiveAction == SET_ACTION_STATIC_VALUE && dataType == DataTypes.NUMERIC) {
                try {
                    Double.parseDouble(inactiveValueToSet);
                }
                catch (NumberFormatException e) {
                    response.addGenericMessage("eventHandlers.invalidInactiveValue");
                }
            }

            if (inactiveAction == SET_ACTION_POINT_VALUE) {
                DataPointVO dpInactive = DataPointDao.instance.getDataPoint(inactivePointId);

                if (dpInactive == null)
                    response.addGenericMessage("eventHandlers.invalidInactiveSource");
                else if (dataType != dpInactive.getPointLocator().getDataTypeId())
                    response.addGenericMessage("eventHandlers.invalidInactiveSourceType");
            }
        }
    }
    
    //
    //
    // Serialization
    //
    private static final long serialVersionUID = -1;
    private static final int version = 1;
    
    private void writeObject(ObjectOutputStream out) throws IOException {
        out.writeInt(version);
        out.writeInt(targetPointId);
        out.writeInt(activeAction);
        SerializationHelper.writeSafeUTF(out, activeValueToSet);
        out.writeInt(activePointId);
        out.writeInt(inactiveAction);
        SerializationHelper.writeSafeUTF(out, inactiveValueToSet);
        out.writeInt(inactivePointId);
    }
    
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        int ver = in.readInt();

        // Switch on the version of the class so that version changes can be elegantly handled.
        if (ver == 1) {
            targetPointId = in.readInt();
            activeAction = in.readInt();
            activeValueToSet = SerializationHelper.readSafeUTF(in);
            activePointId = in.readInt();
            inactiveAction = in.readInt();
            inactiveValueToSet = SerializationHelper.readSafeUTF(in);
            inactivePointId = in.readInt();
        }
    }
    
    @Override
    public void jsonWrite(ObjectWriter writer) throws IOException, JsonException {
    	super.jsonWrite(writer);
    	
        DataPointDao dataPointDao = DataPointDao.instance;
        DataPointVO dp = dataPointDao.getDataPoint(targetPointId);
        if (dp != null)
            writer.writeEntry("targetPointId", dp.getXid());

        // Active
        writer.writeEntry("activeAction", SET_ACTION_CODES.getCode(activeAction));
        if (activeAction == SET_ACTION_POINT_VALUE) {
            dp = dataPointDao.getDataPoint(activePointId);
            if (dp != null)
                writer.writeEntry("activePointId", dp.getXid());
        }
        else if (activeAction == SET_ACTION_STATIC_VALUE)
            writer.writeEntry("activeValueToSet", activeValueToSet);

        // Inactive
        writer.writeEntry("inactiveAction", SET_ACTION_CODES.getCode(inactiveAction));
        if (inactiveAction == SET_ACTION_POINT_VALUE) {
            dp = dataPointDao.getDataPoint(inactivePointId);
            if (dp != null)
                writer.writeEntry("inactivePointId", dp.getXid());
        }
        else if (inactiveAction == SET_ACTION_STATIC_VALUE)
            writer.writeEntry("inactiveValueToSet", inactiveValueToSet);
    }
    
    @Override
    public void jsonRead(JsonReader reader, JsonObject jsonObject) throws JsonException {
        super.jsonRead(reader, jsonObject);
        
    	DataPointDao dataPointDao = DataPointDao.instance;
        String xid = jsonObject.getString("targetPointId");
        if (xid != null) {
            DataPointVO vo = dataPointDao.getDataPoint(xid);
            if (vo == null)
                throw new TranslatableJsonException("emport.error.missingPoint", xid);
            targetPointId = vo.getId();
        }

        // Active
        String text = jsonObject.getString("activeAction");
        if (text != null) {
            activeAction = SET_ACTION_CODES.getId(text);
            if (!SET_ACTION_CODES.isValidId(activeAction))
                throw new TranslatableJsonException("emport.error.eventHandler.invalid", "activeAction", text,
                        SET_ACTION_CODES.getCodeList());
        }

        if (activeAction == SET_ACTION_POINT_VALUE) {
            xid = jsonObject.getString("activePointId");
            if (xid != null) {
                DataPointVO vo = dataPointDao.getDataPoint(xid);
                if (vo == null)
                    throw new TranslatableJsonException("emport.error.missingPoint", xid);
                activePointId = vo.getId();
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
                DataPointVO vo = dataPointDao.getDataPoint(xid);
                if (vo == null)
                    throw new TranslatableJsonException("emport.error.missingPoint", xid);
                inactivePointId = vo.getId();
            }
        }
        else if (inactiveAction == SET_ACTION_STATIC_VALUE) {
            text = jsonObject.getString("inactiveValueToSet");
            if (text != null)
                inactiveValueToSet = text;
        }

    }
    
    
    @Override
    public EventHandlerRT<SetPointEventHandlerVO> createRuntime(){
    	return new SetPointHandlerRT(this);
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
    
	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.vo.event.AbstractEventHandlerVO#asModel()
	 */
	@Override
	public AbstractEventHandlerModel<?> asModel() {
		throw new ShouldNeverHappenException("Un-implemented.");
	}
}
