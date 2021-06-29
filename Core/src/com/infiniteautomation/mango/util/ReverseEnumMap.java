/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.infiniteautomation.mango.util;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * @author Jared Wiltshire
 */
public class ReverseEnumMap<V, E extends Enum<E> & ReverseEnum<V>> {
    private final Map<V, E> map;

    public ReverseEnumMap(Class<E> enumType) {
        E[] constants = enumType.getEnumConstants();
        this.map = new HashMap<V, E>(constants.length, 1);
        for (E enumConstant : constants) {
            this.map.put(enumConstant.value(), enumConstant);
        }
    }

    public E get(V value) {
        E enumConstant = this.map.get(Objects.requireNonNull(value));
        if (enumConstant == null) {
            throw new IllegalArgumentException("Invalid enum value: " + value.toString());
        }
        return enumConstant;
    }

    public E getNullable(V value) {
        return this.map.get(Objects.requireNonNull(value));
    }
}
