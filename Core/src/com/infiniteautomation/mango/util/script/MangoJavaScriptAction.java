/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.infiniteautomation.mango.util.script;

import com.serotonin.m2m2.i18n.ProcessMessage.Level;
import com.serotonin.m2m2.i18n.TranslatableMessage;

/**
 * Container for actions executed in a test script
 * @author Terry Packer
 *
 */
public class MangoJavaScriptAction {

    private final TranslatableMessage message;
    private final Level level;
    
    public MangoJavaScriptAction(TranslatableMessage message) {
        this(message, Level.info);
    }
    
    public MangoJavaScriptAction(TranslatableMessage message, Level level) {
        this.message = message;
        this.level = level;
    }

    /**
     * @return the message
     */
    public TranslatableMessage getMessage() {
        return message;
    }

    /**
     * @return the level
     */
    public Level getLevel() {
        return level;
    }
    
}
