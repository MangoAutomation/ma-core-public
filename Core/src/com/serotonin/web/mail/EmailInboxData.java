/*
    Copyright (C) 2006-2009 Serotonin Software Technologies Inc.
 	@author Matthew Lohbihler
 */
package com.serotonin.web.mail;

/**
 * @author Matthew Lohbihler
 */
public class EmailInboxData {
    private String protocol = "pop3";
    private String host;
    private int port = -1;
    private String user;
    private String password;
    private EmailMessageHandler handler;
    private boolean deleteOnError = true;
    
    public String getProtocol() {
        return protocol;
    }
    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }
    public String getHost() {
        return host;
    }
    public void setHost(String host) {
        this.host = host;
    }
    public int getPort() {
        return port;
    }
    public void setPort(int port) {
        this.port = port;
    }
    public String getUser() {
        return user;
    }
    public void setUser(String user) {
        this.user = user;
    }
    public String getPassword() {
        return password;
    }
    public void setPassword(String password) {
        this.password = password;
    }
    public EmailMessageHandler getHandler() {
        return handler;
    }
    public void setHandler(EmailMessageHandler handler) {
        this.handler = handler;
    }
    public boolean isDeleteOnError() {
        return deleteOnError;
    }
    public void setDeleteOnError(boolean deleteOnError) {
        this.deleteOnError = deleteOnError;
    }
}
