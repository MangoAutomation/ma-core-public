/**
 * Copyright (C) 2020  Infinite Automation Software. All rights reserved.
 */

package com.serotonin.m2m2.vo.mailingList;

import com.infiniteautomation.mango.util.ReverseEnum;
import com.infiniteautomation.mango.util.ReverseEnumMap;
import com.serotonin.m2m2.i18n.TranslatableMessage;

/**
 * Types of recipients for a mailing list
 *
 * @author Terry Packer
 */
public enum RecipientListEntryType implements ReverseEnum<Integer> {

    MAILING_LIST(1, new TranslatableMessage("mailingLists.mailingList")),
    USER(2, new TranslatableMessage("mailingLists.userEmailAddress")),
    ADDRESS(3, new TranslatableMessage("mailingLists.emailAddress")),
    PHONE_NUMBER(4, new TranslatableMessage("mailingLists.phoneNumber")),
    USER_PHONE_NUMBER(5, new TranslatableMessage("mailingLists.userPhoneNumber"));

    private static ReverseEnumMap<Integer, RecipientListEntryType> map = new ReverseEnumMap<>(RecipientListEntryType.class);
    private final int value;
    private final TranslatableMessage description;

    private RecipientListEntryType(int value, TranslatableMessage description) {
        this.value = value;
        this.description = description;
    }

    @Override
    public Integer value() {
        return this.value;
    }

    public TranslatableMessage getDescription() {
        return description;
    }

    public static RecipientListEntryType fromValue(Integer value) {
        return map.get(value);
    }

    public static RecipientListEntryType fromName(String name) {
        return Enum.valueOf(RecipientListEntryType.class, name);
    }

}
