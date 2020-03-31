/*
 * Copyright (C) 2020 Infinite Automation Software. All rights reserved.
 */
package com.infiniteautomation.mango.bootstrap.classloader;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.Enumeration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

import com.infiniteautomation.mango.bootstrap.BootstrapUtils;

/**
 * Used to load classes and resources into memory so the jar can be closed and overwritten.
 *
 * @author Jared Wiltshire
 */
public class MemoryClassLoader extends ClassLoader {

    private final ConcurrentHashMap<String, byte[]> entries = new ConcurrentHashMap<>();

    public MemoryClassLoader() {
        super();
    }

    public MemoryClassLoader(ClassLoader parent) {
        super(parent);
    }

    public void loadJar(String resourceName) throws IOException {
        try (JarInputStream in = new JarInputStream(getParent().getResourceAsStream(resourceName))) {
            for (JarEntry entry; (entry = in.getNextJarEntry()) != null;) {
                if (!entry.isDirectory()) {
                    byte[] data = BootstrapUtils.toByteArray(in, (int) entry.getSize());
                    entries.put(entry.getName(), data);
                }
            }
        }
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        String entryName = name.replace('.', '/').concat(".class");

        byte[] data = entries.remove(entryName);
        if (data == null) {
            throw new ClassNotFoundException(name);
        }

        return defineClass(name, data, 0, data.length);
    }

    @Override
    protected URL findResource(String name) {
        byte[] data = entries.get(name);
        if (data == null) {
            return null;
        }

        try {
            return new URL("byte-array", "", -1, "/".concat(name), new ByteArrayURLStreamHandler(data));
        } catch (MalformedURLException e) {
            return null;
        }
    }

    @Override
    protected Enumeration<URL> findResources(String name) throws IOException {
        URL resource = this.findResource(name);
        if (resource == null) {
            return Collections.emptyEnumeration();
        }
        return Collections.enumeration(Collections.singletonList(resource));
    }

}
