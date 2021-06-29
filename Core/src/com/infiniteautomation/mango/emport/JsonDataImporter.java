/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.infiniteautomation.mango.emport;

import org.apache.commons.lang3.StringUtils;

import com.infiniteautomation.mango.permission.MangoPermission;
import com.infiniteautomation.mango.spring.service.JsonDataService;
import com.infiniteautomation.mango.util.exception.NotFoundException;
import com.infiniteautomation.mango.util.exception.ValidationException;
import com.serotonin.json.JsonException;
import com.serotonin.json.type.JsonObject;
import com.serotonin.m2m2.i18n.TranslatableJsonException;
import com.serotonin.m2m2.vo.json.JsonDataVO;

/**
 * @author Terry Packer
 *
 */
public class JsonDataImporter extends Importer {
    private final JsonDataService service;
    
    public JsonDataImporter(JsonObject json, JsonDataService service) {
        super(json);
        this.service = service;
    }

    @Override
    protected void importImpl() {
        String xid = json.getString("xid");
        JsonDataVO vo = null;
        boolean isNew = false;
        if(StringUtils.isBlank(xid)) {
            xid = service.generateUniqueXid();
        }else {
            try {
                vo = service.get(xid);
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

                //Ensure we have a default permission since null is valid in Mango 3.x
                if(vo.getReadPermission() == null) {
                    vo.setReadPermission(new MangoPermission());
                }
                if(vo.getEditPermission() == null) {
                    vo.setEditPermission(new MangoPermission());
                }

                if(isNew) {
                    service.insert(vo);
                }else {
                    service.update(vo.getId(), vo);
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
