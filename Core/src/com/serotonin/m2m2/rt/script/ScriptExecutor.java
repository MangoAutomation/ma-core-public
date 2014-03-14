/*
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.rt.script;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.script.ScriptEngine;
import javax.script.ScriptException;

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
    private static final String SCRIPT_PREFIX = "function __scriptExecutor__() {";
    private static final String SCRIPT_SUFFIX = "\r\n}\r\n__scriptExecutor__();";

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

    public PointValueTime execute(String script, Map<String, IDataPointValueSource> context, long runtime,
            int dataTypeId, long timestamp) throws ScriptException, ResultTypeException {
        // Create the script engine.
        ScriptEngine engine = ScriptUtils.newEngine();
        // engine.getContext().setErrorWriter(new PrintWriter(System.err));
        // engine.getContext().setWriter(new PrintWriter(System.out));

        // Create the wrapper object context.
        WrapperContext wrapperContext = new WrapperContext(runtime);

        // Add constants to the context.
        ScriptUtils.prepareEngine(engine);
        ScriptUtils.wrapperContext(engine, wrapperContext);

        // Put the context variables into the engine with engine scope.
        for (String varName : context.keySet()) {
            IDataPointValueSource point = context.get(varName);
            engine.put(varName, ScriptUtils.wrapPoint(engine, point));
        }

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

        // Check if a timestamp was set
        Object ts = engine.get("TIMESTAMP");
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
