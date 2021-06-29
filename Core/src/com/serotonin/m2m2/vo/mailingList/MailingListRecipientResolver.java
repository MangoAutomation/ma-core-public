/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.vo.mailingList;

import java.lang.reflect.Type;

import com.serotonin.ShouldNeverHappenException;
import com.serotonin.json.JsonException;
import com.serotonin.json.spi.TypeResolver;
import com.serotonin.json.type.JsonObject;
import com.serotonin.json.type.JsonValue;
import com.serotonin.m2m2.i18n.TranslatableJsonException;

/**
 * Resolve mailing list entry types for JSON import
 *
 * @author Terry Packer
 */
public class MailingListRecipientResolver implements TypeResolver {

    @Override
    public Type resolve(JsonValue jsonValue) throws JsonException {
        if (jsonValue == null)
            return null;

        JsonObject json = jsonValue.toJsonObject();

        String text = json.getString("recipientType");
        if (text == null)
            throw new TranslatableJsonException("emport.error.recipient.missing", "recipientType",
                    RecipientListEntryType.values());

        RecipientListEntryType type = RecipientListEntryType.fromName(text);
        if (type == null) {
            throw new TranslatableJsonException("emport.error.recipient.invalid", "recipientType", text,
                    RecipientListEntryType.values());
        }
        switch(type) {
            case ADDRESS:
                return AddressEntry.class;
            case MAILING_LIST:
                return MailingListEntry.class;
            case PHONE_NUMBER:
                return PhoneEntry.class;
            case USER:
                return UserEntry.class;
            case USER_PHONE_NUMBER:
                return UserPhoneEntry.class;
            default:
                throw new ShouldNeverHappenException("Unknown recipient type: " + type);
        }
    }
}
