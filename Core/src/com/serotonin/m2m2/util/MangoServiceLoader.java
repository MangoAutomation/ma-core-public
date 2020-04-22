/*
 * Copyright (C) 2020 Infinite Automation Software. All rights reserved.
 */
package com.serotonin.m2m2.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Jared Wiltshire
 */
public class MangoServiceLoader {

    public static <T> Set<Class<? extends T>> load(Class<T> service, ClassLoader classloader) throws IOException, ClassNotFoundException {
        return load(service, classloader, null);
    }

    /**
     * Rough equivalent of {@link java.util.ServiceLoader#load(Class, ClassLoader)} that does not instantiate the discovered classes.
     * We need to load the service file using a class loader for a single module then create the instances using the full class loader.
     *
     * <p>TODO Java 9+ use the ServiceLoader.stream() method</p>
     * @param <T>
     * @param service
     * @param classloader
     * @param location defaults to "META-INF/services"
     * @return
     * @throws IOException
     * @throws ClassNotFoundException
     */
    public static <T> Set<Class<? extends T>> load(Class<T> service, ClassLoader classloader, String location) throws IOException, ClassNotFoundException {
        if (location == null) {
            location = "META-INF/services";
        }
        String fullName = location + "/" + service.getName();
        Enumeration<URL> serviceFileUrls = classloader.getResources(fullName);
        Set<Class<? extends T>> classes = new HashSet<>();
        for (URL url : Collections.list(serviceFileUrls)) {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    int comment = line.indexOf('#');
                    if (comment >= 0) line = line.substring(0, comment);
                    String className = line.trim();
                    if (!className.isEmpty()) {
                        Class<?> provider = Class.forName(className, false, classloader);
                        classes.add(provider.asSubclass(service));
                    }
                }
            }
        }
        return classes;
    }
}
