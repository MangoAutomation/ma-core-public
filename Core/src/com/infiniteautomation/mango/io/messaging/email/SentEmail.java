/**
 * Copyright (C) 2020  Infinite Automation Software. All rights reserved.
 */

package com.infiniteautomation.mango.io.messaging.email;

import java.util.Set;

import javax.mail.internet.InternetAddress;

import com.infiniteautomation.mango.io.messaging.MessageTransport;
import com.infiniteautomation.mango.io.messaging.SentMessage;

/**
 *
 * @author Terry Packer
 */
public class SentEmail implements SentMessage {

    private final Set<InternetAddress> recipients;
    private final InternetAddress from;
    private final EmailMessage message;
    private final String session;
    private final MessageTransport transport;

    public SentEmail(Set<InternetAddress> recipients, InternetAddress from, EmailMessage message, String session, MessageTransport transport) {
        this.recipients = recipients;
        this.from = from;
        this.message = message;
        this.session = session;
        this.transport = transport;
    }


    public EmailMessage getMessage() {
        return message;
    }

    public Set<InternetAddress> getRecipients() {
        return recipients;
    }

    public InternetAddress getFrom() {
        return from;
    }

    public String getSession() {
        return session;
    }

    @Override
    public MessageTransport getTransport() {
        return transport;
    }

}
