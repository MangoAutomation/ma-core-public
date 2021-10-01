/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */

package com.infiniteautomation.mango.db.query;

import java.util.function.Consumer;

public class CountingConsumer<T> implements Consumer<T> {
    private long count = 0;

    @Override
    public void accept(T value) {
        count++;
    }

    public long getCount() {
        return count;
    }
}
