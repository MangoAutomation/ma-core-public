/**
 * Copyright (C) 2016 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.web.dwr.emport.importers;

import org.apache.commons.lang3.StringUtils;

import com.serotonin.json.JsonException;
import com.serotonin.json.type.JsonObject;
import com.serotonin.m2m2.db.dao.JsonDataDao;
import com.serotonin.m2m2.i18n.ProcessResult;
import com.serotonin.m2m2.i18n.TranslatableJsonException;
import com.serotonin.m2m2.vo.json.JsonDataVO;
import com.serotonin.m2m2.vo.permission.PermissionHolder;
import com.serotonin.m2m2.web.dwr.emport.Importer;

/**
 * @author Terry Packer
 *
 */
public class JsonDataImporter extends Importer {
    public JsonDataImporter(JsonObject json, PermissionHolder user) {
        super(json, user);
    }

    @Override
    protected void importImpl() {
        String xid = json.getString("xid");

        boolean isNew = false;
        if (StringUtils.isBlank(xid)){
            xid = JsonDataDao.getInstance().generateUniqueXid();
            isNew = true;
        }
            
        JsonDataVO vo = JsonDataDao.getInstance().getByXid(xid);
        if (vo == null) {
        	isNew = true;
        	vo = new JsonDataVO();
        	vo.setXid(xid);
        }

        if (vo != null) {
            try {
                // The VO was found or successfully created. Finish reading it in.
                ctx.getReader().readInto(vo, json);

                // Now validate it. Use a new response object so we can distinguish errors in this vo from
                // other errors.
                ProcessResult voResponse = new ProcessResult();
                vo.validate(voResponse);
                if (voResponse.getHasMessages())
                    setValidationMessages(voResponse, "emport.jsondata.prefix", xid);
                else {
                    // Sweet. Save it.
                    JsonDataDao.getInstance().save(vo);
                    addSuccessMessage(isNew, "emport.jsondata.prefix", xid);
                }
            }
            catch (TranslatableJsonException e) {
                addFailureMessage("emport.jsondata.prefix", xid, e.getMsg());
            }
            catch (JsonException e) {
                addFailureMessage("emport.jsondata.prefix", xid, getJsonExceptionMessage(e));
            }
        }
    }
    
}
