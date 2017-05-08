/*
    Copyright (C) 2006-2009 Serotonin Software Technologies Inc.
 	@author Matthew Lohbihler
 */
package com.serotonin.web.mail;

import javax.mail.Message;

/**
 * @author Matthew Lohbihler
 */
public interface EmailMessageHandler {
    boolean handle(Message message);
}
