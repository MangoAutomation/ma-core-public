/*
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.vo.mailingList;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;

import com.serotonin.json.JsonException;
import com.serotonin.json.JsonReader;
import com.serotonin.json.ObjectWriter;
import com.serotonin.json.spi.JsonProperty;
import com.serotonin.json.type.JsonObject;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.dao.AbstractDao;
import com.serotonin.m2m2.db.dao.MailingListDao;
import com.serotonin.m2m2.db.dao.UserDao;
import com.serotonin.m2m2.i18n.ProcessResult;
import com.serotonin.m2m2.rt.event.AlarmLevels;
import com.serotonin.m2m2.vo.AbstractVO;
import com.serotonin.m2m2.vo.User;
import com.serotonin.m2m2.vo.permission.Permissions;

public class MailingList extends AbstractVO<MailingList> implements EmailRecipient {

    private static final long serialVersionUID = 1L;

    public static final String XID_PREFIX = "ML_";

    @JsonProperty
    private List<EmailRecipient> entries;
    private AlarmLevels receiveAlarmEmails = AlarmLevels.IGNORE;
    @JsonProperty
    private Set<String> readPermissions = Collections.emptySet();
    @JsonProperty
    private Set<String> editPermissions = Collections.emptySet();

    /**
     * Integers that are present in the inactive intervals set are times at which the mailing list schedule is not to be
     * sent to. Intervals are split into 15 minutes, starting at [00:00 to 00:15) on Monday. Thus, there are 4 * 24 * 7
     * = 672 individual periods.
     */
    @JsonProperty
    private Set<Integer> inactiveIntervals = new TreeSet<Integer>();

    @Override
    public int getRecipientType() {
        return EmailRecipient.TYPE_MAILING_LIST;
    }

    @Override
    public String getReferenceAddress() {
        return null;
    }

    @Override
    public int getReferenceId() {
        return id;
    }

    public List<EmailRecipient> getEntries() {
        return entries;
    }

    public void setEntries(List<EmailRecipient> entries) {
        this.entries = entries;
    }

    public Set<Integer> getInactiveIntervals() {
        return inactiveIntervals;
    }

    public void setInactiveIntervals(Set<Integer> inactiveIntervals) {
        this.inactiveIntervals = inactiveIntervals;
    }

    public AlarmLevels getReceiveAlarmEmails() {
        return receiveAlarmEmails;
    }

    public void setReceiveAlarmEmails(AlarmLevels receiveAlarmEmails) {
        this.receiveAlarmEmails = receiveAlarmEmails;
    }

    /**
     * @return the readPermissions
     */
    public Set<String> getReadPermissions() {
        return readPermissions;
    }

    /**
     * @param readPermissions the readPermissions to set
     */
    public void setReadPermissions(Set<String> readPermissions) {
        this.readPermissions = readPermissions;
    }

    /**
     * @return the editPermissions
     */
    public Set<String> getEditPermissions() {
        return editPermissions;
    }

    /**
     * @param editPermissions the editPermissions to set
     */
    public void setEditPermissions(Set<String> editPermissions) {
        this.editPermissions = editPermissions;
    }

    @Override
    public void appendAddresses(Set<String> addresses, DateTime sendTime) {
        if (sendTime != null && inactiveIntervals.contains(getIntervalIdAt(sendTime)))
            return;
        appendAllAddresses(addresses);
    }

    @Override
    public void appendAllAddresses(Set<String> addresses) {
        for (EmailRecipient e : entries)
            e.appendAddresses(addresses, null);
    }

    private static int getIntervalIdAt(DateTime dt) {
        int interval = 0;
        interval += dt.getMinuteOfHour() / 15;
        interval += dt.getHourOfDay() * 4;
        interval += (dt.getDayOfWeek() - 1) * 96;
        return interval;
    }

    @Override
    public void validate(ProcessResult result) {
        super.validate(result);
        if(receiveAlarmEmails == null)
            result.addContextualMessage("receiveAlarmEmails", "validate.invalidValue");

        if(entries == null || entries.size() == 0)
            result.addContextualMessage("recipients", "mailingLists.validate.entries");
        else {
            int index = 0;
            for(EmailRecipient recipient : entries) {
                switch(recipient.getRecipientType()) {
                    case EmailRecipient.TYPE_ADDRESS:
                        //TODO Ensure valid email format...
                        AddressEntry ee = (AddressEntry)recipient;
                        if (StringUtils.isBlank(ee.getAddress()))
                            result.addContextualMessage("recipients[" + index + "]", "validate.required");
                        break;
                    case EmailRecipient.TYPE_MAILING_LIST:
                        result.addContextualMessage("recipients[" + index + "]", "validate.invalidValue");
                        break;
                    case EmailRecipient.TYPE_USER:
                        //Ensure the user exists
                        UserEntry ue = (UserEntry)recipient;
                        if(UserDao.getInstance().get(ue.getUserId()) == null)
                            result.addContextualMessage("recipients[" + index + "]", "validate.invalidValue");
                        break;
                }
                index++;
            }
        }

        User user = Common.getHttpUser();
        if(user == null)
            user = Common.getBackgroundContextUser();

        Set<String> existingReadPermission;
        Set<String> existingEditPermission;
        if(this.id != Common.NEW_ID) {
            MailingList vo = MailingListDao.getInstance().get(id);
            if(vo != null) {
                existingReadPermission = vo.getReadPermissions();
                existingEditPermission = vo.getEditPermissions();
            }else {
                existingReadPermission = null;
                existingEditPermission = null;
            }
        }else {
            existingReadPermission = null;
            existingEditPermission = null;
        }
        Permissions.validatePermissions(result, "readPermissions", user, false, existingReadPermission, readPermissions);
        Permissions.validatePermissions(result, "editPermissions", user, false, existingEditPermission, editPermissions);

        if(inactiveIntervals != null) {
            if(inactiveIntervals.size() > 672)
                result.addContextualMessage("inactiveSchedule", "validate.invalidValue");
        }
    }

    @Override
    public String toString() {
        return "MailingList(" + entries + ")";
    }

    @Override
    public void jsonWrite(ObjectWriter writer) throws IOException, JsonException {
        // Don't call the EmailRecipient.super.jsonWrite method, because a mailing list can't be a member of a mailing list.
        super.jsonWrite(writer);
        writer.writeEntry("receiveAlarmEmails", receiveAlarmEmails.name());
    }

    @Override
    public void jsonRead(JsonReader reader, JsonObject jsonObject) throws JsonException {
        super.jsonRead(reader, jsonObject);
        String text = jsonObject.getString("recieveAlarmEmails");
        if(text != null){
            receiveAlarmEmails = AlarmLevels.fromName(text);
        }
    }

    @Override
    protected AbstractDao<MailingList> getDao() {
        return MailingListDao.getInstance();
    }

    @Override
    public String getTypeKey() {
        return "event.audit.mailingList";
    }
}
