/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */

package com.infiniteautomation.mango.db.query;

public class SingleValueConsumer<T> extends LastValueConsumer<T> {

    @Override
    public void accept(T value) {
        if (this.value != null) throw new IllegalStateException("Already set");
        super.accept(value);
    }

}
