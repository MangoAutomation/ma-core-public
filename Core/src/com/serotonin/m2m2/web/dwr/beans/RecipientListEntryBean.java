/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.web.dwr.beans;

import java.io.IOException;
import java.io.Serializable;

import com.serotonin.ShouldNeverHappenException;
import com.serotonin.json.JsonException;
import com.serotonin.json.JsonReader;
import com.serotonin.json.ObjectWriter;
import com.serotonin.json.spi.JsonSerializable;
import com.serotonin.json.type.JsonObject;
import com.serotonin.m2m2.db.dao.MailingListDao;
import com.serotonin.m2m2.db.dao.UserDao;
import com.serotonin.m2m2.i18n.TranslatableJsonException;
import com.serotonin.m2m2.vo.User;
import com.serotonin.m2m2.vo.mailingList.AddressEntry;
import com.serotonin.m2m2.vo.mailingList.MailingList;
import com.serotonin.m2m2.vo.mailingList.MailingListEntry;
import com.serotonin.m2m2.vo.mailingList.MailingListRecipient;
import com.serotonin.m2m2.vo.mailingList.PhoneEntry;
import com.serotonin.m2m2.vo.mailingList.RecipientListEntryType;
import com.serotonin.m2m2.vo.mailingList.UserEntry;

/**
 * Helper to resolve an entry in a mailing list.  This class is left here
 *  for legacy serialization compatablity as it is stored serialized in the db
 * @author Terry Packer
 *
 */
@Deprecated
public class RecipientListEntryBean implements Serializable, JsonSerializable {
    private static final long serialVersionUID = -1;

    private int recipientType;
    private int referenceId;
    private String referenceAddress;

    /**
     * Convert to mailing list recipient
     */
    public MailingListRecipient createEmailRecipient() {
        RecipientListEntryType type = RecipientListEntryType.fromValue(recipientType);
        switch(type) {
            case ADDRESS:
                AddressEntry a = new AddressEntry();
                a.setAddress(referenceAddress);
                return a;
            case MAILING_LIST:
                MailingListEntry ml = new MailingListEntry();
                ml.setMailingListId(referenceId);
                return ml;
            case PHONE_NUMBER:
                PhoneEntry pe = new PhoneEntry();
                pe.setPhone(referenceAddress);
                return pe;
            case USER:
                UserEntry u = new UserEntry();
                u.setUserId(referenceId);
                return u;
            case USER_PHONE_NUMBER:
                PhoneEntry p = new PhoneEntry();
                p.setPhone(referenceAddress);
                return p;
            default:
                throw new ShouldNeverHappenException("Unknown recipient type: " + recipientType);
        }
    }

    public String getReferenceAddress() {
        return referenceAddress;
    }

    public void setReferenceAddress(String address) {
        referenceAddress = address;
    }

    public int getRecipientType() {
        return recipientType;
    }

    public void setRecipientType(int typeId) {
        recipientType = typeId;
    }

    public int getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(int refId) {
        referenceId = refId;
    }

    @Override
    public void jsonWrite(ObjectWriter writer) throws IOException, JsonException {
        RecipientListEntryType type = RecipientListEntryType.fromValue(recipientType);
        writer.writeEntry("recipientType", type);
        switch(type) {
            case ADDRESS:
                writer.writeEntry("address", referenceAddress);
                break;
            case MAILING_LIST:
                writer.writeEntry("mailingList", MailingListDao.getInstance().getXidById(referenceId));
                break;
            case PHONE_NUMBER:
                writer.writeEntry("phone", referenceAddress);
                break;
            case USER:
            case USER_PHONE_NUMBER:
                writer.writeEntry("username", UserDao.getInstance().getXidById(referenceId));
                break;
            default:
                break;

        }
    }

    @Override
    public void jsonRead(JsonReader reader, JsonObject jsonObject) throws JsonException {
        String text = jsonObject.getString("recipientType");
        RecipientListEntryType type = RecipientListEntryType.fromName(text);
        if (type == null) {
            throw new TranslatableJsonException("emport.error.recipient.invalid", "recipientType", text,
                    RecipientListEntryType.values());
        }

        switch(type) {
            case ADDRESS:
                referenceAddress = jsonObject.getString("address");
                if (referenceAddress == null)
                    throw new TranslatableJsonException("emport.error.recipient.missing.reference", "address");
                break;
            case MAILING_LIST:
                text = jsonObject.getString("mailingList");
                if (text == null)
                    throw new TranslatableJsonException("emport.error.recipient.missing.reference", "mailingList");

                MailingList ml = MailingListDao.getInstance().getByXid(text);
                if (ml == null)
                    throw new TranslatableJsonException("emport.error.recipient.invalid.reference", "mailingList", text);

                referenceId = ml.getId();
                break;
            case PHONE_NUMBER:
                referenceAddress = jsonObject.getString("phone");
                if (referenceAddress == null)
                    throw new TranslatableJsonException("emport.error.recipient.missing.reference", "phone");
                break;
            case USER:
                text = jsonObject.getString("username");
                if (text == null)
                    throw new TranslatableJsonException("emport.error.recipient.missing.reference", "username");

                User user = UserDao.getInstance().getByXid(text);
                if (user == null)
                    throw new TranslatableJsonException("emport.error.recipient.invalid.reference", "user", text);

                referenceId = user.getId();
                referenceAddress = user.getEmail();
                break;
            case USER_PHONE_NUMBER:
                text = jsonObject.getString("username");
                if (text == null)
                    throw new TranslatableJsonException("emport.error.recipient.missing.reference", "username");

                User userPhone = UserDao.getInstance().getByXid(text);
                if (userPhone == null)
                    throw new TranslatableJsonException("emport.error.recipient.invalid.reference", "user", text);

                referenceId = userPhone.getId();
                referenceAddress = userPhone.getPhone();
                break;
            default:
                throw new ShouldNeverHappenException("Unknown recipient type: " + type);
        }
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + recipientType;
        result = prime
                * result
                + ((referenceAddress == null) ? 0 : referenceAddress.hashCode());
        result = prime * result + referenceId;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        RecipientListEntryBean other = (RecipientListEntryBean) obj;
        if (recipientType != other.recipientType)
            return false;
        if (referenceAddress == null) {
            if (other.referenceAddress != null)
                return false;
        } else if (!referenceAddress.equals(other.referenceAddress))
            return false;
        if (referenceId != other.referenceId)
            return false;
        return true;
    }
}
