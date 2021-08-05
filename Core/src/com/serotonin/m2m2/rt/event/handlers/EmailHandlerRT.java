/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.rt.event.handlers;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.text.StringEscapeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.infiniteautomation.mango.spring.service.MailingListService;
import com.infiniteautomation.mango.spring.service.MangoJavaScriptService;
import com.infiniteautomation.mango.spring.service.PermissionService;
import com.infiniteautomation.mango.util.ConfigurationExportData;
import com.infiniteautomation.mango.util.Functions;
import com.infiniteautomation.mango.util.WorkItemInfo;
import com.infiniteautomation.mango.util.script.CompiledMangoJavaScript;
import com.infiniteautomation.mango.util.script.MangoJavaScriptResult;
import com.infiniteautomation.mango.util.script.ScriptPermissions;
import com.serotonin.db.pair.IntStringPair;
import com.serotonin.io.StreamUtils;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.DataTypes;
import com.serotonin.m2m2.db.dao.DataPointDao;
import com.serotonin.m2m2.db.dao.DataSourceDao;
import com.serotonin.m2m2.db.dao.EventDao;
import com.serotonin.m2m2.db.dao.SystemSettingsDao;
import com.serotonin.m2m2.email.MangoEmailContent;
import com.serotonin.m2m2.email.PostEmailRunnable;
import com.serotonin.m2m2.email.UsedImagesDirective;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.i18n.Translations;
import com.serotonin.m2m2.rt.dataImage.DataPointRT;
import com.serotonin.m2m2.rt.dataImage.IDataPointValueSource;
import com.serotonin.m2m2.rt.dataImage.PointValueTime;
import com.serotonin.m2m2.rt.dataImage.RenderedPointValueTime;
import com.serotonin.m2m2.rt.dataImage.SetPointSource;
import com.serotonin.m2m2.rt.dataImage.types.DataValue;
import com.serotonin.m2m2.rt.dataSource.DataSourceRT;
import com.serotonin.m2m2.rt.event.EventInstance;
import com.serotonin.m2m2.rt.event.type.DataPointEventType;
import com.serotonin.m2m2.rt.event.type.SystemEventType;
import com.serotonin.m2m2.rt.maint.work.EmailWorkItem;
import com.serotonin.m2m2.rt.script.DataPointWrapper;
import com.serotonin.m2m2.rt.script.EventInstanceWrapper;
import com.serotonin.m2m2.rt.script.JsonImportExclusion;
import com.serotonin.m2m2.rt.script.OneTimePointAnnotation;
import com.serotonin.m2m2.rt.script.ResultTypeException;
import com.serotonin.m2m2.rt.script.ScriptError;
import com.serotonin.m2m2.rt.script.ScriptLog;
import com.serotonin.m2m2.rt.script.ScriptPointValueSetter;
import com.serotonin.m2m2.util.timeout.ModelTimeoutClient;
import com.serotonin.m2m2.util.timeout.ModelTimeoutTask;
import com.serotonin.m2m2.vo.DataPointVO;
import com.serotonin.m2m2.vo.dataPoint.DataPointWithEventDetectors;
import com.serotonin.m2m2.vo.dataSource.DataSourceVO;
import com.serotonin.m2m2.vo.event.EmailEventHandlerVO;
import com.serotonin.m2m2.vo.mailingList.RecipientListEntryType;
import com.serotonin.timer.TimerTask;
import com.serotonin.web.mail.EmailAttachment;
import com.serotonin.web.mail.EmailAttachment.FileAttachment;
import com.serotonin.web.mail.EmailContent;
import com.serotonin.web.mail.EmailInline;
import com.serotonin.web.mail.EmailInline.FileInline;

public class EmailHandlerRT extends EventHandlerRT<EmailEventHandlerVO> implements ModelTimeoutClient<EventInstance>, SetPointSource {
    private static final Logger LOG = LoggerFactory.getLogger(EmailHandlerRT.class);
    public static final String DO_NOT_SEND_KEY = "CANCEL";

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
    private final MailingListService mailingListService;
    private final PermissionService permissionService;

    public EmailHandlerRT(EmailEventHandlerVO vo) {
        super(vo);
        this.mailingListService = Common.getBean(MailingListService.class);
        this.permissionService = Common.getBean(PermissionService.class);
    }

    public Set<String> getActiveRecipients() {
        return activeRecipients;
    }

    @Override
    public void eventRaised(EventInstance evt) {

        if(vo.getActiveRecipients() != null && !vo.getActiveRecipients().isEmpty()) {
            // Get the email addresses to send to
            activeRecipients = mailingListService.getActiveRecipients(
                    vo.getActiveRecipients(),
                    evt.getActiveTimestamp(),
                    RecipientListEntryType.MAILING_LIST,
                    RecipientListEntryType.ADDRESS,
                    RecipientListEntryType.USER);
        }
        // Send an email to the active recipients.
        sendEmail(evt, NotificationType.ACTIVE, activeRecipients);

        // If an inactive notification is to be sent, save the active recipients.
        if (vo.isSendInactive()) {
            if (vo.isInactiveOverride() && vo.getInactiveRecipients() != null && !vo.getInactiveRecipients().isEmpty()) {
                inactiveRecipients = mailingListService.getActiveRecipients(
                        vo.getInactiveRecipients(),
                        evt.getActiveTimestamp(),
                        RecipientListEntryType.MAILING_LIST,
                        RecipientListEntryType.ADDRESS,
                        RecipientListEntryType.USER);
            }else if(!vo.isInactiveOverride()) {
                inactiveRecipients = activeRecipients;
            }
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
    @Override
    synchronized public void scheduleTimeout(EventInstance evt, long fireTime) {

        Set<String> addresses;
        if(vo.getEscalationRecipients() != null && !vo.getEscalationRecipients().isEmpty()) {
            // Get the email addresses to send to
            addresses = mailingListService.getActiveRecipients(
                    vo.getEscalationRecipients(),
                    fireTime,
                    RecipientListEntryType.MAILING_LIST,
                    RecipientListEntryType.ADDRESS,
                    RecipientListEntryType.USER);
        }else {
            addresses = new HashSet<>();
        }

        // Send the escalation.
        sendEmail(evt, NotificationType.ESCALATION, addresses);


        // If an inactive notification is to be sent, save the escalation recipients, but only if inactive recipients
        // have not been overridden.
        if (vo.isSendInactive() && !vo.isInactiveOverride())
            inactiveRecipients.addAll(addresses);

        if (vo.isRepeatEscalations()) {
            //While evt will probably show ack'ed if ack'ed, the possibility exists for it to be deleted
            // and in which case we want to notice rather than send emails forever.
            EventInstance dbEvent = EventDao.getInstance().get(evt.getId());
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

        // Send an email to the inactive recipients.
        if(vo.isSendInactive())
            sendEmail(evt, NotificationType.INACTIVE, inactiveRecipients);
    }

    /**
     * Should only be called by EventManagerImpl. This method sends emails to all users and mailing lists
     * which have been configured to receive events of this level.
     *
     * @param evt
     * @param addresses  A set of email addresses that will be notified of all events over a certain level
     * which is configured on each user or on a mailing list
     */
    public static void sendActiveEmail(EventInstance evt, Set<String> addresses) {
        sendEmail(evt, NotificationType.ACTIVE, addresses, null, false, 0, false, null, null, null, null, null, null);
    }

    private void sendEmail(EventInstance evt, NotificationType notificationType, Set<String> addresses) {
        sendEmail(evt, notificationType, addresses, vo.getSubject() == EmailEventHandlerVO.SUBJECT_INCLUDE_NAME ? vo.getName() : null, vo.isIncludeSystemInfo(), vo.getIncludePointValueCount(),
                vo.isIncludeLogfile(), vo.getXid(), vo.getCustomTemplate(), vo.getAdditionalContext(), vo.getScript(),
                new SetCallback(vo.getScriptRoles()), vo.getScriptRoles());
    }

    private static void sendEmail(EventInstance evt, NotificationType notificationType, Set<String> addresses,
            String baseSubject, boolean includeSystemInfo, int pointValueCount, boolean includeLogs, String handlerXid,
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
        if(StringUtils.isBlank(baseSubject)){
            //Just set the subject to the message
            baseSubject = evt.getMessage().translate(translations);

            //Strip out the HTML and the &nbsp
            baseSubject = StringEscapeUtils.unescapeHtml4(baseSubject);
            //Since we have <br/> in the code and that isn't proper HTML we need to remove it by hand
            baseSubject = baseSubject.replace("<br/>", "\n");
        }//end if alias was blank

        // Determine the subject to use.
        TranslatableMessage subjectMsg;
        TranslatableMessage notifTypeMsg = new TranslatableMessage(notificationType.getKey());
        if (StringUtils.isBlank(baseSubject)) {
            //Make these more descriptive
            if (evt.getId() == Common.NEW_ID)
                subjectMsg = new TranslatableMessage("ftl.subject.default", notifTypeMsg);
            else
                subjectMsg = new TranslatableMessage("ftl.subject.default.id", notifTypeMsg, evt.getId());
        }
        else {
            if (evt.getId() == Common.NEW_ID)
                subjectMsg = new TranslatableMessage("ftl.subject.alias", baseSubject, notifTypeMsg);
            else
                subjectMsg = new TranslatableMessage("ftl.subject.alias.id", baseSubject, notifTypeMsg, evt.getId());
        }
        String alarmLevel = evt.getAlarmLevel().getDescription().translate(translations);

        String subject = alarmLevel + " - " + subjectMsg.translate(translations);

        //Trim the subject if its too long
        if(subject.length() > 200)
            subject = subject.substring(0,200);

        try {
            String[] toAddrs;
            if(addresses == null)
                toAddrs = new String[0];
            else
                toAddrs = addresses.toArray(new String[0]);

            UsedImagesDirective inlineImages = new UsedImagesDirective();

            // Send the email.
            Map<String, Object> model = new HashMap<String, Object>();
            model.put(EventInstance.CONTEXT_KEY, evt);
            model.put(EventInstanceWrapper.CONTEXT_KEY, new EventInstanceWrapper(evt));
            if (evt.getContext() != null)
                model.putAll(evt.getContext());
            model.put("img", inlineImages);
            model.put("instanceDescription", SystemSettingsDao.instance.getValue(SystemSettingsDao.INSTANCE_DESCRIPTION));
            if(includeSystemInfo){
                //Get the Work Items
                List<WorkItemInfo> highPriorityWorkItems = Common.backgroundProcessing.getHighPriorityServiceItems();
                model.put("highPriorityWorkItems", highPriorityWorkItems);
                List<WorkItemInfo> mediumPriorityWorkItems = Common.backgroundProcessing.getMediumPriorityServiceQueueItems();
                model.put("mediumPriorityWorkItems", mediumPriorityWorkItems);
                List<WorkItemInfo> lowPriorityWorkItems = Common.backgroundProcessing.getLowPriorityServiceQueueItems();
                model.put("lowPriorityWorkItems", lowPriorityWorkItems);
                model.put("threadList", getThreadsList());
            }

            int type = SystemSettingsDao.instance.getIntValue(SystemSettingsDao.EMAIL_CONTENT_TYPE);

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
                Map<String, EmailPointWrapper> context = new HashMap<>();
                for(IntStringPair pair : additionalContext) {
                    EmailPointWrapper point;
                    DataPointRT rt = Common.runtimeManager.getDataPoint(pair.getKey());
                    List<PointValueTime> pointValues;
                    List<RenderedPointValueTime> renderedPointValues;
                    DataPointVO dpvo;
                    if(rt != null) {
                        dpvo = rt.getVO();
                        point = new EmailPointWrapper(dpvo);
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
                        dpvo = DataPointDao.getInstance().get(pair.getKey());
                        if(dpvo == null)
                            continue;

                        point = new EmailPointWrapper(dpvo);
                        pointValues = Common.databaseProxy.newPointValueDao()
                                .getLatestPointValues(dpvo, pointValueCount);
                        renderedPointValues = new ArrayList<RenderedPointValueTime>();
                        for(PointValueTime pvt : pointValues) {
                            RenderedPointValueTime rpvt = new RenderedPointValueTime();
                            rpvt.setValue(Functions.getRenderedText(dpvo, pvt));
                            rpvt.setTime(Functions.getFullSecondTime(pvt.getTime()));
                            renderedPointValues.add(rpvt);
                        }
                    }
                    point.setRawValues(pointValues);
                    point.setValues(renderedPointValues);
                    point.setContextKey(pair.getValue());
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
                        DataPointVO targetVo = DataPointDao.getInstance().get(pair.getKey());
                        if(targetVo == null) {
                            LOG.warn("Additional context point with ID: " + pair.getKey() + " and context name " + pair.getValue() + " could not be found.");
                            continue; //Not worth aborting the email, just warn it
                        }

                        if(targetVo.getDefaultCacheSize() == 0)
                            targetVo.setDefaultCacheSize(1);
                        DataPointWithEventDetectors dp = new DataPointWithEventDetectors(targetVo, new ArrayList<>());
                        DataSourceRT<? extends DataSourceVO> dataSource = DataSourceDao.getInstance().get(targetVo.getDataSourceId()).createDataSourceRT();
                        dprt = new DataPointRT(dp, targetVo.getPointLocator().createRuntime(), dataSource,
                                null, Common.databaseProxy.newPointValueDao(), Common.databaseProxy.getPointValueCacheDao());
                        dprt.resetValues();
                    }
                    context.put(pair.getValue(), dprt);
                }

                modelContext.put(DO_NOT_SEND_KEY, MangoJavaScriptService.UNCHANGED);
                List<JsonImportExclusion> importExclusions = new ArrayList<JsonImportExclusion>(1);
                importExclusions.add(new JsonImportExclusion("xid", handlerXid) {
                    @Override
                    public String getImporterType() {
                        return ConfigurationExportData.EVENT_HANDLERS;
                    }
                });


                try (ScriptLog scriptLog = new ScriptLog("emailScript-" + evt.getId())) {
                    MangoJavaScriptService service = Common.getBean(MangoJavaScriptService.class);
                    long time = evt.isActive() || !evt.isRtnApplicable() ? evt.getActiveTimestamp() : evt.getRtnTimestamp();
                    CompiledMangoJavaScript compiledScript = new CompiledMangoJavaScript(
                            setCallback,
                            scriptLog,
                            modelContext,
                            null,
                            importExclusions,
                            false,
                            service,
                            permissions
                            );
                    compiledScript.compile(script, true);
                    compiledScript.initialize(context);
                    MangoJavaScriptResult r = compiledScript.execute(Common.timer.currentTimeMillis(), time, DataTypes.ALPHANUMERIC);

                    PointValueTime result = (PointValueTime)r.getResult();
                    if(result != null && result.getValue() == MangoJavaScriptService.UNCHANGED) //The script cancelled the email
                        return;

                } catch(ScriptError | ResultTypeException e) {
                    LOG.error("Exception running email handler script: " + e.getTranslatableMessage(), e);
                }
            }

            MangoEmailContent content;
            if(StringUtils.isEmpty(customTemplate))
                content = new MangoEmailContent(notificationType.getFile(), model, translations, subject, StandardCharsets.UTF_8);
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
                content.addInline(new FileInline(s, Common.WEB.resolve(s).toFile()));

            if(toAddrs.length > 0)
                EmailWorkItem.queueEmail(toAddrs, content, postEmail);
        }
        catch (Exception e) {
            LOG.error("Error sending email", e);
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

                content.addAttachment(new FileAttachment(file.getName() + ".zip", zipFile));

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
        private final MangoJavaScriptService service;

        public SetCallback(ScriptPermissions permissions) {
            super(permissions);
            this.service = Common.getBean(MangoJavaScriptService.class);
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
                DataValue mangoValue = service.coerce(value, dprt.getDataTypeId());
                SetPointSource source;
                PointValueTime newValue = new PointValueTime(mangoValue, timestamp);
                if(StringUtils.isBlank(annotation))
                    source = EmailHandlerRT.this;
                else
                    source = new OneTimePointAnnotation(EmailHandlerRT.this, annotation);

                DataSourceRT<? extends DataSourceVO> dsrt = Common.runtimeManager.getRunningDataSource(dprt.getDataSourceId());
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
        return new TranslatableMessage("annotation.email.setPointMessage", vo.getName());
    }

    @Override
    public void raiseRecursionFailureEvent() {
        LOG.error("Recursion failure in setting value from email handler");
    }

    static class EmailPointWrapper extends DataPointWrapper {
        List<PointValueTime> rawValues;
        List<RenderedPointValueTime> values;
        String contextKey;

        public EmailPointWrapper(DataPointVO vo) {
            super(vo, null);
        }

        public void setRawValues(List<PointValueTime> rawValues) {
            this.rawValues = rawValues;
        }

        public List<PointValueTime> getRawValues() {
            return rawValues;
        }

        public void setValues(List<RenderedPointValueTime> values) {
            this.values = values;
        }

        public List<RenderedPointValueTime> getValues() {
            return values;
        }

        public void setContextKey(String contextKey) {
            this.contextKey = contextKey;
        }

        public String getContextKey() {
            return contextKey;
        }
    }
}
