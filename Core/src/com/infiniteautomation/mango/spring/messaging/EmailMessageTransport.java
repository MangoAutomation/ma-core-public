/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */

package com.infiniteautomation.mango.spring.messaging;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.mail.internet.InternetAddress;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.infiniteautomation.mango.io.messaging.Message;
import com.infiniteautomation.mango.io.messaging.MessageReceivedListener;
import com.infiniteautomation.mango.io.messaging.MessageTransport;
import com.infiniteautomation.mango.io.messaging.SentMessage;
import com.infiniteautomation.mango.io.messaging.email.EmailFailedException;
import com.infiniteautomation.mango.io.messaging.email.EmailMessage;
import com.infiniteautomation.mango.io.messaging.email.SentEmail;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.dao.SystemSettingsDao;
import com.serotonin.m2m2.rt.maint.work.WorkItem;
import com.serotonin.timer.RejectedTaskException;
import com.serotonin.timer.RejectedTaskReason;
import com.serotonin.util.LifecycleException;
import com.serotonin.web.mail.EmailSender;

/**
 * Service to send and receive email, currently only sending is supported.
 * @author Terry Packer
 */
@Service()
public class EmailMessageTransport implements MessageTransport {

    private static final Logger LOG = LoggerFactory.getLogger(EmailMessageTransport.class);
    private final List<MessageReceivedListener> listeners = new CopyOnWriteArrayList<MessageReceivedListener>();

    @Override
    public void initialize(boolean safe) throws LifecycleException {

    }

    @Override
    public void terminate() throws LifecycleException {

    }

    @Override
    public void joinTermination() {

    }

    @Override
    public boolean supportsSending(Message type) {
        return EmailMessage.class.equals(type.getClass());
    }

    @Override
    public boolean supportsReceiving(Message type) {
        return false;
    }

    @Override
    public CompletionStage<SentMessage> sendMessage(Message message) {
        CompletableFuture<SentMessage> f = new CompletableFuture<>();

        try {
            EmailMessage m = (EmailMessage)message;
            InternetAddress fromAddress;
            if (m.getFrom() == null) {
                String addr = SystemSettingsDao.instance.getValue(SystemSettingsDao.EMAIL_FROM_ADDRESS);
                String pretty = SystemSettingsDao.instance.getValue(SystemSettingsDao.EMAIL_FROM_NAME);
                fromAddress = new InternetAddress(addr, pretty, StandardCharsets.UTF_8.name());
            }else {
                fromAddress = m.getFrom();
            }
            String subject = m.getSubject();

            if(SystemSettingsDao.instance.getBooleanValue(SystemSettingsDao.EMAIL_DISABLED)) {
                LOG.warn("Not sending email because email is disabled globally.");
                f.complete(new SentEmail(m.getToAddresses(), fromAddress, m, "Email not sent as email is disabled", this));
                return f;
            }

            Common.backgroundProcessing.addWorkItem(new WorkItem() {

                @Override
                public void execute() {

                    final ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    try (PrintStream ps = new PrintStream(baos, true, StandardCharsets.UTF_8.name())){

                        EmailSender emailSender = new EmailSender(
                                SystemSettingsDao.instance.getValue(SystemSettingsDao.EMAIL_SMTP_HOST),
                                SystemSettingsDao.instance.getIntValue(SystemSettingsDao.EMAIL_SMTP_PORT),
                                SystemSettingsDao.instance.getBooleanValue(SystemSettingsDao.EMAIL_AUTHORIZATION),
                                SystemSettingsDao.instance.getValue(SystemSettingsDao.EMAIL_SMTP_USERNAME),
                                SystemSettingsDao.instance.getValue(SystemSettingsDao.EMAIL_SMTP_PASSWORD),
                                SystemSettingsDao.instance.getBooleanValue(SystemSettingsDao.EMAIL_TLS),
                                SystemSettingsDao.instance.getIntValue(SystemSettingsDao.EMAIL_SEND_TIMEOUT));

                        emailSender.setDebug(ps);
                        emailSender.send(fromAddress, m.getToAddresses().stream().toArray(InternetAddress[] ::new), subject, m.getContent());
                        String debug = new String(baos.toByteArray(), StandardCharsets.UTF_8);
                        f.complete(new SentEmail(m.getToAddresses(), fromAddress, m, debug, EmailMessageTransport.this));
                    }catch (Exception e) {
                        LOG.warn("Error sending email", e);
                        String debug = new String(baos.toByteArray(), StandardCharsets.UTF_8);
                        EmailFailedException failure = new EmailFailedException(e, m.getSubject(), m.getToAddresses().toString(), debug);
                        f.completeExceptionally(failure);
                    }
                }

                @Override
                public int getPriority() {
                    return WorkItem.PRIORITY_LOW;
                }

                @Override
                public String getDescription() {
                    return "Sending email from " + fromAddress.toString() + " about " + subject;
                }

                @Override
                public void rejected(RejectedTaskReason reason) {
                    RejectedTaskException e = new RejectedTaskException(reason);
                    f.completeExceptionally(e);
                }

            });
            return f;
        }catch(Exception e) {
            f.completeExceptionally(e);
            return f;
        }
    }

    @Override
    public void addListener(MessageReceivedListener l) {
        this.listeners.add(l);
    }

    @Override
    public void removeListener(MessageReceivedListener l) {
        this.listeners.remove(l);
    }

}
