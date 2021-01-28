/*
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.i18n;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.infiniteautomation.mango.util.exception.ValidationException;
import com.serotonin.m2m2.i18n.ProcessMessage.Level;

/**
 * Represents a generic object that can be returned from a process. Standardized here so that the receiving javascript
 * code can also be standardized. Any of the fields here can be used or not, as appropriate to the context in which it
 * is used.
 *
 * When the TranslatableMessageConverter is used, {@link ProcessMessage} instances have their
 * {@link TranslatableMessage}s automatically converted to translated strings.
 *
 * @author Matthew Lohbihler
 */
public class ProcessResult implements Serializable {

    private static final long serialVersionUID = -9101993007595387629L;

    private List<ProcessMessage> messages = new ArrayList<ProcessMessage>();

    private final String contextKeyPrefix;

    public ProcessResult() {
        this.contextKeyPrefix = "";
    }

    public ProcessResult(String contextKeyPrefix) {
        this.contextKeyPrefix = contextKeyPrefix + ".";
    }

    public boolean getHasMessages() {
        return messages != null && messages.size() > 0;
    }

    public void addGenericMessage(String key, Object... params) {
        addMessage(new ProcessMessage(key, params));
    }

    public void addContextualMessage(String contextKey, String contextualMessageKey, Object... params) {
        addMessage(new ProcessMessage(this.contextKeyPrefix + contextKey, contextualMessageKey, params));
    }

    public void addContextualMessage(String contextKey, TranslatableMessage contextualMessage) {
        addMessage(new ProcessMessage(this.contextKeyPrefix + contextKey, contextualMessage));
    }

    public void addMessage(TranslatableMessage genericMessage) {
        addMessage(new ProcessMessage(genericMessage));
    }

    public void addMessage(Level level, String contextKey, TranslatableMessage contextualMessage) {
        addMessage(new ProcessMessage(level, this.contextKeyPrefix + contextKey, contextualMessage));
    }

    public void addGenericMessage(Level level, String key, Object... params) {
        addMessage(new ProcessMessage(level, key, params));
    }

    public void addContextualMessage(Level level, String contextKey, String contextualMessageKey, Object... params) {
        addMessage(new ProcessMessage(level, this.contextKeyPrefix + contextKey, contextualMessageKey, params));
    }

    public void addMessage(Level level, TranslatableMessage genericMessage) {
        addMessage(new ProcessMessage(level, genericMessage));
    }

    public void addMessage(String contextKey, TranslatableMessage contextualMessage) {
        addMessage(new ProcessMessage(this.contextKeyPrefix + contextKey, contextualMessage));
    }

    public void addMessage(ProcessMessage message) {
        messages.add(message);
    }

    public List<ProcessMessage> getMessages() {
        return messages;
    }

    public void setMessages(List<ProcessMessage> messages) {
        this.messages = messages;
    }

    /**
     * A result is valid if there are no messages at the WARN or ERROR level
     * @return
     */
    public boolean isValid() {
        for(ProcessMessage m : messages)
            if(m.getLevel() == Level.error || m.getLevel() == Level.warning)
                return false;
        return true;
    }

    /**
     * Do not use this method. Instead your VO or model should implement Validatable and override the validate(ProcessResult response) method.
     *
     * Ensure this result is valid
     * @throws ValidationException
     */
    @Deprecated
    public void ensureValid() throws ValidationException {
        if(!isValid())
            throw new ValidationException(this);
    }

    /**
     * Sets a prefix on all context messages
     * @param prefix
     */
    public void prefixContextKey(String prefix) {
        Objects.requireNonNull(prefix);

        if (messages != null) {
            for (ProcessMessage msg : messages) {
                String contextKey = msg.getContextKey();
                if (contextKey != null) {
                    msg.setContextKey(prefix + "." + contextKey);
                }
            }
        }
    }

    /**
     * Copies all messages and data another ProcessResult into this one
     * @param other
     */
    public void addMessages(ProcessResult other) {
        Objects.requireNonNull(other);
        if (other == this) {
            throw new IllegalArgumentException();
        }
        messages.addAll(other.getMessages());
    }

    @Override
    public String toString() {
        return "ProcessResult{" +
                "messages=" + messages +
                ", contextKeyPrefix='" + contextKeyPrefix + '\'' +
                '}';
    }
}
