package com.serotonin.m2m2.rt.maint.work;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.io.comparator.LastModifiedFileComparator;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.joda.time.DateTime;

import com.infiniteautomation.mango.io.serial.virtual.VirtualSerialPortConfigDao;
import com.serotonin.ShouldNeverHappenException;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.dao.DataPointDao;
import com.serotonin.m2m2.db.dao.DataSourceDao;
import com.serotonin.m2m2.db.dao.EventHandlerDao;
import com.serotonin.m2m2.db.dao.JsonDataDao;
import com.serotonin.m2m2.db.dao.MailingListDao;
import com.serotonin.m2m2.db.dao.PublisherDao;
import com.serotonin.m2m2.db.dao.SystemSettingsDao;
import com.serotonin.m2m2.db.dao.TemplateDao;
import com.serotonin.m2m2.db.dao.UserDao;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.module.EmportDefinition;
import com.serotonin.m2m2.module.ModuleRegistry;
import com.serotonin.m2m2.rt.event.type.SystemEventType;
import com.serotonin.m2m2.util.DateUtils;
import com.serotonin.m2m2.web.dwr.EmportDwr;
import com.serotonin.timer.CronTimerTrigger;
import com.serotonin.timer.RejectedTaskReason;
import com.serotonin.timer.TimerTask;

public class BackupWorkItem implements WorkItem {
    private static final Log LOG = LogFactory.getLog(BackupWorkItem.class);
    private static final String TASK_ID = "CONFIG_BACKUP";
    
    public static final String BACKUP_DATE_FORMAT = "MMM-dd-yyyy_HHmmss"; //Used to for filename and property value for last run
    public static final SimpleDateFormat dateFormatter = new SimpleDateFormat(BACKUP_DATE_FORMAT);
    
    //Lock to ensure we don't clobber files by running a backup 
    // during another one.
    public static final Object lock = new Object();
    
    private static BackupSettingsTask task; //Static holder to re-schedule task if necessary
    private String backupLocation; //Location of backup directory on disk
    private volatile boolean finished = false;
    private volatile boolean cancelled = false;
    
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
    		int hour = SystemSettingsDao.getIntValue(SystemSettingsDao.BACKUP_HOUR);
    		int minute = SystemSettingsDao.getIntValue(SystemSettingsDao.BACKUP_MINUTE);
    		
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
		synchronized(lock){
			LOG.info("Starting backup WorkItem.");
			//Create the filename
			String filename = "Mango-Configuration";
			String runtimeString = dateFormatter.format(new Date());
			int maxFiles = SystemSettingsDao.getIntValue(SystemSettingsDao.BACKUP_FILE_COUNT);
			//If > 1 then we will use a date in the filename 
			if(maxFiles > 1){
				//Create Mango-Configuration-date.json
				filename += "-";
				filename += runtimeString;
			}
			filename += ".json";
			//Fill the full path
			String fullFilePath = this.backupLocation;
			if(fullFilePath.endsWith(File.separator)){
				fullFilePath += filename;
			}else{
				fullFilePath += File.separator;
				fullFilePath += filename;
			}

			if(cancelled)
				return;
			//Collect the json backup data
			String jsonData = getBackup();
			
			//Write to file
			try{
				File file = new File(fullFilePath);
				if(!file.exists())
					if(!file.createNewFile()){
						LOG.warn("Unable to create backup file: " + fullFilePath);
			            SystemEventType.raiseEvent(new SystemEventType(SystemEventType.TYPE_BACKUP_FAILURE),
			                    Common.backgroundProcessing.currentTimeMillis(), false,
			                    new TranslatableMessage("event.backup.failure", fullFilePath, "Unable to create backup file"));
	
						return;
					}
				FileWriter fw = new FileWriter(file,false); //Always replace if exists
				BufferedWriter bw = new BufferedWriter(fw);
				bw.write(jsonData);
				bw.close();
				//Store the last successful backup time
				SystemSettingsDao.instance.setValue(SystemSettingsDao.BACKUP_LAST_RUN_SUCCESS,runtimeString);
				
				//Clean up old files, keeping the correct number as the history
				File backupDir = new File(this.backupLocation);
				File[] files = backupDir.listFiles(new FilenameFilter(){
					public boolean accept(File dir, String name){
						return name.toLowerCase().endsWith(".json");
					}
				});
				//Sort the files by date
		        Arrays.sort(files, LastModifiedFileComparator.LASTMODIFIED_REVERSE);
		        
		        //Keep the desired history
		        for(int i=maxFiles; i<files.length; i++){
		        	try{
		        		files[i].delete(); //Remove it
		        	}catch(Exception e){
		        		LOG.warn("Unable to delete file: " + files[i].getAbsolutePath(),e);
		        	}
		        }
		        
			}catch(Exception e){
				LOG.warn(e);
	            SystemEventType.raiseEvent(new SystemEventType(SystemEventType.TYPE_BACKUP_FAILURE),
	                    Common.backgroundProcessing.currentTimeMillis(), false,
	                    new TranslatableMessage("event.backup.failure", fullFilePath, e.getMessage()));
			}finally{
				this.finished = true;
			}
		}
	}

	/**
	 * Get a JSON Backup
	 * @return
	 */
	public String getBackup(){
        Map<String, Object> data = new LinkedHashMap<String, Object>();

        data.put(EmportDwr.DATA_SOURCES, DataSourceDao.instance.getDataSources());
        data.put(EmportDwr.DATA_POINTS, DataPointDao.instance.getDataPoints(null, true));
        data.put(EmportDwr.USERS, UserDao.instance.getUsers());
        data.put(EmportDwr.MAILING_LISTS, MailingListDao.instance.getMailingLists());
        data.put(EmportDwr.PUBLISHERS, PublisherDao.instance.getPublishers());
        data.put(EmportDwr.EVENT_HANDLERS, EventHandlerDao.instance.getEventHandlers());
        data.put(EmportDwr.POINT_HIERARCHY, DataPointDao.instance.getPointHierarchy(true).getRoot().getSubfolders());
        data.put(EmportDwr.SYSTEM_SETTINGS, SystemSettingsDao.instance.getAllSystemSettingsAsCodes());
        data.put(EmportDwr.TEMPLATES, TemplateDao.instance.getAllDataPointTemplates());
        data.put(EmportDwr.JSON_DATA, JsonDataDao.instance.getAll());
        data.put(EmportDwr.VIRTUAL_SERIAL_PORTS, VirtualSerialPortConfigDao.instance.getAll());
        
        //Export all module data too
        for (EmportDefinition def : ModuleRegistry.getDefinitions(EmportDefinition.class)) {
                data.put(def.getElementId(), def.getExportData());
        }

        return EmportDwr.export(data);
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
        		String lastRunDateString = SystemSettingsDao.getValue(SystemSettingsDao.BACKUP_LAST_RUN_SUCCESS);
	        	String backupLocation = SystemSettingsDao.getValue(SystemSettingsDao.BACKUP_FILE_LOCATION);

	        	//Have we ever run?
	        	if(lastRunDateString != null){
	        		Date lastRunDate = dateFormatter.parse(lastRunDateString);
	        		DateTime lastRun = new DateTime(lastRunDate);
	        		//Compute the next run time off of the last run time
		            DateTime nextRun = DateUtils.plus(lastRun, SystemSettingsDao.getIntValue(SystemSettingsDao.BACKUP_PERIOD_TYPE),
		                    SystemSettingsDao.getIntValue(SystemSettingsDao.BACKUP_PERIODS));
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
	                    Common.backgroundProcessing.currentTimeMillis(), false,
	                    new TranslatableMessage("event.backup.failure", "no file", e.getMessage()));

        	}
        }//end run
    }// end backup settings task

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.rt.maint.work.WorkItem#getDescription()
	 */
	@Override
	public String getDescription() {
		return "Backing up system configuration to: " + this.backupLocation;
	}
	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.rt.maint.work.WorkItem#getTaskId()
	 */
	@Override
	public String getTaskId() {
		return TASK_ID;
	}
	
	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.util.timeout.TimeoutClient#getQueueSize()
	 */
	@Override
	public int getQueueSize() {
		return 0;
	}
	
	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.rt.maint.work.WorkItem#rejected(com.serotonin.timer.RejectedTaskReason)
	 */
	@Override
	public void rejected(RejectedTaskReason reason) { }

	public void cancel(){
		this.cancelled = true;
	}
	
	public boolean isFinished(){
		return this.finished;
	}
	
	public void setBackupLocation(String location){
		this.backupLocation = location;
	}
}
