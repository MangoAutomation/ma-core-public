/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2;

import java.util.EnumSet;

import org.checkerframework.checker.nullness.qual.Nullable;

import com.infiniteautomation.mango.util.enums.EnumDeserializer;
import com.infiniteautomation.mango.util.enums.NameEnumDeserializer;
import com.serotonin.m2m2.i18n.TranslatableMessage;

public enum DataType {
    // 0 reserved, used to be UNKNOWN
    BINARY(1, new TranslatableMessage("common.dataTypes.binary")),
    MULTISTATE(2, new TranslatableMessage("common.dataTypes.multistate")),
    NUMERIC(3, new TranslatableMessage("common.dataTypes.numeric")),
    ALPHANUMERIC(4, new TranslatableMessage("common.dataTypes.alphanumeric")),
    IMAGE(5, new TranslatableMessage("common.dataTypes.image"));

    private static final EnumDeserializer<DataType, Integer> IDS = new EnumDeserializer<>(DataType.class, DataType::getId);
    private static final EnumDeserializer<DataType, String> NAMES = new NameEnumDeserializer<>(DataType.class);

    private final int id;
    private final TranslatableMessage description;

    DataType(int id, TranslatableMessage description) {
        this.id = id;
        this.description = description;
    }

    public int getId() {
        return id;
    }

    public TranslatableMessage getDescription() {
        return description;
    }

    public static @Nullable DataType fromId(int id) {
        return IDS.deserializeNullable(id);
    }

    public static @Nullable DataType fromName(String name) {
        return NAMES.deserializeNullable(name);
    }

    public static String formatNames() {
        return NAMES.formatIdentifiers();
    }

    public static String formatNames(EnumSet<DataType> exclude) {
        return NAMES.formatIdentifiers(exclude);
    }
}
