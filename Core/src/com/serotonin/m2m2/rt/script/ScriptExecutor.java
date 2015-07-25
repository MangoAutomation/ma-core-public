/*
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.rt.script;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptException;

import org.perf4j.StopWatch;
import org.perf4j.log4j.Log4JStopWatch;

import com.serotonin.db.pair.IntStringPair;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.rt.dataImage.DataPointRT;
import com.serotonin.m2m2.rt.dataImage.IDataPointValueSource;
import com.serotonin.m2m2.rt.dataImage.PointValueTime;
import com.serotonin.m2m2.rt.dataImage.types.DataValue;

/**
 * @author Matthew Lohbihler
 */
public class ScriptExecutor {
    protected static final String SCRIPT_PREFIX = "function __scriptExecutor__() {";
    protected static final String SCRIPT_SUFFIX = "\r\n}\r\n__scriptExecutor__();";

    @Deprecated //Use convertScriptContext Instead
    public Map<String, IDataPointValueSource> convertContext(List<IntStringPair> context)
            throws DataPointStateException {
        Map<String, IDataPointValueSource> converted = new HashMap<String, IDataPointValueSource>();
        for (IntStringPair contextEntry : context) {
            DataPointRT point = Common.runtimeManager.getDataPoint(contextEntry.getKey());
            if (point == null)
                throw new DataPointStateException(contextEntry.getKey(), new TranslatableMessage(
                        "event.meta.pointMissing"));
            converted.put(contextEntry.getValue(), point);
        }

        return converted;
    }

    /**
     * @param context
     * @return
     * @throws DataPointStateException
     */
    public static Map<String, IDataPointValueSource> convertScriptContext(List<ScriptContextVariable> context)
            throws DataPointStateException {
        Map<String, IDataPointValueSource> converted = new HashMap<String, IDataPointValueSource>();
        for (ScriptContextVariable contextEntry : context) {
            DataPointRT point = Common.runtimeManager.getDataPoint(contextEntry.getDataPointId());
            if (point == null)
                throw new DataPointStateException(contextEntry.getDataPointId(), new TranslatableMessage(
                        "event.meta.pointMissing"));
            converted.put(contextEntry.getVariableName(), point);
        }

        return converted;
    }
    
    /**
     * Execute the script 
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
    public PointValueTime execute(String script, Map<String, IDataPointValueSource> context, 
    		Map<String, Object> additionalContext, long runtime,
            int dataTypeId, long timestamp, 
            ScriptPermissions permissions, 
            PrintWriter scriptWriter, ScriptLog log) throws ScriptException, ResultTypeException {

    	StopWatch stopWatch = new Log4JStopWatch();
		stopWatch.start();

    	// Create the script engine.
        ScriptEngine engine = ScriptUtils.newEngine();

        //Prepare the Engine
        Bindings engineScope = prepareEngine(engine, context, additionalContext, runtime, permissions, scriptWriter, log);
        engine.setBindings(engineScope, ScriptContext.ENGINE_SCOPE);
        
        // Create the script.
        script = SCRIPT_PREFIX + script + SCRIPT_SUFFIX + ScriptUtils.getGlobalFunctions();

        // Execute.
        Object result;
        try {
            result = engine.eval(script);
        }
        catch (ScriptException e) {
            throw prettyScriptMessage(e);
        }

        PointValueTime value = getResult(engine, result, dataTypeId, timestamp);
        stopWatch.stop("execute()");
        return value;
    }

    /**
     * Common method to extract the result
     * @param engine
     * @param result
     * @param dataTypeId
     * @param timestamp
     * @return
     * @throws ResultTypeException
     */
    protected static PointValueTime getResult(ScriptEngine engine, Object result, int dataTypeId, long timestamp) throws ResultTypeException{
        // Check if a timestamp was set
        Object ts = engine.getBindings(ScriptContext.GLOBAL_SCOPE).get(ScriptUtils.TIMESTAMP_CONTEXT_KEY);
        if (ts != null) {
            // Check the type of the object.
            if (ts instanceof Number)
                // Convert to long
                timestamp = ((Number) ts).longValue();
            // else if (ts instanceof ScriptableObject && "Date".equals(((ScriptableObject)ts).getClassName())) {
            // // A native date
            // // It turns out to be a crazy hack to try and get the value from a native date, and the Rhino source
            // // code FTP server is not responding, so, going to have to leave this for now.
            // }
        }

        DataValue value = ScriptUtils.coerce(result, dataTypeId);
        return new PointValueTime(value, timestamp);
        
    }
    
    /**
     * Prepare the engine by creating the context
     * @param engine
     * @param context
     * @param additionalContext
     * @param runtime
     * @param permissions
     * @param scriptWriter
     * @return
     */
    protected static Bindings prepareEngine(ScriptEngine engine, Map<String, 
    		IDataPointValueSource> context, Map<String, Object> additionalContext,
    		long runtime, ScriptPermissions permissions, 
    		PrintWriter scriptWriter, ScriptLog log){
        
    	ScriptUtils.prepareEngine(engine);
    	
    	ScriptUtils.wrapperContext(engine, new WrapperContext(runtime));
        Bindings engineScope = engine.createBindings(); //new SimpleBindings();

        //Add Permissions Required Utilities
        //TODO Bubble PointValueSetter back up to top
        if(permissions != null)
        	ScriptUtils.prepareUtilities(permissions, engine, engineScope, null);
        
        if(additionalContext != null){
        	Set<Entry<String,Object>> entries = additionalContext.entrySet();
        	for(Entry<String,Object> entry: entries)
        		engineScope.put(entry.getKey(), entry.getValue());
        }
                
        // Put the context variables into the engine with engine scope.
        for (String varName : context.keySet()) {
            IDataPointValueSource point = context.get(varName);
            engineScope.put(varName, ScriptUtils.wrapPoint(engine, point));
         }
        
        //Set the print writer if necessary
        if(scriptWriter != null){
        	engine.getContext().setWriter(scriptWriter);
        	engineScope.put(ScriptLog.CONTEXT_KEY, log);
        }
        return engineScope;
    }
    
    /**
     * Pretty up the error messages
     * @param e
     * @return
     */
    public static ScriptException prettyScriptMessage(ScriptException e) {
        while (e.getCause() instanceof ScriptException)
            e = (ScriptException) e.getCause();

        // Try to make the error message look a bit nicer.
        List<String> exclusions = new ArrayList<String>();
        exclusions.add("sun.org.mozilla.javascript.internal.EcmaError: ");
        exclusions.add("sun.org.mozilla.javascript.internal.EvaluatorException: ");
        String message = e.getMessage();
        for (String exclude : exclusions) {
            if (message.startsWith(exclude))
                message = message.substring(exclude.length());
        }
        return new ScriptException(message, e.getFileName(), e.getLineNumber(), e.getColumnNumber());
    }
}
