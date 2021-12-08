/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2;

import java.util.EnumSet;

import org.checkerframework.checker.nullness.qual.Nullable;

import com.infiniteautomation.mango.util.enums.EnumDeserializer;
import com.infiniteautomation.mango.util.enums.NameEnumDeserializer;
import com.serotonin.m2m2.i18n.TranslatableMessage;

public enum DataTypes {
    // 0 reserved, used to be UNKNOWN
    BINARY(1, new TranslatableMessage("common.dataTypes.binary")),
    MULTISTATE(2, new TranslatableMessage("common.dataTypes.multistate")),
    NUMERIC(3, new TranslatableMessage("common.dataTypes.numeric")),
    ALPHANUMERIC(4, new TranslatableMessage("common.dataTypes.alphanumeric")),
    IMAGE(5, new TranslatableMessage("common.dataTypes.image"));

    private static final EnumDeserializer<DataTypes, Integer> IDS = new EnumDeserializer<>(DataTypes.class, DataTypes::getId);
    private static final EnumDeserializer<DataTypes, String> NAMES = new NameEnumDeserializer<>(DataTypes.class);

    private final int id;
    private final TranslatableMessage description;

    DataTypes(int id, TranslatableMessage description) {
        this.id = id;
        this.description = description;
    }

    public int getId() {
        return id;
    }

    public TranslatableMessage getDescription() {
        return description;
    }

    public static @Nullable DataTypes fromId(int id) {
        return IDS.deserializeNullable(id);
    }

    public static @Nullable DataTypes fromName(String name) {
        return NAMES.deserializeNullable(name);
    }

    public static String formatNames() {
        return NAMES.formatIdentifiers();
    }

    public static String formatNames(EnumSet<DataTypes> exclude) {
        return NAMES.formatIdentifiers(exclude);
    }
}
