/*
 * Copyright (C) 2020 Infinite Automation Software. All rights reserved.
 */
package com.infiniteautomation.mango.bootstrap.classloader;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

/**
 * @author Jared Wiltshire
 */
public class ByteArrayURLStreamHandler extends URLStreamHandler {

    private final byte[] data;

    public ByteArrayURLStreamHandler(byte[] data) {
        this.data = data;
    }

    @Override
    protected URLConnection openConnection(URL u) throws IOException {
        return new ByteArrayURLConnection(u);
    }

    private class ByteArrayURLConnection extends URLConnection {
        protected ByteArrayURLConnection(URL url) {
            super(url);
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