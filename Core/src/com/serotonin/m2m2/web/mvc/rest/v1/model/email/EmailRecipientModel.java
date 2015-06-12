/**
 * Copyright (C) 2015 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.web.mvc.rest.v1.model.email;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.serotonin.ShouldNeverHappenException;
import com.serotonin.m2m2.vo.mailingList.AddressEntry;
import com.serotonin.m2m2.vo.mailingList.EmailRecipient;
import com.serotonin.m2m2.vo.mailingList.MailingList;
import com.serotonin.m2m2.vo.mailingList.UserEntry;
import com.serotonin.m2m2.web.mvc.rest.v1.model.AbstractRestModel;

/**
 * @author Terry Packer
 *
 */
public abstract class EmailRecipientModel<T extends EmailRecipient> extends AbstractRestModel<T>{

	/**
	 * @param data
	 */
	public EmailRecipientModel(T data) {
		super(data);
	}

	@JsonGetter("type")
	public String getType(){
		return EmailRecipient.TYPE_CODES.getCode(this.data.getRecipientType());
	}

	/**
	 * Create a subclass model depending on the Email Recipient subclass
	 * @param recipient
	 * @return
	 */
	public static EmailRecipientModel<?> createModel(EmailRecipient recipient) {
		switch(recipient.getRecipientType()){
		case EmailRecipient.TYPE_ADDRESS:
			return new AddressEntryModel((AddressEntry) recipient);
		case EmailRecipient.TYPE_USER:
			return new UserEntryModel((UserEntry) recipient);
		case EmailRecipient.TYPE_MAILING_LIST:
			return new MailingListModel((MailingList)recipient);
		default:
			throw new ShouldNeverHappenException("Unsupported Email Recipient Type: " + recipient.getRecipientType());
		}
	}
	
	
}
