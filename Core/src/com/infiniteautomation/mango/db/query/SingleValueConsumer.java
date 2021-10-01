/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */

package com.infiniteautomation.mango.db.query;

import java.util.Optional;
import java.util.function.Consumer;

public class SingleValueConsumer<T> implements Consumer<T> {
    private T singleValue;

    @Override
    public void accept(T value) {
        if (this.singleValue != null) throw new IllegalStateException("Already set");
        this.singleValue = value;
    }

    public Optional<T> getValue() {
        return Optional.ofNullable(singleValue);
    }
}
