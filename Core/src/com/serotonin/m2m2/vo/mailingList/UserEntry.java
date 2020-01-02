/*
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.vo.mailingList;

import java.io.IOException;
import java.util.Set;

import org.joda.time.DateTime;

import com.serotonin.json.JsonException;
import com.serotonin.json.JsonReader;
import com.serotonin.json.ObjectWriter;
import com.serotonin.json.type.JsonObject;
import com.serotonin.m2m2.db.dao.UserDao;
import com.serotonin.m2m2.i18n.TranslatableJsonException;
import com.serotonin.m2m2.vo.User;

public class UserEntry implements EmailRecipient {
    private int userId;
    private User user;

    @Override
    public int getRecipientType() {
        return EmailRecipient.TYPE_USER;
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

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    @Override
    public void appendAddresses(Set<String> addresses, DateTime sendTime) {
        appendAllAddresses(addresses);
    }

    @Override
    public void appendAllAddresses(Set<String> addresses) {
        if (user == null)
            return;
        if (!user.isDisabled())
            addresses.add(user.getEmail());
    }

    @Override
    public String toString() {
        if (user == null)
            return "userId=" + userId;
        return user.getUsername();
    }

    @Override
    public void jsonWrite(ObjectWriter writer) throws IOException, JsonException {
        EmailRecipient.super.jsonWrite(writer);
        if (user == null)
            user = UserDao.getInstance().get(userId);
        writer.writeEntry("username", user.getUsername());
    }

    @Override
    public void jsonRead(JsonReader reader, JsonObject jsonObject) throws JsonException {
        EmailRecipient.super.jsonRead(reader, jsonObject);

        String username = jsonObject.getString("username");
        if (username == null)
            throw new TranslatableJsonException("emport.error.recipient.missing.reference", "username");

        user = UserDao.getInstance().getByXid(username);
        if (user == null)
            throw new TranslatableJsonException("emport.error.recipient.invalid.reference", "username", username);

        userId = user.getId();
    }
}
