/*
 * Copyright (C) 2020 Infinite Automation Software. All rights reserved.
 */
package com.infiniteautomation.mango.bootstrap.windows;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * @author Jared Wiltshire
 */
public class PrefixClassLoader extends ClassLoader {

    private final String prefix;

    public PrefixClassLoader(String prefix, ClassLoader parent) {
        super(parent);
        this.prefix = prefix + "/";
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        String path = new StringBuilder()
                .append(prefix)
                .append(name.replace('.', '/'))
                .append(".class").toString();

        try (InputStream in = getResourceAsStream(path)) {
            if (in == null) {
                throw new ClassNotFoundException(name);
            }
            byte[] bytes = toByteArray(in);
            return defineClass(name, bytes, 0, bytes.length);
        } catch (IOException e) {
            throw new ClassNotFoundException(name, e);
        }
    }

    public static byte[] toByteArray(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream(Math.max(32, in.available()));
        copy(in, out);
        return out.toByteArray();
    }

    public static long copy(InputStream from, OutputStream to) throws IOException {
        byte[] buf = new byte[8192];
        long total = 0;
        while (true) {
            int r = from.read(buf);
            if (r == -1) {
                break;
            }
            to.write(buf, 0, r);
            total += r;
        }
        return total;
    }

}
