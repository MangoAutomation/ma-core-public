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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.script.Bindings;
import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptException;
import javax.script.SimpleBindings;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import com.infiniteautomation.mango.util.exception.ValidationException;
import com.infiniteautomation.mango.util.script.MangoJavaScript;
import com.infiniteautomation.mango.util.script.MangoJavaScriptAction;
import com.infiniteautomation.mango.util.script.MangoJavaScriptError;
import com.infiniteautomation.mango.util.script.MangoJavaScriptResult;
import com.serotonin.db.pair.IntStringPair;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.DataTypes;
import com.serotonin.m2m2.db.dao.DataPointDao;
import com.serotonin.m2m2.db.dao.DataSourceDao;
import com.serotonin.m2m2.i18n.ProcessMessage.Level;
import com.serotonin.m2m2.i18n.ProcessResult;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.rt.dataImage.DataPointRT;
import com.serotonin.m2m2.rt.dataImage.IDataPointValueSource;
import com.serotonin.m2m2.rt.dataImage.PointValueTime;
import com.serotonin.m2m2.rt.dataImage.types.BinaryValue;
import com.serotonin.m2m2.rt.dataImage.types.DataValue;
import com.serotonin.m2m2.rt.script.DateTimeUtility;
import com.serotonin.m2m2.rt.script.JsonImportExclusion;
import com.serotonin.m2m2.rt.script.ResultTypeException;
import com.serotonin.m2m2.rt.script.ScriptContextVariable;
import com.serotonin.m2m2.rt.script.ScriptLog;
import com.serotonin.m2m2.rt.script.ScriptPermissions;
import com.serotonin.m2m2.rt.script.ScriptPointValueSetter;
import com.serotonin.m2m2.rt.script.ScriptUtils;
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
    public MangoJavaScriptResult testScript(MangoJavaScript vo, PermissionHolder user) throws ValidationException, PermissionException{
        ensureValid(vo, user);
        return testCompiledScript(vo, user);
    }
    
    /**
     * Evaluate a Compiled Script
     * @param vo
     * @param user
     * @return
     */
    protected MangoJavaScriptResult testCompiledScript(MangoJavaScript vo, PermissionHolder user) {
        MangoJavaScriptResult result = new MangoJavaScriptResult();
        final StringWriter scriptOut = new StringWriter();
        try {
            final ScriptPointValueSetter setter = createValidationSetter(result, vo.getPermissions());
            final PrintWriter scriptWriter = new PrintWriter(scriptOut);
            int logLevel = vo.getLogLevel().value();
            if(logLevel == ScriptLog.LogLevel.NONE)
                logLevel = ScriptLog.LogLevel.FATAL;
            try(ScriptLog scriptLog = new ScriptLog("scriptTest-" + user.getPermissionHolderName(), logLevel, scriptWriter);){
                CompiledScript compiledScript = compile(
                        vo.getScript(),
                        vo.isWrapInFunction(),
                        convertContext(vo.getContext(), true),
                        null,
                        vo.getPermissions(), 
                        scriptLog,
                        setter, null, true);

                Object scriptResult;
                long time = Common.timer.currentTimeMillis();
                if(vo.getResultDataTypeId() != null)
                    scriptResult = execute(compiledScript, time, time, vo.getResultDataTypeId());
                else
                    scriptResult = execute(compiledScript, time);
                result.setResult(scriptResult);
            }
        }catch (ScriptException e) {
            result.addError(new MangoJavaScriptError(
                    new TranslatableMessage("common.default", cleanScriptExceptionMessage(e)), e.getLineNumber(), e.getColumnNumber()));
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
     * @param script
     * @param wrapInFunction
     * @param context
     * @param additionalContext
     * @param permissions
     * @param log
     * @param setter
     * @param importExclusions
     * @param testRun
     * @return
     * @throws ScriptException
     */
    public CompiledScript compile(String script, boolean wrapInFunction, Map<String, 
            IDataPointValueSource> context, Map<String, Object> additionalContext,
            Set<String> permissions, ScriptLog log, ScriptPointValueSetter setter, 
            List<JsonImportExclusion> importExclusions, boolean testRun) throws ScriptException {
        
        try {
            final ScriptEngine engine = ScriptUtils.newEngine();
            
            Bindings globalBindings = new SimpleBindings();
            // Add constants to the context.
            globalBindings.put("SECOND", Common.TimePeriods.SECONDS);
            globalBindings.put("MINUTE", Common.TimePeriods.MINUTES);
            globalBindings.put("HOUR", Common.TimePeriods.HOURS);
            globalBindings.put("DAY", Common.TimePeriods.DAYS);
            globalBindings.put("WEEK", Common.TimePeriods.WEEKS);
            globalBindings.put("MONTH", Common.TimePeriods.MONTHS);
            globalBindings.put("YEAR", Common.TimePeriods.YEARS);
            globalBindings.put(ScriptUtils.POINTS_CONTEXT_KEY, new ArrayList<String>());
            
            for(IntStringPair isp : Common.ROLLUP_CODES.getIdKeys(Common.Rollups.NONE))
                globalBindings.put(Common.ROLLUP_CODES.getCode(isp.getKey()), isp.getKey());
            
            //Add in Additional Utilities with Global Scope
            globalBindings.put(DateTimeUtility.CONTEXT_KEY, new DateTimeUtility());
            globalBindings.put(UnitUtility.CONTEXT_KEY, new UnitUtility());
            
            //Holder for modifying timestamps of meta points, in Engine Scope so it can be modified by all
            engine.getBindings(ScriptContext.ENGINE_SCOPE).put(ScriptUtils.TIMESTAMP_CONTEXT_KEY, null);
            
            engine.setBindings(globalBindings, ScriptContext.GLOBAL_SCOPE);
            
            
            Bindings engineScope = engine.getBindings(ScriptContext.ENGINE_SCOPE);
    
            //TODO Make a method in this class: Add Permissions Required Utilities
            if(permissions != null)
                ScriptUtils.prepareUtilities(new ScriptPermissions(permissions), engine, engineScope, setter, importExclusions, testRun);
            
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
            
            //Set the print writer and log
            engine.getContext().setWriter(log.getStdOutWriter());
            engineScope.put(ScriptLog.CONTEXT_KEY, log);
            
            engine.eval(ScriptUtils.getGlobalFunctions());
            
            String toCompile;
            if(wrapInFunction) {
                toCompile = SCRIPT_PREFIX + script + SCRIPT_SUFFIX + SCRIPT_POSTFIX;
            }else {
                toCompile = script;
            }
            
            final CompiledScript compiledScript = ((Compilable)engine).compile(toCompile);
            
            return compiledScript;
        }catch(ScriptException e) {
            throw new ScriptException(cleanScriptExceptionMessage(e), e.getFileName(), e.getLineNumber(), e.getColumnNumber());
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
            Integer resultDataTypeId) throws ScriptException, ResultTypeException {
        try {
            //Setup the wraper context
            compiledScript.getEngine().put(ScriptUtils.WRAPPER_CONTEXT_KEY, new WrapperContext(runtime, timestamp));
            Object result;
            try {
                result = compiledScript.eval();
            }catch (ScriptException e) {
                throw new ScriptException(cleanScriptExceptionMessage(e), e.getFileName(), e.getLineNumber(), e.getColumnNumber());
            }
            
            Object ts = compiledScript.getEngine().getBindings(ScriptContext.ENGINE_SCOPE).get(ScriptUtils.TIMESTAMP_CONTEXT_KEY);
    
            if (ts != null) {
                // Check the type of the object.
                if (ts instanceof Number)
                    // Convert to long
                    timestamp = ((Number) ts).longValue();
            }
            DataValue value = ScriptUtils.coerce(result, resultDataTypeId);
            return new PointValueTime(value, timestamp);
        }catch(ScriptException e) {
            throw new ScriptException(cleanScriptExceptionMessage(e), e.getFileName(), e.getLineNumber(), e.getColumnNumber());
        }
    }
    
    /**
     * Excecute a script that does not return a PointValueTime
     * @param compiledScript
     * @param runtime
     * @return
     * @throws ScriptException
     */
    public Object execute(CompiledScript compiledScript, long runtime) throws ScriptException {
        //Setup the wraper context
        compiledScript.getEngine().put(ScriptUtils.WRAPPER_CONTEXT_KEY, new WrapperContext(runtime));
        try{
            return compiledScript.eval();
        }catch (ScriptException e) {
            throw new ScriptException(cleanScriptExceptionMessage(e), e.getFileName(), e.getLineNumber(), e.getColumnNumber());
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
                    result.addAction(new MangoJavaScriptAction(new TranslatableMessage("javascript.validate.pointPermissionsFailure", dprt.getVO().getXid(), Level.error)));
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
    
    /* Utilities for Script Execution */
    /**
     * Cleanup message to make more readable
     * @param e
     * @return
     */
    public String cleanScriptExceptionMessage(ScriptException e) {
        Throwable t = e;
        while (t.getCause() != null)
            t = t.getCause();
        
        String message = t.getMessage();
        if(message == null)
            return "null";

        Pattern pattern = Pattern.compile("(.*?):(.*?) ([\\s\\S]*)");
        Matcher matcher = pattern.matcher(message);
        if (matcher.find())
            message = matcher.group(3);
        
        return message;
    }


    
    /**
     * Prepare the context of data points
     * @param context
     * @param test
     * @return
     */
    private Map<String, IDataPointValueSource> convertContext(List<ScriptContextVariable> context, boolean test) {
        Map<String, IDataPointValueSource> result = new HashMap<>();
        if(context != null)
            for(ScriptContextVariable variable : context) {
                DataPointVO dpvo = DataPointDao.getInstance().get(variable.getDataPointId());
                if(dpvo != null) {
                    DataPointRT dprt = Common.runtimeManager.getDataPoint(dpvo.getId());
                    //So we can test with points disabled
                    if(dprt == null && test) {
                        if(dpvo.getDefaultCacheSize() == 0)
                            dpvo.setDefaultCacheSize(1);
                        dprt = new DataPointRT(dpvo, dpvo.getPointLocator().createRuntime(), DataSourceDao.getInstance().getDataSource(dpvo.getDataSourceId()), null);
                        dprt.resetValues(); //otherwise variable.value will be empty
                    }
                    if(dprt != null)
                        result.put(variable.getVariableName(), dprt);
                }
            }
        return result;
    }
}
