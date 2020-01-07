package com.serotonin.provider;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Jared Wiltshire
 */
public class Providers {
    private static final Map<Class<?>, Object> PROVIDERS = new ConcurrentHashMap<>();

    public static <T> void add(Class<T> providerId, T o) {
        Object prev = PROVIDERS.putIfAbsent(providerId, o);
        if (prev != null) {
            throw new RuntimeException("Provider already exists, cannot replace");
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> T get(Class<T> providerId) {
        T provider = (T) PROVIDERS.get(providerId);
        // Throw a descriptive runtime exception here instead of NPE
        if (provider == null) {
            throw new ProviderNotFoundException(providerId.getName());
        }
        return provider;
    }
}
