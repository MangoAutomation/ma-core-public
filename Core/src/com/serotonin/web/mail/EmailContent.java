package com.serotonin.web.mail;

import java.util.ArrayList;
import java.util.List;

public class EmailContent {
    protected String plainContent;
    protected String htmlContent;
    private final List<EmailAttachment> attachments = new ArrayList<EmailAttachment>(2);
    private final List<EmailInline> inlineParts = new ArrayList<EmailInline>(2);
    protected String encoding;

    protected EmailContent() {
        // no op
    }

    public EmailContent(String plainContent) {
        this(plainContent, null, null);
    }

    public EmailContent(String plainContent, String htmlContent) {
        this(plainContent, htmlContent, null);
    }

    public EmailContent(String plainContent, String htmlContent, String encoding) {
        this.plainContent = plainContent;
        this.htmlContent = htmlContent;
        this.encoding = encoding;
    }

    public boolean isMultipart() {
        return (plainContent != null && htmlContent != null) || !attachments.isEmpty() || !inlineParts.isEmpty();
    }

    public String getHtmlContent() {
        return htmlContent;
    }

    public String getPlainContent() {
        return plainContent;
    }

    public void addAttachment(EmailAttachment emailAttachment) {
        attachments.add(emailAttachment);
    }

    public List<EmailAttachment> getAttachments() {
        return attachments;
    }

    public void addInline(EmailInline emailInline) {
        inlineParts.add(emailInline);
    }

    public List<EmailInline> getInlines() {
        return inlineParts;
    }

    public String getEncoding() {
        return encoding;
    }
}
