/**
 * Copyright (C) 2019  Infinite Automation Software. All rights reserved.
 */
package com.infiniteautomation.mango.spring.service;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.script.Bindings;
import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.script.SimpleBindings;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import com.infiniteautomation.mango.util.exception.ValidationException;
import com.infiniteautomation.mango.util.script.MangoJavaScript;
import com.infiniteautomation.mango.util.script.MangoJavaScriptAction;
import com.infiniteautomation.mango.util.script.MangoJavaScriptError;
import com.infiniteautomation.mango.util.script.MangoJavaScriptResult;
import com.infiniteautomation.mango.util.script.ScriptLogLevels;
import com.infiniteautomation.mango.util.script.ScriptUtility;
import com.serotonin.ShouldNeverHappenException;
import com.serotonin.db.pair.IntStringPair;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.DataTypes;
import com.serotonin.m2m2.db.dao.DataPointDao;
import com.serotonin.m2m2.db.dao.DataSourceDao;
import com.serotonin.m2m2.i18n.ProcessMessage.Level;
import com.serotonin.m2m2.module.MangoJavascriptContextObjectDefinition;
import com.serotonin.m2m2.module.ModuleRegistry;
import com.serotonin.m2m2.module.ScriptSourceDefinition;
import com.serotonin.m2m2.i18n.ProcessResult;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.rt.dataImage.DataPointRT;
import com.serotonin.m2m2.rt.dataImage.IDataPointValueSource;
import com.serotonin.m2m2.rt.dataImage.PointValueTime;
import com.serotonin.m2m2.rt.dataImage.types.AlphanumericValue;
import com.serotonin.m2m2.rt.dataImage.types.BinaryValue;
import com.serotonin.m2m2.rt.dataImage.types.DataValue;
import com.serotonin.m2m2.rt.dataImage.types.MultistateValue;
import com.serotonin.m2m2.rt.dataImage.types.NumericValue;
import com.serotonin.m2m2.rt.script.AbstractPointWrapper;
import com.serotonin.m2m2.rt.script.AlphanumericPointWrapper;
import com.serotonin.m2m2.rt.script.BinaryPointWrapper;
import com.serotonin.m2m2.rt.script.DataPointStateException;
import com.serotonin.m2m2.rt.script.DateTimeUtility;
import com.serotonin.m2m2.rt.script.ImagePointWrapper;
import com.serotonin.m2m2.rt.script.JsonImportExclusion;
import com.serotonin.m2m2.rt.script.MultistatePointWrapper;
import com.serotonin.m2m2.rt.script.NumericPointWrapper;
import com.serotonin.m2m2.rt.script.ResultTypeException;
import com.serotonin.m2m2.rt.script.ScriptContextVariable;
import com.serotonin.m2m2.rt.script.ScriptError;
import com.serotonin.m2m2.rt.script.ScriptLog;
import com.serotonin.m2m2.rt.script.ScriptPermissions;
import com.serotonin.m2m2.rt.script.ScriptPointValueSetter;
import com.serotonin.m2m2.rt.script.UnitUtility;
import com.serotonin.m2m2.rt.script.WrapperContext;
import com.serotonin.m2m2.util.VarNames;
import com.serotonin.m2m2.vo.DataPointVO;
import com.serotonin.m2m2.vo.permission.PermissionException;
import com.serotonin.m2m2.vo.permission.PermissionHolder;
import com.serotonin.m2m2.vo.permission.Permissions;

/**
 * Service to allow running and validating Mango JavaScript scripts 
 * 
 * @author Terry Packer
 *
 */
@Service
public class MangoJavaScriptService {

    private final String SCRIPT_PREFIX = "function __scriptExecutor__() {";
    private final String SCRIPT_SUFFIX = "\r\n}\r\n";
    private final String SCRIPT_POSTFIX = "\r\n__scriptExecutor__();";

    public static final String WRAPPER_CONTEXT_KEY = "CONTEXT";
    public static final String POINTS_CONTEXT_KEY = "POINTS";
    public static final String POINTS_MAP_KEY = "CONTEXT_POINTS";
    public static final String TIMESTAMP_CONTEXT_KEY = "TIMESTAMP";
    public static final DataValue UNCHANGED = new BinaryValue(false);
    public static final String UNCHANGED_KEY = "UNCHANGED";

    private static final SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/YYY HH:mm:ss");

    public MangoJavaScriptService() {

    }
    
    /**
     * Validate a script with its parts
     * @param vo
     * @param user
     * @return
     */
    public ProcessResult validate(MangoJavaScript vo, PermissionHolder user) {
        ProcessResult result = new ProcessResult();
        
        Permissions.validateAddedPermissions(vo.getPermissions(), user, result, "permissions");
        
        validateContext(vo.getContext(), user, result);
        
        if(vo.getResultDataTypeId() != null) {
            if(!DataTypes.CODES.isValidId(vo.getResultDataTypeId()))
                result.addContextualMessage("resultDataTypeId", "validate.invalidValue");
        }
        
        //Can't validate a null script
        if(StringUtils.isEmpty(vo.getScript()))
            result.addContextualMessage("script", "validate.invalidValue");
        
        return result;
    }
    
    /**
     * Validate a script context
     * @param context
     * @param user
     * @param result
     */
    public void validateContext(List<ScriptContextVariable> context, PermissionHolder user, ProcessResult result) {
        //Validate the context, can we read all points and are the var names valid
        List<String> varNameSpace = new ArrayList<String>();
        for(ScriptContextVariable var : context) {
            String varName = var.getVariableName();
            DataPointVO dp = DataPointDao.getInstance().get(var.getDataPointId());
            if(dp == null)
                result.addContextualMessage("context", "javascript.validate.missingContextPoint", varName);
            else {
                if(!Permissions.hasDataPointReadPermission(user, dp))
                    result.addContextualMessage("context", "javascript.validate.noReadPermissionOnContextPoint", varName);
            }
            if (StringUtils.isBlank(varName)) {
                result.addContextualMessage("context", "validate.allVarNames");
                continue;
            }

            if (!VarNames.validateVarName(varName)) {
                result.addContextualMessage("context", "validate.invalidVarName", varName);
                continue;
            }

            if (varNameSpace.contains(varName)) {
                result.addContextualMessage("context", "validate.duplicateVarName", varName);
                continue;
            }

            varNameSpace.add(varName);
        }

    }
    
    /**
     * Ensure the script is valid
     * @param vo
     * @param user
     * @throws ValidationException
     */
    public void ensureValid(MangoJavaScript vo, PermissionHolder user) throws ValidationException {
        ProcessResult result = validate(vo, user);
        if(!result.isValid())
            throw new ValidationException(result);
    }
    
    /**
     * Test a script
     * @param vo
     * @param user
     * @return
     * @throws ValidationException
     * @throws PermissionException
     */
    public MangoJavaScriptResult testScript(MangoJavaScript vo, PermissionHolder user) throws ValidationException, PermissionException {
        ensureValid(vo, user);
        MangoJavaScriptResult result = new MangoJavaScriptResult();
        final StringWriter scriptOut = new StringWriter();
        try {
            final ScriptPointValueSetter setter = createValidationSetter(result, vo.getPermissions());
            final PrintWriter scriptWriter = new PrintWriter(scriptOut);
            ScriptLogLevels logLevel = vo.getLogLevel();
            if(logLevel == ScriptLogLevels.NONE)
                logLevel = ScriptLogLevels.FATAL;
            try(ScriptLog scriptLog = new ScriptLog("scriptTest-" + user.getPermissionHolderName(), logLevel.value(), scriptWriter);){
                CompiledScript compiledScript = compile(vo.getScript(), vo.isWrapInFunction());

                Object scriptResult;
                long time = Common.timer.currentTimeMillis();
                if(vo.getResultDataTypeId() != null)
                    scriptResult = execute(compiledScript, time, time, vo.getResultDataTypeId(),
                            convertContext(vo.getContext(), true),
                            null,
                            vo.getPermissions(), 
                            scriptLog,
                            setter, null, true);
                else
                    scriptResult = execute(compiledScript, time,
                            convertContext(vo.getContext(), true),
                            null,
                            vo.getPermissions(), 
                            scriptLog,
                            setter, null, true);
                result.setResult(scriptResult);
            }
        }catch (ScriptError e) {
            //The script exception should be clean as both compile() and execute() clean it
            result.addError(new MangoJavaScriptError(
                    new TranslatableMessage("common.default", e.getMessage()), e.getLineNumber(), e.getColumnNumber()));
        }catch(ResultTypeException e) {
            result.addError(new MangoJavaScriptError(e.getTranslatableMessage()));
        }catch (Exception e) {
            result.addError(new MangoJavaScriptError(e.getMessage()));
        }finally {
            result.setScriptOutput(scriptOut.toString());
        }
        return result;
    }
    
    /**
     * 
     * @param vo
     * @param user
     * @return
     * @throws ValidationException
     * @throws PermissionException
     */
    public MangoJavaScriptResult executeScript(MangoJavaScript vo, ScriptPointValueSetter setter, PermissionHolder user) throws ValidationException, PermissionException {
        ensureValid(vo, user);
        MangoJavaScriptResult result = new MangoJavaScriptResult();
        final StringWriter scriptOut = new StringWriter();
        try {
            final PrintWriter scriptWriter = new PrintWriter(scriptOut);
            ScriptLogLevels logLevel = vo.getLogLevel();
            if(logLevel == ScriptLogLevels.NONE)
                logLevel = ScriptLogLevels.FATAL;
            try(ScriptLog scriptLog = new ScriptLog("scriptTest-" + user.getPermissionHolderName(), logLevel.value(), scriptWriter);){
                CompiledScript compiledScript = compile(vo.getScript(), vo.isWrapInFunction());

                Object scriptResult;
                long time = Common.timer.currentTimeMillis();
                if(vo.getResultDataTypeId() != null)
                    scriptResult = execute(compiledScript, time, time, 
                            vo.getResultDataTypeId(), 
                            convertContext(vo.getContext(), false),
                            null,
                            vo.getPermissions(), 
                            scriptLog,
                            setter, null, false);
                else
                    scriptResult = execute(compiledScript, time, 
                            convertContext(vo.getContext(), false),
                            null,
                            vo.getPermissions(), 
                            scriptLog,
                            setter, null, false);
                result.setResult(scriptResult);
            }
        }catch (ScriptError e) {
            //The script exception should be clean as both compile() and execute() clean it
            result.addError(new MangoJavaScriptError(
                    new TranslatableMessage("common.default", e.getMessage()), e.getLineNumber(), e.getColumnNumber()));
        }catch(ResultTypeException e) {
            result.addError(new MangoJavaScriptError(e.getTranslatableMessage()));
        }catch (Exception e) {
            result.addError(new MangoJavaScriptError(e.getMessage()));
        }finally {
            result.setScriptOutput(scriptOut.toString());
        }
        return result;
    }
    
    /**
     * Compile a script to be run
     * 
     * @param script
     * @param wrapInFunction
     * @return
     * @throws ScriptError
     */
    public CompiledScript compile(String script, boolean wrapInFunction) throws ScriptError {
        
        try {
            final ScriptEngine engine = newEngine();
            
            Bindings globalBindings = new SimpleBindings();
            // Add constants to the context.
            globalBindings.put("SECOND", Common.TimePeriods.SECONDS);
            globalBindings.put("MINUTE", Common.TimePeriods.MINUTES);
            globalBindings.put("HOUR", Common.TimePeriods.HOURS);
            globalBindings.put("DAY", Common.TimePeriods.DAYS);
            globalBindings.put("WEEK", Common.TimePeriods.WEEKS);
            globalBindings.put("MONTH", Common.TimePeriods.MONTHS);
            globalBindings.put("YEAR", Common.TimePeriods.YEARS);
            globalBindings.put(POINTS_CONTEXT_KEY, new ArrayList<String>());
            
            for(IntStringPair isp : Common.ROLLUP_CODES.getIdKeys(Common.Rollups.NONE))
                globalBindings.put(Common.ROLLUP_CODES.getCode(isp.getKey()), isp.getKey());
            
            //Add in Additional Utilities with Global Scope
            globalBindings.put(DateTimeUtility.CONTEXT_KEY, new DateTimeUtility());
            globalBindings.put(UnitUtility.CONTEXT_KEY, new UnitUtility());
            
            //Holder for modifying timestamps of meta points, in Engine Scope so it can be modified by all
            engine.getBindings(ScriptContext.ENGINE_SCOPE).put(TIMESTAMP_CONTEXT_KEY, null);
            engine.setBindings(globalBindings, ScriptContext.GLOBAL_SCOPE);

            String toCompile;
            if(wrapInFunction) {
                toCompile = SCRIPT_PREFIX + script + SCRIPT_SUFFIX + SCRIPT_POSTFIX;
            }else {
                toCompile = script;
            }
            
            final CompiledScript compiledScript = ((Compilable)engine).compile(toCompile);
            
            return compiledScript;
        }catch(ScriptException e) {
            throw ScriptError.create(e);
        }
    }
    
    /**
     * Execute a script to return a PointValueTime
     * 
     * @param compiledScript
     * @param runtime - runtime of script
     * @param timestamp - Timestamp in Context and default for point value
     * @param resultDataTypeId
     * @return
     * @throws ScriptException 
     * @throws ResultTypeException 
     */
    public PointValueTime execute(CompiledScript compiledScript, long runtime, long timestamp,
            Integer resultDataTypeId, Map<String, IDataPointValueSource> context, Map<String, Object> additionalContext,
            Set<String> permissions, ScriptLog log, ScriptPointValueSetter setter, 
            List<JsonImportExclusion> importExclusions, boolean testRun) throws ScriptError, ResultTypeException {
        try {
            //Setup the wraper context
            compiledScript.getEngine().put(WRAPPER_CONTEXT_KEY, new WrapperContext(runtime, timestamp));
            //Populate with engine scoped data
            populateEngineScope(compiledScript, context, additionalContext, permissions, log, setter, importExclusions, testRun);
            
            Object result = compiledScript.eval();
            Object ts = compiledScript.getEngine().getBindings(ScriptContext.ENGINE_SCOPE).get(TIMESTAMP_CONTEXT_KEY);
    
            if (ts != null) {
                // Check the type of the object.
                if (ts instanceof Number)
                    // Convert to long
                    timestamp = ((Number) ts).longValue();
            }
            DataValue value = coerce(result, resultDataTypeId);
            return new PointValueTime(value, timestamp);
        }catch(ScriptException e) {
            throw ScriptError.create(e);
        }
    }
    
    /**
     * Excecute a script that does not return a PointValueTime
     * @param compiledScript
     * @param runtime
     * @return
     * @throws ScriptException
     */
    public Object execute(CompiledScript compiledScript, long runtime,
            Map<String, IDataPointValueSource> context, Map<String, Object> additionalContext,
            Set<String> permissions, ScriptLog log, ScriptPointValueSetter setter, 
            List<JsonImportExclusion> importExclusions, boolean testRun) throws ScriptError {
        try{
            //Setup the wraper context
            compiledScript.getEngine().put(WRAPPER_CONTEXT_KEY, new WrapperContext(runtime));
            //Populate with engine scoped data
            populateEngineScope(compiledScript, context, additionalContext, permissions, log, setter, importExclusions, testRun);
            return compiledScript.eval();
        }catch (ScriptException e) {
            throw ScriptError.create(e);
        }
    }
    
    /**
     * Create a dumb setter that tracks actions but does not actually set anything
     * @param vo
     * @param result
     * @param permissions
     * @return
     */
    protected ScriptPointValueSetter createValidationSetter(MangoJavaScriptResult result, Set<String> permissions) {
       return new ScriptPointValueSetter(new ScriptPermissions(permissions)) {
            
           @Override
            public void set(IDataPointValueSource point, Object value, long timestamp, String annotation) {
                DataPointRT dprt = (DataPointRT) point;
                if(!dprt.getVO().getPointLocator().isSettable()) {
                    result.addAction(new MangoJavaScriptAction(new TranslatableMessage("javascript.validate.pointNotSettable", dprt.getVO().getExtendedName()), Level.error));
                    return;
                }

                if(!Permissions.hasDataPointSetPermission(permissions, dprt.getVO())) {
                    result.addAction(new MangoJavaScriptAction(new TranslatableMessage("javascript.validate.pointPermissionsFailure", dprt.getVO().getXid()), Level.warning));
                    return;
                }
                result.addAction(new MangoJavaScriptAction(new TranslatableMessage("javascript.validate.setPointValue", dprt.getVO().getExtendedName(), value, sdf.format(new Date(timestamp)))));
            }

            @Override
            protected void setImpl(IDataPointValueSource point, Object value, long timestamp, String annotation) {
                //not really setting
            }
        };

    }
    
    /**
     * Fill the engine scope with data that may change after a script is compiled
     * @param script
     * @param context
     * @param additionalContext
     * @param permissions
     * @param log
     * @param setter
     * @param importExclusions
     * @param testRun
     * @throws ScriptException
     */
    protected void populateEngineScope(CompiledScript script, Map<String, 
            IDataPointValueSource> context, Map<String, Object> additionalContext,
            Set<String> permissions, ScriptLog log, ScriptPointValueSetter setter, 
            List<JsonImportExclusion> importExclusions, boolean testRun) throws ScriptException {
        
        Bindings engineScope = script.getEngine().getBindings(ScriptContext.ENGINE_SCOPE);
        if(permissions != null) {
            for(MangoJavascriptContextObjectDefinition def : ModuleRegistry.getMangoJavascriptContextObjectDefinitions()) {
                ScriptUtility util = testRun ? def.initializeTestContextObject(new ScriptPermissions(permissions)) : def.initializeContextObject(new ScriptPermissions(permissions));
                util.takeContext(script.getEngine(), engineScope, setter, importExclusions, testRun);
                engineScope.put(def.getContextKey(), util);
            }
        }
        
        engineScope.put(UNCHANGED_KEY, UNCHANGED);            
        if(additionalContext != null){
            Set<Entry<String,Object>> entries = additionalContext.entrySet();
            for(Entry<String,Object> entry: entries)
                engineScope.put(entry.getKey(), entry.getValue());
        }
                
        // Put the context variables into the engine with engine scope.
        List<String> points = getPointList(script.getEngine());
        if(context != null) {
            for (String varName : context.keySet()) {
                IDataPointValueSource point = context.get(varName);
                engineScope.put(varName, wrapPoint(script.getEngine(), point, setter));
                if (points != null)
                    points.add(varName); 
            }
            
            engineScope.put(POINTS_MAP_KEY, context);
        }
        
        //Set the print writer and log
        script.getEngine().getContext().setWriter(log.getStdOutWriter());
        engineScope.put(ScriptLog.CONTEXT_KEY, log);
        
        script.getEngine().eval(getGlobalFunctions());
    }
    
    /* Utilities for Script Execution */
    /**
     * Create a new script engine
     * @return
     */
    public ScriptEngine newEngine() {
        ScriptEngineManager manager = new ScriptEngineManager();
        return manager.getEngineByName("js");
    }
    
    public AbstractPointWrapper wrapPoint(ScriptEngine engine, IDataPointValueSource point,
            ScriptPointValueSetter setter) {
        int dt = point.getDataTypeId();
        if (dt == DataTypes.BINARY)
            return new BinaryPointWrapper(point, engine, setter);
        if (dt == DataTypes.MULTISTATE)
            return new MultistatePointWrapper(point, engine, setter);
        if (dt == DataTypes.NUMERIC)
            return new NumericPointWrapper(point, engine, setter);
        if (dt == DataTypes.ALPHANUMERIC)
            return new AlphanumericPointWrapper(point, engine, setter);
        if (dt == DataTypes.IMAGE)
            return new ImagePointWrapper(point, engine, setter);
        throw new ShouldNeverHappenException("Unknown data type id: " + point.getDataTypeId());
    }
    
    private static String FUNCTIONS;

    /**
     * Get all Module defined functions
     * @return
     */
    public String getGlobalFunctions() {
        if (FUNCTIONS == null) {
            StringWriter sw = new StringWriter();
            List<ScriptSourceDefinition> defs = ModuleRegistry.getDefinitions(ScriptSourceDefinition.class);
            for (ScriptSourceDefinition def : defs) {
                for (String s : def.getScripts())
                    sw.append(s).append("\r\n");
            }
            FUNCTIONS = sw.toString();
        }
        return FUNCTIONS;
    }

    /**
     * Clear all functions so they are re-loaded on next 'get'
     */
    public void clearGlobalFunctions() {
        FUNCTIONS = null;
    }
    
    public void addToContext(CompiledScript script, String varName, DataPointRT dprt, ScriptPointValueSetter setCallback) {
        ScriptEngine engine = script.getEngine();
        engine.put(varName, wrapPoint(engine, dprt, setCallback));
        List<String> points = getPointList(engine);
        if (points != null)
            points.add(varName);
    }

    public void removeFromContext(CompiledScript script, String varName) {
        ScriptEngine engine = script.getEngine();
        List<String> points = getPointList(engine);
        if (points != null)
            points.remove(varName);
        engine.getBindings(ScriptContext.ENGINE_SCOPE).remove(varName);
    }
    
    @SuppressWarnings("unchecked")
    private List<String> getPointList(ScriptEngine engine) {
        return (List<String>) engine.get(POINTS_CONTEXT_KEY);
    }
    
    /**
     * Coerce an object into a DataValue
     * @param input
     * @param toDataTypeId
     * @return
     * @throws ResultTypeException
     */
    public DataValue coerce(Object input, int toDataTypeId) throws ResultTypeException {
        DataValue value;
        
        if(input instanceof DataValue)
            return (DataValue)input;
        
        if (input == null) {
            if (toDataTypeId == DataTypes.BINARY)
                value = new BinaryValue(false);
            else if (toDataTypeId == DataTypes.MULTISTATE)
                value = new MultistateValue(0);
            else if (toDataTypeId == DataTypes.NUMERIC)
                value = new NumericValue(0);
            else if (toDataTypeId == DataTypes.ALPHANUMERIC)
                value = new AlphanumericValue("");
            else
                value = null;
        }
        else if (input instanceof AbstractPointWrapper) {
            value = ((AbstractPointWrapper) input).getValueImpl();
            if ((value != null)&&(value.getDataType() != toDataTypeId))
                throw new ResultTypeException(new TranslatableMessage("event.script.convertError", input,
                        DataTypes.getDataTypeMessage(toDataTypeId)));
        }
        // See if the type matches.
        else if (toDataTypeId == DataTypes.BINARY && input instanceof Boolean)
            value = new BinaryValue((Boolean) input);
        else if (toDataTypeId == DataTypes.MULTISTATE) {
            if (input instanceof Number)
                value = new MultistateValue(((Number) input).intValue());
            else if (input instanceof String) {
                try {
                    value = new MultistateValue(Integer.parseInt((String) input));
                }
                catch (NumberFormatException e) {
                    throw new ResultTypeException(new TranslatableMessage("event.script.convertError", input,
                            DataTypes.getDataTypeMessage(toDataTypeId)));
                }
            }
            else
                throw new ResultTypeException(new TranslatableMessage("event.script.convertError", input,
                        DataTypes.getDataTypeMessage(toDataTypeId)));
        }
        else if (toDataTypeId == DataTypes.NUMERIC) {
            if (input instanceof Number)
                value = new NumericValue(((Number) input).doubleValue());
            else if (input instanceof NumericValue)
                value = (NumericValue) input;
            else if (input instanceof String) {
                try {
                    value = new NumericValue(Double.parseDouble((String) input));
                }
                catch (NumberFormatException e) {
                    throw new ResultTypeException(new TranslatableMessage("event.script.convertError", input,
                            DataTypes.getDataTypeMessage(toDataTypeId)));
                }
            }
            else
                throw new ResultTypeException(new TranslatableMessage("event.script.convertError", input,
                        DataTypes.getDataTypeMessage(toDataTypeId)));
        }
        else if (toDataTypeId == DataTypes.ALPHANUMERIC)
            value = new AlphanumericValue(input.toString());
        else
            // If not, ditch it.
            throw new ResultTypeException(new TranslatableMessage("event.script.convertError", input,
                    DataTypes.getDataTypeMessage(toDataTypeId)));

        return value;
    }

    
    /**
     * Prepare the context of data points
     * @param context
     * @param test
     * @return
     */
    public Map<String, IDataPointValueSource> convertContext(List<ScriptContextVariable> context, boolean test) throws DataPointStateException {
        Map<String, IDataPointValueSource> result = new HashMap<>();
        if(context != null)
            for(ScriptContextVariable variable : context) {
                DataPointVO dpvo = DataPointDao.getInstance().get(variable.getDataPointId());
                if(dpvo != null) {
                    DataPointRT dprt = Common.runtimeManager.getDataPoint(dpvo.getId());
                    //So we can test with points disabled
                    if(dprt == null) {
                        if(test) {
                            if(dpvo.getDefaultCacheSize() == 0)
                                dpvo.setDefaultCacheSize(1);
                            dprt = new DataPointRT(dpvo, dpvo.getPointLocator().createRuntime(), DataSourceDao.getInstance().getDataSource(dpvo.getDataSourceId()), null);
                            dprt.resetValues(); //otherwise variable.value will be empty
                        }else {
                            throw new DataPointStateException(variable.getDataPointId(), new TranslatableMessage(
                                    "event.script.contextPointDisabled", variable.getVariableName(), variable.getDataPointId()));
                        }
                    }
                    if(dprt != null)
                        result.put(variable.getVariableName(), dprt);
                }else {
                    throw new DataPointStateException(variable.getDataPointId(), new TranslatableMessage(
                            "event.script.contextPointMissing", variable.getVariableName(), variable.getDataPointId()));
                }
            }
        return result;
    }
}
