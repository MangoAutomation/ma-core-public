/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.vo.pair;

/**
 * @author Matthew Lohbihler
 */
public class StringIntPair {
    private String s;
    private int i;

    public StringIntPair() {
        // no op
    }

    public StringIntPair(String s, int i) {
        this.s = s;
        this.i = i;
    }

    public String getString() {
        return s;
    }

    public void setString(String s) {
        this.s = s;
    }

    public int getInt() {
        return i;
    }

    public void setInt(int i) {
        this.i = i;
    }
}
