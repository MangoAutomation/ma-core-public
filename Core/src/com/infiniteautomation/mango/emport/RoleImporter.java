/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */

package com.infiniteautomation.mango.emport;

import org.apache.commons.lang3.StringUtils;

import com.infiniteautomation.mango.spring.service.RoleService;
import com.infiniteautomation.mango.util.exception.NotFoundException;
import com.infiniteautomation.mango.util.exception.ValidationException;
import com.serotonin.json.JsonException;
import com.serotonin.json.type.JsonObject;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.vo.role.RoleVO;

/**
 * This importer should be run 1st
 * @author Terry Packer
 */
public class RoleImporter extends Importer {

    private final RoleService service;

    public RoleImporter(JsonObject json, RoleService service) {
        super(json);
        this.service = service;
    }

    @Override
    protected void importImpl() {
        String xid = json.getString("xid");
        String name = json.getString("name");
        RoleVO vo = null;

        if (StringUtils.isBlank(xid)) {
            xid = service.generateUniqueXid();
        }else {
            try {
                vo = service.get(xid);
            }catch(NotFoundException e) {

            }
        }

        if(vo == null) {
            vo = new RoleVO(Common.NEW_ID, xid, name);
        }

        try {
            //Read into the VO to get all properties
            ctx.getReader().readInto(vo, json);

            boolean isnew = vo.getId() == Common.NEW_ID;
            if(isnew) {
                service.insert(vo);
            }else {
                service.update(vo.getId(), vo);
            }
            addSuccessMessage(isnew, "emport.role.prefix", xid);
        }catch(ValidationException e) {
            setValidationMessages(e.getValidationResult(), "emport.role.prefix", xid);
        }catch (JsonException e) {
            addFailureMessage("emport.role.prefix", xid, getJsonExceptionMessage(e));
        }
    }
}
