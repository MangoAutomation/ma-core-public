/*
    Copyright (C) 2006-2007 Serotonin Software Technologies Inc.
 	@author Matthew Lohbihler
 */
package com.serotonin.web.mail;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;

import javax.activation.DataSource;
import javax.mail.MessagingException;

import org.springframework.core.io.InputStreamSource;
import org.springframework.mail.javamail.MimeMessageHelper;

/**
 * @author Matthew Lohbihler
 */
abstract public class EmailAttachment {
    protected String filename;

    public EmailAttachment(String filename) {
        this.filename = filename;
    }

    public String getFilename() {
        return filename;
    }

    abstract public void attach(MimeMessageHelper mimeMessageHelper) throws MessagingException;

    // public static EmailAttachment createFromByteArray(String filename, byte[] content) {
    // return new InputStreamAttachment(filename, new ByteArrayInputStream(content));
    // }
    //    
    // public static EmailAttachment createFromByteArray(String filename, byte[] content, String contentType) {
    // return new InputStreamAttachment(filename, new ByteArrayInputStream(content), contentType);
    // }
    //    
    public static class FileAttachment extends EmailAttachment {
        private final File file;

        public FileAttachment(String filename, String systemFilename) {
            this(filename, new File(systemFilename));
        }

        public FileAttachment(File file) {
            super(file.getName());
            this.file = file;
        }

        public FileAttachment(String filename, File file) {
            super(filename);
            this.file = file;
        }

        @Override
        public void attach(MimeMessageHelper mimeMessageHelper) throws MessagingException {
            mimeMessageHelper.addAttachment(filename, file);
        }
    }

    public static class DataSourceAttachment extends EmailAttachment {
        private final DataSource dataSource;

        public DataSourceAttachment(String filename, DataSource dataSource) {
            super(filename);
            this.dataSource = dataSource;
        }

        @Override
        public void attach(MimeMessageHelper mimeMessageHelper) throws MessagingException {
            mimeMessageHelper.addAttachment(filename, dataSource);
        }
    }

    public static class ByteArrayAttachment extends EmailAttachment {
        final byte[] data;
        private final String contentType;

        public ByteArrayAttachment(String filename, byte[] data) {
            this(filename, data, null);
        }

        public ByteArrayAttachment(String filename, byte[] data, String contentType) {
            super(filename);
            this.data = data;
            this.contentType = contentType;
        }

        @Override
        public void attach(MimeMessageHelper mimeMessageHelper) throws MessagingException {
            InputStreamSource source = new InputStreamSource() {
                public InputStream getInputStream() {
                    return new ByteArrayInputStream(data);
                }
            };

            if (contentType == null)
                mimeMessageHelper.addAttachment(filename, source);
            else
                mimeMessageHelper.addAttachment(filename, source, contentType);
        }
    }
}
