/*
    Copyright (C) 2006-2009 Serotonin Software Technologies Inc.
 	@author Matthew Lohbihler
 */
package com.serotonin.web.dwr;


/**
 * @author Matthew Lohbihler
 */
public class DwrMessage {
    /**
     * This represents a message that will make sense out of context from any fields that it may be associated with.
     * It can be used when messages are displayed together in a single list or in a popup.
     * Example: "There are problems with your submission. Please try again."
     */
    private String genericMessage;
    
    /**
     * If a contextual message is provided, this field provides the context.
     * Example: "firstName"
     */
    private String contextKey;
    
    /**
     * The contextual message. This will presumably make sense when positioned in the appropriate context (such as
     * beside the offending text field). This can therefore be a much shorter message than the generic message. For
     * this field to be of value, a context key must also be provided.
     */
    private String contextualMessage;
    
    public DwrMessage() {
        // Default constructor in case you like doing things the hard way (or the easy way in the case of reflection).
    }
    
    public DwrMessage(String genericMessage) {
        this.genericMessage = genericMessage;
    }

    public DwrMessage(String contextKey, String contextualMessage) {
        this.contextKey = contextKey;
        this.contextualMessage = contextualMessage;
    }

    public DwrMessage(String genericMessage, String contextKey, String contextualMessage) {
        this.genericMessage = genericMessage;
        this.contextKey = contextKey;
        this.contextualMessage = contextualMessage;
    }

    public String getGenericMessage() {
        return genericMessage;
    }

    public void setGenericMessage(String genericMessage) {
        this.genericMessage = genericMessage;
    }

    public String getContextKey() {
        return contextKey;
    }

    public void setContextKey(String contextKey) {
        this.contextKey = contextKey;
    }

    public String getContextualMessage() {
        return contextualMessage;
    }

    public void setContextualMessage(String contextualMessage) {
        this.contextualMessage = contextualMessage;
    }

    @Override
    public String toString() {
        if (contextKey != null)
            return contextKey +" --> "+ contextualMessage;
        return genericMessage;
    }
}
