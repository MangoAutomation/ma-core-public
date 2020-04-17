/**
 * Copyright (C) 2020  Infinite Automation Software. All rights reserved.
 */

package com.infiniteautomation.mango.emport;

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import com.infiniteautomation.mango.spring.service.RoleService;
import com.infiniteautomation.mango.util.exception.NotFoundException;
import com.infiniteautomation.mango.util.exception.ValidationException;
import com.serotonin.ShouldNeverHappenException;
import com.serotonin.json.type.JsonArray;
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

        Set<RoleVO> inheritedRoles = new HashSet<>();
        if(json.containsKey("inheritedRoles")) {
            importInheritedRoles(json.getJsonArray("inheritedRoles"), inheritedRoles);
        }

        RoleVO vo = null;

        if (StringUtils.isBlank(xid)) {
            xid = service.getDao().generateUniqueXid();
        }else {
            try {
                vo = service.get(xid);
                vo = new RoleVO(vo.getId(), xid, name);
            }catch(NotFoundException e) {

            }
        }

        if(vo == null) {
            vo = new RoleVO(Common.NEW_ID, xid, name);
        }

        try {
            boolean isnew = vo.getId() == Common.NEW_ID;
            if(isnew) {
                service.insert(vo);
            }else {
                service.update(vo.getId(), vo);
            }
            addSuccessMessage(isnew, "emport.role.prefix", xid);
        }catch(ValidationException e) {
            setValidationMessages(e.getValidationResult(), "emport.role.prefix", xid);
        }
    }

    /**
     * @param jsonArray
     * @param inheritedRoles
     */
    private void importInheritedRoles(JsonArray jsonArray, Set<RoleVO> inheritedRoles) {
        throw new ShouldNeverHappenException("unimplimented");

    }
}
