/*
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.vo.mailingList;

import java.io.IOException;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;

import com.serotonin.json.JsonException;
import com.serotonin.json.JsonReader;
import com.serotonin.json.ObjectWriter;
import com.serotonin.json.type.JsonObject;
import com.serotonin.m2m2.i18n.TranslatableJsonException;

public class AddressEntry implements EmailRecipient {
    private String address;

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    @Override
    public void appendAddresses(Set<String> addresses, DateTime sendTime) {
        appendAllAddresses(addresses);
    }

    @Override
    public void appendAllAddresses(Set<String> addresses) {
        addresses.add(address);
    }

    @Override
    public int getRecipientType() {
        return EmailRecipient.TYPE_ADDRESS;
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
        EmailRecipient.super.jsonWrite(writer);
        writer.writeEntry("address", address);
    }

    @Override
    public void jsonRead(JsonReader reader, JsonObject jsonObject) throws JsonException {
        EmailRecipient.super.jsonRead(reader, jsonObject);
        address = jsonObject.getString("address");
        if (StringUtils.isBlank(address))
            throw new TranslatableJsonException("emport.error.recipient.missing.reference", "address");
    }
}
