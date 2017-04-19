/*
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.rt.script;

import java.io.PrintWriter;
import java.util.Map;

import javax.script.Bindings;
import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.ScriptEngine;
import javax.script.ScriptException;

import com.serotonin.m2m2.rt.dataImage.IDataPointValueSource;
import com.serotonin.m2m2.rt.dataImage.PointValueTime;

/**
 * @author Matthew Lohbihler
 */
public class CompiledScriptExecutor extends ScriptExecutor{
    private static final String SCRIPT_PREFIX = "function __scriptExecutor__() {";
    private static final String SCRIPT_SUFFIX = "\r\n}\r\n";
    private static final String SCRIPT_POSTFIX = "\r\n__scriptExecutor__();";
    private static ScriptEngine ENGINE;

    /**
     * Compile a script for the common Engine
     * @param script
     * @return
     * @throws ScriptException
     */
    public static CompiledScript compile(String script) throws ScriptException {
//    	StopWatch stopWatch = new Log4JStopWatch();
//		stopWatch.start();
    	script = SCRIPT_PREFIX + script + SCRIPT_SUFFIX + ScriptUtils.getGlobalFunctions() + SCRIPT_POSTFIX;
        //TODO Review change 
//        ensureInit();
//        CompiledScript compiledScript = ((Compilable) ENGINE).compile(script);
        
    	ScriptEngine engine = ScriptUtils.newEngine();
    	ScriptUtils.prepareEngine(engine);
    	CompiledScript compiledScript = ((Compilable)engine).compile(script);
        
//        stopWatch.stop("compile(script)");
        return compiledScript;
    }

    /**
     * Execute the script on the common engine
     * @param script
     * @param context
     * @param additionalContext
     * @param runtime
     * @param dataTypeId
     * @param timestamp
     * @param permissions
     * @param scriptWriter
     * @return
     * @throws ScriptException
     * @throws ResultTypeException
     */
    public static PointValueTime execute(CompiledScript script, Map<String, IDataPointValueSource> context,
           Map<String, Object> additionalContext, long runtime, int dataTypeId, long timestamp, 
           ScriptPermissions permissions, PrintWriter scriptWriter, ScriptLog log) throws ScriptException, ResultTypeException {
       
//    	StopWatch stopWatch = new Log4JStopWatch();
//		stopWatch.start();
    	ensureInit();

        // Create the wrapper object context.
        ScriptEngine engine = script.getEngine();

        //Prepare the Engine
        Bindings engineScope = prepareEngine(engine, context, additionalContext, runtime, permissions, scriptWriter, log);
        
        // Execute.
        Object result;
        try {
            result = script.eval(engineScope);
        }
        catch (ScriptException e) {
            throw prettyScriptMessage(e);
        }

        PointValueTime value = getResult(engine, result, dataTypeId, timestamp);
    	//stopWatch.stop("execute()");
        return value;
    }
    
    


    /**
     * Execute a script on the common engine
     * @param script
     * @param context
     * @param runtime
     * @param dataTypeId
     * @param timestamp
     * @param permissions
     * @return
     * @throws ScriptException
     * @throws ResultTypeException
     */
//    public static PointValueTime execute(CompiledScript script, Map<String, IDataPointValueSource> context,
//            long runtime, int dataTypeId, long timestamp, ScriptPermissions permissions) throws ScriptException, ResultTypeException {
//    	return execute(script, context, null, runtime, dataTypeId, timestamp, permissions, null, null);
//    }

    /**
     * Ensure the Engine is Ready, this Engine is used by all the 
     * Scripts to run them.
     * @throws ScriptException
     */
    private static void ensureInit() throws ScriptException {
        if (ENGINE == null) {
            // Create the script engine.
            try {
            	ENGINE = ScriptUtils.newEngine();
            	ScriptUtils.prepareEngine(ENGINE);
            }
            catch (Exception e) {
                throw new ScriptException(e);
            }
        }
    }
}
