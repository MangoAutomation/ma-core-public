/**
 * Copyright (C) 2013 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.web.dwr.emport.importers;

import java.util.HashMap;
import java.util.Map;

import com.serotonin.json.type.JsonObject;
import com.serotonin.json.type.JsonValue;
import com.serotonin.m2m2.db.dao.SystemSettingsDao;
import com.serotonin.m2m2.i18n.ProcessResult;
import com.serotonin.m2m2.web.dwr.emport.Importer;

/**
 * @author Terry Packer
 *
 */
public class SystemSettingsImporter extends Importer{

	/**
	 * @param json
	 */
	public SystemSettingsImporter(JsonObject json) {
		super(json);
	}

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.web.dwr.emport.Importer#importImpl()
	 */
	@Override
	protected void importImpl() {
		
		try {
			SystemSettingsDao dao = new SystemSettingsDao();
			Map<String, Object> settings = new HashMap<String,Object>();
			
            //Finish reading it in.
			for(String key : json.keySet()){
				JsonValue value = json.get(key);
				if(value != null)
					settings.put(key, value.toNative());
			}

            // Now validate it. Use a new response object so we can distinguish errors in this vo from
            // other errors.
            ProcessResult voResponse = new ProcessResult();
            dao.validate(settings, voResponse);
            if (voResponse.getHasMessages())
                setValidationMessages(voResponse, "emport.systemSettings.prefix","System Settings");
            else {
            	dao.updateSettings(settings);
                addSuccessMessage(false, "emport.systemSettings.prefix","Import Success");
            }
        }
        catch (Exception e) {
            addFailureMessage("emport.systemSettings.prefix","System Settings", e.getMessage());
        }
        
		
		
	}

}
