/**
 * Copyright (C) 2018  Infinite Automation Software. All rights reserved.
 */
package com.serotonin.m2m2.vo.mailingList;

import com.serotonin.m2m2.util.ExportCodes;

/**
 * @author Terry Packer
 *
 */
public class EmailRecipientTypeCodes extends ExportCodes {

    public static final EmailRecipientTypeCodes instance = new EmailRecipientTypeCodes();
    
    private EmailRecipientTypeCodes() {
        addElement(EmailRecipient.TYPE_MAILING_LIST, "MAILING_LIST", "mailingLists.mailingList");
        addElement(EmailRecipient.TYPE_USER, "USER", "mailingLists.emailAddress");
        addElement(EmailRecipient.TYPE_ADDRESS, "ADDRESS", "common.user");
    }
    
}
