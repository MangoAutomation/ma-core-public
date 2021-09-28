/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */

package com.serotonin.db.spring;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.function.Consumer;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.RowMapper;

public class ConsumerRowCallbackHandler<T> implements RowCallbackHandler {
    private final Consumer<T> callback;
    private final RowMapper<T> rowMapper;
    private int rowNum;

    public ConsumerRowCallbackHandler(RowMapper<T> rowMapper, Consumer<T> callback) {
        this.callback = callback;
        this.rowMapper = rowMapper;
    }

    @Override
    public void processRow(@NonNull ResultSet rs) throws SQLException {
        callback.accept(rowMapper.mapRow(rs, rowNum++));
    }
}
