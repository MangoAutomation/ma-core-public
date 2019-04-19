/**
 * Copyright (C) 2019  Infinite Automation Software. All rights reserved.
 */
package com.serotonin.m2m2.rt.script;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.script.Bindings;
import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptException;
import javax.script.SimpleBindings;

import com.infiniteautomation.mango.spring.service.MangoJavaScriptService;
import com.infiniteautomation.mango.util.script.MangoJavaScriptResult;
import com.infiniteautomation.mango.util.script.ScriptPermissions;
import com.infiniteautomation.mango.util.script.ScriptUtility;
import com.serotonin.ShouldNeverHappenException;
import com.serotonin.db.pair.IntStringPair;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.module.MangoJavascriptContextObjectDefinition;
import com.serotonin.m2m2.module.ModuleRegistry;
import com.serotonin.m2m2.rt.dataImage.IDataPointValueSource;
import com.serotonin.m2m2.rt.dataImage.PointValueTime;
import com.serotonin.m2m2.rt.dataImage.types.DataValue;
import com.serotonin.m2m2.vo.permission.PermissionHolder;

/**
 * Container for Compiled Java Scripts to aid in state management 
 * of the various scopes and to help simplify user code.
 * 
 * Usage:
 * 1. Constructe compiled script
 * 2. call compile()
 * 3. call initialize() (anytime the context changes)
 * 4. call execute() when you desire to compute a result from the script
 * 
 * @author Terry Packer
 *
 */
public class CompiledMangoJavaScript {
    
    private CompiledScript compiledScript;
    
    private final List<ScriptUtility> utilities;
    private Map<String, IDataPointValueSource> context;
    
    private final Map<String, Object> additionalContext;
    private final List<ScriptUtility> additionalUtilities;
    private ScriptPermissions permissions;
    private ScriptLog log;
    private ScriptPointValueSetter setter;
    private List<JsonImportExclusion> importExclusions;
    
    
    private final boolean testRun;
    private final MangoJavaScriptService service;
    private final PermissionHolder user;
    private final MangoJavaScriptResult result;
    
    
    public CompiledMangoJavaScript(Map<String, Object> additionalContext, List<ScriptUtility> additionalUtilities, boolean testRun, MangoJavaScriptService service, PermissionHolder user) {
        this.additionalContext = additionalContext == null ? Collections.emptyMap() : additionalContext;
        this.additionalUtilities = additionalUtilities == null ? Collections.emptyList() : additionalUtilities;
        this.testRun = testRun;
        this.service = service;
        this.user = user;
        this.result = new MangoJavaScriptResult();
        this.utilities = new ArrayList<>();
    }
    
    
    public void compile(String script, boolean wrapInFunction) throws ScriptError {
        try {
            final ScriptEngine engine = service.newEngine(user);
            
            //TODO Should these REALLY be globally scoped?
            Bindings globalBindings = new SimpleBindings();
            // Add constants to the context.
            globalBindings.put("SECOND", Common.TimePeriods.SECONDS);
            globalBindings.put("MINUTE", Common.TimePeriods.MINUTES);
            globalBindings.put("HOUR", Common.TimePeriods.HOURS);
            globalBindings.put("DAY", Common.TimePeriods.DAYS);
            globalBindings.put("WEEK", Common.TimePeriods.WEEKS);
            globalBindings.put("MONTH", Common.TimePeriods.MONTHS);
            globalBindings.put("YEAR", Common.TimePeriods.YEARS);
            
            for(IntStringPair isp : Common.ROLLUP_CODES.getIdKeys(Common.Rollups.NONE))
                globalBindings.put(Common.ROLLUP_CODES.getCode(isp.getKey()), isp.getKey());
            
            //Add in Additional Utilities with Global Scope
            globalBindings.put(DateTimeUtility.CONTEXT_KEY, new DateTimeUtility());
            globalBindings.put(UnitUtility.CONTEXT_KEY, new UnitUtility());
            
            //Holder for modifying timestamps of meta points, in Engine Scope so it can be modified by all
            engine.getBindings(ScriptContext.ENGINE_SCOPE).put(MangoJavaScriptService.TIMESTAMP_CONTEXT_KEY, null);
            engine.setBindings(globalBindings, ScriptContext.GLOBAL_SCOPE);

            String toCompile;
            if(wrapInFunction) {
                toCompile = MangoJavaScriptService.SCRIPT_PREFIX + script + MangoJavaScriptService.SCRIPT_SUFFIX + MangoJavaScriptService.SCRIPT_POSTFIX;
            }else {
                toCompile = script;
            }
            
            this.compiledScript = ((Compilable)engine).compile(toCompile);
        }catch(ScriptException e) {
            throw ScriptError.create(e);
        }
    }
    
    /**
     * Clear the engine scope and initialize it
     * 
     * @throws ScriptException
     */
    public void initialize() throws ScriptException {
        Bindings engineScope = compiledScript.getEngine().getBindings(ScriptContext.ENGINE_SCOPE);
        if(setter == null)
            setter = service.createValidationSetter(result, permissions);
        
        engineScope.put(MangoJavaScriptService.UNCHANGED_KEY, MangoJavaScriptService.UNCHANGED);
        List<String> points = new ArrayList<String>();
        engineScope.put(MangoJavaScriptService.POINTS_CONTEXT_KEY, points);
        engineScope.put(MangoJavaScriptService.TIMESTAMP_CONTEXT_KEY, null);
        
        if(permissions != null) {
            utilities.clear();
            for(MangoJavascriptContextObjectDefinition def : ModuleRegistry.getMangoJavascriptContextObjectDefinitions()) {
                ScriptUtility util = testRun ? def.initializeTestContextObject(permissions) : def.initializeContextObject(permissions);
                util.setScriptLog(log);
                util.setResult(result);
                util.takeContext(compiledScript.getEngine(), engineScope, setter, importExclusions, testRun);
                engineScope.put(util.getContextKey(), util);
                utilities.add(util);
            }
            //Initialize additional utilities
            for(ScriptUtility util : additionalUtilities) {
                util.setScriptLog(log);
                util.setResult(result);
                util.takeContext(compiledScript.getEngine(), engineScope, setter, importExclusions, testRun);
                engineScope.put(util.getContextKey(), util);
            }
        }
        
        Set<Entry<String,Object>> entries = additionalContext.entrySet();
        for(Entry<String,Object> entry: entries)
            engineScope.put(entry.getKey(), entry.getValue());
                
        // Put the context variables into the engine with engine scope.
        if(context != null) {
            for (String varName : context.keySet()) {
                IDataPointValueSource point = context.get(varName);
                engineScope.put(varName, service.wrapPoint(compiledScript.getEngine(), point, setter));
                if (points != null)
                    points.add(varName); 
            }
            
            engineScope.put(MangoJavaScriptService.POINTS_MAP_KEY, context);
        }
        
        //Set the print writer and log
        compiledScript.getEngine().getContext().setWriter(log.getStdOutWriter());
        engineScope.put(ScriptLog.CONTEXT_KEY, log);
        
        compiledScript.getEngine().eval(service.getGlobalFunctions());
    }
    
    /**
     * 
     * @param runtime
     * @param timestamp
     * @param resultDataTypeId
     * @return
     * @throws ScriptError
     * @throws ResultTypeException
     * @throws ScriptPermissionsException
     */
    public MangoJavaScriptResult execute(long runtime, long timestamp, Integer resultDataTypeId) throws ScriptError, ResultTypeException, ScriptPermissionsException {
        this.result.reset();
        
        try {
            //Setup the wraper context
            Bindings engineScope = compiledScript.getEngine().getBindings(ScriptContext.ENGINE_SCOPE);
            engineScope.put(MangoJavaScriptService.WRAPPER_CONTEXT_KEY, new WrapperContext(runtime, timestamp));
            
            //Ensure the result is available to the utilities
            for(ScriptUtility util : utilities) {
                util.setScriptLog(log);
                util.setResult(result);
                util.takeContext(compiledScript.getEngine(), engineScope, setter, importExclusions, testRun);
                engineScope.put(util.getContextKey(), util);
            }
            
            //Initialize additional utilities
            if(additionalUtilities != null) {
                for(ScriptUtility util : additionalUtilities)
                    util.setResult(result);
            }

            
            Object resultObject = compiledScript.eval();
            Object ts = compiledScript.getEngine().getBindings(ScriptContext.ENGINE_SCOPE).get(MangoJavaScriptService.TIMESTAMP_CONTEXT_KEY);
    
            if (ts != null) {
                // Check the type of the object.
                if (ts instanceof Number)
                    // Convert to long
                    timestamp = ((Number) ts).longValue();
            }
            DataValue value = service.coerce(resultObject, resultDataTypeId);
            result.setResult(new PointValueTime(value, timestamp));
        }catch(ScriptException e) {
            throw ScriptError.create(e);
        }catch (RuntimeException e) {
            //Nashorn seems to like to wrap exceptions in RuntimeException 
            if(e.getCause() instanceof ScriptPermissionsException)
                throw (ScriptPermissionsException)e.getCause();
            else
                throw new ShouldNeverHappenException(e);
        }
        
        return result;
    }
}
