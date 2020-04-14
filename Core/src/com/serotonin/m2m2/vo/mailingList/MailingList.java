/*
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.vo.mailingList;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import com.infiniteautomation.mango.permission.MangoPermission;
import com.serotonin.json.JsonException;
import com.serotonin.json.JsonReader;
import com.serotonin.json.ObjectWriter;
import com.serotonin.json.spi.JsonProperty;
import com.serotonin.json.type.JsonObject;
import com.serotonin.m2m2.rt.event.AlarmLevels;
import com.serotonin.m2m2.vo.AbstractVO;

public class MailingList extends AbstractVO {

    public static final String XID_PREFIX = "ML_";

    @JsonProperty
    private List<MailingListRecipient> entries;
    private AlarmLevels receiveAlarmEmails = AlarmLevels.IGNORE;
    @JsonProperty
    private MangoPermission editPermission = new MangoPermission();
    @JsonProperty
    private MangoPermission readPermission = new MangoPermission();

    /**
     * Integers that are present in the inactive intervals set are times at which the mailing list schedule is not to be
     * sent to. Intervals are split into 15 minutes, starting at [00:00 to 00:15) on Monday. Thus, there are 4 * 24 * 7
     * = 672 individual periods.
     */
    @JsonProperty
    private Set<Integer> inactiveIntervals = new TreeSet<Integer>();

    public List<MailingListRecipient> getEntries() {
        return entries;
    }

    public void setEntries(List<MailingListRecipient> entries) {
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

    public MangoPermission getEditPermission() {
        return editPermission;
    }

    public void setEditPermission(MangoPermission editPermission) {
        this.editPermission = editPermission;
    }

    public MangoPermission getReadPermission() {
        return readPermission;
    }

    public void setReadPermission(MangoPermission readPermission) {
        this.readPermission = readPermission;
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
    public String getTypeKey() {
        return "event.audit.mailingList";
    }

    //
    //
    // Serialization
    //
    private static final long serialVersionUID = -1;

}
