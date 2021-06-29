/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.infiniteautomation.mango.spring.script;

import javax.script.Bindings;

/**
 * @author Jared Wiltshire
 */
public class EvalResult {
    private final Object value;
    private final Bindings bindings;

    public EvalResult(Object value, Bindings bindings) {
        this.value = value;
        this.bindings = bindings;
    }

    public Object getValue() {
        return value;
    }

    public Object getBindingsValue(String property) {
        return bindings.get(property);
    }
}