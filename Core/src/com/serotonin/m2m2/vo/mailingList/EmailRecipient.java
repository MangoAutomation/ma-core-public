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
import com.serotonin.json.spi.JsonSerializable;
import com.serotonin.json.type.JsonObject;
import com.serotonin.m2m2.util.ExportCodes;

abstract public class EmailRecipient implements JsonSerializable {
    public static final int TYPE_MAILING_LIST = 1;
    public static final int TYPE_USER = 2;
    public static final int TYPE_ADDRESS = 3;

    public static final ExportCodes TYPE_CODES = new ExportCodes();
    static {
        TYPE_CODES.addElement(TYPE_MAILING_LIST, "MAILING_LIST", "mailingLists.mailingList");
        TYPE_CODES.addElement(TYPE_USER, "USER", "mailingLists.emailAddress");
        TYPE_CODES.addElement(TYPE_ADDRESS, "ADDRESS", "common.user");
    }

    abstract public int getRecipientType();

    abstract public void appendAddresses(Set<String> addresses, DateTime sendTime);

    abstract public void appendAllAddresses(Set<String> addresses);

    abstract public int getReferenceId();

    abstract public String getReferenceAddress();

    /**
     * @throws JsonException
     */
    @Override
    public void jsonWrite(ObjectWriter writer) throws IOException, JsonException {
        writer.writeEntry("recipientType", TYPE_CODES.getCode(getRecipientType()));
    }

    @Override
    public void jsonRead(JsonReader reader, JsonObject jsonObject) throws JsonException {
        // no op. The type value is used by the factory.
    }
}
