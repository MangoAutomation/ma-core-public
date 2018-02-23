/*
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.rt.event.handlers;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.script.CompiledScript;
import javax.script.ScriptException;

import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.joda.time.DateTime;

import com.infiniteautomation.mango.util.ConfigurationExportData;
import com.serotonin.db.pair.IntStringPair;
import com.serotonin.io.StreamUtils;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.DataTypes;
import com.serotonin.m2m2.db.dao.DataPointDao;
import com.serotonin.m2m2.db.dao.DataSourceDao;
import com.serotonin.m2m2.db.dao.EventDao;
import com.serotonin.m2m2.db.dao.MailingListDao;
import com.serotonin.m2m2.db.dao.SystemSettingsDao;
import com.serotonin.m2m2.email.MangoEmailContent;
import com.serotonin.m2m2.email.PostEmailRunnable;
import com.serotonin.m2m2.email.UsedImagesDirective;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.i18n.Translations;
import com.serotonin.m2m2.rt.dataImage.DataPointRT;
import com.serotonin.m2m2.rt.dataImage.IDataPointValueSource;
import com.serotonin.m2m2.rt.dataImage.PointValueTime;
import com.serotonin.m2m2.rt.dataImage.SetPointSource;
import com.serotonin.m2m2.rt.dataImage.types.DataValue;
import com.serotonin.m2m2.rt.dataSource.DataSourceRT;
import com.serotonin.m2m2.rt.event.AlarmLevels;
import com.serotonin.m2m2.rt.event.EventInstance;
import com.serotonin.m2m2.rt.event.type.DataPointEventType;
import com.serotonin.m2m2.rt.event.type.SystemEventType;
import com.serotonin.m2m2.rt.maint.work.EmailWorkItem;
import com.serotonin.m2m2.rt.script.CompiledScriptExecutor;
import com.serotonin.m2m2.rt.script.JsonImportExclusion;
import com.serotonin.m2m2.rt.script.OneTimePointAnnotation;
import com.serotonin.m2m2.rt.script.ResultTypeException;
import com.serotonin.m2m2.rt.script.ScriptLog;
import com.serotonin.m2m2.rt.script.ScriptLog.LogLevel;
import com.serotonin.m2m2.rt.script.ScriptPermissions;
import com.serotonin.m2m2.rt.script.ScriptPermissionsException;
import com.serotonin.m2m2.rt.script.ScriptPointValueSetter;
import com.serotonin.m2m2.rt.script.ScriptUtils;
import com.serotonin.m2m2.util.timeout.ModelTimeoutClient;
import com.serotonin.m2m2.util.timeout.ModelTimeoutTask;
import com.serotonin.m2m2.vo.DataPointVO;
import com.serotonin.m2m2.vo.dataSource.DataSourceVO;
import com.serotonin.m2m2.vo.event.EmailEventHandlerVO;
import com.serotonin.m2m2.web.dwr.beans.RenderedPointValueTime;
import com.serotonin.m2m2.web.mvc.rest.v1.model.workitem.WorkItemModel;
import com.serotonin.m2m2.web.taglib.Functions;
import com.serotonin.timer.TimerTask;
import com.serotonin.web.mail.EmailAttachment;
import com.serotonin.web.mail.EmailContent;
import com.serotonin.web.mail.EmailInline;

public class EmailHandlerRT extends EventHandlerRT<EmailEventHandlerVO> implements ModelTimeoutClient<EventInstance>, SetPointSource {
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

    public EmailHandlerRT(EmailEventHandlerVO vo) {
        super(vo);
    }

    public Set<String> getActiveRecipients() {
        return activeRecipients;
    }

    @Override
    public void eventRaised(EventInstance evt) {
        // Get the email addresses to send to
        activeRecipients = MailingListDao.instance.getRecipientAddresses(vo.getActiveRecipients(),
                new DateTime(evt.getActiveTimestamp()));

        // Send an email to the active recipients.
        sendEmail(evt, NotificationType.ACTIVE, activeRecipients);

        // If an inactive notification is to be sent, save the active recipients.
        if (vo.isSendInactive()) {
            if (vo.isInactiveOverride())
                inactiveRecipients = MailingListDao.instance.getRecipientAddresses(vo.getInactiveRecipients(),
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
        Set<String> addresses = MailingListDao.instance.getRecipientAddresses(vo.getEscalationRecipients(), new DateTime(
                fireTime));

        // Send the escalation.
        sendEmail(evt, NotificationType.ESCALATION, addresses);

        // If an inactive notification is to be sent, save the escalation recipients, but only if inactive recipients
        // have not been overridden.
        if (vo.isSendInactive() && !vo.isInactiveOverride())
            inactiveRecipients.addAll(addresses);
        
        if (vo.isRepeatEscalations()) {
            //While evt will probably show ack'ed if ack'ed, the possibility exists for it to be deleted
            // and in which case we want to notice rather than send emails forever.
            EventInstance dbEvent = EventDao.instance.get(evt.getId());
            if(dbEvent != null && !dbEvent.isAcknowledged() && dbEvent.isActive()) {
                long delayMS = Common.getMillis(vo.getEscalationDelayType(), vo.getEscalationDelay());
                escalationTask = new ModelTimeoutTask<EventInstance>(delayMS, this, dbEvent);
            }
        }
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
        sendEmail(evt, NotificationType.ACTIVE, addresses, null, false, 0, false, null, null, null, null, null, null);
    }

    private void sendEmail(EventInstance evt, NotificationType notificationType, Set<String> addresses) {
        sendEmail(evt, notificationType, addresses, vo.getAlias(), vo.isIncludeSystemInfo(), vo.getIncludePointValueCount(), 
        		vo.isIncludeLogfile(), vo.getXid(), vo.getCustomTemplate(), vo.getAdditionalContext(), vo.getScript(), 
        		new SetCallback(vo.getScriptPermissions()), vo.getScriptPermissions());
    }

    private static void sendEmail(EventInstance evt, NotificationType notificationType, Set<String> addresses,
            String alias, boolean includeSystemInfo, int pointValueCount, boolean includeLogs, String handlerXid,
            String customTemplate, List<IntStringPair> additionalContext, String script, SetCallback setCallback, ScriptPermissions permissions) {
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
            if(includeSystemInfo){
            	//Get the Work Items
            	List<WorkItemModel> highPriorityWorkItems = Common.backgroundProcessing.getHighPriorityServiceItems();
            	model.put("highPriorityWorkItems", highPriorityWorkItems);
            	List<WorkItemModel> mediumPriorityWorkItems = Common.backgroundProcessing.getMediumPriorityServiceQueueItems();
            	model.put("mediumPriorityWorkItems", mediumPriorityWorkItems);
            	List<WorkItemModel> lowPriorityWorkItems = Common.backgroundProcessing.getLowPriorityServiceQueueItems();
            	model.put("lowPriorityWorkItems", lowPriorityWorkItems);
            	model.put("threadList", getThreadsList());
            }

            int type = SystemSettingsDao.getIntValue(SystemSettingsDao.EMAIL_CONTENT_TYPE);
            
            //If we are a point event then add the value
            if(evt.getEventType() instanceof DataPointEventType){
            	DataPointVO dp = (DataPointVO)evt.getContext().get("point");
            	if(dp != null){
            		DataPointRT rt = Common.runtimeManager.getDataPoint(dp.getId());
            		if(rt != null){
            			List<PointValueTime> pointValues = null;
            			if( pointValueCount > 0)
            				pointValues = rt.getLatestPointValues(pointValueCount);
            			if((pointValues != null)&&(pointValues.size() > 0)){
            				
            				if (type == MangoEmailContent.CONTENT_TYPE_HTML || type == MangoEmailContent.CONTENT_TYPE_BOTH){
            					List<RenderedPointValueTime> renderedPointValues = new ArrayList<RenderedPointValueTime>();
	            				for(PointValueTime pvt : pointValues){
	            					RenderedPointValueTime rpvt = new RenderedPointValueTime();
	            					
	            					rpvt.setValue(Functions.getHtmlText(rt.getVO(), pvt));
	                                rpvt.setTime(Functions.getFullSecondTime(pvt.getTime()));
	                                renderedPointValues.add(rpvt);
	            				}
	            				model.put("renderedHtmlPointValues", renderedPointValues);
            				}
            				
            				if (type == MangoEmailContent.CONTENT_TYPE_TEXT || type == MangoEmailContent.CONTENT_TYPE_BOTH){
            					List<RenderedPointValueTime> renderedPointValues = new ArrayList<RenderedPointValueTime>();
	            				for(PointValueTime pvt : pointValues){
	            					RenderedPointValueTime rpvt = new RenderedPointValueTime();
	            					rpvt.setValue(Functions.getRenderedText(rt.getVO(), pvt));
	                                rpvt.setTime(Functions.getFullSecondTime(pvt.getTime()));
	                                renderedPointValues.add(rpvt);
	            				}
            				model.put("renderedPointValues", renderedPointValues);
            				}
            			}
            		}
            	}
            }
            
            //Build the additional context for the email model
            if(additionalContext == null || pointValueCount <= 0)
            	model.put("additionalContext", new HashMap<>(0));
            else {
            	Map<String, Map<String, Object>> context = new HashMap<>();
            	for(IntStringPair pair : additionalContext) {
            		Map<String, Object> point = new HashMap<String, Object>();
            		DataPointRT rt = Common.runtimeManager.getDataPoint(pair.getKey());
            		List<PointValueTime> pointValues;
            		List<RenderedPointValueTime> renderedPointValues;
            		DataPointVO dpvo;
            		if(rt != null) {
            			dpvo = rt.getVO();
            			pointValues = rt.getLatestPointValues(pointValueCount);
            			renderedPointValues = new ArrayList<RenderedPointValueTime>();
            			if(pointValues != null && pointValues.size() > 0)
            				for(PointValueTime pvt : pointValues) {
            					RenderedPointValueTime rpvt = new RenderedPointValueTime();
            					rpvt.setValue(Functions.getRenderedText(rt.getVO(), pvt));
                                rpvt.setTime(Functions.getFullSecondTime(pvt.getTime()));
                                renderedPointValues.add(rpvt);
            				}
            		} else {
            			dpvo = DataPointDao.instance.get(pair.getKey());
            			if(dpvo == null)
            				continue;
            			
            			pointValues = Common.databaseProxy.newPointValueDao()
            					.getLatestPointValues(pair.getKey(), pointValueCount);
            			renderedPointValues = new ArrayList<RenderedPointValueTime>();
        				for(PointValueTime pvt : pointValues) {
        					RenderedPointValueTime rpvt = new RenderedPointValueTime();
        					rpvt.setValue(Functions.getRenderedText(dpvo, pvt));
                            rpvt.setTime(Functions.getFullSecondTime(pvt.getTime()));
                            renderedPointValues.add(rpvt);
        				}
            		}
            		point.put("values", renderedPointValues);
    				point.put("deviceName", dpvo.getDeviceName());
    				point.put("name", dpvo.getName());
    				point.put("contextKey", pair.getValue());
    				context.put(pair.getValue(), point);
            	}
            	model.put("additionalContext", context);
            }
            
            if(!StringUtils.isEmpty(script)) {
                //Okay, a script is defined, let's pass it the model so that it may add to it
                Map<String, Object> modelContext = new HashMap<String, Object>();
                modelContext.put("model", model);
                
                Map<String, IDataPointValueSource> context = new HashMap<String, IDataPointValueSource>();
                for(IntStringPair pair : additionalContext) {
                    DataPointRT dprt = Common.runtimeManager.getDataPoint(pair.getKey());
                    if(dprt == null) {
                        DataPointVO targetVo = DataPointDao.instance.getDataPoint(pair.getKey(), false);
                        if(targetVo == null) {
                            LOG.warn("Additional context point with ID: " + pair.getKey() + " and context name " + pair.getValue() + " could not be found.");
                            continue; //Not worth aborting the email, just warn it
                        }
                        
                        if(targetVo.getDefaultCacheSize() == 0)
                            targetVo.setDefaultCacheSize(1);
                        dprt = new DataPointRT(targetVo, targetVo.getPointLocator().createRuntime(), DataSourceDao.instance.getDataSource(targetVo.getDataSourceId()), null);
                        dprt.resetValues();
                    }
                    context.put(pair.getValue(), dprt);
                }
                
                List<JsonImportExclusion> importExclusions = new ArrayList<JsonImportExclusion>(1);
                importExclusions.add(new JsonImportExclusion("xid", handlerXid) {
                    @Override
                    public String getImporterType() {
                        return ConfigurationExportData.EVENT_HANDLERS;
                    }
                });
                
                try {
                    CompiledScript compiledScript = CompiledScriptExecutor.compile(script);
                    CompiledScriptExecutor.execute(compiledScript, context, modelContext, Common.timer.currentTimeMillis(), 
                            DataTypes.ALPHANUMERIC, evt.isActive() || !evt.isRtnApplicable() ? evt.getActiveTimestamp() : evt.getRtnTimestamp(), 
                            permissions, SetPointHandlerRT.NULL_WRITER, new ScriptLog(SetPointHandlerRT.NULL_WRITER,
                            LogLevel.FATAL), setCallback, importExclusions, false);
                } catch(ScriptPermissionsException|ScriptException|ResultTypeException e) {
                    LOG.error("Exception running email handler script: " + e.getMessage(), e);
                }
            }
            
            MangoEmailContent content;
            if(StringUtils.isEmpty(customTemplate))
            	content = new MangoEmailContent(notificationType.getFile(), model, translations, subject, Common.UTF8);
            else
            	content = new MangoEmailContent(handlerXid, customTemplate, model, translations, subject);
            
            PostEmailRunnable[] postEmail = null;
            if(includeLogs){
    	        final File logZip = getZippedLogfile(content, new File(Common.getLogsDir(), "ma.log"));
    	        //Setup To delete the temp files from zip
    	        if (logZip != null) {
    	            // See that the temp file(s) gets deleted after the email is sent.
    	        	PostEmailRunnable deleteTempFile = new PostEmailRunnable() {
    	                @Override
    	                public void run() {
    	                    if (!logZip.delete())
    	                        LOG.warn("Temp file " + logZip.getPath() + " not deleted");
    	                    
    	                    //Set our state to email failed if necessary
    	                    //TODO Create an Event to notify of Failed Emails...
    	                    //if(!this.isSuccess()){}
    	                }
    	            };
    	            postEmail = new PostEmailRunnable[] { deleteTempFile };
    	        }
            }
            
            for (String s : inlineImages.getImageList())
                content.addInline(new EmailInline.FileInline(s, Common.getWebPath(s)));

            EmailWorkItem.queueEmail(toAddrs, content, postEmail);
        }
        catch (Exception e) {
            LOG.error("", e);
        }
    }
    
    private static List<Map<String,Object>> getThreadsList(){
    	List<Map<String,Object>> models = new ArrayList<Map<String,Object>>();
    	List<ThreadInfo> infos = Common.backgroundProcessing.getThreadsList(10);
    	ThreadMXBean tmxb = ManagementFactory.getThreadMXBean();
    	
    	for(ThreadInfo info : infos){
    		Map<String,Object> model = new HashMap<String,Object>();
    		model.put("threadName", info.getThreadName());
    		model.put("threadState", info.getThreadState().name());
    		model.put("cpuTime", tmxb.getThreadCpuTime(info.getThreadId()));
    		if(info.getBlockedTime() < 0)
    			model.put("blockedTime", "n/a");
    		else
    			model.put("blockedTime", info.getBlockedTime());
    		if(info.getWaitedTime() < 0)
    			model.put("waitTime","n/a");
    		else
    			model.put("waitTime",info.getWaitedTime());
    		String name = info.getLockName();
    		if(name != null)
    			model.put("lockName", name);
    		else
    			model.put("lockName", "none");
    		name = info.getLockOwnerName();
    		if(name != null)
    			model.put("lockOwnerName", name);
    		else
    			model.put("lockOwnerName", "none");
    		model.put("suspended", info.isSuspended());
    		model.put("inNative", info.isInNative());
    		models.add(model);
    	}
    	return models;
    }

	private static File getZippedLogfile(EmailContent content, File file){
		if (file != null) {
			try{
	            File zipFile = File.createTempFile("tempZIP", ".zip");
	            ZipOutputStream zipOut = new ZipOutputStream(new FileOutputStream(zipFile));
	            zipOut.putNextEntry(new ZipEntry(file.getName()));
	
	            FileInputStream in = new FileInputStream(file);
	            StreamUtils.transfer(in, zipOut);
	            in.close();
	
	            zipOut.closeEntry();
	            zipOut.close();
	
	            content.addAttachment(new EmailAttachment.FileAttachment(file.getName() + ".zip", zipFile));
	
	            return zipFile;
	        }
	        catch (IOException e) {
	            LOG.error("Failed to create zip file", e);
	        }

		}
		return null;
	}

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.util.timeout.ModelTimeoutClient#getName()
	 */
	@Override
	public String getThreadName() {
		return "Email handler " + vo.getXid(); 
	}

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.util.timeout.ModelTimeoutClient#getTaskId()
	 */
	@Override
	public String getTaskId() {
		return "EmailHandler: " + this.vo.getXid();
	}
	
	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.util.timeout.TimeoutClient#getQueueSize()
	 */
	@Override
	public int getQueueSize() {
		return Common.defaultTaskQueueSize;
	}
    
	class SetCallback extends ScriptPointValueSetter {
        public SetCallback(ScriptPermissions permissions) {
            super(permissions);
        }

        /*
         * (non-Javadoc)
         * 
         * @see
         * com.serotonin.mango.util.script.ScriptPointValueSetter#setImpl(com.serotonin.mango.rt.dataImage.IDataPointValueSource,
         * java.lang.Object, long)
         */
        @Override
        public void setImpl(IDataPointValueSource point, Object value, long timestamp, String annotation) {
            DataPointRT dprt = (DataPointRT) point;

            // We may, however, need to coerce the given value.
            try {
                DataValue mangoValue = ScriptUtils.coerce(value, dprt.getDataTypeId());
                SetPointSource source;
                PointValueTime newValue = new PointValueTime(mangoValue, timestamp);
                if(StringUtils.isBlank(annotation))
                    source = EmailHandlerRT.this;
                else
                    source = new OneTimePointAnnotation(EmailHandlerRT.this, annotation);
                
                DataSourceRT<? extends DataSourceVO<?>> dsrt = Common.runtimeManager.getRunningDataSource(dprt.getDataSourceId());
                dsrt.setPointValue(dprt, newValue, source);
            }
            catch (ResultTypeException e) {
                // Log an error
                LOG.error("Invalid set attempted from email event handler on data point: " + dprt.getVO().getExtendedName());
            }
        }
    }

    @Override
    public String getSetPointSourceType() {
        return "EMAIL_EVENT_HANDLER";
    }

    @Override
    public int getSetPointSourceId() {
        return vo.getId();
    }

    @Override
    public TranslatableMessage getSetPointSourceMessage() {
        return new TranslatableMessage("annotation.eventHandler");
    }

    @Override
    public void raiseRecursionFailureEvent() {
        LOG.error("Recursion failure in setting value from email handler");
    }
}
