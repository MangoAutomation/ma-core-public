package com.serotonin.m2m2.rt.maint.work;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.ProcessBuilder.Redirect;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

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
import com.serotonin.m2m2.rt.event.type.definition.BackupSuccessEventTypeDefinition;
import com.serotonin.m2m2.util.DateUtils;
import com.serotonin.timer.CronTimerTrigger;
import com.serotonin.timer.RejectedTaskReason;
import com.serotonin.timer.TimerTask;

public class DatabaseBackupWorkItem implements WorkItem {
    private static final Log LOG = LogFactory.getLog(DatabaseBackupWorkItem.class);
    // Used for filename and property value for last run
    public static final String BACKUP_DATE_FORMAT = "MMM-dd-yyyy_HHmmss";

    // Lock to ensure we don't clobber files by running a backup
    // during another one.
    public static final Object lock = new Object();

    private static final String TASK_ID = "DatabaseBackupTask";

    private static DatabaseBackupTask task; // Static holder to re-schedule task
    // if necessary
    private String backupLocation; // Location of backup directory on disk
    private volatile boolean finished = false;
    private volatile boolean cancelled = false;
    private volatile boolean failed = false;
    private String filename;
    private final CompletableFuture<Void> completed = new CompletableFuture<Void>();

    /**
     * Statically Schedule a Timer Task that will run this work item.
     *
     * TODO For Startup Make this an RTM and add to the RuntimeManager's list of
     * startup RTMS currently only the Module RTMs get started there. For now
     * this call is used within the RuntimeManager
     */
    public static void schedule() {
        try {
            // Test trigger for running every 25 seconds.
            // String cronTrigger = "0/25 * * * * ?";

            // Trigger to run at Set Hour/Minute "s m h * * ?";
            int hour = SystemSettingsDao.instance.getIntValue(SystemSettingsDao.DATABASE_BACKUP_HOUR);
            int minute = SystemSettingsDao.instance.getIntValue(SystemSettingsDao.DATABASE_BACKUP_MINUTE);

            String cronTrigger = "0 " + minute + " " + hour + " * * ?";
            task = new DatabaseBackupTask(cronTrigger);
            Common.timer.schedule(task);
        } catch (ParseException e) {
            throw new ShouldNeverHappenException(e);
        }
    }

    /**
     * Safely unschedule the timer task for this work item
     */
    public static void unschedule() {
        if (task != null) {
            task.cancel();
        }
    }

    @Override
    public int getPriority() {
        return WorkItem.PRIORITY_MEDIUM;
    }

    /**
     * To know when the task is completed
     * @return
     */
    public Future<Void> getCompletableFuture() {
        return completed;
    }

    /**
     * Queue a backup for execution
     *
     * @param backupLocation
     */
    public static Future<Void> queueBackup(String backupLocation) {
        DatabaseBackupWorkItem item = new DatabaseBackupWorkItem();
        item.backupLocation = backupLocation;
        Common.backgroundProcessing.addWorkItem(item);
        return item.getCompletableFuture();
    }

    @Override
    public void execute() {
        synchronized (lock) {
            LOG.info("Starting database backup WorkItem.");

            // Create the filename
            String filename = "core-database-" + Common.databaseProxy.getType();
            SimpleDateFormat dateFormatter = new SimpleDateFormat(BACKUP_DATE_FORMAT);
            String runtimeString = dateFormatter.format(new Date());
            int maxFiles = SystemSettingsDao.instance.getIntValue(SystemSettingsDao.DATABASE_BACKUP_FILE_COUNT);
            // If > 1 then we will use a date in the filename
            if (maxFiles > 1) {
                // Create Mango-Configuration-date.json
                filename += "-";
                filename += runtimeString;
            }

            Path backupFilePath = Paths.get(this.backupLocation).resolve(filename + ".zip").toAbsolutePath().normalize();
            this.filename = backupFilePath.toString();

            if(cancelled)
                return;

            // Execute the Backup
            try {

                switch (Common.databaseProxy.getType()) {
                    case H2:
                        String[] backupScript = new String[] { "SCRIPT DROP TO '" + this.filename + "' COMPRESSION ZIP;" };
                        try (OutputStream out = Common.databaseProxy.createLogOutputStream(this.getClass())) {
                            Common.databaseProxy.runScript(backupScript, out);
                        }
                        break;
                    case MYSQL:
                        String dumpExePath = Common.envProps.getString("db.mysqldump", "mysqldump");
                        // Of the form: jdbc:mysql://localhost/mango2712
                        // or: jdbc:mysql://localhost:3306/sakila?profileSQL=true
                        String cnxn = Common.envProps.getString("db.url");
                        String[] parts = cnxn.split("/");
                        String[] hostPort = parts[2].split(":");
                        String host = hostPort[0];
                        String port;
                        if (hostPort.length > 1)
                            port = hostPort[1];
                        else
                            port = "3306";
                        String user = Common.envProps.getString("db.username");
                        String password = Common.databaseProxy.getDatabasePassword("");
                        // Split off any extra stuff on the db
                        String[] dbParts = parts[3].split("\\?");
                        String database = dbParts[0];
                        backupMysqlWithOutDatabase(dumpExePath, host, port, user, password, database, backupFilePath, filename);
                        break;
                    case DERBY:
                    case MSSQL:
                    case POSTGRES:
                    default:
                        LOG.warn(
                                "Unable to backup database, because no script for type: " + Common.databaseProxy.getType());
                        return;

                }

                if (!Files.exists(backupFilePath)) {
                    failed = true;
                    LOG.warn("Unable to create backup file: " + this.filename);
                    backupFailed(this.filename, "Unable to create backup file");
                    return;
                }

                SystemEventType.raiseEvent(new SystemEventType(SystemEventType.TYPE_BACKUP_SUCCESS),
                        Common.timer.currentTimeMillis(), false,
                        new TranslatableMessage("event.backup.success", backupFilePath.getFileName().toString()),
                        Collections.singletonMap(BackupSuccessEventTypeDefinition.BACKUP_PATH_CONTEXT_KEY, backupFilePath));

                // Store the last successful backup time
                SystemSettingsDao.instance.setValue(SystemSettingsDao.DATABASE_BACKUP_LAST_RUN_SUCCESS, runtimeString);

                // Clean up old files, keeping the correct number as the history
                File[] files = getBackupFiles(this.backupLocation);

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
                backupFailed(this.filename, e.getMessage());
            }finally{
                finished = true;
                completed.complete(null);
            }
        }
    }

    private void backupFailed(String fullFilePath, String message) {
        SystemEventType.raiseEvent(new SystemEventType(SystemEventType.TYPE_BACKUP_FAILURE),
                Common.timer.currentTimeMillis(), false, new TranslatableMessage("event.backup.failure",
                        fullFilePath, message));
    }

    /**
     * Timer task that uses this Backup Work Item in its execution
     *
     * @author tpacker
     *
     */
    static class DatabaseBackupTask extends TimerTask {
        DatabaseBackupTask(String cronTrigger) throws ParseException {
            super(new CronTimerTrigger(cronTrigger), "Database backup");
        }

        @Override
        public void run(long runtime) {

            // Should we run the backup?
            try {
                String lastRunDateString = SystemSettingsDao
                        .instance.getValue(SystemSettingsDao.DATABASE_BACKUP_LAST_RUN_SUCCESS);
                String backupLocation = SystemSettingsDao.instance.getValue(SystemSettingsDao.DATABASE_BACKUP_FILE_LOCATION);

                // Have we ever run?
                if (lastRunDateString != null) {
                    SimpleDateFormat dateFormatter = new SimpleDateFormat(BACKUP_DATE_FORMAT);
                    Date lastRunDate;
                    try{
                        lastRunDate = dateFormatter.parse(lastRunDateString);
                    }catch(Exception e) {
                        lastRunDate = new Date();
                        LOG.warn("Failed to parse last backup date, using Jan 1 1970.", e);
                    }
                    DateTime lastRun = new DateTime(lastRunDate);
                    // Compute the next run time off of the last run time
                    DateTime nextRun = DateUtils.plus(lastRun,
                            SystemSettingsDao.instance.getIntValue(SystemSettingsDao.DATABASE_BACKUP_PERIOD_TYPE),
                            SystemSettingsDao.instance.getIntValue(SystemSettingsDao.DATABASE_BACKUP_PERIODS));
                    // Is the next run time now or before now?
                    if (!nextRun.isAfter(runtime)) {
                        DatabaseBackupWorkItem.queueBackup(backupLocation);
                    }

                } else {
                    DatabaseBackupWorkItem.queueBackup(backupLocation);
                }
            } catch (Exception e) {
                LOG.error(e);
                SystemEventType.raiseEvent(new SystemEventType(SystemEventType.TYPE_BACKUP_FAILURE),
                        Common.timer.currentTimeMillis(), false,
                        new TranslatableMessage("event.backup.failure", "no file", e.getMessage()));

            }
        }// end run
    }// end backup settings task

    /**
     * @return
     */
    public static File[] getBackupFiles(String backupLocation) {
        File backupDir = new File(backupLocation);
        String nameStart = "core-database-" + Common.databaseProxy.getType();
        final String lowerNameStart = nameStart.toLowerCase();
        File[] files = backupDir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.toLowerCase(Locale.ROOT).startsWith(lowerNameStart);
            }
        });
        return files;
    }

    /**
     * Restore a database from a backup
     *
     * @param filename
     */
    public static ProcessResult restore(String filename) {
        ProcessResult result = new ProcessResult();

        // Fill the full path
        String fullFilePath = SystemSettingsDao.instance.getValue(SystemSettingsDao.DATABASE_BACKUP_FILE_LOCATION);

        if (fullFilePath.endsWith(File.separator)) {
            fullFilePath += filename;
        } else {
            fullFilePath += File.separator;
            fullFilePath += filename;
        }

        File file = new File(fullFilePath);
        if (file.exists() && file.canRead()) {
            LOG.info("Attempting to restore database backup from: " + fullFilePath);
            try {

                switch (Common.databaseProxy.getType()) {
                    case H2:
                        String[] backupScript = new String[] { "RUNSCRIPT FROM '" + fullFilePath + "' COMPRESSION ZIP;" };
                        // TODO Create a stream to print to the result
                        Common.databaseProxy.runScript(backupScript, System.out);
                        break;
                    case MYSQL:
                        String mySqlPath = Common.envProps.getString("db.mysql", "mysql");
                        // Of the form: jdbc:mysql://localhost/mango2712
                        // or: jdbc:mysql://localhost:3306/sakila?profileSQL=true
                        String cnxn = Common.envProps.getString("db.url");
                        String[] parts = cnxn.split("/");
                        String[] hostPort = parts[2].split(":");
                        String host = hostPort[0];
                        String port;
                        if (hostPort.length > 1)
                            port = hostPort[1];
                        else
                            port = "3306";
                        String user = Common.envProps.getString("db.username");
                        String password = Common.databaseProxy.getDatabasePassword("");
                        // Split off any extra stuff on the db
                        String[] dbParts = parts[3].split("\\?");
                        String database = dbParts[0];
                        result.addMessage(restoreMysqlToDatabase(mySqlPath, host, port, user, password, database, fullFilePath));
                        break;
                    case DERBY:
                    case MSSQL:

                    case POSTGRES:
                    default:
                        LOG.warn(
                                "Unable to restore database, because no script for type: " + Common.databaseProxy.getType());
                        result.addMessage(new TranslatableMessage("systemSettings.databaseRestoreNotSupported",
                                Common.databaseProxy.getType()));
                        return result;
                }
                LOG.info("Database backup restore finished");
            } catch (Exception e) {
                LOG.error(e.getMessage(), e);
                result.addMessage(new TranslatableMessage("common.default", e.getMessage()));
            }

        } else {
            LOG.info("Database backup not found at: " + fullFilePath);
            result.addMessage(new TranslatableMessage("literal", "Database backup not found at: " + fullFilePath));
        }

        return result;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.serotonin.m2m2.rt.maint.work.WorkItem#getDescription()
     */
    @Override
    public String getDescription() {
        return "Backing up database to " + this.backupLocation;
    }

    /**
     * Backup mysql database without the database name implied, easier to import
     * into various systems
     *
     * @param dumpExePath
     * @param host
     * @param port
     * @param user
     * @param password
     * @param database
     * @param backupPath
     * @return
     */
    public void backupMysqlWithOutDatabase(String dumpExePath, String host, String port, String user,
            String password, String database, Path backupPath, String baseFilename) {
        try {
            Process p = null;

            // only backup the data not included create database
            List<String> args = new ArrayList<>();
            args.add(dumpExePath);
            args.add("-h" + (host.isEmpty() ? "localhost" : host));
            args.add("-P" + port);
            args.add("-u" + user);
            args.add("-p" + password);
            args.add(database);

            ProcessBuilder pb = new ProcessBuilder(args);
            pb.redirectError(Redirect.INHERIT);

            Path sqlFile = backupPath.getParent().resolve(baseFilename + ".sql");
            pb.redirectOutput(Redirect.to(sqlFile.toFile()));

            p = pb.start();
            int processComplete = p.waitFor();

            if (processComplete == 0) {
                try {
                    try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(backupPath))) {
                        ZipEntry ze = new ZipEntry(sqlFile.getFileName().toString());
                        zos.putNextEntry(ze);
                        Files.copy(sqlFile, zos);
                        zos.closeEntry();
                    }
                } finally {
                    // Delete the sql file
                    Files.deleteIfExists(sqlFile);
                }
                LOG.info("Backup created successfully for " + database + " in " + host + ":" + port);
            } else {
                LOG.info("Could not create the backup for " + database + " in " + host + ":" + port);
                backupFailed(backupPath.toString(), "Process exit status: "+processComplete);
            }
        } catch (Exception e) {
            LOG.error(e, e.getCause());
            backupFailed(backupPath.toString(), e.getMessage());
        }
    }

    public static TranslatableMessage restoreMysqlToDatabase(String mysqlPath, String host, String port, String user, String password,
            String database, String backupZipFile) {
        TranslatableMessage status = null;
        File sqlFile = new File(backupZipFile.replace(".zip", ".sql"));
        try {
            Process p = null;

            // only backup the data not included create database
            List<String> args = new ArrayList<String>();
            args.add(mysqlPath);
            args.add("-h" + host);
            args.add("-P" + port);
            args.add("-u" + user);
            args.add("-p" + password);
            args.add(database);

            // Unzip the File
            byte[] buffer = new byte[1024];
            try {
                // get the zip file content
                ZipInputStream zis = new ZipInputStream(new FileInputStream(backupZipFile));
                // get the zipped file list entry
                ZipEntry ze = zis.getNextEntry();

                if (ze != null) {
                    FileOutputStream fos = new FileOutputStream(sqlFile);

                    int len;
                    while ((len = zis.read(buffer)) > 0) {
                        fos.write(buffer, 0, len);
                    }

                    fos.close();
                    ze = zis.getNextEntry();
                }

                zis.closeEntry();
                zis.close();
            } catch (IOException ex) {
                LOG.error(ex, ex.getCause());
                return new TranslatableMessage("systemSettings.databaseRestoreFailed", ex.getMessage());
            }

            ProcessBuilder pb = new ProcessBuilder(args);
            pb.redirectError(Redirect.INHERIT);
            pb.redirectInput(Redirect.from(sqlFile));

            p = pb.start();

            int processComplete = p.waitFor();

            if (processComplete == 0) {
                status = new TranslatableMessage("systemSettings.databaseRestored");
                LOG.info("Backup restored successfully for" + database + " in " + host + ":" + port);
            } else {
                status = new TranslatableMessage("systemSettings.databaseRestoreFailed", processComplete);
                LOG.info("Could not restore the backup for " + database + " in " + host + ":" + port);
            }

        } catch (IOException ioe) {
            LOG.error(ioe, ioe.getCause());
            status = new TranslatableMessage("systemSettings.databaseRestoreFailed", ioe.getMessage());
        } catch (Exception e) {
            LOG.error(e, e.getCause());
            new TranslatableMessage("systemSettings.databaseRestoreFailed", e.getMessage());
        }finally{
            if(sqlFile.exists())
                sqlFile.delete();
        }
        return status;
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
        return Common.defaultTaskQueueSize;
    }

    @Override
    public void rejected(RejectedTaskReason reason) {
        this.completed.completeExceptionally(new RuntimeException(reason.getDescription()));
    }

    /**
     * @param location
     */
    public void setBackupLocation(String location) {
        this.backupLocation = location;
    }

    /**
     * @return
     */
    public boolean isFinished() {
        return this.finished;
    }

    public boolean isFailed(){
        return this.failed;
    }

    /**
     *
     */
    public void cancel() {
        this.cancelled = true;
    }

    public String getFilename(){
        return this.filename;
    }
}
