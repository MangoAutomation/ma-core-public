package com.serotonin.provider;

import java.util.HashMap;
import java.util.Map;

public class Providers {
    private static final Map<String, Provider> PROVIDERS = new HashMap<String, Provider>();

    public static void add(Class<? extends Provider> clazz, Provider o) {
        add(clazz.getName(), o);
    }

    public static void add(String providerId, Provider o) {
        if (PROVIDERS.containsKey(providerId))
            throw new RuntimeException("Provider id already exists, cannot replace");
        PROVIDERS.put(providerId, o);
    }

    public static <T extends Provider> T get(Class<T> clazz) throws ProviderNotFoundException {
        return Providers.<T> get(clazz.getName());
    }

    @SuppressWarnings("unchecked")
    public static <T extends Provider> T get(String providerId) throws ProviderNotFoundException {
        T provider = (T) PROVIDERS.get(providerId);

        // Throw a descriptive runtime exception here, since this will typically otherwise result in an
        // undescriptive NPE.
        if (provider == null)
            throw new ProviderNotFoundException(providerId);

        return provider;
    }
}
