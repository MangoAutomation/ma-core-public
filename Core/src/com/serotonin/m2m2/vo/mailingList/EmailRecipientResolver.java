/*
    Copyright (C) 2006-2011 Serotonin Software Technologies Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.vo.mailingList;

import java.lang.reflect.Type;

import com.serotonin.json.JsonException;
import com.serotonin.json.spi.TypeResolver;
import com.serotonin.json.type.JsonObject;
import com.serotonin.json.type.JsonValue;
import com.serotonin.m2m2.i18n.TranslatableJsonException;

public class EmailRecipientResolver implements TypeResolver {
    @Override
    public Type resolve(JsonValue jsonValue) throws JsonException {
        if (jsonValue == null)
            return null;

        JsonObject json = jsonValue.toJsonObject();

        String text = json.getString("recipientType");
        if (text == null)
            throw new TranslatableJsonException("emport.error.recipient.missing", "recipientType",
                    EmailRecipient.TYPE_CODES);

        int type = EmailRecipient.TYPE_CODES.getId(text);
        if (!EmailRecipient.TYPE_CODES.isValidId(type))
            throw new TranslatableJsonException("emport.error.recipient.invalid", "recipientType", text,
                    EmailRecipient.TYPE_CODES.getCodeList());

        if (type == EmailRecipient.TYPE_MAILING_LIST)
            return MailingList.class;
        if (type == EmailRecipient.TYPE_USER)
            return UserEntry.class;
        return AddressEntry.class;
    }
}
