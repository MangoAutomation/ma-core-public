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

import org.perf4j.StopWatch;
import org.perf4j.log4j.Log4JStopWatch;

import com.serotonin.m2m2.rt.dataImage.IDataPointValueSource;
import com.serotonin.m2m2.rt.dataImage.PointValueTime;

/**
 * @author Matthew Lohbihler
 */
public class CompiledScriptExecutor extends ScriptExecutor{
    private static final String SCRIPT_PREFIX = "function __scriptExecutor__() {";
    private static final String SCRIPT_SUFFIX = "\r\n}\r\n__scriptExecutor__();";
    private static ScriptEngine ENGINE;

    /**
     * Compile a script for the common Engine
     * @param script
     * @return
     * @throws ScriptException
     */
    public static CompiledScript compile(String script) throws ScriptException {
    	StopWatch stopWatch = null;
    	if(useMetrics)
    		stopWatch = new Log4JStopWatch();

        script =  ScriptUtils.getGlobalFunctions() + SCRIPT_PREFIX + script + SCRIPT_SUFFIX ;

        ScriptEngine engine = ScriptUtils.newEngine();
    	ScriptUtils.prepareEngine(engine);
    	CompiledScript compiledScript = ((Compilable)engine).compile(script);

    	if(useMetrics)
    		stopWatch.stop("compile(String)");
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
       
    	StopWatch stopWatch = null;
    	if(useMetrics)
    		stopWatch = new Log4JStopWatch();
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
    	if(useMetrics)
    		stopWatch.stop("execute(CompiledScript)");
        return value;
    }

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
