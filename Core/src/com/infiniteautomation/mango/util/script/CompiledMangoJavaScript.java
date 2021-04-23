/**
 * Copyright (C) 2019  Infinite Automation Software. All rights reserved.
 */
package com.infiniteautomation.mango.util.script;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.script.CompiledScript;
import javax.script.ScriptEngine;
import javax.script.ScriptException;

import org.apache.commons.lang3.mutable.MutableObject;
import org.springframework.util.Assert;

import com.infiniteautomation.mango.spring.service.MangoJavaScriptService;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.dao.DataPointDao;
import com.serotonin.m2m2.db.dao.DataSourceDao;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.rt.dataImage.DataPointRT;
import com.serotonin.m2m2.rt.dataImage.IDataPointValueSource;
import com.serotonin.m2m2.rt.dataSource.DataSourceRT;
import com.serotonin.m2m2.rt.script.DataPointStateException;
import com.serotonin.m2m2.rt.script.JsonImportExclusion;
import com.serotonin.m2m2.rt.script.ResultTypeException;
import com.serotonin.m2m2.rt.script.ScriptContextVariable;
import com.serotonin.m2m2.rt.script.ScriptError;
import com.serotonin.m2m2.rt.script.ScriptLog;
import com.serotonin.m2m2.rt.script.ScriptPermissionsException;
import com.serotonin.m2m2.rt.script.ScriptPointValueSetter;
import com.serotonin.m2m2.vo.DataPointVO;
import com.serotonin.m2m2.vo.dataPoint.DataPointWithEventDetectors;
import com.serotonin.m2m2.vo.dataSource.DataSourceVO;
import com.serotonin.m2m2.vo.permission.PermissionHolder;

/**
 * Container for Compiled Java Scripts to aid in state management
 * of the various scopes and to help simplify user code.
 *
 * Usage:
 * 1. Constructe compiled script
 * 2. call compile()
 * 3. call initialize() (anytime the context changes but now while executing the script)
 * 4. call execute() when you desire to compute a result from the script
 *
 * @author Terry Packer
 *
 */
public class CompiledMangoJavaScript {

    private CompiledScript compiledScript;

    private final ScriptPointValueSetter setter;
    private final ScriptLog log;

    private final Map<String, Object> additionalContext;
    private final List<ScriptUtility> additionalUtilities;
    private final List<JsonImportExclusion> importExclusions;
    private final boolean testRun;
    private final MangoJavaScriptService service;

    private final MangoJavaScriptResult result;
    private final List<ScriptUtility> utilities;
    private final PermissionHolder permissionHolder;

    //State settings
    private boolean initialized;
    private boolean wrapInFunction;

    /**
     * Create a compiled script container to run live
     *
     * NOTE: Script engine and utilities are run using permissions of vo
     *
     * @param vo
     * @param setter
     * @param log
     * @param result
     * @param service
     */
    public CompiledMangoJavaScript(MangoJavaScript vo, ScriptPointValueSetter setter, ScriptLog log, MangoJavaScriptResult result, MangoJavaScriptService service) {
        this.log = log;
        this.additionalContext = vo.getAdditionalContext() == null ? new HashMap<>(0) : vo.getAdditionalContext();
        this.additionalUtilities = vo.getAdditionalUtilities() == null ? new ArrayList<>(0) : vo.getAdditionalUtilities();
        this.importExclusions = new ArrayList<>(0);
        this.testRun = true;
        this.service = service;
        this.result = result;
        this.utilities = new ArrayList<>(0);
        this.permissionHolder = vo.getPermissions();
        this.setter = setter;
    }



    /**
     * Create a compiled script to run live
     *
     * NOTE: Script engine and utilities are run using permissions of user
     *
     * @param setter
     * @param log
     * @param importExclusions
     * @param permissionHolder
     */
    public CompiledMangoJavaScript(
            ScriptPointValueSetter setter,
            ScriptLog log,
            List<JsonImportExclusion> importExclusions,
            PermissionHolder permissionHolder) {
        this(setter, log, null, null, importExclusions, false, Common.getBean(MangoJavaScriptService.class), permissionHolder);
    }

    /**
     * Complete constructor for compiled script
     * @param setter
     * @param log
     * @param additionalContext
     * @param additionalUtilities
     * @param importExclusions
     * @param testRun
     * @param service
     * @param permissionHolder
     */
    public CompiledMangoJavaScript(
            ScriptPointValueSetter setter,
            ScriptLog log,
            Map<String, Object> additionalContext,
            List<ScriptUtility> additionalUtilities,
            List<JsonImportExclusion> importExclusions,
            boolean testRun,
            MangoJavaScriptService service,
            PermissionHolder permissionHolder) {
        this.setter = setter;
        this.log = log;
        this.additionalContext = additionalContext == null ? new HashMap<>(0) : additionalContext;
        this.additionalUtilities = additionalUtilities == null ? new ArrayList<>(0) : additionalUtilities;
        this.importExclusions = importExclusions == null ? new ArrayList<>(0) : importExclusions;
        this.testRun = testRun;
        this.service = service;

        this.result = new MangoJavaScriptResult();
        this.utilities = new ArrayList<>();
        this.permissionHolder = permissionHolder;
    }

    /**
     * Compile the script and add global bindings
     *
     * @param script
     * @param wrapInFunction - Should the script be wrapped in a function call which is called during execution
     * @throws ScriptError
     */
    public void compile(String script, boolean wrapInFunction) throws ScriptError {
        MutableObject<ScriptError> error = new MutableObject<>();
        try{
            this.compiledScript = service.compile(script, wrapInFunction);
            this.wrapInFunction = wrapInFunction;
        }catch(ScriptError e) {
            error.setValue(e);
        }
        if(error.getValue() != null) {
            throw error.getValue();
        }
    }

    /**
     * Clear the engine scope and initialize it with an expandable context which is returned
     *
     * @throws ScriptError
     * @throws ScriptPermissionsException
     */
    public Map<String, IDataPointValueSource> initialize() throws ScriptError, ScriptPermissionsException {
        Map<String, IDataPointValueSource> context = new HashMap<>();
        this.initialize(context);
        return context;
    }

    /**
     *
     * Clear the engine scope and initialize it with an expandable context which is filled with the ScriptContextVariables and returned
     *
     * @param variables
     * @return
     * @throws ScriptPermissionsException - permission denied executing a command
     * @throws ScriptError - Execution failure, generally will have line and column number with message
     * @throws DataPointStateException - If a point is not enabled or missing (unless testRun is true, then a dummy point is created)
     */
    public Map<String, IDataPointValueSource> initialize(List<ScriptContextVariable> variables) throws ScriptPermissionsException, ScriptError, DataPointStateException {
        Map<String, IDataPointValueSource> context = new HashMap<>();
        if(variables != null) {
            for(ScriptContextVariable variable : variables) {
                DataPointVO dpvo = DataPointDao.getInstance().get(variable.getDataPointId());
                if(dpvo != null) {
                    DataPointRT dprt = Common.runtimeManager.getDataPoint(dpvo.getId());
                    //So we can test with points disabled
                    if(dprt == null) {
                        if(testRun) {
                            if(dpvo.getDefaultCacheSize() == 0) {
                                dpvo.setDefaultCacheSize(1);
                            }
                            //Generate some fake event detectors
                            DataPointWithEventDetectors dp = new DataPointWithEventDetectors(dpvo, new ArrayList<>());
                            DataSourceRT<? extends DataSourceVO> dataSource = DataSourceDao.getInstance().get(dpvo.getDataSourceId()).createDataSourceRT();
                            dprt = new DataPointRT(dp, dpvo.getPointLocator().createRuntime(), dataSource,
                                    null, Common.databaseProxy.newPointValueDao(), Common.databaseProxy.getPointValueCacheDao());
                            dprt.resetValues(); //otherwise variable.value will be empty
                        }else {
                            throw new DataPointStateException(variable.getDataPointId(), new TranslatableMessage(
                                    "event.script.contextPointDisabled", variable.getVariableName(), dpvo.getXid()));
                        }
                    }
                    if(dprt != null)
                        context.put(variable.getVariableName(), dprt);
                }else {
                    throw new DataPointStateException(variable.getDataPointId(), new TranslatableMessage(
                            "event.script.contextPointMissing", variable.getVariableName(), variable.getDataPointId()));
                }
            }
        }
        this.initialize(context);
        return context;
    }

    /**
     * Clear the engine scope and initialize it with a known context which will be stored in the engine
     *
     * @throws ScriptException
     * @throws DataPointStateException
     * @throws ScriptPermissionsException
     */
    public void initialize(Map<String, IDataPointValueSource> context) throws ScriptError, ScriptPermissionsException {
        Assert.notNull(compiledScript, "Script must be compiled first");
        Assert.notNull(context, "Context cannot be null");
        Assert.notNull(log, "Log must be set");
        this.service.initialize(this, context);
        this.initialized = true;
    }

    /**
     * Execute this script expecting a PointValueTime result
     * @param runtime
     * @param timestamp
     * @param resultDataTypeId
     * @return
     * @throws ScriptError
     * @throws ResultTypeException
     * @throws ScriptPermissionsException
     */
    public MangoJavaScriptResult execute(long runtime, long timestamp, Integer resultDataTypeId) throws ScriptError, ResultTypeException, ScriptPermissionsException {
        Assert.notNull(compiledScript, "Script must be compiled first");
        Assert.isTrue(initialized, "Context must be initialized first");
        Assert.notNull(log, "Log must be set");
        service.execute(this, runtime, timestamp, resultDataTypeId);
        return result;
    }

    /**
     * Execute this script expecting any type of result
     * @param runtime
     * @param timestamp
     * @return
     * @throws ScriptError
     * @throws ScriptPermissionsException
     */
    public MangoJavaScriptResult execute(long runtime, long timestamp) throws ScriptError, ScriptPermissionsException {
        Assert.notNull(compiledScript, "Script must be compiled first");
        Assert.isTrue(initialized, "Context must be initialized first");
        Assert.notNull(log, "Log must be set");
        service.execute(this, runtime, timestamp);
        return result;
    }

    public ScriptPointValueSetter getSetter() {
        return setter;
    }
    public ScriptLog getLog() {
        return log;
    }
    public boolean isTestRun() {
        return testRun;
    }
    public List<ScriptUtility> getUtilities() {
        return utilities;
    }
    public List<ScriptUtility> getAdditionalUtilities() {
        return additionalUtilities;
    }
    public Map<String, Object> getAdditionalContext() {
        return additionalContext;
    }
    public List<JsonImportExclusion> getImportExclusions() {
        return importExclusions;
    }
    public CompiledScript getCompiledScript() {
        Assert.notNull(compiledScript, "Script must be compiled first");
        return compiledScript;
    }
    public ScriptEngine getEngine() {
        Assert.notNull(compiledScript, "Script must be compiled first");
        return compiledScript.getEngine();
    }
    public MangoJavaScriptResult getResult() {
        return result;
    }
    public boolean isCompiled() {
        return compiledScript != null;
    }
    public PermissionHolder getPermissionHolder() {
        return this.permissionHolder;
    }
    public boolean isWrapInFunction() {
        return wrapInFunction;
    }
}
