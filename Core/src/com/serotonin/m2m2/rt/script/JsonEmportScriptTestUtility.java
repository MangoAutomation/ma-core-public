package com.serotonin.m2m2.rt.script;

import java.util.ArrayList;
import java.util.List;

import com.serotonin.m2m2.i18n.ProcessMessage;
import com.serotonin.m2m2.i18n.TranslatableMessage;

public class JsonEmportScriptTestUtility extends JsonEmportScriptUtility {

	public JsonEmportScriptTestUtility(ScriptPermissions permissions, List<JsonImportExclusion> importExclusions) {
		super(permissions, importExclusions);
	}
	
	@Override
	public void doImport(String json) {
		//No import in testing!
	}
	
	@Override
	public List<ProcessMessage> doImportGetStatus(String json) {
		List<ProcessMessage> result = new ArrayList<>(1);
		result.add(new ProcessMessage(new TranslatableMessage("literal", "Cannot run or test imports during validation.")));
		return result;
	}

}
