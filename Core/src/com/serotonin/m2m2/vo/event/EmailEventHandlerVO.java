/**
 * Copyright (C) 2016 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.vo.event;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import com.infiniteautomation.mango.spring.service.MangoJavaScriptService;
import com.infiniteautomation.mango.util.script.ScriptPermissions;
import com.serotonin.db.pair.IntStringPair;
import com.serotonin.json.JsonException;
import com.serotonin.json.JsonReader;
import com.serotonin.json.ObjectWriter;
import com.serotonin.json.type.JsonArray;
import com.serotonin.json.type.JsonBoolean;
import com.serotonin.json.type.JsonObject;
import com.serotonin.json.type.JsonValue;
import com.serotonin.json.util.TypeDefinition;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.Common.TimePeriods;
import com.serotonin.m2m2.db.dao.DataPointDao;
import com.serotonin.m2m2.db.dao.EventHandlerDao;
import com.serotonin.m2m2.i18n.ProcessResult;
import com.serotonin.m2m2.i18n.TranslatableJsonException;
import com.serotonin.m2m2.rt.event.handlers.EmailHandlerRT;
import com.serotonin.m2m2.rt.event.handlers.EventHandlerRT;
import com.serotonin.m2m2.rt.script.ScriptError;
import com.serotonin.m2m2.util.ExportCodes;
import com.serotonin.m2m2.vo.DataPointVO;
import com.serotonin.m2m2.vo.User;
import com.serotonin.m2m2.vo.permission.Permissions;
import com.serotonin.m2m2.web.dwr.beans.RecipientListEntryBean;
import com.serotonin.m2m2.web.mvc.rest.v1.model.events.handlers.EmailEventHandlerModel;
import com.serotonin.util.SerializationHelper;

import freemarker.template.Template;

/**
 * @author Terry Packer
 *
 */
public class EmailEventHandlerVO extends AbstractEventHandlerVO<EmailEventHandlerVO> {

    public static final int RECIPIENT_TYPE_ACTIVE = 1;
    public static final int RECIPIENT_TYPE_ESCALATION = 2;
    public static final int RECIPIENT_TYPE_INACTIVE = 3;

    public static final int SUBJECT_INCLUDE_NAME = 1;
    public static final int SUBJECT_INCLUDE_EVENT_MESSAGE = 2;
    
    public static ExportCodes RECIPIENT_TYPE_CODES = new ExportCodes();
    public static ExportCodes SUBJECT_INCLUDE_CODES = new ExportCodes();
    static {
        RECIPIENT_TYPE_CODES.addElement(RECIPIENT_TYPE_ACTIVE, "ACTIVE", "eventHandlers.recipientType.active");
        RECIPIENT_TYPE_CODES.addElement(RECIPIENT_TYPE_ESCALATION, "ESCALATION",
                "eventHandlers.recipientType.escalation");
        RECIPIENT_TYPE_CODES.addElement(RECIPIENT_TYPE_INACTIVE, "INACTIVE", "eventHandlers.recipientType.inactive");
        
        SUBJECT_INCLUDE_CODES.addElement(SUBJECT_INCLUDE_NAME, "INCLUDE_NAME", "eventHandlers.includeName");
        SUBJECT_INCLUDE_CODES.addElement(SUBJECT_INCLUDE_EVENT_MESSAGE, "INCLUDE_EVENT_MESSAGE", "eventHandlers.includeEventMessage");
        
    }
	
	private List<RecipientListEntryBean> activeRecipients;
    private boolean sendEscalation;
    private boolean repeatEscalations;
    private int escalationDelayType = TimePeriods.HOURS;
    private int escalationDelay = 1;
    private List<RecipientListEntryBean> escalationRecipients;
    private boolean sendInactive;
    private boolean inactiveOverride;
    private List<RecipientListEntryBean> inactiveRecipients;
    private boolean includeSystemInfo; //Include Work Items and Service Thread Pool Data
    private int includePointValueCount = 10;
    private boolean includeLogfile;
    private String customTemplate;
    private List<IntStringPair> additionalContext = new ArrayList<IntStringPair>();
    private ScriptPermissions scriptPermissions;
    private String script;
    private int subject = SUBJECT_INCLUDE_EVENT_MESSAGE;
    
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
    
    public boolean isRepeatEscalations() {
        return repeatEscalations;
    }
    
    public void setRepeatEscalations(boolean repeatEscalations) {
        this.repeatEscalations = repeatEscalations;
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
	
	public String getCustomTemplate() {
		return customTemplate;
	}
	
	public void setCustomTemplate(String customTemplate) {
		this.customTemplate = customTemplate;
	}
	
	public List<IntStringPair> getAdditionalContext() {
		return additionalContext;
	}
	
	public void setAdditionalContext(List<IntStringPair> additionalContext) {
		this.additionalContext = additionalContext;
	}
	
	public ScriptPermissions getScriptPermissions() {
	    return scriptPermissions;
	}
	
	public void setScriptPermissions(ScriptPermissions scriptPermissions) {
	    this.scriptPermissions = scriptPermissions;
	}
	
	public String getScript() {
	    return script;
	}
	
	public void setScript(String script) {
	    this.script = script;
	}
	
    public void setSubject(int subject) {
        this.subject = subject;
    }
    
    public int getSubject() {
        return subject;
    }

	@Override
	public void validate(ProcessResult response) {
		super.validate(response);
        if(activeRecipients != null) {
            int pos = 0;
            for(RecipientListEntryBean b : activeRecipients) {
                validateRecipient("activeRecipients[" + pos + "]", b, response);
                pos++;
            }
        }
        
        if (sendEscalation) {
            if (escalationDelay <= 0)
                response.addContextualMessage("escalationDelay", "eventHandlers.escalDelayError");
            if(!Common.TIME_PERIOD_CODES.isValidId(escalationDelayType))
                response.addContextualMessage("escalationDelayType", "validate.invalidValue");
            if(escalationRecipients != null) {
                int pos = 0;
                for(RecipientListEntryBean b : escalationRecipients) {
                    validateRecipient("escalationRecipients[" + pos + "]", b, response);
                    pos++;
                }
            }
        } else if(repeatEscalations)
            setRepeatEscalations(false);
        
        try {
            new Template("customTemplate", new StringReader(customTemplate), Common.freemarkerConfiguration);
        }catch(Exception e) {
            response.addContextualMessage("customTemplate", "common.default", e.getMessage());
        }
        
        if(additionalContext != null)
            validateScriptContext(additionalContext, response);
        else
            setAdditionalContext(new ArrayList<>());
        User user = Common.getHttpUser();
        if(user == null)
            user = Common.getBackgroundContextUser();
        if(!StringUtils.isEmpty(script)) {
            try {
                Common.getBean(MangoJavaScriptService.class).compile(script, true, scriptPermissions);
            } catch(ScriptError e) {
                response.addContextualMessage("script", "eventHandlers.invalidActiveScriptError", e.getTranslatableMessage());
            }
        }
        
        if(scriptPermissions != null) {
            Set<String> existingPermissions;
            boolean owner = false;
            if(this.id != Common.NEW_ID) {
                AbstractEventHandlerVO<?> existing = EventHandlerDao.getInstance().get(id);
                if(existing instanceof EmailEventHandlerVO) {
                    existingPermissions = ((EmailEventHandlerVO)existing).scriptPermissions != null ? ((EmailEventHandlerVO)existing).scriptPermissions.getPermissionsSet() : Collections.emptySet();
                    //If it already exists we don't want to check to make sure we have access as we may not already
                    owner = true;
                }else
                    existingPermissions = null;
            }else {
                existingPermissions = null;
            }
            Permissions.validatePermissions(response, "scriptPermissions", user, owner, existingPermissions, scriptPermissions.getPermissionsSet());
        }
        
        if(!SUBJECT_INCLUDE_CODES.isValidId(subject))
            response.addContextualMessage("subject", "validate.invalidValue");
	}
    
	//
    //
    // Serialization
    //
    private static final long serialVersionUID = -1;
    private static final int version = 7;

    private void writeObject(ObjectOutputStream out) throws IOException {
    	out.writeInt(version);
    	out.writeObject(activeRecipients);
        out.writeBoolean(sendEscalation);
        out.writeBoolean(repeatEscalations);
        out.writeInt(escalationDelayType);
        out.writeInt(escalationDelay);
        out.writeObject(escalationRecipients);
        out.writeBoolean(sendInactive);
        out.writeBoolean(inactiveOverride);
        out.writeObject(inactiveRecipients);
        out.writeBoolean(includeSystemInfo);
        out.writeInt(includePointValueCount);
        out.writeBoolean(includeLogfile);
        SerializationHelper.writeSafeUTF(out, customTemplate);
        out.writeObject(additionalContext);
        out.writeObject(scriptPermissions);
        SerializationHelper.writeSafeUTF(out, script);
        out.writeInt(subject);
    }
	
    @SuppressWarnings({"unchecked", "deprecation"})
	private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        int ver = in.readInt();
        subject = SUBJECT_INCLUDE_EVENT_MESSAGE;
        if (ver == 1) {
        	activeRecipients = (List<RecipientListEntryBean>) in.readObject();
            RecipientListEntryBean.cleanRecipientList(activeRecipients);
            sendEscalation = in.readBoolean();
            repeatEscalations = false;
            escalationDelayType = in.readInt();
            escalationDelay = in.readInt();
            escalationRecipients = (List<RecipientListEntryBean>) in.readObject();
            RecipientListEntryBean.cleanRecipientList(escalationRecipients);
            sendInactive = in.readBoolean();
            inactiveOverride = in.readBoolean();
            inactiveRecipients = (List<RecipientListEntryBean>) in.readObject();
            RecipientListEntryBean.cleanRecipientList(inactiveRecipients);
            includeSystemInfo = in.readBoolean();
            includePointValueCount = in.readInt();
            includeLogfile = in.readBoolean();
            customTemplate = null;
            additionalContext = new ArrayList<IntStringPair>();
            scriptPermissions = new ScriptPermissions();
            script = null;
        }
        else if (ver == 2) {
        	activeRecipients = (List<RecipientListEntryBean>) in.readObject();
            RecipientListEntryBean.cleanRecipientList(activeRecipients);
            sendEscalation = in.readBoolean();
            repeatEscalations = false;
            escalationDelayType = in.readInt();
            escalationDelay = in.readInt();
            escalationRecipients = (List<RecipientListEntryBean>) in.readObject();
            RecipientListEntryBean.cleanRecipientList(escalationRecipients);
            sendInactive = in.readBoolean();
            inactiveOverride = in.readBoolean();
            inactiveRecipients = (List<RecipientListEntryBean>) in.readObject();
            RecipientListEntryBean.cleanRecipientList(inactiveRecipients);
            includeSystemInfo = in.readBoolean();
            includePointValueCount = in.readInt();
            includeLogfile = in.readBoolean();
            customTemplate = SerializationHelper.readSafeUTF(in);
            additionalContext = new ArrayList<IntStringPair>();
            scriptPermissions = new ScriptPermissions();
            script = null;
        }
        else if (ver == 3) {
        	activeRecipients = (List<RecipientListEntryBean>) in.readObject();
            RecipientListEntryBean.cleanRecipientList(activeRecipients);
            sendEscalation = in.readBoolean();
            repeatEscalations = false;
            escalationDelayType = in.readInt();
            escalationDelay = in.readInt();
            escalationRecipients = (List<RecipientListEntryBean>) in.readObject();
            RecipientListEntryBean.cleanRecipientList(escalationRecipients);
            sendInactive = in.readBoolean();
            inactiveOverride = in.readBoolean();
            inactiveRecipients = (List<RecipientListEntryBean>) in.readObject();
            RecipientListEntryBean.cleanRecipientList(inactiveRecipients);
            includeSystemInfo = in.readBoolean();
            includePointValueCount = in.readInt();
            includeLogfile = in.readBoolean();
            customTemplate = SerializationHelper.readSafeUTF(in);
            additionalContext = (List<IntStringPair>) in.readObject();
            scriptPermissions = new ScriptPermissions();
            script = null;
        }
        else if (ver == 4) {
            activeRecipients = (List<RecipientListEntryBean>) in.readObject();
            RecipientListEntryBean.cleanRecipientList(activeRecipients);
            sendEscalation = in.readBoolean();
            repeatEscalations = false;
            escalationDelayType = in.readInt();
            escalationDelay = in.readInt();
            escalationRecipients = (List<RecipientListEntryBean>) in.readObject();
            RecipientListEntryBean.cleanRecipientList(escalationRecipients);
            sendInactive = in.readBoolean();
            inactiveOverride = in.readBoolean();
            inactiveRecipients = (List<RecipientListEntryBean>) in.readObject();
            RecipientListEntryBean.cleanRecipientList(inactiveRecipients);
            includeSystemInfo = in.readBoolean();
            includePointValueCount = in.readInt();
            includeLogfile = in.readBoolean();
            customTemplate = SerializationHelper.readSafeUTF(in);
            additionalContext = (List<IntStringPair>) in.readObject();
            com.serotonin.m2m2.rt.script.ScriptPermissions oldPermissions = (com.serotonin.m2m2.rt.script.ScriptPermissions) in.readObject();
            if(oldPermissions != null)
                scriptPermissions = new ScriptPermissions(oldPermissions.getPermissionsSet());
            else
                scriptPermissions = new ScriptPermissions();
            script = SerializationHelper.readSafeUTF(in);
        }
        else if (ver == 5) {
            activeRecipients = (List<RecipientListEntryBean>) in.readObject();
            RecipientListEntryBean.cleanRecipientList(activeRecipients);
            sendEscalation = in.readBoolean();
            repeatEscalations = in.readBoolean();
            escalationDelayType = in.readInt();
            escalationDelay = in.readInt();
            escalationRecipients = (List<RecipientListEntryBean>) in.readObject();
            RecipientListEntryBean.cleanRecipientList(escalationRecipients);
            sendInactive = in.readBoolean();
            inactiveOverride = in.readBoolean();
            inactiveRecipients = (List<RecipientListEntryBean>) in.readObject();
            RecipientListEntryBean.cleanRecipientList(inactiveRecipients);
            includeSystemInfo = in.readBoolean();
            includePointValueCount = in.readInt();
            includeLogfile = in.readBoolean();
            customTemplate = SerializationHelper.readSafeUTF(in);
            additionalContext = (List<IntStringPair>) in.readObject();
            com.serotonin.m2m2.rt.script.ScriptPermissions oldPermissions = (com.serotonin.m2m2.rt.script.ScriptPermissions) in.readObject();
            if(oldPermissions != null)
                scriptPermissions = new ScriptPermissions(oldPermissions.getPermissionsSet());
            else
                scriptPermissions = new ScriptPermissions();
            script = SerializationHelper.readSafeUTF(in);
        }else if (ver == 6) {
            activeRecipients = (List<RecipientListEntryBean>) in.readObject();
            RecipientListEntryBean.cleanRecipientList(activeRecipients);
            sendEscalation = in.readBoolean();
            repeatEscalations = in.readBoolean();
            escalationDelayType = in.readInt();
            escalationDelay = in.readInt();
            escalationRecipients = (List<RecipientListEntryBean>) in.readObject();
            RecipientListEntryBean.cleanRecipientList(escalationRecipients);
            sendInactive = in.readBoolean();
            inactiveOverride = in.readBoolean();
            inactiveRecipients = (List<RecipientListEntryBean>) in.readObject();
            RecipientListEntryBean.cleanRecipientList(inactiveRecipients);
            includeSystemInfo = in.readBoolean();
            includePointValueCount = in.readInt();
            includeLogfile = in.readBoolean();
            customTemplate = SerializationHelper.readSafeUTF(in);
            additionalContext = (List<IntStringPair>) in.readObject();
            scriptPermissions = (ScriptPermissions)in.readObject();
            script = SerializationHelper.readSafeUTF(in);
        }else if(ver == 7) {
            activeRecipients = (List<RecipientListEntryBean>) in.readObject();
            RecipientListEntryBean.cleanRecipientList(activeRecipients);
            sendEscalation = in.readBoolean();
            repeatEscalations = in.readBoolean();
            escalationDelayType = in.readInt();
            escalationDelay = in.readInt();
            escalationRecipients = (List<RecipientListEntryBean>) in.readObject();
            RecipientListEntryBean.cleanRecipientList(escalationRecipients);
            sendInactive = in.readBoolean();
            inactiveOverride = in.readBoolean();
            inactiveRecipients = (List<RecipientListEntryBean>) in.readObject();
            RecipientListEntryBean.cleanRecipientList(inactiveRecipients);
            includeSystemInfo = in.readBoolean();
            includePointValueCount = in.readInt();
            includeLogfile = in.readBoolean();
            customTemplate = SerializationHelper.readSafeUTF(in);
            additionalContext = (List<IntStringPair>) in.readObject();
            scriptPermissions = (ScriptPermissions)in.readObject();
            script = SerializationHelper.readSafeUTF(in);
            subject = in.readInt();
        }
    }
    
    @Override
    public void jsonWrite(ObjectWriter writer) throws IOException, JsonException {
    	super.jsonWrite(writer);
        writer.writeEntry("activeRecipients", activeRecipients);
        writer.writeEntry("sendEscalation", sendEscalation);
        if (sendEscalation) {
            writer.writeEntry("escalationDelayType", Common.TIME_PERIOD_CODES.getCode(escalationDelayType));
            writer.writeEntry("escalationDelay", escalationDelay);
            writer.writeEntry("escalationRecipients", escalationRecipients);
            writer.writeEntry("keepSendingEscalations", repeatEscalations);
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
        writer.writeEntry("customTemplate", customTemplate);
        
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
        writer.writeEntry("script", script);
        writer.writeEntry("scriptPermissions", scriptPermissions == null ? null : scriptPermissions.getPermissions());
        writer.writeEntry("subject", SUBJECT_INCLUDE_CODES.getCode(subject));
    }
    
    @SuppressWarnings("unchecked")
	@Override
    public void jsonRead(JsonReader reader, JsonObject jsonObject) throws JsonException {
    	super.jsonRead(reader, jsonObject);
    	
        String text = null;
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

            Integer i = jsonObject.getInt("escalationDelay", 1);
            if (i != null)
                escalationDelay = i;

            JsonArray jsonEscalationRecipients = jsonObject.getJsonArray("escalationRecipients");
            if (jsonEscalationRecipients != null)
                escalationRecipients = (List<RecipientListEntryBean>) reader.read(recipType,
                        jsonEscalationRecipients);
            
            b = jsonObject.getJsonBoolean("keepSendingEscalations");
            if(b != null)
                repeatEscalations = b.booleanValue();
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
        if(b != null)
        	includeSystemInfo = b.booleanValue();
        
        includePointValueCount = jsonObject.getInt("includePointValueCount", 0);
        
        b = jsonObject.getJsonBoolean("includeLogfile");
        if(b != null)
        	includeSystemInfo = b.booleanValue();
        
        customTemplate = jsonObject.getString("customTemplate");
        
        JsonArray context = jsonObject.getJsonArray("additionalContext");
        if(context != null) {
        	List<IntStringPair> additionalContext = new ArrayList<>();
        	for(JsonValue jv : context) {
        		JsonObject jo = jv.toJsonObject();
        		String dataPointXid = jo.getString("dataPointXid");
        		if(dataPointXid == null)
        			throw new TranslatableJsonException("emport.error.context.missing", "dataPointXid");
        		
        		DataPointVO dpvo = DataPointDao.getInstance().getByXid(dataPointXid);
        		if(dpvo == null)
        			throw new TranslatableJsonException("emport.error.missingPoint", dataPointXid);
        		
        		String contextKey = jo.getString("contextKey");
        		if(contextKey == null)
        			throw new TranslatableJsonException("emport.error.context.missing", "contextKey");
        		
        		additionalContext.add(new IntStringPair(dpvo.getId(), contextKey));
        	}
        	this.additionalContext = additionalContext;
        } else
        	this.additionalContext = new ArrayList<>();
        
        script = jsonObject.getString("script");
        if(jsonObject.containsKey("scriptPermissions")) {
            Set<String> permissions = null;
            try{
                JsonObject o = jsonObject.getJsonObject("scriptPermissions");
                permissions = new HashSet<>();
                permissions.addAll(Permissions.explodePermissionGroups(o.getString("dataSourcePermissions")));
                permissions.addAll(Permissions.explodePermissionGroups(o.getString("dataPointSetPermissions")));
                permissions.addAll(Permissions.explodePermissionGroups(o.getString("dataPointReadPermissions")));
                permissions.addAll(Permissions.explodePermissionGroups(o.getString("customPermissions")));
                this.scriptPermissions = new ScriptPermissions(permissions);
            }catch(ClassCastException e) {
               //Munchy munch, not a legacy script permissions object 
            }
            if(permissions == null) {
                this.scriptPermissions = new ScriptPermissions(Permissions.explodePermissionGroups(jsonObject.getString("scriptPermissions")));
            }
        }
        text = jsonObject.getString("subject");
        if (text != null) {
            subject = SUBJECT_INCLUDE_CODES.getId(text);
            if (subject == -1)
                throw new TranslatableJsonException("emport.error.invalid", "subject", text,
                        SUBJECT_INCLUDE_CODES.getCodeList());
        }else {
            //For legacy compatibility
            subject = StringUtils.isEmpty(name) ? SUBJECT_INCLUDE_EVENT_MESSAGE : SUBJECT_INCLUDE_NAME;
        }
    }
    
    @Override
    public EventHandlerRT<EmailEventHandlerVO> createRuntime(){
    	return new EmailHandlerRT(this);
    }

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.vo.event.AbstractEventHandlerVO#asModel()
	 */
	@Override
	public EmailEventHandlerModel asModel() {
		return new EmailEventHandlerModel(this);
	}
}
