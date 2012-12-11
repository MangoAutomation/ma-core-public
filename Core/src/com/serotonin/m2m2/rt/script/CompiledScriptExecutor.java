/*
    Copyright (C) 2006-2011 Serotonin Software Technologies Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.rt.script;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.script.Bindings;
import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.script.SimpleBindings;

import com.serotonin.ShouldNeverHappenException;
import com.serotonin.db.pair.IntStringPair;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.DataTypes;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.rt.dataImage.DataPointRT;
import com.serotonin.m2m2.rt.dataImage.IDataPointValueSource;
import com.serotonin.m2m2.rt.dataImage.PointValueTime;
import com.serotonin.m2m2.rt.dataImage.types.AlphanumericValue;
import com.serotonin.m2m2.rt.dataImage.types.BinaryValue;
import com.serotonin.m2m2.rt.dataImage.types.DataValue;
import com.serotonin.m2m2.rt.dataImage.types.MultistateValue;
import com.serotonin.m2m2.rt.dataImage.types.NumericValue;

/**
 * @author Matthew Lohbihler
 */
public class CompiledScriptExecutor {
    private static final String SCRIPT_PREFIX = "function __scriptExecutor__() {";
    private static final String SCRIPT_SUFFIX = "\r\n}\r\n__scriptExecutor__();";
    private static ScriptEngine ENGINE;

    public static Map<String, IDataPointValueSource> convertContext(List<IntStringPair> context)
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

    public static CompiledScript compile(String script) throws ScriptException {
        script = SCRIPT_PREFIX + script + SCRIPT_SUFFIX + ScriptUtils.getGlobalFunctions();
        ensureInit();
        return ((Compilable) ENGINE).compile(script);
    }

    public static PointValueTime execute(CompiledScript script, Map<String, IDataPointValueSource> context,
            long runtime, int dataTypeId, long timestamp) throws ScriptException, ResultTypeException {
        ensureInit();

        // Create the wrapper object context.
        ScriptEngine engine = script.getEngine();
        ScriptUtils.wrapperContext(engine, new WrapperContext(runtime));
        Bindings engineScope = new SimpleBindings();

        // Put the context variables into the engine with engine scope.
        for (String varName : context.keySet()) {
            IDataPointValueSource point = context.get(varName);
            int dt = point.getDataTypeId();
            if (dt == DataTypes.BINARY)
                engineScope.put(varName, new BinaryPointWrapper(point, engine, null));
            else if (dt == DataTypes.MULTISTATE)
                engineScope.put(varName, new MultistatePointWrapper(point, engine, null));
            else if (dt == DataTypes.NUMERIC)
                engineScope.put(varName, new NumericPointWrapper(point, engine, null));
            else if (dt == DataTypes.ALPHANUMERIC)
                engineScope.put(varName, new AlphanumericPointWrapper(point, engine, null));
            else
                throw new ShouldNeverHappenException("Unknown data type id: " + point.getDataTypeId());
        }

        // Execute.
        Object result;
        try {
            result = script.eval(engineScope);
        }
        catch (ScriptException e) {
            throw prettyScriptMessage(e);
        }

        // Check if a timestamp was set
        Object ts = engineScope.get("TIMESTAMP");
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

        DataValue value;
        if (result == null) {
            if (dataTypeId == DataTypes.BINARY)
                value = new BinaryValue(false);
            else if (dataTypeId == DataTypes.MULTISTATE)
                value = new MultistateValue(0);
            else if (dataTypeId == DataTypes.NUMERIC)
                value = new NumericValue(0);
            else if (dataTypeId == DataTypes.ALPHANUMERIC)
                value = new AlphanumericValue("");
            else
                value = null;
        }
        else if (result instanceof AbstractPointWrapper)
            value = ((AbstractPointWrapper) result).getValueImpl();

        // See if the type matches.
        else if (dataTypeId == DataTypes.BINARY && result instanceof BinaryValue)
            value = (BinaryValue) result;
        else if (dataTypeId == DataTypes.BINARY && result instanceof Boolean)
            value = new BinaryValue((Boolean) result);

        else if (dataTypeId == DataTypes.MULTISTATE && result instanceof MultistateValue)
            value = (MultistateValue) result;
        else if (dataTypeId == DataTypes.MULTISTATE && result instanceof Number)
            value = new MultistateValue(((Number) result).intValue());

        else if (dataTypeId == DataTypes.NUMERIC && result instanceof NumericValue)
            value = (NumericValue) result;
        else if (dataTypeId == DataTypes.NUMERIC && result instanceof Number)
            value = new NumericValue(((Number) result).doubleValue());

        else if (dataTypeId == DataTypes.ALPHANUMERIC && result instanceof AlphanumericValue)
            value = (AlphanumericValue) result;
        else if (dataTypeId == DataTypes.ALPHANUMERIC && result instanceof String)
            value = new AlphanumericValue((String) result);

        else
            // If not, ditch it.
            throw new ResultTypeException(new TranslatableMessage("event.script.convertError", result,
                    DataTypes.getDataTypeMessage(dataTypeId)));

        return new PointValueTime(value, timestamp);
    }

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

    private static void ensureInit() throws ScriptException {
        if (ENGINE == null) {
            // Create the script engine.
            ScriptEngineManager manager;
            try {
                manager = new ScriptEngineManager();
            }
            catch (Exception e) {
                throw new ScriptException(e);
            }
            ENGINE = manager.getEngineByName("js");

            Bindings globalBindings = new SimpleBindings();
            // Add constants to the context.
            globalBindings.put("SECOND", Common.TimePeriods.SECONDS);
            globalBindings.put("MINUTE", Common.TimePeriods.MINUTES);
            globalBindings.put("HOUR", Common.TimePeriods.HOURS);
            globalBindings.put("DAY", Common.TimePeriods.DAYS);
            globalBindings.put("WEEK", Common.TimePeriods.WEEKS);
            globalBindings.put("MONTH", Common.TimePeriods.MONTHS);
            globalBindings.put("YEAR", Common.TimePeriods.YEARS);

            ENGINE.setBindings(globalBindings, ScriptContext.GLOBAL_SCOPE);
        }
    }
}
