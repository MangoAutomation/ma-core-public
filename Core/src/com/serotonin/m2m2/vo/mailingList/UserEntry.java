/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.vo.mailingList;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import com.serotonin.json.JsonException;
import com.serotonin.json.JsonReader;
import com.serotonin.json.ObjectWriter;
import com.serotonin.json.type.JsonObject;
import com.serotonin.m2m2.db.dao.UserDao;
import com.serotonin.m2m2.i18n.TranslatableJsonException;

/**
 * Entry for a user's email address
 *
 * @author Terry Packer
 */
public class UserEntry implements MailingListRecipient {
    private int userId;

    @Override
    public RecipientListEntryType getRecipientType() {
        return RecipientListEntryType.USER;
    }

    @Override
    public int getReferenceId() {
        return userId;
    }

    @Override
    public String getReferenceAddress() {
        return null;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    @Override
    public String toString() {
        return "userId=" + userId;
    }

    @Override
    public void jsonWrite(ObjectWriter writer) throws IOException, JsonException {
        MailingListRecipient.super.jsonWrite(writer);
        writer.writeEntry("username", UserDao.getInstance().getXidById(userId));
    }

    @Override
    public void jsonRead(JsonReader reader, JsonObject jsonObject) throws JsonException {
        MailingListRecipient.super.jsonRead(reader, jsonObject);

        String username = jsonObject.getString("username");
        if (username == null)
            throw new TranslatableJsonException("emport.error.recipient.missing.reference", "username");

        Integer user = UserDao.getInstance().getIdByXid(username);
        if (user == null)
            throw new TranslatableJsonException("emport.error.recipient.invalid.reference", "username", username);

        userId = user;
    }

    //
    //
    // Serialization
    //
    private static final long serialVersionUID = -1;
    private static final int version = 1;

    private void writeObject(ObjectOutputStream out) throws IOException {
        out.writeInt(version);
        out.writeInt(userId);
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        int ver = in.readInt();
        if(ver == 1) {
            userId = in.readInt();
        }
    }
}
