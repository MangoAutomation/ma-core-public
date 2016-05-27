package com.serotonin.m2m2.rt.maint.work;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.OutputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;

import org.apache.commons.io.comparator.LastModifiedFileComparator;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.joda.time.DateTime;

import com.serotonin.ShouldNeverHappenException;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.dao.SystemSettingsDao;
import com.serotonin.m2m2.i18n.ProcessResult;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.rt.event.type.SystemEventType;
import com.serotonin.m2m2.util.DateUtils;
import com.serotonin.m2m2.util.timeout.RejectableTimerTask;
import com.serotonin.timer.CronTimerTrigger;
import com.serotonin.timer.RejectedTaskReason;
import com.serotonin.util.StringUtils;

public class DatabaseBackupWorkItem implements WorkItem {
	
    private static final Log LOG = LogFactory.getLog(DatabaseBackupWorkItem.class);
    public static final String BACKUP_DATE_FORMAT = "MMM-dd-yyyy_HHmmss"; //Used to for filename and property value for last run
    public static final SimpleDateFormat dateFormatter = new SimpleDateFormat(BACKUP_DATE_FORMAT);
    private static final String TASK_ID = "DB_BACKUP";
    
    //Lock to ensure we don't clobber files by running a backup 
    // during another one.
    public static final Object lock = new Object();
    
    private static DatabaseBackupTask task; //Static holder to re-schedule task if necessary
    private String backupLocation; //Location of backup directory on disk

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
    		int hour = SystemSettingsDao.getIntValue(SystemSettingsDao.DATABASE_BACKUP_HOUR);
    		int minute = SystemSettingsDao.getIntValue(SystemSettingsDao.DATABASE_BACKUP_MINUTE);
    		
    		String cronTrigger = "0 " + minute + " " + hour + " * * ?"; 
    		task = new DatabaseBackupTask(cronTrigger);
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
		DatabaseBackupWorkItem item = new DatabaseBackupWorkItem();
		item.backupLocation = backupLocation;
		Common.backgroundProcessing.addWorkItem(item);
	}
    
	@Override
	public void execute() {
		synchronized(lock){
			LOG.info("Starting database backup WorkItem.");
			
			//Create the filename
			String filename = "core-database-" + Common.databaseProxy.getType();
			String runtimeString = dateFormatter.format(new Date());
			int maxFiles = SystemSettingsDao.getIntValue(SystemSettingsDao.DATABASE_BACKUP_FILE_COUNT);
			//If > 1 then we will use a date in the filename 
			if(maxFiles > 1){
				//Create Mango-Configuration-date.json
				filename += "-";
				filename += runtimeString;
			}
			filename += ".zip";
			//Fill the full path
			String fullFilePath = this.backupLocation;
			if(fullFilePath.endsWith(File.separator)){
				fullFilePath += filename;
			}else{
				fullFilePath += File.separator;
				fullFilePath += filename;
			}

			//Execute the Backup
			try{
				String[] backupScript;
				switch(Common.databaseProxy.getType()){
				case H2:
					backupScript = new String[]{"SCRIPT DROP TO '" + fullFilePath + "' COMPRESSION ZIP;" };
					break;
				case DERBY:
				case MSSQL:
				case MYSQL:
				case POSTGRES:
				default:
					LOG.warn("Unable to backup database, because no script for type: " + Common.databaseProxy.getType());
					return;
					
				}
				OutputStream out = createLogOutputStream();
				Common.databaseProxy.runScript(backupScript, out);
				
				File file = new File(fullFilePath);
				if(!file.exists())
					if(!file.createNewFile()){
						LOG.warn("Unable to create backup file: " + fullFilePath);
			            SystemEventType.raiseEvent(new SystemEventType(SystemEventType.TYPE_BACKUP_FAILURE),
			                    Common.backgroundProcessing.currentTimeMillis(), false,
			                    new TranslatableMessage("event.backup.failure", fullFilePath, "Unable to create backup file"));
	
						return;
					}
				//Store the last successful backup time
				SystemSettingsDao dao = new SystemSettingsDao();
				dao.setValue(SystemSettingsDao.DATABASE_BACKUP_LAST_RUN_SUCCESS,runtimeString);
				
				//Clean up old files, keeping the correct number as the history
				File[] files = getBackupFiles(this.backupLocation);
				
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
			}
		}
	}

    protected OutputStream createLogOutputStream() {
        String dir = Common.envProps.getString("db.update.log.dir", "");
        dir = StringUtils.replaceMacros(dir, System.getProperties());

        File logDir = new File(dir);
        File logFile = new File(logDir, getClass().getName() + ".log");
        LOG.info("Writing upgrade log to " + logFile.getAbsolutePath());

        try {
            if (logDir.isDirectory() && logDir.canWrite())
                return new FileOutputStream(logFile);
        }
        catch (Exception e) {
            LOG.error("Failed to create database backup log file.", e);
        }

        LOG.warn("Failing over to console for printing database backup messages");
        return System.out;
    }
	

	/**
	 * Timer task that uses this Backup Work Item in its execution
	 * @author tpacker
	 *
	 */
    static class DatabaseBackupTask extends RejectableTimerTask {
    	DatabaseBackupTask(String cronTrigger) throws ParseException {
            super(new CronTimerTrigger(cronTrigger), "Database backup", "DatabaseBackup", 0, true);
        }

        @Override
        public void run(long runtime) {

        	//Should we run the backup?
        	try{
        		String lastRunDateString = SystemSettingsDao.getValue(SystemSettingsDao.DATABASE_BACKUP_LAST_RUN_SUCCESS);
	        	String backupLocation = SystemSettingsDao.getValue(SystemSettingsDao.DATABASE_BACKUP_FILE_LOCATION);

	        	//Have we ever run?
	        	if(lastRunDateString != null){
	        		Date lastRunDate = dateFormatter.parse(lastRunDateString);
	        		DateTime lastRun = new DateTime(lastRunDate);
	        		//Compute the next run time off of the last run time
		            DateTime nextRun = DateUtils.plus(lastRun, SystemSettingsDao.getIntValue(SystemSettingsDao.DATABASE_BACKUP_PERIOD_TYPE),
		                    SystemSettingsDao.getIntValue(SystemSettingsDao.DATABASE_BACKUP_PERIODS));
		        	//Is the next run time now or before now?
		            if(!nextRun.isAfter(runtime)){
			        	DatabaseBackupWorkItem.queueBackup(backupLocation);
		            }

	        	}else{
		        	DatabaseBackupWorkItem.queueBackup(backupLocation);
	        	}
        	}catch(Exception e){
        		LOG.error(e);
	            SystemEventType.raiseEvent(new SystemEventType(SystemEventType.TYPE_BACKUP_FAILURE),
	                    Common.backgroundProcessing.currentTimeMillis(), false,
	                    new TranslatableMessage("event.backup.failure", "no file", e.getMessage()));

        	}
        }//end run
    }// end backup settings task


	/**
	 * @return
	 */
	public static File[] getBackupFiles(String backupLocation) {
		File backupDir = new File(backupLocation);
		String nameStart = "core-database-" + Common.databaseProxy.getType();
		final String lowerNameStart = nameStart.toLowerCase();
		File[] files = backupDir.listFiles(new FilenameFilter(){
			public boolean accept(File dir, String name){
				return name.toLowerCase().startsWith(lowerNameStart);
			}
		});
		return files;
	}

	/**
	 * Restore a database from a backup
	 * @param file
	 */
	public static ProcessResult restore(String filename) {
		ProcessResult result = new ProcessResult();
		
		//Fill the full path
		String fullFilePath = SystemSettingsDao.getValue(SystemSettingsDao.DATABASE_BACKUP_FILE_LOCATION);

		if(fullFilePath.endsWith(File.separator)){
			fullFilePath += filename;
		}else{
			fullFilePath += File.separator;
			fullFilePath += filename;
		}
		
		File file = new File(fullFilePath);
		if(file.exists()&&file.canRead()){
			
			try{
				String[] backupScript;
				switch(Common.databaseProxy.getType()){
				case H2:
					backupScript = new String[]{"RUNSCRIPT FROM '" + fullFilePath + "' COMPRESSION ZIP;" };
					break;
				case DERBY:
				case MSSQL:
				case MYSQL:
				case POSTGRES:
				default:
					LOG.warn("Unable to backup database, because no script for type: " + Common.databaseProxy.getType());
					result.addMessage(new TranslatableMessage("systemSettings.databaseRestoreNotSupported", Common.databaseProxy.getType()));
					return result;
				}
				//TODO Create a stream to print to the result
				Common.databaseProxy.runScript(backupScript, System.out);
			}catch(Exception e){
				LOG.error(e.getMessage(),e);
				result.addMessage(new TranslatableMessage("common.default", e.getMessage()));
			}
			
			
		}
		
		return result;
	}

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.rt.maint.work.WorkItem#getDescription()
	 */
	@Override
	public String getDescription() {
		return "Backing up database to " + this.backupLocation;
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
	 * @see com.serotonin.m2m2.rt.maint.work.WorkItem#isQueueable()
	 */
	@Override
	public boolean isQueueable() {
		return true;
	}
	
	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.rt.maint.work.WorkItem#rejected(com.serotonin.timer.RejectedTaskReason)
	 */
	@Override
	public void rejected(RejectedTaskReason reason) { }

}
