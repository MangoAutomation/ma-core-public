/**
 * Copyright (C) 2016 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.infiniteautomation.mango.emport;

import org.apache.commons.lang3.StringUtils;

import com.infiniteautomation.mango.spring.service.JsonDataService;
import com.infiniteautomation.mango.util.exception.NotFoundException;
import com.infiniteautomation.mango.util.exception.ValidationException;
import com.serotonin.json.JsonException;
import com.serotonin.json.type.JsonObject;
import com.serotonin.m2m2.db.dao.JsonDataDao;
import com.serotonin.m2m2.i18n.ProcessResult;
import com.serotonin.m2m2.i18n.TranslatableJsonException;
import com.serotonin.m2m2.vo.json.JsonDataVO;
import com.serotonin.m2m2.vo.permission.PermissionHolder;

/**
 * @author Terry Packer
 *
 */
public class JsonDataImporter extends Importer {
    private final JsonDataService service;
    
    public JsonDataImporter(JsonObject json, JsonDataService service, PermissionHolder user) {
        super(json, user);
        this.service = service;
    }

    @Override
    protected void importImpl() {
        String xid = json.getString("xid");
        JsonDataVO vo = null;
        boolean isNew = false;
        if(StringUtils.isBlank(xid)) {
            xid = service.getDao().generateUniqueXid();
        }else {
            try {
                vo = service.getFull(xid, user);
            }catch(NotFoundException e) {

            }
        }

        if (vo == null) {
        	isNew = true;
        	vo = new JsonDataVO();
        	vo.setXid(xid);
        }

        if (vo != null) {
            try {
                // The VO was found or successfully created. Finish reading it in.
                ctx.getReader().readInto(vo, json);
                if(isNew) {
                    service.insert(vo, user);
                }else {
                    service.update(vo.getId(), vo, user);
                }
                addSuccessMessage(isNew, "emport.jsondata.prefix", xid);
            }catch(ValidationException e) {
                setValidationMessages(e.getValidationResult(), "emport.jsondata.prefix", xid);
            }catch (TranslatableJsonException e) {
                addFailureMessage("emport.jsondata.prefix", xid, e.getMsg());
            }catch (JsonException e) {
                addFailureMessage("emport.jsondata.prefix", xid, getJsonExceptionMessage(e));
            }
        }
    }
    
}
