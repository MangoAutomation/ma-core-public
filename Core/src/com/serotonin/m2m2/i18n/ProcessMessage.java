/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.i18n;

import java.io.Serializable;

import com.serotonin.m2m2.Common;

/**
 * @author Matthew Lohbihler
 */
public class ProcessMessage implements Serializable {

    private static final long serialVersionUID = 49963516519482601L;

    public enum Level {
        info, warning, error;
    }

    private Level level;

    /**
     * This represents a message that will make sense out of context from any fields that it may be associated with. It
     * can be used when messages are displayed together in a single list or in a popup. Example:
     * "There are problems with your submission. Please try again."
     */
    private TranslatableMessage genericMessage;

    /**
     * If a contextual message is provided, this field provides the context. Example: "firstName"
     */
    private String contextKey;

    /**
     * The contextual message. This will presumably make sense when positioned in the appropriate context (such as
     * beside the offending text field). This can therefore be a much shorter message than the generic message. For this
     * field to be of value, a context key must also be provided.
     */
    private TranslatableMessage contextualMessage;

    public ProcessMessage() {
        // Default constructor in case you like doing things the hard way (or the easy way in the case of reflection).
    }

    public ProcessMessage(String genericMessageKey, Object... genericMessageParams) {
        this(Level.error, new TranslatableMessage(genericMessageKey, genericMessageParams));
    }

    public ProcessMessage(TranslatableMessage genericMessage) {
        this(Level.error, genericMessage, null, null);
    }

    public ProcessMessage(String contextKey, String contextualMessageKey, Object... contextualMessageParams) {
        this(Level.error, contextKey, new TranslatableMessage(contextualMessageKey, contextualMessageParams));
    }

    public ProcessMessage(String contextKey, TranslatableMessage contextualMessage) {
        this(Level.error, null, contextKey, contextualMessage);
    }

    public ProcessMessage(String genericMessageKey, String contextKey, String contextualMessageKey) {
        this(Level.error, new TranslatableMessage(genericMessageKey), contextKey, new TranslatableMessage(
                contextualMessageKey));
    }

    public ProcessMessage(TranslatableMessage genericMessage, String contextKey, TranslatableMessage contextualMessage) {
        this(Level.error, genericMessage, contextKey, contextualMessage);
    }

    public ProcessMessage(Level level, String genericMessageKey, Object... genericMessageParams) {
        this(level, new TranslatableMessage(genericMessageKey, genericMessageParams));
    }

    public ProcessMessage(Level level, TranslatableMessage genericMessage) {
        this(level, genericMessage, null, null);
    }

    public ProcessMessage(Level level, String contextKey, String contextualMessageKey,
            Object... contextualMessageParams) {
        this(level, contextKey, new TranslatableMessage(contextualMessageKey, contextualMessageParams));
    }

    public ProcessMessage(Level level, String contextKey, TranslatableMessage contextualMessage) {
        this(level, null, contextKey, contextualMessage);
    }

    public ProcessMessage(Level level, String genericMessageKey, String contextKey, String contextualMessageKey) {
        this(level, new TranslatableMessage(genericMessageKey), contextKey, new TranslatableMessage(
                contextualMessageKey));
    }

    public ProcessMessage(Level level, TranslatableMessage genericMessage, String contextKey,
            TranslatableMessage contextualMessage) {
        this.level = level;
        this.genericMessage = genericMessage;
        this.contextKey = contextKey;
        this.contextualMessage = contextualMessage;
    }

    public Level getLevel() {
        return level;
    }

    public void setLevel(Level level) {
        this.level = level;
    }

    public TranslatableMessage getGenericMessage() {
        return genericMessage;
    }

    public void setGenericMessage(TranslatableMessage genericMessage) {
        this.genericMessage = genericMessage;
    }

    public String getContextKey() {
        return contextKey;
    }

    public void setContextKey(String contextKey) {
        this.contextKey = contextKey;
    }

    public TranslatableMessage getContextualMessage() {
        return contextualMessage;
    }

    public void setContextualMessage(TranslatableMessage contextualMessage) {
        this.contextualMessage = contextualMessage;
    }

    public String toString(Translations translations) {
        if (contextKey != null)
            return contextKey + " --> " + contextualMessage.translate(translations);
        return genericMessage.translate(translations);
    }

    @Override
    public String toString() {
        if (contextKey != null)
            return contextKey + " --> " + contextualMessage.translate(Common.getTranslations());
        return genericMessage.translate(Common.getTranslations());

    }
}
