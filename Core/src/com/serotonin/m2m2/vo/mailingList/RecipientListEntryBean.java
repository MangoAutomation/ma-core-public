/**
 * Copyright (C) 2019  Infinite Automation Software. All rights reserved.
 */
package com.serotonin.m2m2.vo.mailingList;

import java.io.IOException;
import java.io.Serializable;
import java.util.List;
import java.util.ListIterator;

import org.apache.commons.lang3.StringUtils;

import com.serotonin.ShouldNeverHappenException;
import com.serotonin.db.spring.ExtendedJdbcTemplate;
import com.serotonin.json.JsonException;
import com.serotonin.json.JsonReader;
import com.serotonin.json.ObjectWriter;
import com.serotonin.json.spi.JsonSerializable;
import com.serotonin.json.type.JsonObject;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.dao.MailingListDao;
import com.serotonin.m2m2.db.dao.UserDao;
import com.serotonin.m2m2.i18n.TranslatableJsonException;
import com.serotonin.m2m2.vo.User;

/**
 * TODO Mango 4.0 remove or rename to not have bean in the name
 * @author Terry Packer
 *
 */
public class RecipientListEntryBean implements Serializable, JsonSerializable {
    private static final long serialVersionUID = -1;

    private int recipientType;
    private int referenceId;
    private String referenceAddress;

    public EmailRecipient createEmailRecipient() {
        switch (recipientType) {
        case EmailRecipient.TYPE_MAILING_LIST:
            MailingList ml = new MailingList();
            ml.setId(referenceId);
            return ml;
        case EmailRecipient.TYPE_USER:
            UserEntry u = new UserEntry();
            u.setUserId(referenceId);
            return u;
        case EmailRecipient.TYPE_ADDRESS:
            AddressEntry a = new AddressEntry();
            a.setAddress(referenceAddress);
            return a;
        }
        throw new ShouldNeverHappenException("Unknown email recipient type: " + recipientType);
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
        writer.writeEntry("recipientType", EmailRecipient.TYPE_CODES.getCode(recipientType));
        if (recipientType == EmailRecipient.TYPE_MAILING_LIST)
            writer.writeEntry("mailingList", MailingListDao.getInstance().get(referenceId).getXid());
        else if (recipientType == EmailRecipient.TYPE_USER)
            writer.writeEntry("username", UserDao.getInstance().get(referenceId).getUsername());
        else if (recipientType == EmailRecipient.TYPE_ADDRESS)
            writer.writeEntry("address", referenceAddress);
    }

    @Override
    public void jsonRead(JsonReader reader, JsonObject jsonObject) throws JsonException {
        String text = jsonObject.getString("recipientType");
        if (text == null)
            throw new TranslatableJsonException("emport.error.recipient.missing", "recipientType",
                    EmailRecipient.TYPE_CODES.getCodeList());

        recipientType = EmailRecipient.TYPE_CODES.getId(text);
        if (recipientType == -1)
            throw new TranslatableJsonException("emport.error.recipient.invalid", "recipientType", text,
                    EmailRecipient.TYPE_CODES.getCodeList());

        if (recipientType == EmailRecipient.TYPE_MAILING_LIST) {
            text = jsonObject.getString("mailingList");
            if (text == null)
                throw new TranslatableJsonException("emport.error.recipient.missing.reference", "mailingList");

            MailingList ml = MailingListDao.getInstance().getByXid(text);
            if (ml == null)
                throw new TranslatableJsonException("emport.error.recipient.invalid.reference", "mailingList", text);

            referenceId = ml.getId();
        }
        else if (recipientType == EmailRecipient.TYPE_USER) {
            text = jsonObject.getString("username");
            if (text == null)
                throw new TranslatableJsonException("emport.error.recipient.missing.reference", "username");

            User user = UserDao.getInstance().getByXid(text);
            if (user == null)
                throw new TranslatableJsonException("emport.error.recipient.invalid.reference", "user", text);

            referenceId = user.getId();
        }
        else if (recipientType == EmailRecipient.TYPE_ADDRESS) {
            referenceAddress = jsonObject.getString("address");
            if (referenceAddress == null)
                throw new TranslatableJsonException("emport.error.recipient.missing.reference", "address");
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
    
    /**
     * Clean a list of beans by removing any entries with dead references.
     * @param list
     */
    public static void cleanRecipientList(List<RecipientListEntryBean> list){
        if(list == null)
            return;
        
        ListIterator<RecipientListEntryBean> it = list.listIterator();
        ExtendedJdbcTemplate ejt = new ExtendedJdbcTemplate();
        ejt.setDataSource(Common.databaseProxy.getDataSource());

        while(it.hasNext()){
            RecipientListEntryBean bean = it.next();
            switch(bean.recipientType){
            case EmailRecipient.TYPE_ADDRESS:
                if(StringUtils.isEmpty(bean.referenceAddress))
                    it.remove();
                break;
            case EmailRecipient.TYPE_MAILING_LIST:
                if(ejt.queryForInt("SELECT id from mailingLists WHERE id=?", new Object[] {bean.referenceId}, Common.NEW_ID) == Common.NEW_ID)
                    it.remove();
                break;
            case EmailRecipient.TYPE_USER:
                if(ejt.queryForInt("SELECT id from users WHERE id=?", new Object[] {bean.referenceId}, Common.NEW_ID) == Common.NEW_ID)
                    it.remove();
                break;
            }
        }
        
    }
    
}
