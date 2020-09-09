/**
 * Copyright (C) 2016 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.vo.event;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.infiniteautomation.mango.spring.service.PermissionService;
import com.infiniteautomation.mango.util.script.ScriptPermissions;
import com.serotonin.db.pair.IntStringPair;
import com.serotonin.json.JsonException;
import com.serotonin.json.JsonReader;
import com.serotonin.json.ObjectWriter;
import com.serotonin.json.spi.JsonProperty;
import com.serotonin.json.type.JsonArray;
import com.serotonin.json.type.JsonObject;
import com.serotonin.json.type.JsonValue;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.dao.DataPointDao;
import com.serotonin.m2m2.i18n.TranslatableJsonException;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.util.ExportCodes;
import com.serotonin.m2m2.vo.DataPointVO;
import com.serotonin.m2m2.vo.role.Role;
import com.serotonin.util.SerializationHelper;

/**
 * @author Terry Packer
 *
 */
public class SetPointEventHandlerVO extends AbstractEventHandlerVO {

    public static final int SET_ACTION_NONE = 0;
    public static final int SET_ACTION_POINT_VALUE = 1;
    public static final int SET_ACTION_STATIC_VALUE = 2;
    public static final int SET_ACTION_SCRIPT_VALUE = 3;

    public static final String TARGET_CONTEXT_KEY = "target";

    public static ExportCodes SET_ACTION_CODES = new ExportCodes();
    static {
        SET_ACTION_CODES.addElement(SET_ACTION_NONE, "NONE", "eventHandlers.action.none");
        SET_ACTION_CODES.addElement(SET_ACTION_POINT_VALUE, "POINT_VALUE", "eventHandlers.action.point");
        SET_ACTION_CODES.addElement(SET_ACTION_STATIC_VALUE, "STATIC_VALUE", "eventHandlers.action.static");
        SET_ACTION_CODES.addElement(SET_ACTION_SCRIPT_VALUE, "SCRIPT_VALUE", "eventHandlers.action.script");
    }

    private int targetPointId;
    private int activeAction;
    private String activeValueToSet;
    private int activePointId;
    private int inactiveAction;
    private String inactiveValueToSet;
    private int inactivePointId;
    private String activeScript;
    private String inactiveScript;
    @JsonProperty(readAliases = {"scriptPermissions"})
    private ScriptPermissions scriptRoles = new ScriptPermissions();
    private List<IntStringPair> additionalContext = new ArrayList<>();

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

    public String getActiveScript() {
        return activeScript;
    }

    public void setActiveScript(String activeScript) {
        this.activeScript = activeScript;
    }

    public String getInactiveScript() {
        return inactiveScript;
    }

    public void setInactiveScript(String inactiveScript) {
        this.inactiveScript = inactiveScript;
    }

    public ScriptPermissions getScriptRoles() {
        return scriptRoles;
    }

    public void setScriptRoles(ScriptPermissions scriptRoles) {
        this.scriptRoles = scriptRoles;
    }

    public List<IntStringPair> getAdditionalContext() {
        return additionalContext;
    }

    public void setAdditionalContext(List<IntStringPair> additionalContext) {
        this.additionalContext = additionalContext;
    }

    //
    //
    // Serialization
    //
    private static final long serialVersionUID = -1;
    private static final int version = 4;

    private void writeObject(ObjectOutputStream out) throws IOException {
        out.writeInt(version);
        out.writeInt(targetPointId);
        out.writeInt(activeAction);
        SerializationHelper.writeSafeUTF(out, activeValueToSet);
        out.writeInt(activePointId);
        out.writeInt(inactiveAction);
        SerializationHelper.writeSafeUTF(out, inactiveValueToSet);
        out.writeInt(inactivePointId);
        SerializationHelper.writeSafeUTF(out, activeScript);
        SerializationHelper.writeSafeUTF(out, inactiveScript);
        out.writeObject(additionalContext);
        out.writeObject(scriptRoles);
    }

    @SuppressWarnings({"unchecked", "deprecation"})
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
            activeScript = inactiveScript = null;
            additionalContext = new ArrayList<IntStringPair>();
            scriptRoles = new ScriptPermissions();
        } else if (ver == 2) {
            targetPointId = in.readInt();
            activeAction = in.readInt();
            activeValueToSet = SerializationHelper.readSafeUTF(in);
            activePointId = in.readInt();
            inactiveAction = in.readInt();
            inactiveValueToSet = SerializationHelper.readSafeUTF(in);
            inactivePointId = in.readInt();
            activeScript = SerializationHelper.readSafeUTF(in);
            inactiveScript = SerializationHelper.readSafeUTF(in);
            additionalContext = new ArrayList<IntStringPair>();
            scriptRoles = new ScriptPermissions();
        } else if (ver == 3) {
            targetPointId = in.readInt();
            activeAction = in.readInt();
            activeValueToSet = SerializationHelper.readSafeUTF(in);
            activePointId = in.readInt();
            inactiveAction = in.readInt();
            inactiveValueToSet = SerializationHelper.readSafeUTF(in);
            inactivePointId = in.readInt();
            activeScript = SerializationHelper.readSafeUTF(in);
            inactiveScript = SerializationHelper.readSafeUTF(in);
            additionalContext = (List<IntStringPair>) in.readObject();
            com.serotonin.m2m2.rt.script.ScriptPermissions oldPermissions = (com.serotonin.m2m2.rt.script.ScriptPermissions) in.readObject();
            if(oldPermissions != null) {
                PermissionService permissionService = Common.getBean(PermissionService.class);
                Set<Role> roles = permissionService.upgradeScriptRoles(oldPermissions.getAllLegacyPermissions());
                scriptRoles = new ScriptPermissions(roles, oldPermissions.getPermissionHolderName());
            }else {
                scriptRoles = new ScriptPermissions();
            }
        } else if (ver == 4) {
            targetPointId = in.readInt();
            activeAction = in.readInt();
            activeValueToSet = SerializationHelper.readSafeUTF(in);
            activePointId = in.readInt();
            inactiveAction = in.readInt();
            inactiveValueToSet = SerializationHelper.readSafeUTF(in);
            inactivePointId = in.readInt();
            activeScript = SerializationHelper.readSafeUTF(in);
            inactiveScript = SerializationHelper.readSafeUTF(in);
            additionalContext = (List<IntStringPair>) in.readObject();
            scriptRoles = (ScriptPermissions) in.readObject();
        }
    }

    @Override
    public void jsonWrite(ObjectWriter writer) throws IOException, JsonException {
        super.jsonWrite(writer);

        String dpXid = DataPointDao.getInstance().getXidById(targetPointId);
        writer.writeEntry("targetPointId", dpXid);

        // Active
        writer.writeEntry("activeAction", SET_ACTION_CODES.getCode(activeAction));
        if (activeAction == SET_ACTION_POINT_VALUE) {
            dpXid = DataPointDao.getInstance().getXidById(activePointId);
            writer.writeEntry("activePointId", dpXid);
        }
        else if (activeAction == SET_ACTION_STATIC_VALUE)
            writer.writeEntry("activeValueToSet", activeValueToSet);
        else if (activeAction == SET_ACTION_SCRIPT_VALUE)
            writer.writeEntry("activeScript", activeScript);

        // Inactive
        writer.writeEntry("inactiveAction", SET_ACTION_CODES.getCode(inactiveAction));
        if (inactiveAction == SET_ACTION_POINT_VALUE) {
            dpXid = DataPointDao.getInstance().getXidById(inactivePointId);
            writer.writeEntry("inactivePointId", dpXid);
        }
        else if (inactiveAction == SET_ACTION_STATIC_VALUE)
            writer.writeEntry("inactiveValueToSet", inactiveValueToSet);
        else if (inactiveAction == SET_ACTION_SCRIPT_VALUE)
            writer.writeEntry("inactiveScript", inactiveScript);

        JsonArray context = new JsonArray();
        for(IntStringPair pnt : additionalContext) {
            DataPointVO dpvo = DataPointDao.getInstance().get(pnt.getKey());
            if(dpvo != null) {
                JsonObject point = new JsonObject();
                point.put("dataPointXid", dpvo.getXid());
                point.put("contextKey", pnt.getValue());
                context.add(point);
            }
        }
        writer.writeEntry("additionalContext", context);
    }

    @Override
    public void jsonRead(JsonReader reader, JsonObject jsonObject) throws JsonException {
        super.jsonRead(reader, jsonObject);

        DataPointDao dataPointDao = DataPointDao.getInstance();
        String xid = jsonObject.getString("targetPointId");
        if (xid != null) {
            Integer id = dataPointDao.getIdByXid(xid);
            if (id == null)
                throw new TranslatableJsonException("emport.error.missingPoint", xid);
            targetPointId = id;
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
        else if (activeAction == SET_ACTION_SCRIPT_VALUE) {
            text = jsonObject.getString("activeScript");
            if (text == null)
                throw new TranslatableJsonException("emport.error.eventHandler.invalid", "inactiveScript");
            activeScript = text;
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
        else if (inactiveAction == SET_ACTION_SCRIPT_VALUE) {
            text = jsonObject.getString("inactiveScript");
            if (text == null)
                throw new TranslatableJsonException("emport.error.eventHandler.invalid", "inactiveScript");
            inactiveScript = text;
        }

        JsonArray context = jsonObject.getJsonArray("additionalContext");
        if(context != null) {
            List<IntStringPair> additionalContext = new ArrayList<>();
            for(JsonValue jv : context) {
                JsonObject jo = jv.toJsonObject();
                String dataPointXid = jo.getString("dataPointXid");
                if(dataPointXid == null)
                    throw new TranslatableJsonException("emport.error.context.missing", "dataPointXid");

                Integer id = DataPointDao.getInstance().getIdByXid(dataPointXid);
                if(id == null)
                    throw new TranslatableJsonException("emport.error.missingPoint", dataPointXid);

                String contextKey = jo.getString("contextKey");
                if(contextKey == null)
                    throw new TranslatableJsonException("emport.error.context.missing", "contextKey");

                additionalContext.add(new IntStringPair(id, contextKey));
            }
            this.additionalContext = additionalContext;
        } else {
            this.additionalContext = new ArrayList<>();
        }
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
}
