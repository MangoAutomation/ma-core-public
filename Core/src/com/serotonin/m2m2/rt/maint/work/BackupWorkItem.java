package com.serotonin.m2m2.rt.maint.work;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.StringWriter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.io.comparator.LastModifiedFileComparator;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.joda.time.DateTime;

import com.infiniteautomation.mango.spring.service.EmportService;
import com.infiniteautomation.mango.spring.service.PermissionService;
import com.infiniteautomation.mango.util.ConfigurationExportData;
import com.serotonin.ShouldNeverHappenException;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.dao.SystemSettingsDao;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.rt.event.type.SystemEventType;
import com.serotonin.m2m2.util.DateUtils;
import com.serotonin.timer.CronTimerTrigger;
import com.serotonin.timer.RejectedTaskReason;
import com.serotonin.timer.TimerTask;

public class BackupWorkItem implements WorkItem {
    private static final Log LOG = LogFactory.getLog(BackupWorkItem.class);
    private static final String TASK_ID = "CONFIG_BACKUP";

    public static final String BACKUP_DATE_FORMAT = "MMM-dd-yyyy_HHmmss"; //Used to for filename and property value for last run

    //Lock to ensure we don't clobber files by running a backup
    // during another one.
    public static final Object lock = new Object();

    private static BackupSettingsTask task; //Static holder to re-schedule task if necessary
    private String backupLocation; //Location of backup directory on disk
    private volatile boolean finished = false;
    private volatile boolean cancelled = false;
    private volatile boolean failed = false;
    private String filename;

    /**
     * Statically Schedule a Timer Task that will run this work item.
     *
     * TODO For Startup Make this an RTM and add to the RuntimeManager's list of startup RTMS
     * currently only the Module RTMs get started there.  For now this call is
     * used within the RuntimeManager
     */
    public static void schedule() {
        try {
            // Test trigger for running every 25 seconds.
            //String cronTrigger = "0/25 * * * * ?";
            // Trigger to run at Set Hour/Minute "s m h * * ?";
            int hour = SystemSettingsDao.instance.getIntValue(SystemSettingsDao.BACKUP_HOUR);
            int minute = SystemSettingsDao.instance.getIntValue(SystemSettingsDao.BACKUP_MINUTE);

            String cronTrigger = "0 " + minute + " " + hour + " * * ?";
            task = new BackupSettingsTask(cronTrigger);
            Common.backgroundProcessing.schedule(task);
        }
        catch (ParseException e) {
            throw new ShouldNeverHappenException(e);
        }
    }

    /**
     * Safely unschedule the timer task for this work item
     */
    public static void unschedule(){
        if(task != null){
            task.cancel();
        }
    }



    @Override
    public int getPriority() {
        return WorkItem.PRIORITY_MEDIUM;
    }

    /**
     * Queue a backup for execution
     * @param backupLocation
     */
    public static void queueBackup(String backupLocation){
        BackupWorkItem item = new BackupWorkItem();
        item.backupLocation = backupLocation;
        Common.backgroundProcessing.addWorkItem(item);
    }

    @Override
    public void execute() {
        synchronized (lock) {
            LOG.info("Starting backup WorkItem.");
            // Create the filename
            String filename = "Mango-Configuration";
            String runtimeString = new SimpleDateFormat(BACKUP_DATE_FORMAT).format(new Date());
            int maxFiles = SystemSettingsDao.instance.getIntValue(SystemSettingsDao.BACKUP_FILE_COUNT);
            // If > 1 then we will use a date in the filename
            if (maxFiles > 1) {
                // Create Mango-Configuration-date.json
                filename += "-";
                filename += runtimeString;
            }
            filename += ".json";
            // Fill the full path
            String fullFilePath = this.backupLocation;
            if (fullFilePath.endsWith(File.separator)) {
                fullFilePath += filename;
            } else {
                fullFilePath += File.separator;
                fullFilePath += filename;
            }

            if (cancelled)
                return;
            // Collect the json backup data
            String jsonData = getBackup();

            // Write to file
            try {
                File file = new File(fullFilePath);
                if (!file.exists())
                    if (!file.createNewFile()) {
                        failed = true;
                        LOG.warn("Unable to create backup file: " + fullFilePath);
                        SystemEventType.raiseEvent(
                                new SystemEventType(SystemEventType.TYPE_BACKUP_FAILURE),
                                Common.timer.currentTimeMillis(), false,
                                new TranslatableMessage("event.backup.failure", fullFilePath,
                                        "Unable to create backup file"));

                        return;
                    }
                FileWriter fw = new FileWriter(file, false); // Always replace if exists
                BufferedWriter bw = new BufferedWriter(fw);
                bw.write(jsonData);
                bw.close();

                // Save the filename
                this.filename = file.getAbsolutePath();

                // Store the last successful backup time
                SystemSettingsDao.instance.setValue(SystemSettingsDao.BACKUP_LAST_RUN_SUCCESS,
                        runtimeString);

                // Clean up old files, keeping the correct number as the history
                File backupDir = new File(this.backupLocation);
                File[] files = backupDir.listFiles(new FilenameFilter() {
                    @Override
                    public boolean accept(File dir, String name) {
                        return name.toLowerCase(Locale.ROOT).endsWith(".json");
                    }
                });
                // Sort the files by date
                Arrays.sort(files, LastModifiedFileComparator.LASTMODIFIED_REVERSE);

                // Keep the desired history
                for (int i = maxFiles; i < files.length; i++) {
                    try {
                        files[i].delete(); // Remove it
                    } catch (Exception e) {
                        LOG.warn("Unable to delete file: " + files[i].getAbsolutePath(), e);
                    }
                }

            } catch (Exception e) {
                LOG.warn(e);
                failed = true;
                SystemEventType.raiseEvent(new SystemEventType(SystemEventType.TYPE_BACKUP_FAILURE),
                        Common.timer.currentTimeMillis(), false, new TranslatableMessage(
                                "event.backup.failure", fullFilePath, e.getMessage()));
            } finally {
                this.finished = true;
                LOG.info("Finished backup WorkItem.");
            }
        }
    }

    /**
     * Get a JSON Backup
     * @return
     */
    public String getBackup(){
        return Common.getBean(PermissionService.class).runAsSystemAdmin(() -> {
            Map<String, Object> data = ConfigurationExportData.createExportDataMap(null);
            StringWriter stringWriter = new StringWriter();
            Common.getBean(EmportService.class).export(data, stringWriter, 3);
            return stringWriter.toString();
        });
    }

    /**
     * Timer task that uses this Backup Work Item in its execution
     * @author tpacker
     *
     */
    static class BackupSettingsTask extends TimerTask {
        BackupSettingsTask(String cronTrigger) throws ParseException {
            super(new CronTimerTrigger(cronTrigger), "Settings backup", "SettingsBackup", 0);
        }

        @Override
        public void run(long runtime) {

            //Should we run the backup?
            try{
                String lastRunDateString = SystemSettingsDao.instance.getValue(SystemSettingsDao.BACKUP_LAST_RUN_SUCCESS);
                String backupLocation = SystemSettingsDao.instance.getValue(SystemSettingsDao.BACKUP_FILE_LOCATION);

                //Have we ever run?
                if(lastRunDateString != null){
                    Date lastRunDate;
                    try {
                        lastRunDate = new SimpleDateFormat(BACKUP_DATE_FORMAT).parse(lastRunDateString);
                    }catch(Exception e) {
                        lastRunDate = new Date();
                        LOG.warn("Failed to parse last backup date, using Jan 1 1970.", e);
                    }
                    DateTime lastRun = new DateTime(lastRunDate);
                    //Compute the next run time off of the last run time
                    DateTime nextRun = DateUtils.plus(lastRun, SystemSettingsDao.instance.getIntValue(SystemSettingsDao.BACKUP_PERIOD_TYPE),
                            SystemSettingsDao.instance.getIntValue(SystemSettingsDao.BACKUP_PERIODS));
                    //Is the next run time now or before now?
                    if(!nextRun.isAfter(runtime)){
                        BackupWorkItem.queueBackup(backupLocation);
                    }

                }else{
                    BackupWorkItem.queueBackup(backupLocation);
                }
            }catch(Exception e){
                LOG.error(e);
                SystemEventType.raiseEvent(new SystemEventType(SystemEventType.TYPE_BACKUP_FAILURE),
                        Common.timer.currentTimeMillis(), false,
                        new TranslatableMessage("event.backup.failure", "no file", e.getMessage()));

            }
        }//end run
    }// end backup settings task

    @Override
    public String getDescription() {
        return "Backing up system configuration to: " + this.backupLocation;
    }

    @Override
    public String getTaskId() {
        return TASK_ID;
    }

    @Override
    public int getQueueSize() {
        return 0;
    }

    @Override
    public void rejected(RejectedTaskReason reason) { }

    public void cancel(){
        this.cancelled = true;
    }

    public boolean isFinished(){
        return this.finished;
    }

    public boolean isFailed(){
        return this.failed;
    }

    public void setBackupLocation(String location){
        this.backupLocation = location;
    }

    public String getFilename(){
        return this.filename;
    }
}
