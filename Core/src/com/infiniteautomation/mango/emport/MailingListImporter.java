/*
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.infiniteautomation.mango.emport;

import org.apache.commons.lang3.StringUtils;

import com.infiniteautomation.mango.spring.service.MailingListService;
import com.infiniteautomation.mango.util.exception.NotFoundException;
import com.infiniteautomation.mango.util.exception.ValidationException;
import com.serotonin.json.JsonException;
import com.serotonin.json.type.JsonObject;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.i18n.TranslatableJsonException;
import com.serotonin.m2m2.vo.mailingList.MailingList;

public class MailingListImporter extends Importer {
    
    private final MailingListService service;
    
    public MailingListImporter(JsonObject json,
            MailingListService service) {
        super(json);
        this.service = service;
    }

    @Override
    protected void importImpl() {
        String xid = json.getString("xid");
        MailingList vo = null;
        
        if (StringUtils.isBlank(xid)) {
            xid = service.generateUniqueXid();
        }else {
            try {
                vo = service.get(xid);
            }catch(NotFoundException e) {

            }
        }
        
        if(vo == null) {
            vo = new MailingList();
            vo.setXid(xid);
        }

        try {
            ctx.getReader().readInto(vo, json);
            boolean isnew = vo.getId() == Common.NEW_ID;
            if(isnew) {
                service.insert(vo);
            }else {
                service.update(vo.getId(), vo);
            }
            addSuccessMessage(isnew, "emport.mailingList.prefix", xid);
        }catch(ValidationException e) {
            setValidationMessages(e.getValidationResult(), "emport.mailingList.prefix", xid);
        }catch (TranslatableJsonException e) {
            addFailureMessage("emport.mailingList.prefix", xid, e.getMsg());
        }catch (JsonException e) {
            addFailureMessage("emport.mailingList.prefix", xid, getJsonExceptionMessage(e));
        }
    }
}

