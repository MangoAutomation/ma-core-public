/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.vo.mailingList;

import java.io.IOException;
import java.io.Serializable;

import com.serotonin.json.JsonException;
import com.serotonin.json.JsonReader;
import com.serotonin.json.ObjectWriter;
import com.serotonin.json.spi.JsonSerializable;
import com.serotonin.json.type.JsonObject;

/**
 * Recipient for a mailing list, can be an attribute of a VO such as a user's email address or
 *  a raw attribute such as any email address
 *
 * @author Terry Packer
 */
abstract public interface MailingListRecipient extends Serializable, JsonSerializable {

    /**
     * Return the type of recipient
     */
    abstract public RecipientListEntryType getRecipientType();

    /**
     * For reference types this will return the referenced VO's id
     */
    abstract public int getReferenceId();

    /**
     * For raw types this will return the address to use
     */
    abstract public String getReferenceAddress();

    /**
     */
    @Override
    default public void jsonWrite(ObjectWriter writer) throws IOException, JsonException {
        writer.writeEntry("recipientType", getRecipientType().name());
    }

    @Override
    default public void jsonRead(JsonReader reader, JsonObject jsonObject) throws JsonException {
        // no op. The type value is used by the factory.
    }
}
