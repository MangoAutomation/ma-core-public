/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */

package com.infiniteautomation.mango.spring.service;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.BiFunction;

import javax.script.Bindings;
import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.script.SimpleBindings;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.infiniteautomation.mango.pointvaluecache.PointValueCache;
import com.infiniteautomation.mango.spring.components.RunAs;
import com.infiniteautomation.mango.spring.script.engines.NashornScriptEngineDefinition;
import com.infiniteautomation.mango.util.exception.ValidationException;
import com.infiniteautomation.mango.util.script.CompiledMangoJavaScript;
import com.infiniteautomation.mango.util.script.MangoJavaScript;
import com.infiniteautomation.mango.util.script.MangoJavaScriptAction;
import com.infiniteautomation.mango.util.script.MangoJavaScriptError;
import com.infiniteautomation.mango.util.script.MangoJavaScriptResult;
import com.infiniteautomation.mango.util.script.ScriptUtility;
import com.serotonin.ShouldNeverHappenException;
import com.serotonin.db.pair.IntStringPair;
import com.serotonin.io.NullWriter;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.DataType;
import com.serotonin.m2m2.db.dao.DataPointDao;
import com.serotonin.m2m2.db.dao.PointValueDao;
import com.serotonin.m2m2.i18n.ProcessMessage.Level;
import com.serotonin.m2m2.i18n.ProcessResult;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.i18n.Translations;
import com.serotonin.m2m2.module.MangoJavascriptContextObjectDefinition;
import com.serotonin.m2m2.module.ModuleRegistry;
import com.serotonin.m2m2.module.ScriptEngineDefinition;
import com.serotonin.m2m2.module.ScriptSourceDefinition;
import com.serotonin.m2m2.module.definitions.permissions.DataSourcePermissionDefinition;
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
import com.serotonin.m2m2.rt.script.MultistatePointWrapper;
import com.serotonin.m2m2.rt.script.NumericPointWrapper;
import com.serotonin.m2m2.rt.script.ResultTypeException;
import com.serotonin.m2m2.rt.script.ScriptContextVariable;
import com.serotonin.m2m2.rt.script.ScriptError;
import com.serotonin.m2m2.rt.script.ScriptLog;
import com.serotonin.m2m2.rt.script.ScriptPermissionsException;
import com.serotonin.m2m2.rt.script.ScriptPointValueSetter;
import com.serotonin.m2m2.rt.script.UnitUtility;
import com.serotonin.m2m2.rt.script.WrapperContext;
import com.serotonin.m2m2.util.VarNames;
import com.serotonin.m2m2.util.log.LogLevel;
import com.serotonin.m2m2.util.log.NullPrintWriter;
import com.serotonin.m2m2.vo.DataPointVO;
import com.serotonin.m2m2.vo.permission.PermissionException;
import com.serotonin.m2m2.vo.permission.PermissionHolder;

/**
 * Service to allow running and validating Mango JavaScript scripts
 *
 * @author Terry Packer
 *
 */
@Service
public class MangoJavaScriptService {

    public static final String SCRIPT_PREFIX = "function __scriptExecutor__() {";
    public static final String SCRIPT_SUFFIX = "\n} __scriptExecutor__();";

    public static final String WRAPPER_CONTEXT_KEY = "CONTEXT";
    public static final String POINTS_CONTEXT_KEY = "POINTS";
    public static final String POINTS_MAP_KEY = "CONTEXT_POINTS";
    public static final String SELF_POINT_XID_KEY = "SELF_POINT_XID";
    public static final String EXTERNAL_POINTS_KEY = "EXTERNAL_POINTS";
    public static final String EXTERNAL_POINTS_ARRAY_KEY = "EXTERNAL_POINTS_ARRAY";
    public static final String TIMESTAMP_CONTEXT_KEY = "TIMESTAMP";
    public static final DataValue UNCHANGED = new BinaryValue(false);
    public static final String UNCHANGED_KEY = "UNCHANGED";

    private static final String DATE_FORMAT = "dd MMM yyyy HH:mm:ss z";

    private static final Object globalFunctionsLock = new Object();

    private final PermissionService permissionService;
    private final DataSourcePermissionDefinition dataSourcePermissionDefinition;
    private final DataPointService dataPointService;
    private final RunAs runAs;
    private final NashornScriptEngineDefinition nashornEngineDefinition;
    private final ScriptEngineFactory nashornFactory;
    private final PointValueDao pointValueDao;
    private final PointValueCache pointValueCache;

    @Autowired
    public MangoJavaScriptService(PermissionService permissionService,
                                  @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection") DataSourcePermissionDefinition dataSourcePermissionDefinition,
                                  DataPointService dataPointService, RunAs runAs,
                                  ScriptEngineManager manager,
                                  List<ScriptEngineDefinition> engineDefinitions,
                                  PointValueDao pointValueDao,
                                  PointValueCache pointValueCache) {
        this.dataPointService = dataPointService;
        this.permissionService = permissionService;
        this.dataSourcePermissionDefinition = dataSourcePermissionDefinition;
        this.runAs = runAs;

        this.nashornFactory = manager.getEngineFactories().stream()
                .filter(f -> f.getNames().contains("nashorn"))
                .findFirst()
                .orElse(null);

        this.nashornEngineDefinition = engineDefinitions.stream()
                .filter(def -> nashornFactory != null && def.supports(nashornFactory))
                .filter(def -> def instanceof NashornScriptEngineDefinition)
                .map(def -> (NashornScriptEngineDefinition) def)
                .findFirst()
                .orElse(null);
        this.pointValueDao = pointValueDao;
        this.pointValueCache = pointValueCache;
    }

    /**
     * Validate a script with its parts
     */
    public ProcessResult validate(MangoJavaScript vo) {
        ProcessResult result = new ProcessResult();
        PermissionHolder user = Common.getUser();

        //Ensure the user has ALL of the permissions as we will likely test/run this script
        if(!permissionService.hasSupersetOfRoles(user, vo.getPermissions()))
            result.addContextualMessage("permissions", "permission.exception.doesNotHaveRequiredPermission");

        validateContext(vo.getContext(), result);

        //Can't validate a null script
        if(StringUtils.isEmpty(vo.getScript()))
            result.addContextualMessage("script", "validate.invalidValue");

        return result;
    }

    /**
     * Validate a script context
     */
    public void validateContext(List<ScriptContextVariable> context, ProcessResult result) {
        PermissionHolder user = Common.getUser();
        //Validate the context, can we read all points and are the var names valid
        List<String> varNameSpace = new ArrayList<>();
        for(ScriptContextVariable var : context) {
            String varName = var.getVariableName();
            DataPointVO dp = DataPointDao.getInstance().get(var.getDataPointId());
            if(dp == null)
                result.addContextualMessage("context", "javascript.validate.missingContextPoint", varName);
            else {
                if(!dataPointService.hasReadPermission(user, dp))
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
     */
    public void ensureValid(MangoJavaScript vo) throws ValidationException {
        PermissionHolder user = Common.getUser();
        permissionService.ensurePermission(user, dataSourcePermissionDefinition.getPermission());
        ProcessResult result = validate(vo);
        if(!result.isValid())
            throw new ValidationException(result);
    }

    /**
     * Test a script
     */
    public MangoJavaScriptResult testScript(MangoJavaScript vo, String noChangeKey) throws ValidationException, PermissionException {
        return testScript(vo, (result, holder) -> createValidationSetter(result), noChangeKey);
    }

    /**
     * Test a script using the default no change key
     */
    public MangoJavaScriptResult testScript(MangoJavaScript vo) throws ValidationException, PermissionException {
        return testScript(vo, (result, holder) -> createValidationSetter(result), "eventHandlers.script.successUnchanged");
    }

    /**
     */
    public MangoJavaScriptResult testScript(MangoJavaScript vo, BiFunction<MangoJavaScriptResult, PermissionHolder, ScriptPointValueSetter> createSetter, String noChangeKey) {
        PermissionHolder user = Common.getUser();

        ensureValid(vo);
        final StringWriter scriptOut = new StringWriter();
        MangoJavaScriptResult result = new  MangoJavaScriptResult();
        try {
            final PrintWriter scriptWriter = new PrintWriter(scriptOut);
            try(ScriptLog scriptLog = new ScriptLog("scriptTest-" + user.getPermissionHolderName(), vo.getLogLevel(), scriptWriter)){
                CompiledMangoJavaScript script = new CompiledMangoJavaScript(
                        vo, createSetter.apply(result, vo.getPermissions()), scriptLog, result, this, pointValueDao, pointValueCache);

                script.compile(vo.getScript(), vo.isWrapInFunction());
                script.initialize(vo.getContext());

                long time = Common.timer.currentTimeMillis();
                runAs.runAsCallable(vo.getPermissions(), () -> {
                    if(vo.getResultDataType() != null) {
                        script.execute(time, time, vo.getResultDataType());
                        //Convert the UNCHANGED value
                        Object o = script.getResult().getResult();
                        if(o instanceof PointValueTime && ((PointValueTime)o).getValue() == UNCHANGED) {
                            //TODO fix this display hack:
                            String unchanged = new TranslatableMessage(noChangeKey).translate(Translations.getTranslations(user.getLocaleObject()));
                            script.getResult().setResult(new PointValueTime(unchanged, ((PointValueTime)o).getTime()));
                        }
                    }else {
                        script.execute(time, time);
                    }
                    return null;
                });
            }
        }catch (ScriptError e) {
            //The script exception should be clean as both compile() and execute() clean it
            result.addError(new MangoJavaScriptError(e.getTranslatableMessage(), e.getLineNumber(), e.getColumnNumber()));
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
     * The preferred way to execute a script
     */
    public MangoJavaScriptResult executeScript(MangoJavaScript vo, ScriptPointValueSetter setter) throws ValidationException, PermissionException {
        PermissionHolder user = Common.getUser();

        ensureValid(vo);
        MangoJavaScriptResult result = new MangoJavaScriptResult();
        final Writer scriptOut;
        final PrintWriter scriptWriter;
        if(vo.isReturnLogOutput()) {
            scriptOut = new StringWriter();
            scriptWriter = new PrintWriter(scriptOut);
        }else {
            NullWriter writer = new NullWriter();
            scriptWriter = new NullPrintWriter(writer);
            scriptOut = writer;
        }

        try {
            try(ScriptLogExtender scriptLog = new ScriptLogExtender("scriptTest-" + user.getPermissionHolderName(), vo.getLogLevel(), scriptWriter, vo.getLog(), vo.isCloseLog())){

                CompiledMangoJavaScript script = new CompiledMangoJavaScript(
                        vo, setter, scriptLog, result, this, pointValueDao, pointValueCache);

                script.compile(vo.getScript(), vo.isWrapInFunction());
                script.initialize(vo.getContext());

                long time = Common.timer.currentTimeMillis();
                runAs.runAsCallable(script.getPermissionHolder(), () -> {
                    if(vo.getResultDataType() != null) {
                        script.execute(time, time, vo.getResultDataType());
                    }else {
                        script.execute(time, time);
                    }
                    return null;
                });
            }
        }catch (ScriptError e) {
            //The script exception should be clean as both compile() and execute() clean it
            result.addError(new MangoJavaScriptError(e.getTranslatableMessage(), e.getLineNumber(), e.getColumnNumber()));
        }catch(ResultTypeException | DataPointStateException e) {
            result.addError(new MangoJavaScriptError(e.getTranslatableMessage()));
        } catch (Exception e) {
            result.addError(new MangoJavaScriptError(e.getMessage()));
        }finally {
            if(vo.isReturnLogOutput())
                result.setScriptOutput(scriptOut.toString());
        }
        return result;
    }

    /**
     * Compile a script to be run and add global bindings
     *
     */
    public CompiledScript compile(String script, boolean wrapInFunction) throws ScriptError {
        try {
            final ScriptEngine engine = newEngine();

            // Add constants to the context
            Bindings globalBindings = new SimpleBindings();

            //left here for legacy compatibility
            globalBindings.put("SECOND", Common.TimePeriods.SECONDS);
            globalBindings.put("MINUTE", Common.TimePeriods.MINUTES);
            globalBindings.put("HOUR", Common.TimePeriods.HOURS);
            globalBindings.put("DAY", Common.TimePeriods.DAYS);
            globalBindings.put("WEEK", Common.TimePeriods.WEEKS);
            globalBindings.put("MONTH", Common.TimePeriods.MONTHS);
            globalBindings.put("YEAR", Common.TimePeriods.YEARS);

            for(IntStringPair isp : Common.TIME_PERIOD_CODES.getIdKeys())
                globalBindings.put(Common.TIME_PERIOD_CODES.getCode(isp.getKey()), isp.getKey());

            for(IntStringPair isp : Common.ROLLUP_CODES.getIdKeys(Common.Rollups.NONE))
                globalBindings.put(Common.ROLLUP_CODES.getCode(isp.getKey()), isp.getKey());

            //Add in Additional Utilities with Global Scope
            globalBindings.put(DateTimeUtility.CONTEXT_KEY, new DateTimeUtility());
            globalBindings.put(UnitUtility.CONTEXT_KEY, new UnitUtility());

            engine.setBindings(globalBindings, ScriptContext.GLOBAL_SCOPE);

            String toCompile;
            if(wrapInFunction) {
                toCompile = SCRIPT_PREFIX + script + SCRIPT_SUFFIX;
            }else {
                toCompile = script;
            }

            return ((Compilable)engine).compile(toCompile);
        }catch(ScriptException e) {
            throw ScriptError.create(e, wrapInFunction);
        }
    }

    /**
     * Reset the engine scope of a script and initialize for running
     * @param context - if provided points will be wrapped with script's setter (alternatively use script.addToContext()
     */
    public void initialize(CompiledMangoJavaScript script, Map<String, IDataPointValueSource> context) throws ScriptError {
        //TODO assert compiled
        //TODO assert permissions to execute global scripts
        //TODO assert setter not null

        if (context == null) {
            context = new HashMap<>();
        }

        Bindings engineScope = script.getEngine().getBindings(ScriptContext.ENGINE_SCOPE);
        //TODO Clear engine scope completely?

        engineScope.put(MangoJavaScriptService.UNCHANGED_KEY, MangoJavaScriptService.UNCHANGED);

        Set<String> points = new HashSet<>();
        engineScope.put(MangoJavaScriptService.POINTS_CONTEXT_KEY, points);
        //Holder for modifying timestamps of meta points, in Engine Scope so it can be modified by all
        engineScope.put(MangoJavaScriptService.TIMESTAMP_CONTEXT_KEY, null);

        if(script.getPermissionHolder() != null) {
            script.getUtilities().clear();
            for(MangoJavascriptContextObjectDefinition def : ModuleRegistry.getMangoJavascriptContextObjectDefinitions()) {
                ScriptUtility util = def.initializeContextObject(script);
                util.setScriptLog(script.getLog());
                util.setResult(script.getResult());
                util.takeContext(script.getEngine(), engineScope, script.getSetter(), script.getImportExclusions(), script.isTestRun());
                engineScope.put(util.getContextKey(), util);
                script.getUtilities().add(util);
            }
            //Initialize additional utilities
            for(ScriptUtility util : script.getAdditionalUtilities()) {
                util.setScriptLog(script.getLog());
                util.setResult(script.getResult());
                util.takeContext(script.getEngine(), engineScope, script.getSetter(), script.getImportExclusions(), script.isTestRun());
                engineScope.put(util.getContextKey(), util);
            }
        }

        Set<Entry<String,Object>> entries = script.getAdditionalContext().entrySet();
        for(Entry<String,Object> entry: entries)
            engineScope.put(entry.getKey(), entry.getValue());

        String selfPointXid = (String) script.getAdditionalContext().get(SELF_POINT_XID_KEY);

        Map<String, AbstractPointWrapper> external = new HashMap<>();
        for (String varName : context.keySet()) {
            IDataPointValueSource point = context.get(varName);
            AbstractPointWrapper wrapped = wrapPoint(script.getEngine(), point, script.getSetter());
            engineScope.put(varName, wrapped);
            points.add(varName);
            if (!point.getVO().getXid().equals(selfPointXid)) {
                external.put(varName, wrapped);
            }
        }
        engineScope.put(EXTERNAL_POINTS_KEY, external);
        engineScope.put(EXTERNAL_POINTS_ARRAY_KEY, external.values());
        engineScope.put(POINTS_MAP_KEY, context);

        //Set the print writer and log
        script.getEngine().getContext().setWriter(script.getLog().getStdOutWriter());
        engineScope.put(ScriptLog.CONTEXT_KEY, script.getLog());

        try {
            script.getEngine().eval(getGlobalFunctions());
        } catch (ScriptException e) {
            throw ScriptError.create(e, script.isWrapInFunction());
        } catch (RuntimeException e) {
            // Nashorn seems to like to wrap exceptions in RuntimeException
            if (e.getCause() instanceof ScriptPermissionsException)
                throw (ScriptPermissionsException) e.getCause();
            else if (e.getCause() != null)
                throw ScriptError.createFromThrowable(e.getCause());
            else
                throw new ShouldNeverHappenException(e);
        }
    }

    /**
     * Reset result and execute script for any type of result
     *
     */
    public void execute(CompiledMangoJavaScript script, long runtime, long timestamp) throws ScriptError, ScriptPermissionsException {
        try {
            runAs.runAsCallable(script.getPermissionHolder(), () -> {
                script.getResult().reset();

                //Setup the wrapper context
                Bindings engineScope = script.getEngine().getBindings(ScriptContext.ENGINE_SCOPE);
                engineScope.put(MangoJavaScriptService.WRAPPER_CONTEXT_KEY, new WrapperContext(runtime, timestamp));

                //Ensure the result is available to the utilities
                for(ScriptUtility util : script.getUtilities()) {
                    util.setResult(script.getResult());
                }

                //Initialize additional utilities
                for(ScriptUtility util : script.getAdditionalUtilities())
                    util.setResult(script.getResult());

                Object resultObject = script.getCompiledScript().eval();
                script.getResult().setResult(resultObject);

                return null;
            });
        }catch(ScriptException e) {
            throw ScriptError.create(e, script.isWrapInFunction());
        }catch (RuntimeException e) {
            //Nashorn seems to like to wrap exceptions in RuntimeException
            if(e.getCause() instanceof ScriptPermissionsException)
                throw (ScriptPermissionsException)e.getCause();
            else
                throw new ShouldNeverHappenException(e);
        }catch(Exception e) {
            throw new ShouldNeverHappenException(e);
        }
    }

    /**
     * Reset the result and execute for PointValueTime result
     */
    public void execute(CompiledMangoJavaScript script, long runtime, long timestamp, DataType resultDataType) throws ScriptError, ResultTypeException, ScriptPermissionsException {
        try {
            runAs.runAsCallable(script.getPermissionHolder(), () -> {
                execute(script, runtime, timestamp);

                Object ts = script.getEngine().getBindings(ScriptContext.ENGINE_SCOPE).get(MangoJavaScriptService.TIMESTAMP_CONTEXT_KEY);
                long scriptRuntime;
                if (ts != null) {
                    // Check the type of the object.
                    if (ts instanceof Number) {
                        // Convert to long
                        scriptRuntime = ((Number) ts).longValue();
                    }else {
                        scriptRuntime = timestamp;
                    }
                }else {
                    scriptRuntime = timestamp;
                }
                Object resultObject = script.getResult().getResult();
                DataValue value = coerce(resultObject, resultDataType);
                script.getResult().setResult(new PointValueTime(value, scriptRuntime));
                return null;
            });

        }catch(ScriptException e) {
            throw ScriptError.create(e, script.isWrapInFunction());
        }catch (RuntimeException e) {
            //Nashorn seems to like to wrap exceptions in RuntimeException
            if(e.getCause() instanceof ScriptPermissionsException)
                throw (ScriptPermissionsException)e.getCause();
            else
                throw new ShouldNeverHappenException(e);
        }catch(Exception e) {
            if(e instanceof ResultTypeException) {
                throw (ResultTypeException)e;
            }
            throw new ShouldNeverHappenException(e);
        }
    }

    /**
     * Create a dumb setter that tracks actions but does not actually set anything
     */
    public ScriptPointValueSetter createValidationSetter(MangoJavaScriptResult result) {
        PermissionHolder permissions = Common.getUser();

        return new ScriptPointValueSetter(permissions) {

            @Override
            public void set(IDataPointValueSource point, Object value, long timestamp, String annotation) {
                DataPointRT dprt = (DataPointRT) point;
                if(!dprt.getVO().getPointLocator().isSettable()) {
                    result.addAction(new MangoJavaScriptAction(new TranslatableMessage("javascript.validate.pointNotSettable", dprt.getVO().getExtendedName()), Level.error));
                    return;
                }

                if(!dataPointService.hasSetPermission(permissions, dprt.getVO())) {
                    result.addAction(new MangoJavaScriptAction(new TranslatableMessage("javascript.validate.pointPermissionsFailure", dprt.getVO().getXid()), Level.warning));
                    return;
                }
                if(annotation != null)
                    result.addAction(new MangoJavaScriptAction(new TranslatableMessage("javascript.validate.setPointValueAnnotation", dprt.getVO().getExtendedName(), value,
                            getDateFormat().format(new Date(timestamp)), annotation)));
                else
                    result.addAction(new MangoJavaScriptAction(new TranslatableMessage("javascript.validate.setPointValue", dprt.getVO().getExtendedName(), value,
                            getDateFormat().format(new Date(timestamp)))));
            }

            @Override
            protected void setImpl(IDataPointValueSource point, Object value, long timestamp, String annotation) {
                //not really setting
            }
        };

    }

    /* Utilities for Script Execution */
    /**
     * Create a new script engine
     */
    public ScriptEngine newEngine() {
        if (nashornFactory == null || nashornEngineDefinition == null) {
            throw new UnsupportedOperationException("Nashorn engine is not available");
        }

        PermissionHolder user = Common.getUser();
        permissionService.ensurePermission(user, nashornEngineDefinition.requiredPermission());
        return nashornEngineDefinition.createScriptEngine(nashornFactory, permissionService.hasAdminRole(user) ? null : c -> false);
    }

    /**
     * Wrap a data point for insertion into script context
     */
    public AbstractPointWrapper wrapPoint(ScriptEngine engine, IDataPointValueSource point,
            ScriptPointValueSetter setter) {
        DataType dt = point.getDataType();
        if (dt == DataType.BINARY)
            return new BinaryPointWrapper(point, engine, setter);
        if (dt == DataType.MULTISTATE)
            return new MultistatePointWrapper(point, engine, setter);
        if (dt == DataType.NUMERIC)
            return new NumericPointWrapper(point, engine, setter);
        if (dt == DataType.ALPHANUMERIC)
            return new AlphanumericPointWrapper(point, engine, setter);
        throw new ShouldNeverHappenException("Unknown data type id: " + point.getDataType());
    }

    private static String FUNCTIONS;

    /**
     * Get all Module defined functions
     */
    public String getGlobalFunctions() {
        synchronized(globalFunctionsLock) {
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
    }

    /**
     * Clear all functions so they are re-loaded on next 'get'
     */
    public void clearGlobalFunctions() {
        synchronized(globalFunctionsLock) {
            FUNCTIONS = null;
        }
    }

    /**
     * Add a data point to the engine scope bindings.
     *
     * Only to be called while script is not executing.
     *
     */
    @SuppressWarnings("unchecked")
    public void addToContext(ScriptEngine engine, String varName, DataPointRT dprt, ScriptPointValueSetter setCallback) {
        Bindings engineBindings = engine.getBindings(ScriptContext.ENGINE_SCOPE);

        AbstractPointWrapper wrapper = wrapPoint(engine, dprt, setCallback);
        engineBindings.put(varName, wrapper);

        Map<String, IDataPointValueSource> context = (Map<String, IDataPointValueSource>)engineBindings.get(POINTS_MAP_KEY);
        context.put(varName, dprt);

        Set<String> points = (Set<String>) engineBindings.get(POINTS_CONTEXT_KEY);
        if (points != null) {
            points.remove(varName);
            points.add(varName);
        }

        String selfPointXid = (String) engineBindings.get(SELF_POINT_XID_KEY);
        Map<String, AbstractPointWrapper> external = (Map<String, AbstractPointWrapper>) engineBindings.get(EXTERNAL_POINTS_KEY);
        if (!dprt.getVO().getXid().equals(selfPointXid)) {
            external.put(varName, wrapper);
        }
    }

    /**
     * Remove a data point from the engine scope bindings.
     *
     * Only to be called while the script is not executing.
     */
    @SuppressWarnings("unchecked")
    public void removeFromContext(ScriptEngine engine, String varName) {
        Bindings engineBindings = engine.getBindings(ScriptContext.ENGINE_SCOPE);

        Map<String, IDataPointValueSource> context = (Map<String, IDataPointValueSource>)engineBindings.get(POINTS_MAP_KEY);
        context.remove(varName);

        Set<String> points = (Set<String>) engineBindings.get(POINTS_CONTEXT_KEY);
        if (points != null) {
            points.remove(varName);
        }
        engineBindings.remove(varName);

        Map<String, AbstractPointWrapper> external = (Map<String, AbstractPointWrapper>) engineBindings.get(EXTERNAL_POINTS_KEY);
        external.remove(varName);
    }

    /**
     * Coerce an object into a DataValue
     */
    public DataValue coerce(Object input, DataType toDataType) throws ResultTypeException {
        DataValue value;

        if(input instanceof DataValue)
            return (DataValue)input;
        else if(input instanceof PointValueTime)
            return ((PointValueTime)input).getValue();

        if (input == null) {
            if (toDataType == DataType.BINARY)
                value = new BinaryValue(false);
            else if (toDataType == DataType.MULTISTATE)
                value = new MultistateValue(0);
            else if (toDataType == DataType.NUMERIC)
                value = new NumericValue(0);
            else if (toDataType == DataType.ALPHANUMERIC)
                value = new AlphanumericValue("");
            else
                value = null;
        }
        else if (input instanceof AbstractPointWrapper) {
            value = ((AbstractPointWrapper) input).getValueImpl();
            if ((value != null)&&(value.getDataType() != toDataType))
                throw new ResultTypeException(new TranslatableMessage("event.script.convertError", input,
                        toDataType.getDescription()));
        }
        // See if the type matches.
        else if (toDataType == DataType.BINARY && input instanceof Boolean)
            value = new BinaryValue((Boolean) input);
        else if (toDataType == DataType.MULTISTATE) {
            if (input instanceof Number)
                value = new MultistateValue(((Number) input).intValue());
            else if (input instanceof String) {
                try {
                    value = new MultistateValue(Integer.parseInt((String) input));
                }
                catch (NumberFormatException e) {
                    throw new ResultTypeException(new TranslatableMessage("event.script.convertError", input,
                            toDataType.getDescription()));
                }
            }
            else
                throw new ResultTypeException(new TranslatableMessage("event.script.convertError", input,
                        toDataType.getDescription()));
        }
        else if (toDataType == DataType.NUMERIC) {
            if (input instanceof Number)
                value = new NumericValue(((Number) input).doubleValue());
            else if (input instanceof String) {
                try {
                    value = new NumericValue(Double.parseDouble((String) input));
                }
                catch (NumberFormatException e) {
                    throw new ResultTypeException(new TranslatableMessage("event.script.convertError", input,
                            toDataType.getDescription()));
                }
            }
            else
                throw new ResultTypeException(new TranslatableMessage("event.script.convertError", input,
                        toDataType.getDescription()));
        }
        else if (toDataType == DataType.ALPHANUMERIC)
            value = new AlphanumericValue(input.toString());
        else
            // If not, ditch it.
            throw new ResultTypeException(new TranslatableMessage("event.script.convertError", input,
                    toDataType.getDescription()));

        return value;
    }

    public SimpleDateFormat getDateFormat() {
        return new SimpleDateFormat(DATE_FORMAT);
    }

    public PermissionService getPermissionService() {
        return this.permissionService;
    }

    private static class ScriptLogExtender extends ScriptLog {

        private final ScriptLog logger;
        private final boolean closeExtendedLog;

        /**
         */
        public ScriptLogExtender(String id, LogLevel level, PrintWriter out, ScriptLog logger, boolean closeExtendedLog) {
            super(id, level, out);
            this.logger = logger;
            this.closeExtendedLog = closeExtendedLog;
        }


        @Override
        public void trace(Object o) {
            if(logger != null)
                logger.trace(o);
            super.trace(o);
        }

        @Override
        public void debug(Object o) {
            if(logger != null)
                logger.debug(o);
            super.debug(o);
        }

        @Override
        public void info(Object o) {
            if(logger != null)
                logger.info(o);
            super.info(o);
        }

        @Override
        public void warn(Object o) {
            if(logger != null)
                logger.warn(o);
            super.warn(o);
        }

        @Override
        public void error(Object o) {
            if(logger != null)
                logger.error(o);
            super.error(o);
        }

        @Override
        public void fatal(Object o) {
            if(logger != null)
                logger.fatal(o);
            super.fatal(o);
        }

        @Override
        public PrintWriter getStdOutWriter() {
            if(logger != null)
                return logger.getStdOutWriter();
            else
                return super.getStdOutWriter();
        }

        /**
         * Get the file currently being written to
         */
        @Override
        public File getFile(){
            if(logger != null)
                return logger.getFile();
            else
                return super.getFile();
        }

        @Override
        public PrintWriter getPrintWriter() {
            if(logger != null)
                return logger.getPrintWriter();
            else
                return super.getPrintWriter();
        }

        @Override
        public void close() {
            if(logger != null && closeExtendedLog)
                logger.close();
            else
                super.close();
        }

        @Override
        public String getId() {
            if(logger != null)
                return logger.getId();
            else
                return super.getId();

        }

        @Override
        public LogLevel getLogLevel() {
            if(logger != null)
                return logger.getLogLevel();
            else
                return super.getLogLevel();
        }

        @Override
        public void setLogLevel(LogLevel logLevel) {
            if(logger != null)
                logger.setLogLevel(logLevel);
            super.setLogLevel(logLevel);
        }

        //
        // Trace
        @Override
        public boolean isTraceEnabled() {
            if(logger != null && logger.isTraceEnabled())
                return true;
            else
                return super.isTraceEnabled();
        }

        @Override
        public void trace(String s) {
            if(logger != null)
                logger.trace(s);
            super.trace(s);
        }

        @Override
        public void trace(Throwable t) {
            if(logger != null)
                logger.trace(t);
            super.trace(t);
        }

        @Override
        public void trace(String s, Throwable t) {
            if(logger != null)
                logger.trace(s, t);
            super.trace(s, t);
        }

        //
        // Debug
        @Override
        public boolean isDebugEnabled() {
            if(logger != null && logger.isDebugEnabled())
                return true;
            else
                return super.isDebugEnabled();
        }

        @Override
        public void debug(String s) {
            if(logger != null)
                logger.debug(s);
            super.debug(s);
        }

        @Override
        public void debug(Throwable t) {
            if(logger != null)
                logger.debug(t);
            super.debug(t);
        }

        @Override
        public void debug(String s, Throwable t) {
            if(logger != null)
                logger.debug(s, t);
            super.debug(s, t);
        }

        //
        // Info
        @Override
        public boolean isInfoEnabled() {
            if(logger != null && logger.isInfoEnabled())
                return true;
            else
                return super.isInfoEnabled();
        }

        @Override
        public void info(String s) {
            if(logger != null)
                logger.info(s);
            super.info(s);
        }

        @Override
        public void info(Throwable t) {
            if(logger != null)
                logger.info(t);
            super.info(t);
        }

        @Override
        public void info(String s, Throwable t) {
            if(logger != null)
                logger.info(s, t);
            super.info(s, t);
        }

        //
        // Warn
        @Override
        public boolean isWarnEnabled() {
            if(logger != null && logger.isWarnEnabled())
                return true;
            else
                return super.isWarnEnabled();
        }

        @Override
        public void warn(String s) {
            if(logger != null)
                logger.warn(s);
            super.warn(s);
        }

        @Override
        public void warn(Throwable t) {
            if(logger != null)
                logger.warn(t);
            super.warn(t);
        }

        @Override
        public void warn(String s, Throwable t) {
            if(logger != null)
                logger.warn(s, t);
            super.warn(s, t);
        }

        //
        // Error
        @Override
        public boolean isErrorEnabled() {
            if(logger != null && logger.isErrorEnabled())
                return true;
            else
                return super.isErrorEnabled();
        }

        @Override
        public void error(String s) {
            if(logger != null)
                logger.error(s);
            super.error(s);
        }

        @Override
        public void error(Throwable t) {
            if(logger != null)
                logger.error(t);
            super.error(t);
        }

        @Override
        public void error(String s, Throwable t) {
            if(logger != null)
                logger.error(s, t);
            super.error(s, t);
        }

        //
        // Fatal
        @Override
        public boolean isFatalEnabled() {
            if(logger != null && logger.isFatalEnabled())
                return true;
            else
                return super.isFatalEnabled();
        }

        @Override
        public void fatal(String s) {
            if(logger != null)
                logger.fatal(s);
            super.fatal(s);
        }

        @Override
        public void fatal(Throwable t) {
            if(logger != null)
                logger.fatal(t);
            super.fatal(t);
        }

        @Override
        public void fatal(String s, Throwable t) {
            if(logger != null)
                logger.fatal(s, t);
            super.fatal(s, t);
        }


        @Override
        public boolean trouble() {
            if(logger != null)
                return logger.trouble();
            else
                return super.trouble();
        }

        /**
         * List all the files
         */
        @Override
        public File[] getFiles(){
            if(logger != null)
                return logger.getFiles();
            else
                return super.getFiles();
        }
    }
}
