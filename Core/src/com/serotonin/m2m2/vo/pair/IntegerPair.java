/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.vo.pair;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.jdbc.core.RowMapper;

/**
 * @author Matthew Lohbihler
 */
public class IntegerPair {
    public static final RowMapper<IntegerPair> ROW_MAPPER = new RowMapper<IntegerPair>() {
        @Override
        public IntegerPair mapRow(ResultSet rs, int index) throws SQLException {
            return new IntegerPair(rs.getInt(1), rs.getInt(2));
        }
    };

    private int i1;
    private int i2;

    public IntegerPair() {
        // no op
    }

    public IntegerPair(int i1, int i2) {
        this.i1 = i1;
        this.i2 = i2;
    }

    public int getI1() {
        return i1;
    }

    public void setI1(int i1) {
        this.i1 = i1;
    }

    public int getI2() {
        return i2;
    }

    public void setI2(int i2) {
        this.i2 = i2;
    }
}
