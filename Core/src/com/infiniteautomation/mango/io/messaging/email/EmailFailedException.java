/**
 * Copyright (C) 2020  Infinite Automation Software. All rights reserved.
 */

package com.infiniteautomation.mango.io.messaging.email;

import com.infiniteautomation.mango.io.messaging.MessageSendException;
import com.serotonin.m2m2.i18n.TranslatableMessage;

/**
 * Email failed to send, session information will be recorded
 *  in the session field
 * @author Terry Packer
 */
public class EmailFailedException extends MessageSendException {

    private static final long serialVersionUID = 1L;

    private final String session;

    public EmailFailedException(Exception root, String subject, String to, String session) {
        super(new TranslatableMessage("event.email.failure", subject, to, root.getMessage()), root);
        this.session = session;
    }

    public String getSession() {
        return session;
    }
}
