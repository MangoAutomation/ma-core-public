package com.infiniteautomation.mango.emport;

import org.apache.commons.lang3.StringUtils;

import com.serotonin.json.JsonException;
import com.serotonin.json.type.JsonObject;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.i18n.ProcessResult;
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
        if (StringUtils.isBlank(xid))
            xid = ctx.getMailingListDao().generateUniqueXid();

        MailingList vo = ctx.getMailingListDao().getFullByXid(xid);
        if (vo == null) {
            vo = new MailingList();
            vo.setXid(xid);
        }

        try {
            ctx.getReader().readInto(vo, json);

            // Now validate it. Use a new response object so we can distinguish errors in this vo from other errors.
            ProcessResult voResponse = new ProcessResult();
            vo.validate(voResponse);
            if (voResponse.getHasMessages())
                setValidationMessages(voResponse, "emport.mailingList.prefix", xid);
            else {
                // Sweet. Save it.
                boolean isnew = vo.getId() == Common.NEW_ID;
                ctx.getMailingListDao().saveFull(vo);
                addSuccessMessage(isnew, "emport.mailingList.prefix", xid);
            }
        }
        catch (TranslatableJsonException e) {
            addFailureMessage("emport.mailingList.prefix", xid, e.getMsg());
        }
        catch (JsonException e) {
            addFailureMessage("emport.mailingList.prefix", xid, getJsonExceptionMessage(e));
        }
    }
}
