/*
    Copyright (C) 2006-2007 Serotonin Software Technologies Inc.
 	@author Matthew Lohbihler
 */
package com.serotonin.web.mail;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;

import javax.mail.MessagingException;

import org.springframework.core.io.InputStreamSource;
import org.springframework.mail.javamail.ConfigurableMimeFileTypeMap;
import org.springframework.mail.javamail.MimeMessageHelper;

/**
 * @author Matthew Lohbihler
 */
abstract public class EmailInline {
    protected String contentId;

    public EmailInline(String contentId) {
        this.contentId = contentId;
    }

    public String getContentId() {
        return contentId;
    }

    abstract public void attach(MimeMessageHelper mimeMessageHelper) throws MessagingException;

    public static class FileInline extends EmailInline {
        private final File file;

        public FileInline(String contentId, String filename) {
            this(contentId, new File(filename));
        }

        public FileInline(String contentId, File file) {
            super(contentId);
            this.file = file;
        }

        @Override
        public void attach(MimeMessageHelper mimeMessageHelper) throws MessagingException {
            mimeMessageHelper.addInline(contentId, file);
        }
    }

    public static class ByteArrayInline extends EmailInline {
        final byte[] content;
        private final String contentType;

        public ByteArrayInline(String contentId, byte[] content) {
            super(contentId);
            this.content = content;

            ConfigurableMimeFileTypeMap fileTypeMap = new ConfigurableMimeFileTypeMap();
            fileTypeMap.afterPropertiesSet();
            this.contentType = fileTypeMap.getContentType(contentId);
        }

        public ByteArrayInline(String contentId, byte[] content, String contentType) {
            super(contentId);
            this.content = content;
            this.contentType = contentType;
        }

        @Override
        public void attach(MimeMessageHelper mimeMessageHelper) throws MessagingException {
            mimeMessageHelper.addInline(contentId, new InputStreamSource() {
                public InputStream getInputStream() {
                    return new ByteArrayInputStream(content);
                }
            }, contentType);
        }
    }
}
