/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.rt.script;

import java.io.Closeable;
import java.io.PrintWriter;

import org.apache.commons.io.output.NullWriter;

import com.serotonin.m2m2.util.log.LogLevel;
import com.serotonin.m2m2.util.log.NullPrintWriter;
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
    
    /**
     * Create a null log writer, useful for when logging is set to NONE
     */
    public ScriptLog(String id) {
        super(null, id, LogLevel.NONE, false, new NullPrintWriter() ,true);
    }
    
    /**
     * Create a non rolling logger with a PrintWriter, useful 
     * for writing to a String, Stream etc.
     */
    public ScriptLog(String id, LogLevel level, PrintWriter out) {
        super(null, id, level, false, out, true);
    }
    
    /**
     * Construct a rolling script log in logs dir.
     * Name: id + ".log" 
     * 
     * @param id - Unique to all Process/Script logs
     */
    public ScriptLog(String id, LogLevel level, int logSize, int logCount) {
        super(null, id, level, false, logSize, logCount);
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
