/*
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.rt.script;

import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Pattern;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptException;

import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.dao.DataPointDao;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.rt.dataImage.DataPointRT;
import com.serotonin.m2m2.rt.dataImage.IDataPointValueSource;
import com.serotonin.m2m2.rt.dataImage.PointValueTime;
import com.serotonin.m2m2.rt.dataImage.types.DataValue;
import com.serotonin.m2m2.vo.DataPointVO;
import com.serotonin.util.StringUtils;

/**
 * @author Matthew Lohbihler
 */
public class ScriptExecutor {
	
	//For extracting error messages Java 7
	private static final Pattern PATTERN = Pattern.compile("(.*?): (.*?) \\(.*?\\)");
	 
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
    		long runtime, ScriptPermissions permissions, 
    		PrintWriter scriptWriter, ScriptLog log){
        
    	ScriptUtils.prepareEngine(engine);
    	
    	ScriptUtils.wrapperContext(engine, new WrapperContext(runtime));
        Bindings engineScope = engine.getBindings(ScriptContext.ENGINE_SCOPE);

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
    	return createScriptError(e);
    }
    
    public static ScriptException createScriptError(Exception e){
	    	try{
	    	//Search the stack trace to see if we can pull out any useful script info
	    	if(e instanceof ScriptException){
	    		ScriptException ex = (ScriptException)e;
	    		while (ex.getCause() instanceof ScriptException)
	    			ex = (ScriptException) ex.getCause();
	    		
	            String message = null;
	            //EvaluatorException e1;
	            if((e.getCause() != null)&&e.getCause().getClass().getName().endsWith("EvaluatorException")){
	            	//Get the detail message
	            	Throwable cause = e.getCause();
	            	Class<?> causeClass = cause.getClass();
	            	while(causeClass.getSuperclass() != null){
		            	try {
		                	Field f = causeClass.getDeclaredField("detailMessage");
		                	f.setAccessible(true);
							message = (String)f.get(cause);
						} catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException | SecurityException e1) {
							//Don't care
						}
		            	causeClass = causeClass.getSuperclass();
	            	}
	            	if(message == null)
	            		message = ex.getMessage();
	            }else{
	                String m = ex.getMessage();
	                return new ScriptException(StringUtils.findGroup(PATTERN, m, 2), "javascript",
	                        ex.getLineNumber(), ex.getColumnNumber());
	            }
	
	            return new ScriptException(message, "javascript", ex.getLineNumber(), ex.getColumnNumber());
	    	}else{
	    		//Must get it from the trace
		    	for(StackTraceElement element : e.getStackTrace()){
		    		//Compiled Scripts will be run via the __scriptExecutor__ method, Regular scripts are run in the :program method
		    		if(element.getClassName().startsWith("jdk.nashorn.internal.scripts.Script")&&(element.getMethodName().equals("__scriptExecutor__")||(element.getMethodName().equals(":program")))){
		    			return new ScriptException(e.getClass().getSimpleName() + ": " + e.getMessage(), "javascript", element.getLineNumber(), -1);
		    		}
		    	}
		    	
		    	return new ScriptException(e.getMessage(), "javascript", -1, -1);
	    	}
	    }catch(Exception all){
	    	//For sanity until we rework this class as per #909
	    	return new ScriptException(e.getMessage(), "javascript", -1, -1);
	    }
    }
    
    
}
