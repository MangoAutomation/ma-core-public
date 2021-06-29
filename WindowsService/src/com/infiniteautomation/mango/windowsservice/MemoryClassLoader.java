/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.infiniteautomation.mango.windowsservice;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.util.Collections;
import java.util.Enumeration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
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
    private final AtomicLong size = new AtomicLong();

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
                    size.addAndGet(data.length);
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
        size.addAndGet(-data.length);

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

    public long getSize() {
        return size.get();
    }

    public static class ByteArrayURLStreamHandler extends URLStreamHandler {
        private final byte[] data;

        public ByteArrayURLStreamHandler(byte[] data) {
            this.data = data;
        }

        @Override
        protected URLConnection openConnection(URL u) throws IOException {
            return new ByteArrayURLConnection(u, this.data);
        }
    }

    public static class ByteArrayURLConnection extends URLConnection {
        private final byte[] data;

        protected ByteArrayURLConnection(URL url, byte[] data) {
            super(url);
            this.data = data;
        }

        @Override
        public void connect() throws IOException {
            this.connected = true;
        }

        @Override
        public InputStream getInputStream() throws IOException {
            return new ByteArrayInputStream(data);
        }
    }
}
