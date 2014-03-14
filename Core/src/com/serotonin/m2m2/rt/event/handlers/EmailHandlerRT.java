/*
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.rt.event.handlers;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.joda.time.DateTime;

import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.dao.MailingListDao;
import com.serotonin.m2m2.db.dao.SystemSettingsDao;
import com.serotonin.m2m2.email.MangoEmailContent;
import com.serotonin.m2m2.email.UsedImagesDirective;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.i18n.Translations;
import com.serotonin.m2m2.rt.event.AlarmLevels;
import com.serotonin.m2m2.rt.event.EventInstance;
import com.serotonin.m2m2.rt.event.type.SystemEventType;
import com.serotonin.m2m2.rt.maint.work.EmailWorkItem;
import com.serotonin.m2m2.util.timeout.ModelTimeoutClient;
import com.serotonin.m2m2.util.timeout.ModelTimeoutTask;
import com.serotonin.m2m2.vo.event.EventHandlerVO;
import com.serotonin.timer.TimerTask;
import com.serotonin.web.mail.EmailInline;

public class EmailHandlerRT extends EventHandlerRT implements ModelTimeoutClient<EventInstance> {
    private static final Log LOG = LogFactory.getLog(EmailHandlerRT.class);

    private TimerTask escalationTask;

    private Set<String> activeRecipients;

    private enum NotificationType {
        ACTIVE("active", "ftl.subject.active"), //
        ESCALATION("escalation", "ftl.subject.escalation"), //
        INACTIVE("inactive", "ftl.subject.inactive");

        String file;
        String key;

        private NotificationType(String file, String key) {
            this.file = file;
            this.key = key;
        }

        public String getFile() {
            return file;
        }

        public String getKey() {
            return key;
        }
    }

    /**
     * The list of all of the recipients - active and escalation - for sending upon inactive if configured to do so.
     */
    private Set<String> inactiveRecipients;

    public EmailHandlerRT(EventHandlerVO vo) {
        this.vo = vo;
    }

    public Set<String> getActiveRecipients() {
        return activeRecipients;
    }

    @Override
    public void eventRaised(EventInstance evt) {
        // Get the email addresses to send to
        activeRecipients = new MailingListDao().getRecipientAddresses(vo.getActiveRecipients(),
                new DateTime(evt.getActiveTimestamp()));

        // Send an email to the active recipients.
        sendEmail(evt, NotificationType.ACTIVE, activeRecipients);

        // If an inactive notification is to be sent, save the active recipients.
        if (vo.isSendInactive()) {
            if (vo.isInactiveOverride())
                inactiveRecipients = new MailingListDao().getRecipientAddresses(vo.getInactiveRecipients(),
                        new DateTime(evt.getActiveTimestamp()));
            else
                inactiveRecipients = activeRecipients;
        }

        // If an escalation is to be sent, set up timeout to trigger it.
        if (vo.isSendEscalation()) {
            long delayMS = Common.getMillis(vo.getEscalationDelayType(), vo.getEscalationDelay());
            escalationTask = new ModelTimeoutTask<EventInstance>(delayMS, this, evt);
        }
    }

    //
    // TimeoutClient
    //
    synchronized public void scheduleTimeout(EventInstance evt, long fireTime) {
        // Get the email addresses to send to
        Set<String> addresses = new MailingListDao().getRecipientAddresses(vo.getEscalationRecipients(), new DateTime(
                fireTime));

        // Send the escalation.
        sendEmail(evt, NotificationType.ESCALATION, addresses);

        // If an inactive notification is to be sent, save the escalation recipients, but only if inactive recipients
        // have not been overridden.
        if (vo.isSendInactive() && !vo.isInactiveOverride())
            inactiveRecipients.addAll(addresses);
    }

    @Override
    synchronized public void eventInactive(EventInstance evt) {
        // Cancel the escalation job in case it's there
        if (escalationTask != null)
            escalationTask.cancel();

        if (inactiveRecipients != null && inactiveRecipients.size() > 0)
            // Send an email to the inactive recipients.
            sendEmail(evt, NotificationType.INACTIVE, inactiveRecipients);
    }

    public static void sendActiveEmail(EventInstance evt, Set<String> addresses) {
        sendEmail(evt, NotificationType.ACTIVE, addresses, null);
    }

    private void sendEmail(EventInstance evt, NotificationType notificationType, Set<String> addresses) {
        sendEmail(evt, notificationType, addresses, vo.getAlias());
    }

    private static void sendEmail(EventInstance evt, NotificationType notificationType, Set<String> addresses,
            String alias) {
        if (evt.getEventType().isSystemMessage()) {
            if (((SystemEventType) evt.getEventType()).getSystemEventType().equals(
                    SystemEventType.TYPE_EMAIL_SEND_FAILURE)) {
                // Don't send email notifications about email send failures.
                LOG.info("Not sending email for event raised due to email failure");
                return;
            }
        }

       Translations translations = Common.getTranslations();
       if(StringUtils.isBlank(alias)){
    	   //Just set the subject to the message
    	   alias = evt.getMessage().translate(translations);
    	   
    	   //Strip out the HTML and the &nbsp
    	   alias = StringEscapeUtils.unescapeHtml4(alias);
           //Since we have <br/> in the code and that isn't proper HTML we need to remove it by hand
           alias = alias.replace("<br/>", "\n");
    	   
//	       if(evt.getEventType() instanceof DataPointEventType){
//	    	   DataPointEventType type = (DataPointEventType) evt.getEventType();
//	    	   PointEventDetectorVO vo = DataPointDao.instance.getEventDetector(type.getPointEventDetectorId());
//	    	   //With this DetectorVO we can get all the nice info on this event
//	    	   if(vo != null)
//	    		   alias = vo.getDescription().translate(translations);
//	    	   else
//	    		   alias = Common.translate("eventHandlers.pointEventDetector");
//	       }else if(evt.getEventType() instanceof DataSourceEventType){
//	    	   DataSourceEventType type = (DataSourceEventType) evt.getEventType();
//	    	   DataSourceVO<?> vo = Common.runtimeManager.getDataSource(type.getDataSourceId());
//	    	   EventTypeVO evtVO = vo.getEventType(type.getDataSourceEventTypeId());
//	    	   if(evtVO!=null)
//	    		   alias = evtVO.getDescription().translate(translations);
//	    	   else
//	    		   alias = Common.translate("eventHandlers.dataSourceEvents");
//	       }else if(evt.getEventType() instanceof SystemEventType){
//	    	   SystemEventType type = (SystemEventType) evt.getEventType();
//	    	   alias = Common.translate("eventHandlers.systemEvents");
//	       }else if(evt.getEventType() instanceof PublisherEventType){
//	    	   PublisherEventType type = (PublisherEventType)evt.getEventType();
//	    	   alias = Common.translate("eventHandlers.publisherEvents");
//	    	   
//	       }else if(evt.getEventType() instanceof AuditEventType){
//	    	   AuditEventType type = (AuditEventType)evt.getEventType();	    	   
//	    	   if(type.getAuditEventType().equals(AuditEventType.TYPE_DATA_SOURCE)){
//	    		   DataSourceVO<?> vo = Common.runtimeManager.getDataSource(type.getReferenceId1());
//	    		   if(vo != null)
//	    			   alias = vo.getName();
//	    	   }else if(type.getAuditEventType().equals(AuditEventType.TYPE_DATA_POINT)){
//	    		   DataPointRT rt = Common.runtimeManager.getDataPoint(type.getReferenceId1());
//	    		   if(rt != null){
//	    			   alias = rt.getVO().getName();
//	    		   }
//	    	   }else if(type.getAuditEventType().equals(AuditEventType.TYPE_EVENT_HANDLER)){
//	    		   alias = Common.translate("eventHandlers.auditEvents");
//	    	   }else if(type.getAuditEventType().equals(AuditEventType.TYPE_POINT_EVENT_DETECTOR)){
//	    		   alias = Common.translate("eventHandlers.auditEvents");
//	    	   }
//	    	   else
//	    		   alias = Common.translate("eventHandlers.auditEvents");
//	       }
       }//end if alias was blank
       
        // Determine the subject to use.
        TranslatableMessage subjectMsg;
        TranslatableMessage notifTypeMsg = new TranslatableMessage(notificationType.getKey());
        if (StringUtils.isBlank(alias)) {
        	//Make these more descriptive
            if (evt.getId() == Common.NEW_ID)
                subjectMsg = new TranslatableMessage("ftl.subject.default", notifTypeMsg);
            else
                subjectMsg = new TranslatableMessage("ftl.subject.default.id", notifTypeMsg, evt.getId());
        }
        else {
            if (evt.getId() == Common.NEW_ID)
                subjectMsg = new TranslatableMessage("ftl.subject.alias", alias, notifTypeMsg);
            else
                subjectMsg = new TranslatableMessage("ftl.subject.alias.id", alias, notifTypeMsg, evt.getId());
        }
        String alarmLevel = AlarmLevels.getAlarmLevelMessage(evt.getAlarmLevel()).translate(translations);

        String subject = alarmLevel + " - " + subjectMsg.translate(translations);

        //Trim the subject if its too long
        if(subject.length() > 200)
        	subject = subject.substring(0,200);
        
        try {
            String[] toAddrs = addresses.toArray(new String[0]);
            UsedImagesDirective inlineImages = new UsedImagesDirective();

            // Send the email.
            Map<String, Object> model = new HashMap<String, Object>();
            model.put("evt", evt);
            if (evt.getContext() != null)
                model.putAll(evt.getContext());
            model.put("img", inlineImages);
            model.put("instanceDescription", SystemSettingsDao.getValue(SystemSettingsDao.INSTANCE_DESCRIPTION));

            MangoEmailContent content = new MangoEmailContent(notificationType.getFile(), model, translations, subject,
                    Common.UTF8);

            for (String s : inlineImages.getImageList())
                content.addInline(new EmailInline.FileInline(s, Common.getWebPath(s)));

            EmailWorkItem.queueEmail(toAddrs, content);
        }
        catch (Exception e) {
            LOG.error("", e);
        }
    }
}
