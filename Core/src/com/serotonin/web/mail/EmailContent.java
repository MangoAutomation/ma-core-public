package com.serotonin.web.mail;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class EmailContent {
    protected String plainContent;
    protected String htmlContent;
    private final List<EmailAttachment> attachments = new ArrayList<EmailAttachment>(2);
    private final List<EmailInline> inlineParts = new ArrayList<EmailInline>(2);
    protected Charset encoding;

    protected EmailContent() {
        // no op
    }
    
    public EmailContent(String plainContent) {
        this(plainContent, null, StandardCharsets.UTF_8);
    }

    public EmailContent(String plainContent, String htmlContent) {
        this(plainContent, htmlContent, StandardCharsets.UTF_8);
    }

    public EmailContent(String plainContent, String htmlContent, Charset encoding) {
        this.plainContent = plainContent;
        this.htmlContent = htmlContent;
        this.encoding = Objects.requireNonNull(encoding);
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

    public Charset getEncoding() {
        return encoding;
    }
}
