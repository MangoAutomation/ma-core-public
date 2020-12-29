/*
 * Copyright (C) 2019 Infinite Automation Software. All rights reserved.
 */
package com.serotonin.m2m2.util.log;

import java.io.Closeable;
import java.io.File;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.commons.io.output.NullWriter;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.serotonin.m2m2.Common;

/**
 * Logger and container for tracking all logs.  
 *
 */
public class ProcessLog implements Closeable {
    private static final Log LOG = LogFactory.getLog(ProcessLog.class);

    private static final Object lock = new Object();
    private static List<ProcessLog> processLogs = new CopyOnWriteArrayList<ProcessLog>();

    public static List<String> getProcessLogIds() {
        List<String> ids = new ArrayList<String>();
        for (ProcessLog pl : processLogs)
            ids.add(pl.getId());
        return ids;
    }

    public static boolean setLogLevel(String id, LogLevel logLevel) {
        if (logLevel != null) {
            for (ProcessLog pl : processLogs) {
                if (StringUtils.equals(pl.getId(), id)) {
                    pl.setLogLevel(logLevel);
                    pl.log("Log level changed to " + logLevel.name(), null, logLevel);
                    return true;
                }
            }
        }
        return false;
    }
    
    public static List<ProcessLog> getProcessLogs(){
    	return processLogs;
    }

    private static final String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss,SSS";

    protected final SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT);
    protected final String id;
    protected PrintWriter out;
    protected LogLevel logLevel;
    protected File file;
    protected final boolean includeLocationInfo;  //Write class, method and line numbers
    
    //Rolling Members
    protected boolean roll;
    protected int fileSize;
    protected int maxFiles;
    protected int currentFileNumber;

    /**
     * Shortcut for Null Writer
     * @param id
     */
    public ProcessLog(String id) {
        this("processLog.", id, LogLevel.NONE, false, new PrintWriter(new NullWriter()) ,true);
    }
    
    /**
     * Create a Log Writer using the supplied writer
     * 
     * @param id
     * @param logLevel
     * @param includeLocationInfo
     * @param out
     */
    public ProcessLog(String id, LogLevel logLevel, boolean includeLocationInfo, PrintWriter out) {
    	this("processLog.", id, logLevel, includeLocationInfo, out, false);
    }
    
    public ProcessLog(String id, LogLevel logLevel, boolean includeLocationInfo, int fileSize, int maxFiles) {
        this("processLog.", id, logLevel, includeLocationInfo, fileSize, maxFiles);
    }
    
    /**
     * Construct a rolling Process Log in the ${paths.logs}
     * Name: prefix + id + ".log"
     * 
     * @param prefix
     * @param id
     * @param logLevel
     * @param includeLocation
     * @param fileSize
     * @param maxFiles
     */
    public ProcessLog(String prefix, String id, LogLevel logLevel, boolean includeLocationInfo, int fileSize, int maxFiles) {
        this(prefix, id, logLevel, includeLocationInfo, null, false);
        this.roll = true;
        this.fileSize = fileSize;
        this.maxFiles = maxFiles;
    }
    
    /**
     * Create a non-rolling Process Log in ${paths.logs}
     * 
     * Name: prefix + id + ".log"
     * 
     * @param prefix
     * @param id
     * @param logLevel
     * @param includeLocation
     * @param out
     * @param deleteExisting
     */
    public ProcessLog(String prefix, String id, LogLevel logLevel, boolean includeLocationInfo, PrintWriter out, boolean deleteExisting) {
        this.id = id;
        this.includeLocationInfo = includeLocationInfo;
        if (logLevel == null)
            this.logLevel = LogLevel.INFO;
        else
            this.logLevel = logLevel;

        file = new File(Common.getLogsDir(), prefix == null ? id + ".log" : prefix + id + ".log");
        if (file.exists() && deleteExisting)
            file.delete();
        
        if (out == null)
        	createOut();
        else
        	this.out = out;

        if(!(this.out instanceof NullPrintWriter))
            processLogs.add(this);
    }
    
    /**
     * Get the file currently being written to
     * @return
     */
    public File getFile(){
    	return file;
    }
    
    public PrintWriter getPrintWriter() {
        return out;
    }
    
    public void close() {
        out.close();
        if(!(out instanceof NullPrintWriter))
            processLogs.remove(this);
    }

    public String getId() {
        return id;
    }

    public LogLevel getLogLevel() {
        return logLevel;
    }

    public void setLogLevel(LogLevel logLevel) {
        this.logLevel = logLevel;
    }

    //
    // Trace
    public boolean isTraceEnabled() {
        return logLevel.ordinal() <= LogLevel.TRACE.ordinal();
    }

    public void trace(String s) {
        log(s, null, LogLevel.TRACE);
    }

    public void trace(Throwable t) {
        log(null, t, LogLevel.TRACE);
    }

    public void trace(String s, Throwable t) {
        log(s, t, LogLevel.TRACE);
    }

    //
    // Debug
    public boolean isDebugEnabled() {
        return logLevel.ordinal() <= LogLevel.DEBUG.ordinal();
    }

    public void debug(String s) {
        log(s, null, LogLevel.DEBUG);
    }

    public void debug(Throwable t) {
        log(null, t, LogLevel.DEBUG);
    }

    public void debug(String s, Throwable t) {
        log(s, t, LogLevel.DEBUG);
    }

    //
    // Info
    public boolean isInfoEnabled() {
        return logLevel.ordinal() <= LogLevel.INFO.ordinal();
    }

    public void info(String s) {
        log(s, null, LogLevel.INFO);
    }

    public void info(Throwable t) {
        log(null, t, LogLevel.INFO);
    }

    public void info(String s, Throwable t) {
        log(s, t, LogLevel.INFO);
    }

    //
    // Warn
    public boolean isWarnEnabled() {
        return logLevel.ordinal() <= LogLevel.WARN.ordinal();
    }

    public void warn(String s) {
        log(s, null, LogLevel.WARN);
    }

    public void warn(Throwable t) {
        log(null, t, LogLevel.WARN);
    }

    public void warn(String s, Throwable t) {
        log(s, t, LogLevel.WARN);
    }

    //
    // Error
    public boolean isErrorEnabled() {
        return logLevel.ordinal() <= LogLevel.ERROR.ordinal();
    }

    public void error(String s) {
        log(s, null, LogLevel.ERROR);
    }

    public void error(Throwable t) {
        log(null, t, LogLevel.ERROR);
    }

    public void error(String s, Throwable t) {
        log(s, t, LogLevel.ERROR);
    }

    //
    // Fatal
    public boolean isFatalEnabled() {
        return logLevel.ordinal() <= LogLevel.FATAL.ordinal();
    }

    public void fatal(String s) {
        log(s, null, LogLevel.FATAL);
    }

    public void fatal(Throwable t) {
        log(null, t, LogLevel.FATAL);
    }

    public void fatal(String s, Throwable t) {
        log(s, t, LogLevel.FATAL);
    }

    protected void log(String s, Throwable t, LogLevel level) {
        if (level.ordinal() < logLevel.ordinal())
            return;
        
        synchronized (lock) {
            //Check to roll
            if(roll)
                sizeCheck();
            
            out.append(level.name());
            out.append(' ');
            out.append(sdf.format(new Date()));
            if(includeLocationInfo) {
                out.append(" (");
                StackTraceElement e = new RuntimeException().getStackTrace()[2];
                out.append(e.getClassName()).append('.').append(e.getMethodName()).append(':')
                        .append(Integer.toString(e.getLineNumber())).append(") - ");
            }else {
                out.append(" - ");
            }
            
            out.println(s == null ? "null" : s.toString());
            if (t != null)
                t.printStackTrace(out);
            out.flush();
        }
    }

    /**
     * Check the size of the log file and perform adjustments
     * as necessary
     */
    protected void sizeCheck() {
        if(logLevel == LogLevel.NONE)
            return;
        // Check if the file should be rolled.
        if (file.length() > this.fileSize) {
            out.close();

            try{
                //Do rollover
                for(int i=this.currentFileNumber; i>0; i--){
                    Path source = Paths.get( this.file.getAbsolutePath() + "." + i);
                    Path target = Paths.get(this.file.getAbsolutePath() + "." + (i + 1));
                    Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
                }
                
                Path source = Paths.get(this.file.toURI());
                Path target = Paths.get(this.file.getAbsolutePath() + "." + 1);
                Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
                
                if(this.currentFileNumber < this.maxFiles - 1){
                    //Use file number
                    this.currentFileNumber++;
                }
                
            }catch(IOException e){
                LOG.error(e.getMessage(), e);
            }
             
            createOut();
        }
    }
    
    /**
     * Create the Print Writer output
     */
    protected void createOut() {
        try {
            if(logLevel == LogLevel.NONE)
                out = new PrintWriter(new NullWriter());
            else
                out = new PrintWriter(new FileWriter(file, true));
        }
        catch (IOException e) {
            out = new PrintWriter(new NullWriter());
            LOG.error("Error while creating process log", e);
        }
    }
    
    public boolean trouble() {
        synchronized (lock) {
            return out.checkError();
        }
    }
    
    /**
     * List all the files
     * @return
     */
    public File[] getFiles(){
        if(roll) {
            File[] files = Common.getLogsDir().listFiles(new LogFilenameFilter(file.getName()));
            return files;
        }else {
            return new File [] {file};
        }
    }
    
    /**
     * Class to filter log filenames from a directory listing
     * @author Terry Packer
     *
     */
    class LogFilenameFilter implements FilenameFilter{
        
        private String nameToMatch;
        
        public LogFilenameFilter(String nameToMatch){
            this.nameToMatch = nameToMatch;
        }

        @Override
        public boolean accept(File dir, String name) {
            return name.startsWith(this.nameToMatch);
        }
        
    }
}
