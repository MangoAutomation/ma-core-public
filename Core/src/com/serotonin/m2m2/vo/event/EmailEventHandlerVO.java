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

import com.serotonin.json.JsonException;
import com.serotonin.json.JsonReader;
import com.serotonin.json.ObjectWriter;
import com.serotonin.json.type.JsonArray;
import com.serotonin.json.type.JsonObject;
import com.serotonin.json.util.TypeDefinition;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.dao.MailingListDao;
import com.serotonin.m2m2.db.dao.UserDao;
import com.serotonin.m2m2.i18n.ProcessResult;
import com.serotonin.m2m2.i18n.TranslatableJsonException;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.rt.event.handlers.EmailHandlerRT;
import com.serotonin.m2m2.rt.event.handlers.EventHandlerRT;
import com.serotonin.m2m2.rt.event.type.AuditEventType;
import com.serotonin.m2m2.util.ExportCodes;
import com.serotonin.m2m2.vo.mailingList.EmailRecipient;
import com.serotonin.m2m2.web.dwr.beans.RecipientListEntryBean;

/**
 * @author Terry Packer
 *
 */
public class EmailEventHandlerVO extends AbstractEventHandlerVO{

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

	@Override
	public void validate(ProcessResult response) {
		super.validate(response);
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
    
	@Override
    public void addProperties(List<TranslatableMessage> list) {
		super.addProperties(list);
		AuditEventType.addPropertyMessage(list, "eventHandlers.emailRecipients",
                createRecipientMessage(activeRecipients));
        AuditEventType.addPropertyMessage(list, "eventHandlers.escal", sendEscalation);
        if (sendEscalation) {
            AuditEventType
                    .addPeriodMessage(list, "eventHandlers.escalPeriod", escalationDelayType, escalationDelay);
            AuditEventType.addPropertyMessage(list, "eventHandlers.escalRecipients",
                    createRecipientMessage(escalationRecipients));
        }
        AuditEventType.addPropertyMessage(list, "eventHandlers.inactiveNotif", sendInactive);
        if (sendInactive) {
            AuditEventType.addPropertyMessage(list, "eventHandlers.inactiveOverride", inactiveOverride);
            if (inactiveOverride)
                AuditEventType.addPropertyMessage(list, "eventHandlers.inactiveRecipients",
                        createRecipientMessage(inactiveRecipients));
        }
        AuditEventType.addPropertyMessage(list, "eventHandlers.includeSystemInfo", includeSystemInfo);
        AuditEventType.addPropertyMessage(list, "eventHandlers.includePointValueCount", includePointValueCount);
        AuditEventType.addPropertyMessage(list, "eventHandlers.includeLogfile", includeLogfile);
	}
	
    @Override
    public void addPropertyChanges(List<TranslatableMessage> list, AbstractEventHandlerVO vo) {
    	super.addPropertyChanges(list, vo);
        
    	EmailEventHandlerVO from = (EmailEventHandlerVO)vo;
    	
    	AuditEventType.maybeAddPropertyChangeMessage(list, "eventHandlers.emailRecipients",
                createRecipientMessage(from.activeRecipients), createRecipientMessage(activeRecipients));
        AuditEventType.maybeAddPropertyChangeMessage(list, "eventHandlers.escal", from.sendEscalation,
                sendEscalation);
        AuditEventType.maybeAddPeriodChangeMessage(list, "eventHandlers.escalPeriod", from.escalationDelayType,
                from.escalationDelay, escalationDelayType, escalationDelay);
        AuditEventType.maybeAddPropertyChangeMessage(list, "eventHandlers.escalRecipients",
                createRecipientMessage(from.escalationRecipients), createRecipientMessage(escalationRecipients));
        AuditEventType.maybeAddPropertyChangeMessage(list, "eventHandlers.inactiveNotif", from.sendInactive,
                sendInactive);
        AuditEventType.maybeAddPropertyChangeMessage(list, "eventHandlers.inactiveOverride", from.inactiveOverride,
                inactiveOverride);
        AuditEventType.maybeAddPropertyChangeMessage(list, "eventHandlers.inactiveRecipients",
                createRecipientMessage(from.inactiveRecipients), createRecipientMessage(inactiveRecipients));
        AuditEventType.maybeAddPropertyChangeMessage(list, "eventHandlers.includeSystemInfo", from.includeSystemInfo, includeSystemInfo);
        AuditEventType.maybeAddPropertyChangeMessage(list, "eventHandlers.includePointValueCount", from.includePointValueCount, includePointValueCount);
        AuditEventType.maybeAddPropertyChangeMessage(list, "eventHandlers.includeLogfile", from.includeLogfile, includeLogfile);

    }
	
	
	//
    //
    // Serialization
    //
    private static final long serialVersionUID = -1;
    private static final int version = 1;

    private void writeObject(ObjectOutputStream out) throws IOException {
    	out.writeInt(version);
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
	
    @SuppressWarnings("unchecked")
	private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        int ver = in.readInt();
        if (ver == 1) {
        	activeRecipients = (List<RecipientListEntryBean>) in.readObject();
            RecipientListEntryBean.cleanRecipientList(activeRecipients);
            sendEscalation = in.readBoolean();
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
    
    @SuppressWarnings("unchecked")
	@Override
    public void jsonRead(JsonReader reader, JsonObject jsonObject) throws JsonException {
    	super.jsonRead(reader, jsonObject);
    	
        String text = null;
    	TypeDefinition recipType = new TypeDefinition(List.class, RecipientListEntryBean.class);
        JsonArray jsonActiveRecipients = jsonObject.getJsonArray("activeRecipients");
        if (jsonActiveRecipients != null)
            activeRecipients = (List<RecipientListEntryBean>) reader.read(recipType, jsonActiveRecipients);

        Boolean b = jsonObject.getBoolean("sendEscalation");
        if (b != null)
            sendEscalation = b;

        if (sendEscalation) {
            text = jsonObject.getString("escalationDelayType");
            if (text != null) {
                escalationDelayType = Common.TIME_PERIOD_CODES.getId(text);
                if (escalationDelayType == -1)
                    throw new TranslatableJsonException("emport.error.invalid", "escalationDelayType", text,
                            Common.TIME_PERIOD_CODES.getCodeList());
            }

            Integer i = jsonObject.getInt("escalationDelay");
            if (i != null)
                escalationDelay = i;

            JsonArray jsonEscalationRecipients = jsonObject.getJsonArray("escalationRecipients");
            if (jsonEscalationRecipients != null)
                escalationRecipients = (List<RecipientListEntryBean>) reader.read(recipType,
                        jsonEscalationRecipients);
        }

        b = jsonObject.getBoolean("sendInactive");
        if (b != null)
            sendInactive = b;

        if (sendInactive) {
            b = jsonObject.getBoolean("inactiveOverride");
            if (b != null)
                inactiveOverride = b;

            if (inactiveOverride) {
                JsonArray jsonInactiveRecipients = jsonObject.getJsonArray("inactiveRecipients");
                if (jsonInactiveRecipients != null)
                    inactiveRecipients = (List<RecipientListEntryBean>) reader.read(recipType,
                            jsonInactiveRecipients);
            }
        }
        b = jsonObject.getBoolean("includeSystemInformation");
        if(b != null){
        	includeSystemInfo = b;
        }
        
        includePointValueCount = jsonObject.getInt("includePointValueCount", 0);
        
        b = jsonObject.getBoolean("includeLogfile");
        if(b != null){
        	includeSystemInfo = b;
        }
    }
    
    @Override
    public EventHandlerRT<EmailEventHandlerVO> createRuntime(){
    	return new EmailHandlerRT(this);
    }
    
    private static TranslatableMessage createRecipientMessage(List<RecipientListEntryBean> recipients) {
        MailingListDao mailingListDao = new MailingListDao();
        UserDao userDao = new UserDao();
        ArrayList<TranslatableMessage> params = new ArrayList<TranslatableMessage>();
        for (RecipientListEntryBean recip : recipients) {
            TranslatableMessage msg;
            if (recip.getRecipientType() == EmailRecipient.TYPE_MAILING_LIST)
                msg = new TranslatableMessage("event.audit.recip.mailingList", mailingListDao.getMailingList(
                        recip.getReferenceId()).getName());
            else if (recip.getRecipientType() == EmailRecipient.TYPE_USER)
                msg = new TranslatableMessage("event.audit.recip.user", userDao.getUser(recip.getReferenceId())
                        .getUsername());
            else
                msg = new TranslatableMessage("event.audit.recip.address", recip.getReferenceAddress());
            params.add(msg);
        }

        return new TranslatableMessage("event.audit.recip.list." + params.size(), params.toArray());
    }
}
