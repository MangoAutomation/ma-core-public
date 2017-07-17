package com.serotonin.m2m2.rt.script;

import java.util.ArrayList;
import java.util.List;

public class JsonEmportScriptTestUtility extends JsonEmportScriptUtility {

	public JsonEmportScriptTestUtility(ScriptPermissions permissions) {
		super(permissions);
	}
	
	@Override
	public void doImport(String json) {
		//No import in testing!
	}
	
	@Override
	public List<String> doImportGetStatus(String json) {
		List<String> result = new ArrayList<>(1);
		result.add("Cannot run or test imports during validation.");
		return result;
	}

}
