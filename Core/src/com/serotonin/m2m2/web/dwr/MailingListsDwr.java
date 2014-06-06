/*
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.web.dwr;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.dao.MailingListDao;
import com.serotonin.m2m2.db.dao.UserDao;
import com.serotonin.m2m2.email.MangoEmailContent;
import com.serotonin.m2m2.i18n.ProcessResult;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.i18n.Translations;
import com.serotonin.m2m2.rt.maint.work.EmailWorkItem;
import com.serotonin.m2m2.vo.mailingList.EmailRecipient;
import com.serotonin.m2m2.vo.mailingList.MailingList;
import com.serotonin.m2m2.web.dwr.beans.RecipientListEntryBean;
import com.serotonin.m2m2.web.dwr.util.DwrPermission;

public class MailingListsDwr extends BaseDwr {
    private final Log log = LogFactory.getLog(MailingListsDwr.class);

    @DwrPermission(admin = true)
    public ProcessResult init() {
        ProcessResult response = new ProcessResult();
        response.addData("lists", new MailingListDao().getMailingLists());
        response.addData("users", new UserDao().getUsers());
        return response;
    }

    @DwrPermission(admin = true)
    public MailingList getMailingList(int id) {
        if (id == Common.NEW_ID) {
            MailingList ml = new MailingList();
            ml.setId(Common.NEW_ID);
            ml.setXid(new MailingListDao().generateUniqueXid());
            ml.setEntries(new LinkedList<EmailRecipient>());
            return ml;
        }
        return new MailingListDao().getMailingList(id);
    }

    @DwrPermission(admin = true)
    public ProcessResult saveMailingList(int id, String xid, String name, List<RecipientListEntryBean> entryBeans,
            List<Integer> inactiveIntervals) {
        ProcessResult response = new ProcessResult();
        MailingListDao mailingListDao = new MailingListDao();

        // Validate the given information. If there is a problem, return an appropriate error message.
        MailingList ml = createMailingList(id, xid, name, entryBeans);
        ml.getInactiveIntervals().addAll(inactiveIntervals);

        if (StringUtils.isBlank(xid))
            response.addContextualMessage("xid", "validate.required");
        else if (!mailingListDao.isXidUnique(xid, id))
            response.addContextualMessage("xid", "validate.xidUsed");

        ml.validate(response);

        if (!response.getHasMessages()) {
            // Save the mailing list
            mailingListDao.saveMailingList(ml);
            response.addData("mlId", ml.getId());
        }

        return response;
    }

    @DwrPermission(admin = true)
    public void deleteMailingList(int mlId) {
        new MailingListDao().deleteMailingList(mlId);
    }

    @DwrPermission(admin = true)
    public ProcessResult sendTestEmail(int id, String name, List<RecipientListEntryBean> entryBeans) {
        ProcessResult response = new ProcessResult();

        MailingList ml = createMailingList(id, null, name, entryBeans);
        new MailingListDao().populateEntrySubclasses(ml.getEntries());

        Set<String> addresses = new HashSet<String>();
        ml.appendAddresses(addresses, null);
        String[] toAddrs = addresses.toArray(new String[0]);

        try {
            Translations translations = Common.getTranslations();
            Map<String, Object> model = new HashMap<String, Object>();
            model.put("message", new TranslatableMessage("ftl.userTestEmail", ml.getName()));
            MangoEmailContent cnt = new MangoEmailContent("testEmail", model, translations,
                    translations.translate("ftl.testEmail"), Common.UTF8);
            EmailWorkItem.queueEmail(toAddrs, cnt);
        }
        catch (Exception e) {
            response.addGenericMessage("mailingLists.testerror", e.getMessage());
            log.warn("", e);
        }

        return response;
    }

    //
    // /
    // / Private helper methods
    // /
    //
    private MailingList createMailingList(int id, String xid, String name, List<RecipientListEntryBean> entryBeans) {
        // Convert the incoming information into more useful types.
        MailingList ml = new MailingList();
        ml.setId(id);
        ml.setXid(xid);
        ml.setName(name);

        List<EmailRecipient> entries = new ArrayList<EmailRecipient>(entryBeans.size());
        for (RecipientListEntryBean bean : entryBeans)
            entries.add(bean.createEmailRecipient());
        ml.setEntries(entries);

        return ml;
    }
}
