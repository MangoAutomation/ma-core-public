/*
 * Copyright (C) 2018 Infinite Automation Software. All rights reserved.
 */
package com.infiniteautomation.mango.util;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * @author Jared Wiltshire
 */
public class ReverseEnumMap<V, E extends Enum<E> & ReverseEnum<V>> {
    private final Map<V, E> map = new HashMap<V, E>();

    public ReverseEnumMap(Class<E> enumType) {
        for (E enumConstant : enumType.getEnumConstants()) {
            map.put(enumConstant.value(), enumConstant);
        }
    }

    public E get(V value) {
        E enumConstant = map.get(value);
        Objects.requireNonNull(enumConstant, "Invalid enum value");
        return enumConstant;
    }
}
