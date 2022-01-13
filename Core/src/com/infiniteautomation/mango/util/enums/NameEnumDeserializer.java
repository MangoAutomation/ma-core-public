/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */

package com.infiniteautomation.mango.util.enums;

import java.util.Map;

/**
 * Specialized implementation for deserializing from an enum name, more efficient.
 * @param <E> enum type
 */
public class NameEnumDeserializer<E extends Enum<E>> extends EnumDeserializer<E, String> {

    public NameEnumDeserializer(Class<E> enumType) {
        super(enumType, Enum::name);
    }

    @Override
    protected Map<String, E> constructIdMap() {
        // not used
        return Map.of();
    }

    @Override
    public E deserialize(String identifier) {
        return Enum.valueOf(enumType, identifier);
    }
}
