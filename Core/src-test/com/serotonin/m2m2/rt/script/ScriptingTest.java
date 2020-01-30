/**
 * Copyright (C) 2014 Infinite Automation Software. All rights reserved.
 *
 * @author Terry Packer
 */
package com.serotonin.m2m2.rt.script;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.Assert;
import org.junit.Test;

import com.infiniteautomation.mango.util.script.CompiledMangoJavaScript;
import com.infiniteautomation.mango.util.script.MangoJavaScriptResult;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.DataTypes;
import com.serotonin.m2m2.MangoTestBase;
import com.serotonin.m2m2.rt.dataImage.IDataPointValueSource;
import com.serotonin.m2m2.rt.dataImage.PointValueTime;
import com.serotonin.m2m2.util.log.LogLevel;
import com.serotonin.m2m2.vo.DataPointVO;
import com.serotonin.m2m2.vo.dataPoint.MockPointLocatorVO;
import com.serotonin.m2m2.vo.permission.PermissionHolder;
import com.serotonin.m2m2.vo.role.Role;

/**
 * @author Terry Packer
 *
 */
public class ScriptingTest extends MangoTestBase {

    @Test
    public void testAnalogStatistics() {

        String script = "var a = p1.past(MINUTE,50);";
        script += "return a.average;";

        ScriptContextVariable p1 = new ScriptContextVariable();
        p1.setContextUpdate(true);
        p1.setDataPointId(1);
        p1.setVariableName("p1");

        try {
            Map<String, IDataPointValueSource> context =
                    new HashMap<String, IDataPointValueSource>();

            DataPointVO vo = new DataPointVO();
            vo.setId(p1.getDataPointId());
            vo.setPointLocator(new MockPointLocatorVO());
            ScriptingTestPointValueRT p1Rt = new ScriptingTestPointValueRT(vo);

            context.put(p1.getVariableName(), p1Rt);

            final StringWriter scriptOut = new StringWriter();
            final PrintWriter scriptWriter = new PrintWriter(scriptOut);
            try(ScriptLog scriptLog =
                    new ScriptLog("testScriptLogger", LogLevel.TRACE, scriptWriter)){

                ScriptPointValueSetter setter = null;
                CompiledMangoJavaScript compiled = new CompiledMangoJavaScript(
                        setter,
                        scriptLog,
                        new ArrayList<>(),
                        admin
                        );
                compiled.compile(script, true);
                compiled.initialize(context);

                MangoJavaScriptResult result = compiled.execute(
                        Common.timer.currentTimeMillis(),
                        Common.timer.currentTimeMillis(),
                        DataTypes.NUMERIC);

                PointValueTime pvt = (PointValueTime)result.getResult();
                assertNotNull(pvt);
            }
        } catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    private final String logRegex = "(\\D.*) \\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2},\\d{3} - (.*)";
    @Test
    public void testScriptLog() {
        String script = "LOG.trace('Trace message');";
        script += "LOG.debug('Debug message');";
        script += "LOG.info('Info message');";
        script += "LOG.warn('Warn message');";
        script += "LOG.error('Error message');";
        script += "LOG.fatal('Fatal message');";

        try {
            Map<String, IDataPointValueSource> context =
                    new HashMap<String, IDataPointValueSource>();

            final StringWriter scriptOut = new StringWriter();
            final PrintWriter scriptWriter = new PrintWriter(scriptOut);
            try(ScriptLog scriptLog =
                    new ScriptLog("testScriptLogger", LogLevel.TRACE, scriptWriter)) {


                ScriptPointValueSetter setter = null;
                CompiledMangoJavaScript compiled = new CompiledMangoJavaScript(
                        setter,
                        scriptLog,
                        new ArrayList<>(),
                        admin
                        );
                compiled.compile(script, true);
                compiled.initialize(context);

                compiled.execute(
                        Common.timer.currentTimeMillis(),
                        Common.timer.currentTimeMillis(),
                        DataTypes.NUMERIC);

                String result = scriptOut.toString();
                String[] messages = result.split("\\n");
                Assert.assertEquals(6, messages.length);

                for(int i=0; i<messages.length; i++) {
                    Pattern p = Pattern.compile(logRegex);
                    Matcher m = p.matcher(messages[i]);
                    Assert.assertEquals(true, m.matches());
                    String level = m.group(1);
                    String message = m.group(2);
                    switch(level) {
                        case "TRACE":
                            Assert.assertEquals("Trace message", message);
                            break;
                        case "DEBUG":
                            Assert.assertEquals("Debug message", message);
                            break;
                        case "INFO":
                            Assert.assertEquals("Info message", message);
                            break;
                        case "WARN":
                            Assert.assertEquals("Warn message", message);
                            break;
                        case "ERROR":
                            Assert.assertEquals("Error message", message);
                            break;
                        case "FATAL":
                            Assert.assertEquals("Fatal message", message);
                            break;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    @Test
    public void testScriptLogNullWriter() {
        String script = "LOG.trace('Trace message');";
        script += "LOG.debug('Debug message');";
        script += "LOG.info('Info message');";
        script += "LOG.warn('Warn message');";
        script += "LOG.error('Error message');";
        script += "LOG.fatal('Fatal message');";

        try {
            Map<String, IDataPointValueSource> context =
                    new HashMap<String, IDataPointValueSource>();

            try(ScriptLog scriptLog = new ScriptLog("testNullWriter")){
                ScriptPointValueSetter setter = null;
                CompiledMangoJavaScript compiled = new CompiledMangoJavaScript(
                        setter,
                        scriptLog,
                        new ArrayList<>(),
                        admin
                        );
                compiled.compile(script, true);
                compiled.initialize(context);

                compiled.execute(
                        Common.timer.currentTimeMillis(),
                        Common.timer.currentTimeMillis(),
                        DataTypes.NUMERIC);
                Assert.assertTrue(!scriptLog.getFile().exists());
            }
        }catch(Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    @Test
    public void testScriptLogFileWriter() {
        String script = "LOG.trace('Trace message');";
        script += "LOG.debug('Debug message');";
        script += "LOG.info('Info message');";
        script += "LOG.warn('Warn message');";
        script += "LOG.error('Error message');";
        script += "LOG.fatal('Fatal message');";
        script += "print('Print message');";

        try {
            Map<String, IDataPointValueSource> context =
                    new HashMap<String, IDataPointValueSource>();

            //Delete the file
            File log = new File(Common.getLogsDir(), "testFileWriter-1.log");
            if(log.exists()) {
                log.delete();
                log.createNewFile();
            }

            try(ScriptLog scriptLog = new ScriptLog("testFileWriter-1", LogLevel.TRACE, 100000, 2)){

                ScriptPointValueSetter setter = null;
                CompiledMangoJavaScript compiled = new CompiledMangoJavaScript(
                        setter,
                        scriptLog,
                        new ArrayList<>(),
                        admin
                        );
                compiled.compile(script, true);
                compiled.initialize(context);

                compiled.execute(
                        Common.timer.currentTimeMillis(),
                        Common.timer.currentTimeMillis(),
                        DataTypes.NUMERIC);

                Assert.assertTrue(scriptLog.getFile().exists());

                String result = readFile(scriptLog.getFile().toPath());
                String[] messages = result.split("\\n");
                Assert.assertEquals(6, messages.length);

                for(int i=0; i<messages.length; i++) {
                    Pattern p = Pattern.compile(logRegex);
                    Matcher m = p.matcher(messages[i]);
                    Assert.assertEquals(true, m.matches());
                    String level = m.group(1);
                    String message = m.group(2);
                    switch(level) {
                        case "TRACE":
                            Assert.assertEquals("Trace message", message);
                            break;
                        case "DEBUG":
                            Assert.assertEquals("Debug message", message);
                            break;
                        case "INFO":
                            Assert.assertEquals("Info message", message);
                            break;
                        case "WARN":
                            Assert.assertEquals("Warn message", message);
                            break;
                        case "ERROR":
                            Assert.assertEquals("Error message", message);
                            break;
                        case "FATAL":
                            Assert.assertEquals("Fatal message", message);
                            break;
                    }
                }
            }
        }catch(Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    @Test
    public void testScriptContextWriter() {
        String script = "print('testing context writer');";

        try {
            Map<String, IDataPointValueSource> context =
                    new HashMap<String, IDataPointValueSource>();

            final StringWriter scriptOut = new StringWriter();
            final PrintWriter scriptWriter = new PrintWriter(scriptOut);
            try(ScriptLog scriptLog = new ScriptLog("testContextWriter-", LogLevel.TRACE, scriptWriter)) {
                ScriptPointValueSetter setter = null;
                CompiledMangoJavaScript compiled = new CompiledMangoJavaScript(
                        setter,
                        scriptLog,
                        new ArrayList<>(),
                        admin
                        );
                compiled.compile(script, true);
                compiled.initialize(context);

                compiled.execute(
                        Common.timer.currentTimeMillis(),
                        Common.timer.currentTimeMillis(),
                        DataTypes.NUMERIC);

                String result = scriptOut.toString();
                Assert.assertEquals("testing context writer\n", result);
            }
        }catch(Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    @Test
    public void testScriptLogWriteNullValue() {
        String script = "LOG.trace(null);";
        script += "LOG.debug(null);";
        script += "LOG.info(null);";
        script += "LOG.warn(null);";
        script += "LOG.error(null);";
        script += "LOG.fatal(null);";
        script += "print(null);";

        try {
            Map<String, IDataPointValueSource> context =
                    new HashMap<String, IDataPointValueSource>();

            //Delete the file
            File log = new File(Common.getLogsDir(), "testNullValueWriter-1.log");
            if(log.exists()) {
                log.delete();
                log.createNewFile();
            }

            try(ScriptLog scriptLog = new ScriptLog("testNullValueWriter-1", LogLevel.TRACE, 100000, 2)){

                ScriptPointValueSetter setter = null;
                CompiledMangoJavaScript compiled = new CompiledMangoJavaScript(
                        setter,
                        scriptLog,
                        new ArrayList<>(),
                        admin
                        );
                compiled.compile(script, true);
                compiled.initialize(context);

                compiled.execute(
                        Common.timer.currentTimeMillis(),
                        Common.timer.currentTimeMillis(),
                        DataTypes.NUMERIC);

                Assert.assertTrue(scriptLog.getFile().exists());

                String result = readFile(scriptLog.getFile().toPath());
                String[] messages = result.split("\\n");
                Assert.assertEquals(6, messages.length);

                for(int i=0; i<messages.length; i++) {
                    Pattern p = Pattern.compile(logRegex);
                    Matcher m = p.matcher(messages[i]);
                    Assert.assertEquals(true, m.matches());
                    String level = m.group(1);
                    String message = m.group(2);
                    switch(level) {
                        case "TRACE":
                            Assert.assertEquals("null", message);
                            break;
                        case "DEBUG":
                            Assert.assertEquals("null", message);
                            break;
                        case "INFO":
                            Assert.assertEquals("null", message);
                            break;
                        case "WARN":
                            Assert.assertEquals("null", message);
                            break;
                        case "ERROR":
                            Assert.assertEquals("null", message);
                            break;
                        case "FATAL":
                            Assert.assertEquals("null", message);
                            break;
                    }
                }
            }
        }catch(Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    private PermissionHolder admin = new PermissionHolder() {

        @Override
        public String getPermissionHolderName() {
            return "admin";
        }

        @Override
        public boolean isPermissionHolderDisabled() {
            return false;
        }

        @Override
        public Set<Role> getRoles() {
            return Collections.singleton(PermissionHolder.SUPERADMIN_ROLE);
        }
    };

    /**
     * Helper to read files in
     *
     * @param path
     * @return
     * @throws IOException
     */
    static String readFile(Path path) throws IOException {
        byte[] encoded = Files.readAllBytes(path);
        return new String(encoded, Common.UTF8_CS);
    }

}
