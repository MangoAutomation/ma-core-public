/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.epoll;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Poll input streams for input in a single Thread
 *
 */
public class InputStreamEPoll implements Runnable {
    static final Logger LOG = LoggerFactory.getLogger(InputStreamEPoll.class);

    private final List<InputStreamWrapper> wrappers = new CopyOnWriteArrayList<InputStreamWrapper>();
    private volatile boolean terminated;

    // Reusable buffer
    private final byte[] byteBuffer = new byte[1028];
    private int readcount;

    public void add(InputStream inputStream, InputStreamCallback callback) {
        if (callback == null)
            callback = new NullCallback();

        synchronized (this) {
            wrappers.add(new InputStreamWrapper(inputStream, callback));
            // Ensure that the thread is notified since it may be waiting.
            notify();
        }
    }

    public void terminate() {
        terminated = true;
        synchronized (this) {
            notify();
        }
    }

    public InputStreamCallback getCallback(InputStream inputStream) {
        synchronized (this) {
            for (InputStreamWrapper wrapper : wrappers) {
                if (wrapper.inputStream == inputStream)
                    return wrapper.callback;
            }
        }
        return null;
    }

    public boolean remove(InputStream inputStream) {
        synchronized (this) {
            for (int i = 0; i < wrappers.size(); i++) {
                InputStreamWrapper wrapper = wrappers.get(i);
                if (wrapper.inputStream == inputStream) {
                    wrappers.remove(i);
                    return true;
                }
            }
        }
        return false;
    }

    public int getInputStreamCount() {
        return wrappers.size();
    }

    @Override
    public void run() {
        while (!terminated) {
            if (wrappers.isEmpty()) {
                // If there is nothing to do, wait until there is.
                synchronized (this) {
                    try {
                        // Just check again to be sure.
                        if (wrappers.isEmpty()) {
                            wait();

                            if (terminated)
                                break;
                        }
                    }
                    catch (InterruptedException e) {
                        // no op
                    }
                }
            }

            boolean activity = false;
            for (InputStreamWrapper wrapper : wrappers) {
                // Check the input streams
                try {
                    try {
                        if (wrapper.inputStream.available() > 0) {
                            readcount = wrapper.inputStream.read(byteBuffer);
                            wrapper.callback.input(byteBuffer, readcount);
                            activity = true;
                        }
                    }
                    catch (IOException e) {
                        activity = true;
                        if (StringUtils.equals(e.getMessage(), "Stream closed.")) {
                            wrappers.remove(wrapper);
                            wrapper.callback.closed();
                        }
                        else
                            wrapper.callback.ioException(e);
                    }
                }
                catch (Exception e) {
                    // Perhaps a problem in the callback.
                    LOG.error("Input stream callback exception", e);
                }
            }

            if (!activity) {
                // If there was no activity, let's just wait for a bit.
                synchronized (this) {
                    try {
                        wait(20);
                    }
                    catch (InterruptedException e) {
                        // Ignore
                    }
                }
            }
        }

        // Notify any remaining input streams of termination.
        for (InputStreamWrapper wrapper : wrappers) {
            try {
                wrapper.callback.terminated();
            }
            catch (Exception e) {
                LOG.error("Input stream callback exception", e);
            }
        }
    }

    private class InputStreamWrapper {
        final InputStream inputStream;
        final InputStreamCallback callback;

        public InputStreamWrapper(InputStream inputStream, InputStreamCallback callback) {
            this.inputStream = inputStream;
            this.callback = callback;
        }
    }

    static class NullCallback implements InputStreamCallback {
        public void input(byte[] buf, int len) {
            if (LOG.isDebugEnabled())
                LOG.debug("NullCallback input: " + toHexString(buf, 0, len));
        }

        public void closed() {
            if (LOG.isDebugEnabled())
                LOG.debug("NullCallback closed");
        }

        public void ioException(IOException e) {
            if (LOG.isDebugEnabled())
                LOG.debug("NullCallback exception", e);
        }

        public void terminated() {
            if (LOG.isDebugEnabled())
                LOG.debug("NullCallback terminated");
        }
    }

    // Some utilities
    public static String toHexString(byte[] bytes, int start, int len) {
        if (len == 0)
            return "[]";

        StringBuffer sb = new StringBuffer();
        sb.append('[');
        sb.append(Integer.toHexString(bytes[start] & 0xff));
        for (int i = 1; i < len; i++)
            sb.append(',').append(Integer.toHexString(bytes[start + i] & 0xff));
        sb.append("]");

        return sb.toString();
    }
}
