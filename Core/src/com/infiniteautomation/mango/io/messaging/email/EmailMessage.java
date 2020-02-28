/**
 * Copyright (C) 2020  Infinite Automation Software. All rights reserved.
 */

package com.infiniteautomation.mango.io.messaging.email;

import java.util.Set;

import javax.mail.internet.InternetAddress;

import com.infiniteautomation.mango.io.messaging.Message;
import com.infiniteautomation.mango.io.messaging.MessageType;
import com.serotonin.web.mail.EmailContent;

/**
 *
 * @author Terry Packer
 */
public class EmailMessage implements Message {

    private final InternetAddress from;
    private final Set<InternetAddress> toAddresses;
    private final String subject;
    private final EmailContent content;

    public EmailMessage(InternetAddress from, Set<InternetAddress> toAddresses, String subject, EmailContent content) {
        this.from = from;
        this.toAddresses = toAddresses;
        this.subject = subject;
        this.content = content;
    }

    public InternetAddress getFrom() {
        return from;
    }

    public Set<InternetAddress> getToAddresses() {
        return toAddresses;
    }
    public String getSubject() {
        return subject;
    }

    public EmailContent getContent() {
        return content;
    }

    @Override
    public MessageType getType() {
        return MessageType.EMAIL;
    }
}
