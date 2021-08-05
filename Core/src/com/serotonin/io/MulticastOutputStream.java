package com.serotonin.io;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MulticastOutputStream extends OutputStream {
    static final Logger LOG = LoggerFactory.getLogger(MulticastOutputStream.class);

    private final List<OutputStream> streams = new CopyOnWriteArrayList<>();
    private IOExceptionHandler exceptionHandler = new IOExceptionHandler() {
        @Override
        public void ioException(OutputStream stream, IOException e) {
            LOG.error("Error in stream " + stream, e);
        }
    };

    public MulticastOutputStream() {
        // no op
    }

    public MulticastOutputStream(OutputStream out) {
        addStream(out);
    }

    public void setExceptionHandler(IOExceptionHandler exceptionHandler) {
        this.exceptionHandler = exceptionHandler;
    }

    public void addStream(OutputStream out) {
        streams.add(out);
    }

    public void removeStream(OutputStream out) {
        streams.remove(out);
    }

    @Override
    public void write(int b) throws IOException {
        for (OutputStream stream : streams) {
            try {
                stream.write(b);
            }
            catch (IOException e) {
                exceptionHandler.ioException(stream, e);
            }
        }
    }

    @Override
    public void flush() throws IOException {
        for (OutputStream stream : streams) {
            try {
                stream.flush();
            }
            catch (IOException e) {
                exceptionHandler.ioException(stream, e);
            }
        }
    }

    @Override
    public void close() throws IOException {
        for (OutputStream stream : streams) {
            try {
                stream.close();
            }
            catch (IOException e) {
                exceptionHandler.ioException(stream, e);
            }
        }
    }

    public static interface IOExceptionHandler {
        void ioException(OutputStream stream, IOException e);
    }
}