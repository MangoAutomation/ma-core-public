/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */

package com.infiniteautomation.mango.db.query;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;

public class LastValueConsumer<T> implements Consumer<T> {
    protected T value;

    @Override
    public void accept(T value) {
        this.value = value;
    }

    public Optional<T> getValue() {
        return Optional.ofNullable(value);
    }

    /**
     * @return lazy stream that returns the value
     */
    public Stream<T> streamValue() {
        return Stream.generate(() -> value).takeWhile(Objects::nonNull).limit(1);
    }
}
