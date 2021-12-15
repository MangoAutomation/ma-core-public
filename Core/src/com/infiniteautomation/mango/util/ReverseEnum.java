/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.infiniteautomation.mango.util;

import java.util.Objects;

/**
 * @author Jared Wiltshire
 */
public interface ReverseEnum<V> {
    V value();

    /**
     * Iterates over the enum constants and finds the enum constant that matches the value.
     * Typically one would use a ReverseEnumMap instead.
     *
     */
    public static <X, E extends Enum<E> & ReverseEnum<X>> E fromValue(Class<E> enumType, X value) {
        Objects.requireNonNull(enumType);
        Objects.requireNonNull(value);

        E[] constants = enumType.getEnumConstants();
        for (E enumConstant : constants) {
            if (value.equals(enumConstant.value())) {
                return enumConstant;
            }
        }

        throw new IllegalArgumentException();
    }
}
