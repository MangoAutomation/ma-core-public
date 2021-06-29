/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */

package com.serotonin.m2m2.vo.mailingList;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.apache.commons.lang3.StringUtils;

import com.serotonin.json.JsonException;
import com.serotonin.json.JsonReader;
import com.serotonin.json.ObjectWriter;
import com.serotonin.json.type.JsonObject;
import com.serotonin.m2m2.i18n.TranslatableJsonException;
import com.serotonin.util.SerializationHelper;

/**
 * Any phone number
 * @author Terry Packer
 */
public class PhoneEntry implements MailingListRecipient {

    private String phone;

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    @Override
    public RecipientListEntryType getRecipientType() {
        return RecipientListEntryType.PHONE_NUMBER;
    }

    @Override
    public int getReferenceId() {
        return 0;
    }

    @Override
    public String getReferenceAddress() {
        return phone;
    }

    @Override
    public void jsonWrite(ObjectWriter writer) throws IOException, JsonException {
        MailingListRecipient.super.jsonWrite(writer);
        writer.writeEntry("phone", phone);
    }

    @Override
    public void jsonRead(JsonReader reader, JsonObject jsonObject) throws JsonException {
        MailingListRecipient.super.jsonRead(reader, jsonObject);
        phone = jsonObject.getString("phone");
        if (StringUtils.isBlank(phone))
            throw new TranslatableJsonException("emport.error.recipient.missing.reference", "phone");
    }

    //
    //
    // Serialization
    //
    private static final long serialVersionUID = -1;
    private static final int version = 1;

    private void writeObject(ObjectOutputStream out) throws IOException {
        out.writeInt(version);
        SerializationHelper.writeSafeUTF(out, phone);
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        int ver = in.readInt();
        if(ver == 1) {
            phone = SerializationHelper.readSafeUTF(in);
        }
    }

}
