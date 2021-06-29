/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.infiniteautomation.mango.util.script;

import com.infiniteautomation.mango.util.ReverseEnum;
import com.infiniteautomation.mango.util.ReverseEnumMap;
import com.serotonin.m2m2.i18n.TranslatableMessage;

/**
 * Types of ways a Script can be triggered via its context points
 * 
 * @author Terry Packer
 *
 */
public enum ContextUpdateEvent implements ReverseEnum<Integer> {

    UPDATE (0, new TranslatableMessage("dsEdit.pointEvent.update")),
    CHANGE(1, new TranslatableMessage("dsEdit.pointEvent.change")),
    LOGGED(2, new TranslatableMessage("dsEdit.pointEvent.logged")),
    NONE(3, new TranslatableMessage("dsEdit.pointEvent.none")),
    CRON(4, new TranslatableMessage("dsEdit.pointEvent.cron"));

    private static ReverseEnumMap<Integer, ContextUpdateEvent> map = new ReverseEnumMap<>(ContextUpdateEvent.class);
    private final int value;
    private final TranslatableMessage description;

    private ContextUpdateEvent(int value, TranslatableMessage description) {
        this.value = value;
        this.description = description;
    }

    @Override
    public Integer value() {
        return this.value;
    }

    public TranslatableMessage getDescription() {
        return description;
    }

    public static ContextUpdateEvent fromValue(Integer value) {
        return map.get(value);
    }

    public static ContextUpdateEvent fromName(String name) {
        return Enum.valueOf(ContextUpdateEvent.class, name);
    }
    
}
