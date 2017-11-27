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

import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.dao.DataPointDao;
import com.serotonin.m2m2.db.dao.DataSourceDao;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.rt.dataImage.DataPointRT;
import com.serotonin.m2m2.rt.dataImage.IDataPointValueSource;
import com.serotonin.m2m2.rt.dataImage.PointValueTime;
import com.serotonin.m2m2.rt.dataImage.types.DataValue;
import com.serotonin.m2m2.vo.DataPointVO;

/**
 * @author Matthew Lohbihler
 */
public class ScriptExecutor {
		 
    protected static final String SCRIPT_PREFIX = "function __scriptExecutor__() {";
    protected static final String SCRIPT_SUFFIX = "\r\n}\r\n__scriptExecutor__();";

    protected static final boolean useMetrics = Common.envProps.getBoolean("runtime.javascript.metrics", false);

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
            if (point == null){
            	DataPointVO vo = DataPointDao.instance.get(contextEntry.getDataPointId());
            	if(vo == null)
            		throw new DataPointStateException(contextEntry.getDataPointId(), new TranslatableMessage(
                        "event.script.contextPointMissing", contextEntry.getVariableName(), contextEntry.getDataPointId()));
            	else
            		throw new DataPointStateException(contextEntry.getDataPointId(), new TranslatableMessage(
                            "event.script.contextPointDisabled", contextEntry.getVariableName(), contextEntry.getDataPointId()));
            }
            converted.put(contextEntry.getVariableName(), point);
        }

        return converted;
    }
    
    public static Map<String, IDataPointValueSource> convertScriptContextForScriptValidation(List<ScriptContextVariable> context)
            throws DataPointStateException {
        Map<String, IDataPointValueSource> converted = new HashMap<String, IDataPointValueSource>();
        for (ScriptContextVariable contextEntry : context) {
            DataPointRT point = Common.runtimeManager.getDataPoint(contextEntry.getDataPointId());
            if (point == null){
                DataPointVO vo = DataPointDao.instance.get(contextEntry.getDataPointId());
                if(vo == null)
                    throw new DataPointStateException(contextEntry.getDataPointId(), new TranslatableMessage(
                        "event.script.contextPointMissing", contextEntry.getVariableName(), contextEntry.getDataPointId()));
                if(vo.getDefaultCacheSize() == 0)
                    vo.setDefaultCacheSize(1);
                point = new DataPointRT(vo, vo.getPointLocator().createRuntime(), DataSourceDao.instance.getDataSource(vo.getDataSourceId()), null);
                point.resetValues();
            }
            converted.put(contextEntry.getVariableName(), point);
        }

        return converted;
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
        Object ts = engine.getBindings(ScriptContext.ENGINE_SCOPE).get(ScriptUtils.TIMESTAMP_CONTEXT_KEY);

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
    		long runtime, long timestamp, ScriptPermissions permissions, PrintWriter scriptWriter,
    		ScriptLog log, ScriptPointValueSetter setter, List<JsonImportExclusion> importExclusions, boolean testRun){
        
    	ScriptUtils.prepareEngine(engine);
    	
    	ScriptUtils.wrapperContext(engine, new WrapperContext(runtime, timestamp));
        Bindings engineScope = engine.getBindings(ScriptContext.ENGINE_SCOPE);

        //Add Permissions Required Utilities
        //TODO Bubble PointValueSetter back up to top
        if(permissions != null)
        	ScriptUtils.prepareUtilities(permissions, engine, engineScope, setter, importExclusions, testRun);
        
        if(additionalContext != null){
        	Set<Entry<String,Object>> entries = additionalContext.entrySet();
        	for(Entry<String,Object> entry: entries)
        		engineScope.put(entry.getKey(), entry.getValue());
        }
                
        // Put the context variables into the engine with engine scope.
        for (String varName : context.keySet()) {
            IDataPointValueSource point = context.get(varName);
            engineScope.put(varName, ScriptUtils.wrapPoint(engine, point, setter));
         }
        
        engineScope.put(ScriptUtils.POINTS_MAP_KEY, context);
        
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
