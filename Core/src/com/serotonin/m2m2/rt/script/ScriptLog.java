/*
    Copyright (C) 2018 Infinite Automation Systems Inc. All rights reserved.
    @author Terry Packer
 */
package com.serotonin.m2m2.rt.script;

import java.io.Closeable;
import java.io.PrintWriter;

import org.apache.commons.io.output.NullWriter;

import com.serotonin.ShouldNeverHappenException;
import com.serotonin.m2m2.util.ExportCodes;
import com.serotonin.m2m2.util.log.ProcessLog;

/**
 * Class for logging in Mango Javascript.
 * 
 * Be sure to close() this logger when done as it cleans up the ProcessLog singleton.
 *
 * @author Terry Packer
 */
public class ScriptLog extends ProcessLog implements Closeable {
	
	public static String CONTEXT_KEY = "LOG";
	
    public interface LogLevel {
        int TRACE = 1;
        int DEBUG = 2;
        int INFO = 3;
        int WARN = 4;
        int ERROR = 5;
        int FATAL = 6;
        int NONE = 10;
    }

    public static final ExportCodes LOG_LEVEL_CODES = new ExportCodes();
    static {
        LOG_LEVEL_CODES.addElement(LogLevel.NONE, "NONE", "common.none");
        LOG_LEVEL_CODES.addElement(LogLevel.TRACE, "TRACE", "common.logging.trace");
        LOG_LEVEL_CODES.addElement(LogLevel.DEBUG, "DEBUG", "common.logging.debug");
        LOG_LEVEL_CODES.addElement(LogLevel.INFO, "INFO", "common.logging.info");
        LOG_LEVEL_CODES.addElement(LogLevel.WARN, "WARN", "common.logging.warn");
        LOG_LEVEL_CODES.addElement(LogLevel.ERROR, "ERROR", "common.logging.error");
        LOG_LEVEL_CODES.addElement(LogLevel.FATAL, "FATAL", "common.logging.fatal");
    }

    public static ProcessLog.LogLevel convertLevel(int scriptLogLevel){
        switch(scriptLogLevel) {
            case LogLevel.TRACE:
                return ProcessLog.LogLevel.TRACE;
            case LogLevel.DEBUG:
                return ProcessLog.LogLevel.DEBUG;
            case LogLevel.INFO:
                return ProcessLog.LogLevel.INFO;
            case LogLevel.WARN:
                return ProcessLog.LogLevel.WARN;
            case LogLevel.FATAL:
                return ProcessLog.LogLevel.FATAL;
            case LogLevel.NONE:
            default:
                throw new ShouldNeverHappenException("NONE level logging should use a ProcessLog with NullWriter");
        }
    }
    
    /**
     * Create a null log writer, useful for when logging is set to NONE
     * @param id
     */
    public ScriptLog(String id) {
        super(null, id, ProcessLog.LogLevel.FATAL, false, new PrintWriter(new NullWriter()) ,true);
    }
    
    /**
     * Create a non rolling logger with a PrintWriter, useful 
     * for writing to a String, Stream etc.
     * @param id
     * @param level
     * @param out
     */
    public ScriptLog(String id, int level, PrintWriter out) {
        super(null, id, convertLevel(level), false, out, true);
    }
    
    /**
     * Construct a rolling script log in logs dir.
     * Name: id + ".log" 
     * 
     * @param id - Unique to all Process/Script logs
     * @param level
     * @param logSize
     * @param logCount
     */
    public ScriptLog(String id, int level, int logSize, int logCount) {
        super(null, id, convertLevel(level), false, logSize, logCount);
    }

    public void trace(Object o) {
        if(o != null)
            trace(o.toString());
        else
            trace((String)null);
    }

    public void debug(Object o) {
        if(o != null)
            debug(o.toString());
        else
            debug((String)null);
    }

    public void info(Object o) {
        if(o != null)
            info(o.toString());
        else
            info((String)null);
    }

    public void warn(Object o) {
        if(o != null)
            warn(o.toString());
        else
            warn((String)null);
    }

    public void error(Object o) {
        if(o != null)
            error(o.toString());
        else
            error((String)null);
    }

    public void fatal(Object o) {
        if(o != null)
            fatal(o.toString());
        else
            fatal((String)null);
    }
    
    public PrintWriter getStdOutWriter() {
        if(roll)
            return new PrintWriter(new NullWriter());
        else
            return out;
    }
}
