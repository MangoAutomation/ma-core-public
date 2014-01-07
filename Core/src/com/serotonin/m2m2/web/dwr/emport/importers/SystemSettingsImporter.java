/**
 * Copyright (C) 2013 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.web.dwr.emport.importers;

import com.serotonin.json.JsonException;
import com.serotonin.json.type.JsonObject;
import com.serotonin.m2m2.db.dao.SystemSettingsDao;
import com.serotonin.m2m2.i18n.ProcessResult;
import com.serotonin.m2m2.i18n.TranslatableJsonException;
import com.serotonin.m2m2.vo.systemSettings.SystemSettingsVO;
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
			SystemSettingsVO vo = dao.getSystemSettings();
			
            // The VO was found or successfully created. Finish reading it in.
            ctx.getReader().readInto(vo, json);

            // Now validate it. Use a new response object so we can distinguish errors in this vo from
            // other errors.
            ProcessResult voResponse = new ProcessResult();
            vo.validate(voResponse);
            if (voResponse.getHasMessages())
                setValidationMessages(voResponse, "emport.systemSettings.prefix","System Settings");
            else {
                
            	dao.updateSettings(vo);
            	
                addSuccessMessage(false, "emport.systemSettings.prefix","Import Success");
            }
        }
        catch (TranslatableJsonException e) {
            addFailureMessage("emport.systemSettings.prefix","System Settings", e.getMsg());
        }
        catch (JsonException e) {
            addFailureMessage("emport.systemSettings.prefix", "System Settings", getJsonExceptionMessage(e));
        }
		
		
	}

}
