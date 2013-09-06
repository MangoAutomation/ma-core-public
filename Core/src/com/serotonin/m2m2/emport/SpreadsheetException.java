/*
    Copyright (C) 2013 Deltamation Software All rights reserved.
    @author Terry Packer
 */
package com.serotonin.m2m2.emport;

import java.util.ArrayList;
import java.util.List;

import com.serotonin.m2m2.i18n.TranslatableMessage;

/**
 * Copyright (C) 2013 Deltamation Software. All Rights Reserved.
 * @author Terry Packer
 *
 */
public class SpreadsheetException extends Exception {
    private List<TranslatableMessage> messages;
    private Integer rowNum = null;

    public void setRowNum(Integer rowNum) {
        this.rowNum = rowNum;
    }

    /**
     * @param rowNum
     * @param key
     * @param args
     */
    public SpreadsheetException(Integer rowNum, String key, Object... args) {
        messages = new ArrayList<TranslatableMessage>();
        messages.add(new TranslatableMessage(key, args));
        this.rowNum = rowNum;
    }
    
    /**
     * @param rowNum
     * @param translatableMessage
     */
    public SpreadsheetException(Integer rowNum, List<TranslatableMessage> messages) {
        this.messages = new ArrayList<TranslatableMessage>();
        this.messages.addAll(messages);
        this.rowNum = rowNum;
    }
    
    /**
     * @param key
     * @param args
     */
    public SpreadsheetException(String key, Object... args) {
        messages = new ArrayList<TranslatableMessage>();
        messages.add(new TranslatableMessage(key, args));
    }
    
    /**
     * @param translatableMessage
     */
    public SpreadsheetException(List<TranslatableMessage> messages) {
        this.messages = new ArrayList<TranslatableMessage>();
        this.messages.addAll(messages);
    }
    
    public List<TranslatableMessage> getMessages() {
        List<TranslatableMessage> rowMessages = new ArrayList<TranslatableMessage>();
        if (rowNum != null) {
            for (TranslatableMessage msg : messages) {
                rowMessages.add(new TranslatableMessage("delta.util.spreadsheet.rowError", rowNum+1, msg));
            }
        }
        return rowMessages;
    }

    private static final long serialVersionUID = 1L;
}