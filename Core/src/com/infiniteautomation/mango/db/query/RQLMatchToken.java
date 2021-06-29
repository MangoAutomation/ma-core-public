/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.infiniteautomation.mango.db.query;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.serotonin.m2m2.Common;

/**
 * @author Jared Wiltshire
 */
public enum RQLMatchToken {
    SINGLE_CHARACTER_WILDCARD('?'), MULTI_CHARACTER_WILDCARD('*');

    private char value;

    private RQLMatchToken(char value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return String.valueOf(this.value);
    }

    private static final Map<String, RQLMatchToken> MAP_STRINGS;
    static {
        MAP_STRINGS = Arrays.stream(RQLMatchToken.values()).collect(Collectors.toMap(RQLMatchToken::toString, Function.identity()));
    }

    private static final Pattern MATCH_SPECIAL_TOKENS = Pattern.compile(Arrays.stream(RQLMatchToken.values()).map(t -> {
        return "\\" + t;
    }).collect(Collectors.joining("|")));

    public static Stream<Object> tokenize(String matchString) {
        return Common.tokenize(MATCH_SPECIAL_TOKENS, matchString).stream().map(t -> {
            RQLMatchToken mt = MAP_STRINGS.get(t);
            if (mt != null) {
                return mt;
            }
            return t;
        });
    }
}