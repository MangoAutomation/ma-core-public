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
public class DwrResponseI18n {
    private List<DwrMessageI18n> messages = new ArrayList<DwrMessageI18n>();
    private Map<String, Object> data = new HashMap<String, Object>();
    
    public boolean getHasMessages() {
        return messages != null && messages.size() > 0;
    }
    
    public void addGenericMessage(String key, Object... params) {
        addMessage(new DwrMessageI18n(key, params));
    }
    
    public void addContextualMessage(String contextKey, String contextualMessageKey, Object... params) {
        addMessage(new DwrMessageI18n(contextKey, contextualMessageKey, params));
    }
    
    public void addMessage(LocalizableMessage genericMessage) {
        addMessage(new DwrMessageI18n(genericMessage));
    }
    
    public void addMessage(String contextKey, LocalizableMessage contextualMessage) {
        addMessage(new DwrMessageI18n(contextKey, contextualMessage));
    }
    
    public void addMessage(DwrMessageI18n message) {
        messages.add(message);
    }
    
    public List<DwrMessageI18n> getMessages() {
        return messages;
    }
    
    public void setMessages(List<DwrMessageI18n> messages) {
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
