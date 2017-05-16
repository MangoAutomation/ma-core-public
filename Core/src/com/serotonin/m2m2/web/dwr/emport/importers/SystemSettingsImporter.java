/**
 * Copyright (C) 2013 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.web.dwr.emport.importers;

import java.util.HashMap;
import java.util.Map;

import com.serotonin.json.type.JsonObject;
import com.serotonin.json.type.JsonValue;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.dao.SystemSettingsDao;
import com.serotonin.m2m2.i18n.ProcessResult;
import com.serotonin.m2m2.i18n.TranslatableMessage;
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
			Map<String, Object> settings = new HashMap<String,Object>();
			
            //Finish reading it in.
			for(String key : json.keySet()){
				JsonValue value = json.get(key);
				//Don't import null values or database schemas
				if((value != null)&&(!key.startsWith(SystemSettingsDao.DATABASE_SCHEMA_VERSION))){
					Object o = value.toNative();
					if(o instanceof String){
						//Could be an export code so try and convert it
						Integer id = SystemSettingsDao.instance.convertToValueFromCode(key, (String)o);
						if(id != null)
							settings.put(key, id);
						else
							settings.put(key, o);
					}else{
						settings.put(key, o);
					}	
				}
			}

            // Now validate it. Use a new response object so we can distinguish errors in this vo from
            // other errors.
            ProcessResult voResponse = new ProcessResult();
            SystemSettingsDao.instance.validate(settings, voResponse);
            if (voResponse.getHasMessages())
                setValidationMessages(voResponse, "emport.systemSettings.prefix", new TranslatableMessage("header.systemSettings").translate(Common.getTranslations()));
            else {
            	SystemSettingsDao.instance.updateSettings(settings);
                addSuccessMessage(false, "emport.systemSettings.prefix", new TranslatableMessage("header.systemSettings").translate(Common.getTranslations()));
            }
        }
        catch (Exception e) {
            addFailureMessage("emport.systemSettings.prefix", new TranslatableMessage("header.systemSettings").translate(Common.getTranslations()), e.getMessage());
        }
	}

}
