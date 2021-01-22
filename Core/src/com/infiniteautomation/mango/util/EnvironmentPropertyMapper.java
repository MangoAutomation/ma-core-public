/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */

package com.infiniteautomation.mango.util;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

import org.springframework.core.env.Environment;

public class EnvironmentPropertyMapper {
    protected final Environment env;
    protected final String[] prefixes;

    public EnvironmentPropertyMapper(Environment env) {
        this(env, "");
    }

    public EnvironmentPropertyMapper(Environment env, String... prefixes) {
        this.env = env;
        this.prefixes = prefixes;
    }

    public <R> Optional<R> map(String property, Class<R> type) {
        return map(property, type, Function.identity());
    }

    public Optional<String> map(String property) {
        return map(property, String.class);
    }

    public <R> Optional<R> map(String property, Function<String,R> transform) {
        return map(property, String.class, transform);
    }

    public <K,R> Optional<R> map(String property, Class<K> type, Function<K,R> transform) {
        return Arrays.stream(prefixes)
                .map(prefix -> env.getProperty(prefix + property, type))
                .filter(Objects::nonNull)
                .map(transform)
                .filter(Objects::nonNull)
                .findFirst();
    }
}
