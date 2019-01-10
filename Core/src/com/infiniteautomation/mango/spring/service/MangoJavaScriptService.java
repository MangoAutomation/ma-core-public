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
import java.util.Iterator;
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

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import com.infiniteautomation.mango.util.exception.ValidationException;
import com.infiniteautomation.mango.util.script.MangoJavaScript;
import com.infiniteautomation.mango.util.script.MangoJavaScriptAction;
import com.infiniteautomation.mango.util.script.MangoJavaScriptError;
import com.infiniteautomation.mango.util.script.MangoJavaScriptResult;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.DataTypes;
import com.serotonin.m2m2.i18n.ProcessMessage.Level;
import com.serotonin.m2m2.i18n.ProcessResult;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.rt.dataImage.DataPointRT;
import com.serotonin.m2m2.rt.dataImage.IDataPointValueSource;
import com.serotonin.m2m2.rt.dataImage.PointValueTime;
import com.serotonin.m2m2.rt.dataImage.types.BinaryValue;
import com.serotonin.m2m2.rt.dataImage.types.DataValue;
import com.serotonin.m2m2.rt.script.JsonImportExclusion;
import com.serotonin.m2m2.rt.script.ResultTypeException;
import com.serotonin.m2m2.rt.script.ScriptLog;
import com.serotonin.m2m2.rt.script.ScriptPermissions;
import com.serotonin.m2m2.rt.script.ScriptPointValueSetter;
import com.serotonin.m2m2.rt.script.ScriptUtils;
import com.serotonin.m2m2.rt.script.WrapperContext;
import com.serotonin.m2m2.util.VarNames;
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
    
    public ProcessResult validate(MangoJavaScript vo, PermissionHolder user) {
        ProcessResult result = new ProcessResult();
        
        Permissions.validateAddedPermissions(vo.getPermissions(), user, result, "permissions");
        
        //Validate the context, can we read all points and are the var names valid
        List<String> varNameSpace = new ArrayList<String>();
        Iterator<String> it = vo.getContext().keySet().iterator();
        while(it.hasNext()) {
            String varName = it.next();
            IDataPointValueSource idp = vo.getContext().get(varName);
            if(idp == null)
                result.addContextualMessage("context", "javascript.validate.missingContextPoint", varName);
            
            if(!Permissions.hasDataPointReadPermission(user, idp.getVO()))
                result.addContextualMessage("context", "javascript.validate.noReadPermissionOnContextPoint", varName);
            
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
        
        if(vo.getResultDataTypeId() != null) {
            if(!DataTypes.CODES.isValidId(vo.getResultDataTypeId()))
                result.addContextualMessage("resultDataTypeId", "validate.invalidValue");
        }
        
        return result;
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
            String script;
            if(vo.isWrapInFunction()) {
                script = SCRIPT_PREFIX + vo.getScript() + SCRIPT_SUFFIX + ScriptUtils.getGlobalFunctions() + SCRIPT_POSTFIX;
            }else {
                script = vo.getScript();
            }
            final ScriptEngine engine = ScriptUtils.newEngine();
            ScriptUtils.prepareEngine(engine);
            final CompiledScript compiledScript = ((Compilable)engine).compile(script);
            final ScriptPointValueSetter setter = createValidationSetter(result, vo.createScriptPermissions());
            final PrintWriter scriptWriter = new PrintWriter(scriptOut);
            int logLevel = vo.getLogLevel().value();
            if(logLevel == ScriptLog.LogLevel.NONE)
                logLevel = ScriptLog.LogLevel.FATAL;
            try(ScriptLog scriptLog = new ScriptLog("scriptTest-" + user.getPermissionHolderName(), logLevel, scriptWriter);){
                long time = Common.timer.currentTimeMillis();
                Object scriptResult;
                if(vo.getResultDataTypeId() != null) {
                    scriptResult = execute(compiledScript, vo.getContext(), 
                            new HashMap<String, Object>(), time, vo.getResultDataTypeId(), time, 
                            vo.createScriptPermissions(), scriptLog, setter, null, true);
                }else {
                    scriptResult = execute(compiledScript, vo.getContext(), 
                            new HashMap<String, Object>(), time, time, 
                            vo.createScriptPermissions(), scriptLog, setter, null, true);
                }
                result.setResult(scriptResult);
            }

        }catch (ScriptException e) {
            result.addError(new MangoJavaScriptError(createScriptExceptionMessage(e), e.getLineNumber(), e.getColumnNumber()));
        }catch(ResultTypeException e) {
            result.addError(new MangoJavaScriptError(e.getTranslatableMessage()));
        }catch (Exception e) {
            result.addError(new MangoJavaScriptError(e.getMessage()));
        }finally {
            result.setScriptOutput(scriptOut.toString());
        }
        return result;
    }
    
    @Deprecated
    protected MangoJavaScriptResult testRawScript(MangoJavaScript vo, PermissionHolder user) {
        MangoJavaScriptResult result = new MangoJavaScriptResult();
        final StringWriter scriptOut = new StringWriter();
        try {
            final ScriptEngine engine = ScriptUtils.newEngine();
            ScriptUtils.prepareEngine(engine);
            final ScriptPointValueSetter setter = createValidationSetter(result, vo.createScriptPermissions());
            final PrintWriter scriptWriter = new PrintWriter(scriptOut);
            int logLevel = vo.getLogLevel().value();
            if(logLevel == ScriptLog.LogLevel.NONE)
                logLevel = ScriptLog.LogLevel.FATAL;
            try(ScriptLog scriptLog = new ScriptLog("scriptTest-" + user.getPermissionHolderName(), logLevel, scriptWriter);){
                long time = Common.timer.currentTimeMillis();
                final String script = vo.getScript();
                Object scriptResult;
                if(vo.getResultDataTypeId() != null) {
                    scriptResult = execute(script, vo.getContext(), 
                            new HashMap<String, Object>(), time, vo.getResultDataTypeId(), time, 
                            vo.createScriptPermissions(), scriptLog, setter, null, true);
                }else {
                    scriptResult = execute(script, vo.getContext(), 
                            new HashMap<String, Object>(), time, time, 
                            vo.createScriptPermissions(), scriptLog, setter, null, true);
                }
                result.setResult(scriptResult);
            }

        }catch (ScriptException e) {
            result.addError(new MangoJavaScriptError(createScriptExceptionMessage(e), e.getLineNumber(), e.getColumnNumber()));
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
     * Execute a compiled script
     * @param script
     * @param context
     * @param additionalContext
     * @param runtime - epoch when run
     * @param timestamp - default timestamp in context
     * @param permissions
     * @param log
     * @param setter
     * @param importExclusions
     * @param testRun
     * @return
     * @throws ScriptException
     */
    public Object execute(CompiledScript script, Map<String, IDataPointValueSource> context,
            Map<String, Object> additionalContext, long runtime, long timestamp, 
            ScriptPermissions permissions, ScriptLog log, ScriptPointValueSetter setter,
            List<JsonImportExclusion> importExclusions, boolean testRun) throws ScriptException {
        // Create the wrapper object context.
        ScriptEngine engine = script.getEngine();

        //Prepare the Engine
        Bindings engineScope = prepareEngine(engine, context, additionalContext, runtime, timestamp, permissions, 
                log, setter, importExclusions, testRun);
        
        try {
            return script.eval(engineScope);
        }
        catch (ScriptException e) {
            throw prettyScriptMessage(e);
        }
    }
    
    /**
     * Execute a compiled script and coerce the result to a PointValueTime
     * @param script
     * @param context
     * @param additionalContext
     * @param runtime
     * @param dataTypeId - data type of point value time
     * @param timestamp - default timestamp for result
     * @param permissions
     * @param log
     * @param setter
     * @param importExclusions
     * @param testRun
     * @return
     * @throws ResultTypeException
     * @throws ScriptException
     */
    public PointValueTime execute(CompiledScript script, Map<String, IDataPointValueSource> context,
            Map<String, Object> additionalContext, long runtime, int dataTypeId, long timestamp, 
            ScriptPermissions permissions, ScriptLog log, ScriptPointValueSetter setter,
            List<JsonImportExclusion> importExclusions, boolean testRun) throws ResultTypeException, ScriptException {
        Object result = execute(script, context, 
                additionalContext, runtime, timestamp, 
                permissions, log, setter, importExclusions, testRun);
        
        //Coerce to data type desired
     // Check if a timestamp was set
        Object ts = script.getEngine().getBindings(ScriptContext.ENGINE_SCOPE).get(ScriptUtils.TIMESTAMP_CONTEXT_KEY);

        if (ts != null) {
            // Check the type of the object.
            if (ts instanceof Number)
                // Convert to long
                timestamp = ((Number) ts).longValue();
        }
        DataValue value = ScriptUtils.coerce(result, dataTypeId);
        return new PointValueTime(value, timestamp);
    }
    
    /**
     * Execute a non compiled script for any result
     * @param script
     * @param context
     * @param additionalContext
     * @param runtime
     * @param timestamp
     * @param permissions
     * @param log
     * @param setter
     * @param importExclusions
     * @param testRun
     * @return
     * @throws ScriptException
     */
    public Object execute(String script, Map<String, IDataPointValueSource> context,
            Map<String, Object> additionalContext, long runtime, long timestamp, 
            ScriptPermissions permissions, ScriptLog log, ScriptPointValueSetter setter,
            List<JsonImportExclusion> importExclusions, boolean testRun) throws ScriptException {
        // Create the wrapper object context.
        ScriptEngine engine = ScriptUtils.newEngine();

        //Prepare the Engine
        prepareEngine(engine, context, additionalContext, runtime, timestamp, permissions, 
                log, setter, importExclusions, testRun);
        
        return engine.eval(script);

    }
    
    /**
     * Excecute a non compiled script for a point value time result
     * @param script
     * @param context
     * @param additionalContext
     * @param runtime
     * @param dataTypeId
     * @param timestamp
     * @param permissions
     * @param log
     * @param setter
     * @param importExclusions
     * @param testRun
     * @return
     * @throws ResultTypeException
     * @throws ScriptException
     */
    public PointValueTime execute(String script, Map<String, IDataPointValueSource> context,
            Map<String, Object> additionalContext, long runtime, int dataTypeId, long timestamp, 
            ScriptPermissions permissions, ScriptLog log, ScriptPointValueSetter setter,
            List<JsonImportExclusion> importExclusions, boolean testRun) throws ResultTypeException, ScriptException {
        Object result = null;
        
        // Create the wrapper object context.
        ScriptEngine engine = ScriptUtils.newEngine();

        //Prepare the Engine
        prepareEngine(engine, context, additionalContext, runtime, timestamp, permissions, 
                log, setter, importExclusions, testRun);
        
        //TODO Move into this class
        engine.eval(ScriptUtils.getGlobalFunctions());
        result = engine.eval(script);

        
        //Coerce to data type desired
        // Check if a timestamp was set
        Object ts = engine.getBindings(ScriptContext.ENGINE_SCOPE).get(ScriptUtils.TIMESTAMP_CONTEXT_KEY);

        if (ts != null) {
            // Check the type of the object.
            if (ts instanceof Number)
                // Convert to long
                timestamp = ((Number) ts).longValue();
        }
        
        DataValue value = ScriptUtils.coerce(result, dataTypeId);
        return new PointValueTime(value, timestamp);
    }
    
    /**
     * Create a dumb setter that tracks actions but does not actually set anything
     * @param vo
     * @param result
     * @param permissions
     * @return
     */
    protected ScriptPointValueSetter createValidationSetter(MangoJavaScriptResult result, ScriptPermissions permissions) {
       return new ScriptPointValueSetter(permissions) {
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
     * Generate a translatable message from the exception
     * @param e
     * @return
     */
    public TranslatableMessage createScriptExceptionMessage(ScriptException e) {
        Throwable t = e;
        while (t.getCause() != null)
            t = t.getCause();
        
        String message = t.getMessage();
        Pattern pattern = Pattern.compile("(.*?):(.*?) ([\\s\\S]*)");
        Matcher matcher = pattern.matcher(message);
        if (matcher.find())
            message = matcher.group(3);
        
        return new TranslatableMessage("common.default", message);
    }
    
    //TODO Move to Script Utils?
    protected Bindings prepareEngine(ScriptEngine engine, Map<String, 
            IDataPointValueSource> context, Map<String, Object> additionalContext,
            long runtime, long timestamp, ScriptPermissions permissions, 
            ScriptLog log, ScriptPointValueSetter setter, List<JsonImportExclusion> importExclusions, boolean testRun){
        
        ScriptUtils.prepareEngine(engine);
        
        ScriptUtils.wrapperContext(engine, new WrapperContext(runtime, timestamp));
        Bindings engineScope = engine.getBindings(ScriptContext.ENGINE_SCOPE);

        //Add Permissions Required Utilities
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
        
        //Set the print writer and log
        engine.getContext().setWriter(log.getStdOutWriter());
        engineScope.put(ScriptLog.CONTEXT_KEY, log);

        return engineScope;
    }
    
    public ScriptException prettyScriptMessage(ScriptException e) {
        while (e.getCause() instanceof ScriptException)
            e = (ScriptException) e.getCause();
        //TODO See ScriptError class
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
