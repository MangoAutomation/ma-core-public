/*
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.infiniteautomation.mango.emport;

import org.apache.commons.lang3.StringUtils;

import com.infiniteautomation.mango.util.exception.NotFoundException;
import com.infiniteautomation.mango.util.exception.ValidationException;
import com.serotonin.json.JsonException;
import com.serotonin.json.type.JsonObject;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.i18n.TranslatableJsonException;
import com.serotonin.m2m2.vo.mailingList.MailingList;
import com.serotonin.m2m2.vo.permission.PermissionHolder;

public class MailingListImporter extends Importer {
    public MailingListImporter(JsonObject json, PermissionHolder user) {
        super(json, user);
    }

    @Override
    protected void importImpl() {
        String xid = json.getString("xid");
        if (StringUtils.isBlank(xid)) {
            xid = ctx.getMailingListService().getDao().generateUniqueXid();
        }
        MailingList vo = null;
        try {
            vo = ctx.getMailingListService().getFull(xid, user);
        }catch(NotFoundException e) {
            vo = new MailingList();
            vo.setXid(xid);
        }

        try {
            ctx.getReader().readInto(vo, json);
            boolean isnew = vo.getId() == Common.NEW_ID;
            if(isnew) {
                ctx.getMailingListService().insert(vo, user);
            }else {
                ctx.getMailingListService().update(vo.getXid(), vo, user);
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

