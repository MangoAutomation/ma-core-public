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

public class AddressEntry implements MailingListRecipient {

    private String address;

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    @Override
    public RecipientListEntryType getRecipientType() {
        return RecipientListEntryType.ADDRESS;
    }

    @Override
    public int getReferenceId() {
        return 0;
    }

    @Override
    public String getReferenceAddress() {
        return address;
    }

    @Override
    public String toString() {
        return address;
    }

    @Override
    public void jsonWrite(ObjectWriter writer) throws IOException, JsonException {
        MailingListRecipient.super.jsonWrite(writer);
        writer.writeEntry("address", address);
    }

    @Override
    public void jsonRead(JsonReader reader, JsonObject jsonObject) throws JsonException {
        MailingListRecipient.super.jsonRead(reader, jsonObject);
        address = jsonObject.getString("address");
        if (StringUtils.isBlank(address))
            throw new TranslatableJsonException("emport.error.recipient.missing.reference", "address");
    }

    //
    //
    // Serialization
    //
    private static final long serialVersionUID = -1;
    private static final int version = 1;

    private void writeObject(ObjectOutputStream out) throws IOException {
        out.writeInt(version);
        SerializationHelper.writeSafeUTF(out, address);
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        int ver = in.readInt();
        if(ver == 1) {
            address = SerializationHelper.readSafeUTF(in);
        }
    }

}
