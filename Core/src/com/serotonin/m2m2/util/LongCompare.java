package com.serotonin.m2m2.util;

public class LongCompare {
    public static int compare(long x, long y) {
        return (x < y) ? -1 : ((x == y) ? 0 : 1);
    }
}
