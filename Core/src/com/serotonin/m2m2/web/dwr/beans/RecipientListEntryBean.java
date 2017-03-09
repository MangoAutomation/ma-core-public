/*
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.web.dwr.beans;

import java.io.IOException;
import java.io.Serializable;
import java.util.List;
import java.util.ListIterator;

import org.apache.commons.lang3.StringUtils;

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
import com.serotonin.m2m2.vo.mailingList.EmailRecipient;
import com.serotonin.m2m2.vo.mailingList.MailingList;
import com.serotonin.m2m2.vo.mailingList.UserEntry;

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
            writer.writeEntry("mailingList", MailingListDao.instance.getMailingList(referenceId).getXid());
        else if (recipientType == EmailRecipient.TYPE_USER)
            writer.writeEntry("username", UserDao.instance.getUser(referenceId).getUsername());
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

            MailingList ml = MailingListDao.instance.getMailingList(text);
            if (ml == null)
                throw new TranslatableJsonException("emport.error.recipient.invalid.reference", "mailingList", text);

            referenceId = ml.getId();
        }
        else if (recipientType == EmailRecipient.TYPE_USER) {
            text = jsonObject.getString("username");
            if (text == null)
                throw new TranslatableJsonException("emport.error.recipient.missing.reference", "username");

            User user = UserDao.instance.getUser(text);
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
    	MailingListDao mlDao = MailingListDao.instance;
    	
    	while(it.hasNext()){
    		RecipientListEntryBean bean = it.next();
    		switch(bean.recipientType){
    		case EmailRecipient.TYPE_ADDRESS:
    			if(StringUtils.isEmpty(bean.referenceAddress))
    				it.remove();
    			break;
    		case EmailRecipient.TYPE_MAILING_LIST:
    			if(mlDao.getMailingList(bean.referenceId) == null)
    				it.remove();
    			break;
    		case EmailRecipient.TYPE_USER:
    			if(!UserDao.instance.userExists(bean.referenceId))
    				it.remove();
    			break;
    		}
    	}
    	
    }
    
}
