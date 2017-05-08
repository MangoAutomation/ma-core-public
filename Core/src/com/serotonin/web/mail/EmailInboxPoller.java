/*
    Copyright (C) 2006-2009 Serotonin Software Technologies Inc.
 	@author Matthew Lohbihler
 */
package com.serotonin.web.mail;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import javax.mail.BodyPart;
import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.internet.MimeMultipart;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * @author Matthew Lohbihler
 */
public class EmailInboxPoller implements Runnable {
    private static final Log LOG = LogFactory.getLog(EmailInboxPoller.class);

    private long checkDelay;
    private String debugFile;
    private final List<EmailInboxData> inboxes = new ArrayList<EmailInboxData>();

    private volatile boolean running;
    private Thread thread;

    public EmailInboxPoller() {
        // no op
    }

    public EmailInboxPoller(long checkDelay) {
        this.checkDelay = checkDelay;
    }

    public EmailInboxPoller(long checkDelay, String host, String user, String password, EmailMessageHandler handler) {
        this(checkDelay);
        addEmailInbox(host, user, password, handler);
    }

    public long getCheckDelay() {
        return checkDelay;
    }

    public void setCheckDelay(long checkDelay) {
        this.checkDelay = checkDelay;
    }

    public String getDebugFile() {
        return debugFile;
    }

    public void setDebugFile(String debugFile) {
        this.debugFile = debugFile;
    }

    public List<EmailInboxData> getInboxes() {
        return inboxes;
    }

    public void addEmailInbox(String host, String user, String password, EmailMessageHandler handler) {
        EmailInboxData data = new EmailInboxData();
        data.setHost(host);
        data.setUser(user);
        data.setPassword(password);
        data.setHandler(handler);
        inboxes.add(data);
    }

    public void addEmailInbox(String protocol, String host, int port, String user, String password,
            EmailMessageHandler handler, boolean deleteOnError) {
        EmailInboxData data = new EmailInboxData();
        data.setProtocol(protocol);
        data.setHost(host);
        data.setPort(port);
        data.setUser(user);
        data.setPassword(password);
        data.setHandler(handler);
        data.setDeleteOnError(deleteOnError);
        inboxes.add(data);
    }

    public void addEmailInbox(EmailInboxData inboxData) {
        inboxes.add(inboxData);
    }

    public void initialize() {
        synchronized (this) {
            if (!running) {
                running = true;
                thread = new Thread(this, "EmailInboxPoller");
                thread.start();
            }
        }
    }

    public void terminate() {
        synchronized (this) {
            if (running) {
                running = false;
                notify();
            }
        }

        Thread localThread = thread;
        if (localThread != null) {
            try {
                localThread.join();
            }
            catch (InterruptedException e) {
                // ignore
            }
            thread = null;
        }
    }

    public void run() {
        LOG.info("EmailInboxPoller started");

        while (running) {
            checkMailboxes();

            synchronized (this) {
                try {
                    if (running)
                        wait(checkDelay);
                }
                catch (InterruptedException e) {
                    // ignore
                }
            }
        }

        LOG.info("EmailInboxPoller stopped");
    }

    public void checkMailboxes() {
        LOG.info("EmailInboxPoller checking all " + inboxes.size() + " inboxes");

        // This method is public, which means it can be called without the poller actually running. So, we need to have
        // an override so that shutdowns can be distinguished from outside calls.
        boolean runningOverride = false;
        if (!running)
            runningOverride = true;

        for (EmailInboxData data : inboxes) {
            if (!running && !runningOverride) {
                LOG.info("EmailInboxPoller shutdown detected. Cancelling mailbox check");
                break;
            }

            try {
                checkMailbox(data);
            }
            catch (Exception e) {
                LOG.error("Exception in getMail: host=" + data.getHost() + ", port=" + data.getPort() + ", user="
                        + data.getUser(), e);
            }
        }
    }

    public void checkMailbox(EmailInboxData data) throws IOException, MessagingException {
        PrintStream debugOut = null;
        Store store = null;
        Folder folder = null;
        try {
            Properties props = new Properties(System.getProperties());
            props.put("mail.store.protocol", data.getProtocol());
            props.put("mail.user", data.getUser());
            props.put("mail.password", data.getPassword());

            // Get a Session object
            Session session = Session.getDefaultInstance(props, null);

            if (!StringUtils.isBlank(debugFile)) {
                session.setDebug(true);
                debugOut = new PrintStream(new FileOutputStream(debugFile, true));
                debugOut.println();
                debugOut.println("*****************************************");
                debugOut.println("*** GetMail at " + new Date());
                session.setDebugOut(debugOut);
            }
            else
                session.setDebug(false);

            // Get a Store object
            store = session.getStore();

            // Connect
            store.connect(data.getHost(), data.getPort(), data.getUser(), data.getPassword());

            // Open the Folder
            folder = store.getDefaultFolder();
            if (folder == null) {
                LOG.error("No default folder");
                return;
            }

            folder = folder.getFolder("INBOX");
            if (folder == null) {
                LOG.error("Invalid folder");
                return;
            }

            folder.open(Folder.READ_WRITE);

            int totalMessages = folder.getMessageCount();
            if (totalMessages == 0) {
                // LOG.debug("No messages to process");
                return;
            }

            if (LOG.isDebugEnabled())
                LOG.debug("Total messages = " + totalMessages);

            // Attributes & Flags for all messages ..
            Message[] msgs = folder.getMessages();

            // Use a suitable FetchProfile
            // FetchProfile fp = new FetchProfile();
            // fp.add(FetchProfile.Item.ENVELOPE);
            // fp.add(FetchProfile.Item.FLAGS);
            // fp.add("X-Mailer");
            // folder.fetch(msgs, fp);
            for (int i = 0; i < msgs.length; i++) {
                LOG.debug("Processing message " + (i + 1));
                // dumpEnvelope(msgs[i]);

                boolean markForDeletion;
                try {
                    markForDeletion = data.getHandler().handle(msgs[i]);
                }
                catch (RuntimeException e) {
                    LOG.debug("Error in email handler", e);
                    markForDeletion = data.isDeleteOnError();
                }

                if (markForDeletion)
                    msgs[i].setFlag(Flags.Flag.DELETED, true);
            }
        }
        finally {
            try {
                if (folder != null)
                    folder.close(true);
            }
            catch (MessagingException e) {
                LOG.warn("Error closing folder", e);
            }

            try {
                if (store != null)
                    store.close();
            }
            catch (MessagingException e) {
                LOG.warn("Error closing store", e);
            }

            if (debugOut != null)
                debugOut.flush();
        }
    }

    public static String getEmailBodyContent(Message message) throws IOException, MessagingException {
        Object content = message.getContent();
        if (content instanceof String)
            return (String) content;

        if (content instanceof MimeMultipart) {
            MimeMultipart mp = (MimeMultipart) content;

            if (mp.getCount() == 1 && mp.getBodyPart(0).getContentType().startsWith("multipart/related"))
                mp = (MimeMultipart) mp.getBodyPart(0).getContent();

            // Look for a text part
            for (int i = 0; i < mp.getCount(); i++) {
                BodyPart bp = mp.getBodyPart(i);
                if (bp.getContentType().startsWith("text/plain;"))
                    return (String) bp.getContent();
            }

            // Look for an HTML part
            for (int i = 0; i < mp.getCount(); i++) {
                BodyPart bp = mp.getBodyPart(i);
                if (bp.getContentType().startsWith("text/html;"))
                    return (String) bp.getContent();
            }

            // So, what is there?
            for (int i = 0; i < mp.getCount(); i++) {
                BodyPart bp = mp.getBodyPart(i);
                LOG.debug("ContentType: " + bp.getContentType());
            }
        }

        LOG.error("Don't know how to handle an email with this type of content: " + content.getClass() + ", type="
                + message.getContentType());

        return "";
    }
}
