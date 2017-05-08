/*
    Copyright (C) 2006-2009 Serotonin Software Technologies Inc.
 	@author Matthew Lohbihler
 */
package com.serotonin.web.dwr;

import java.util.ResourceBundle;

import com.serotonin.web.i18n.LocalizableMessage;

/**
 * @author Matthew Lohbihler
 */
public class DwrMessageI18n {
    /**
     * This represents a message that will make sense out of context from any fields that it may be associated with.
     * It can be used when messages are displayed together in a single list or in a popup.
     * Example: "There are problems with your submission. Please try again."
     */
    private LocalizableMessage genericMessage;
    
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
    private LocalizableMessage contextualMessage;
    
    public DwrMessageI18n() {
        // Default constructor in case you like doing things the hard way (or the easy way in the case of reflection).
    }
    
    public DwrMessageI18n(String genericMessageKey, Object... genericMessageParams) {
        this(new LocalizableMessage(genericMessageKey, genericMessageParams));
    }

    public DwrMessageI18n(LocalizableMessage genericMessage) {
        this.genericMessage = genericMessage;
    }

    public DwrMessageI18n(String contextKey, String contextualMessageKey, Object... contextualMessageParams) {
        this(contextKey, new LocalizableMessage(contextualMessageKey, contextualMessageParams));
    }

    public DwrMessageI18n(String contextKey, LocalizableMessage contextualMessage) {
        this.contextKey = contextKey;
        this.contextualMessage = contextualMessage;
    }

    public DwrMessageI18n(String genericMessageKey, String contextKey, String contextualMessageKey) {
        this(new LocalizableMessage(genericMessageKey), contextKey, new LocalizableMessage(contextualMessageKey));
    }

    public DwrMessageI18n(LocalizableMessage genericMessage, String contextKey,
            LocalizableMessage contextualMessage) {
        this.genericMessage = genericMessage;
        this.contextKey = contextKey;
        this.contextualMessage = contextualMessage;
    }

    public LocalizableMessage getGenericMessage() {
        return genericMessage;
    }

    public void setGenericMessage(LocalizableMessage genericMessage) {
        this.genericMessage = genericMessage;
    }

    public String getContextKey() {
        return contextKey;
    }

    public void setContextKey(String contextKey) {
        this.contextKey = contextKey;
    }

    public LocalizableMessage getContextualMessage() {
        return contextualMessage;
    }

    public void setContextualMessage(LocalizableMessage contextualMessage) {
        this.contextualMessage = contextualMessage;
    }

    public String toString(ResourceBundle bundle) {
        if (contextKey != null)
            return contextKey +" --> "+ contextualMessage.getLocalizedMessage(bundle);
        return genericMessage.getLocalizedMessage(bundle);
    }
}
