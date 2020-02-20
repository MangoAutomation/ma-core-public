/**
 * Copyright (C) 2020  Infinite Automation Software. All rights reserved.
 */

package com.serotonin.m2m2.vo.mailingList;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import com.serotonin.json.JsonException;
import com.serotonin.json.JsonReader;
import com.serotonin.json.ObjectWriter;
import com.serotonin.json.type.JsonObject;
import com.serotonin.m2m2.db.dao.MailingListDao;
import com.serotonin.m2m2.i18n.TranslatableJsonException;

/**
 * Used when a mailing list is part of a recipient list
 * @author Terry Packer
 */
public class MailingListEntry implements MailingListRecipient {

    private int mailingListId;

    @Override
    public RecipientListEntryType getRecipientType() {
        return RecipientListEntryType.MAILING_LIST;
    }

    @Override
    public int getReferenceId() {
        return mailingListId;
    }

    @Override
    public String getReferenceAddress() {
        return null;
    }

    public int getMailingListId() {
        return mailingListId;
    }

    public void setMailingListId(int mailingListId) {
        this.mailingListId = mailingListId;
    }

    @Override
    public void jsonWrite(ObjectWriter writer) throws IOException, JsonException {
        MailingListRecipient.super.jsonWrite(writer);
        writer.writeEntry("mailingList", MailingListDao.getInstance().getXidById(mailingListId));
    }

    @Override
    public void jsonRead(JsonReader reader, JsonObject jsonObject) throws JsonException {
        MailingListRecipient.super.jsonRead(reader, jsonObject);
        String xid = jsonObject.getString("mailingList");
        Integer id = MailingListDao.getInstance().getIdByXid(xid);
        if(id == null) {
            throw new TranslatableJsonException("emport.error.recipient.invalid.reference", "mailingList", xid);
        }else {
            mailingListId = id;
        }
    }

    //
    //
    // Serialization
    //
    private static final long serialVersionUID = -1;
    private static final int version = 1;

    private void writeObject(ObjectOutputStream out) throws IOException {
        out.writeInt(version);
        out.writeInt(mailingListId);
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        int ver = in.readInt();
        if(ver == 1) {
            mailingListId = in.readInt();
        }
    }

}
