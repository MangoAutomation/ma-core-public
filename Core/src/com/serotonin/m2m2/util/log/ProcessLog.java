package com.serotonin.m2m2.util.log;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
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

public class ProcessLog {
    private static final Log LOG = LogFactory.getLog(ProcessLog.class);

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

    public static enum LogLevel {
        TRACE("TRACE"), DEBUG("DEBUG"), INFO("INFO "), WARN("WARN "), ERROR("ERROR"), FATAL("FATAL");

        public final String logName;

        private LogLevel(String logName) {
            this.logName = logName;
        }
    }

    private static final String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss,SSS";

    protected final SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT);
    protected final String id;
    protected PrintWriter out;
    protected LogLevel logLevel;
    protected File file;

    public ProcessLog(String id, LogLevel logLevel) {
        this(id, logLevel, null);
    }
    
    public ProcessLog(String id, LogLevel logLevel, PrintWriter out) {
    	this(id, logLevel, out, false);
    }
    
    public ProcessLog(String id, LogLevel logLevel, PrintWriter out, boolean deleteExisting) {
        this.id = id;

        if (logLevel == null)
            this.logLevel = LogLevel.INFO;
        else
            this.logLevel = logLevel;

        file = new File(Common.getLogsDir(), "processLog." + id + ".log");
        if (file.exists() && deleteExisting)
            file.delete();
        
        if (out == null)
        	createOut();
        else
        	this.out = out;

        processLogs.add(this);
    }

    public File getFile(){
    	return file;
    }
    
    public void close() {
        out.close();
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

    public boolean trouble() {
        return out.checkError();
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

    private void log(String s, Throwable t, LogLevel level) {
        if (level.ordinal() < logLevel.ordinal())
            return;
        
        synchronized (out) {
            //Check to roll
            sizeCheck();
            
            out.append(level.logName).append(' ');
            out.append(sdf.format(new Date())).append(" (");
            StackTraceElement e = new RuntimeException().getStackTrace()[2];
            out.append(e.getClassName()).append('.').append(e.getMethodName()).append(':')
                    .append(Integer.toString(e.getLineNumber())).append(") - ");
            out.println(s);
            if (t != null)
                t.printStackTrace(out);
            out.flush();
        }
    }

    /**
     * Check the size of the log file and perform adjustments
     * as necessary
     */
    protected void sizeCheck() { }
    
    /**
     * Create the Print Writer output
     */
    protected void createOut() {
        try {
            out = new PrintWriter(new FileWriter(file, true));
        }
        catch (IOException e) {
            out = new PrintWriter(new NullWriter());
            LOG.error("Error while creating process log", e);
        }
    }
    
    public static void main(String[] args) {
        new ProcessLog("test", LogLevel.DEBUG, new PrintWriter(System.out)).info("test");
    }
}
