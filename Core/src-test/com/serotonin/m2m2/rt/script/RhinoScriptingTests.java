/**
 * Copyright (C) 2014 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.rt.script;

import static org.junit.Assert.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import javax.script.CompiledScript;

import org.junit.Test;

import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.DataTypes;
import com.serotonin.m2m2.rt.dataImage.IDataPointValueSource;
import com.serotonin.m2m2.rt.dataImage.PointValueTime;

/**
 * @author Terry Packer
 * 
 */
public class RhinoScriptingTests {

	@Test
	public void testAnalogStatistics() {


		String script = "var a = p1.past(MINUTES,50);";
		script += "return a.average;";

		ScriptContextVariable p1 = new ScriptContextVariable();
		p1.setContextUpdate(true);
		p1.setDataPointId(1);
		p1.setVariableName("p1");

		try {
			Map<String, IDataPointValueSource> context = new HashMap<String, IDataPointValueSource>();
			
			RhinoScriptingTestPointValueRT p1Rt = new RhinoScriptingTestPointValueRT(p1.getDataPointId(), DataTypes.NUMERIC);
			
			context.put(p1.getVariableName(), p1Rt);
			
			CompiledScript s = CompiledScriptExecutor.compile(script);
			PointValueTime pvt = CompiledScriptExecutor.execute(s,
					context, System.currentTimeMillis(),
					DataTypes.NUMERIC, -1);
			assertNotNull(pvt);
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	/**
	 * Helper to read files in
	 * @param path
	 * @return
	 * @throws IOException
	 */
	static String readFile(String path) throws IOException {
		byte[] encoded = Files.readAllBytes(Paths.get(path));
		return new String(encoded, Common.UTF8_CS);
	}

}
