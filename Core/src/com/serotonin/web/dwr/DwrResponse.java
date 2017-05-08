/*
    Copyright (C) 2006-2007 Serotonin Software Technologies Inc.
 	@author Matthew Lohbihler
 */
package com.serotonin.web.dwr;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.serotonin.web.i18n.LocalizableMessage;

/**
 * Represents a generic object that can be returned from a DWR request handler. Standardized here so that the receiving
 * javascript code can also be standardized. Any of the fields here can be used or not, as appropriate to the context
 * in which it is used.
 * 
 * When the LocalizableMessageConverter is used, {@link DwrMessageI18n} instances have their 
 * {@link LocalizableMessage}s automatically converted to localized strings. 
 * 
 * @author Matthew Lohbihler
 */
public class DwrResponse {
    private List<DwrMessage> messages = new ArrayList<DwrMessage>();
    private Map<String, Object> data = new HashMap<String, Object>();
    
    public boolean getHasMessages() {
        return messages != null && messages.size() > 0;
    }
    
    public void addMessage(String genericMessage) {
        addMessage(new DwrMessage(genericMessage));
    }
    
    public void addMessage(String contextKey, String contextualMessage) {
        addMessage(new DwrMessage(contextKey, contextualMessage));
    }
    
    public void addMessage(DwrMessage message) {
        messages.add(message);
    }
    
    public List<DwrMessage> getMessages() {
        return messages;
    }
    
    public void setMessages(List<DwrMessage> messages) {
        this.messages = messages;
    }
    
    public void addData(String key, Object value) {
        data.put(key, value);
    }
    
    public Map<String, Object> getData() {
        return data;
    }
    
    public void setData(Map<String, Object> data) {
        this.data = data;
    }
}
