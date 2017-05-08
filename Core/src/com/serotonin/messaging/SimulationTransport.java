package com.serotonin.messaging;

import java.io.IOException;

/**
 * A transport implementation for recreating the handling of data from a trace.
 */
public class SimulationTransport implements Transport {
    private DataConsumer consumer;

    public void setConsumer(DataConsumer consumer) throws IOException {
        this.consumer = consumer;
    }

    public void removeConsumer() {
        this.consumer = null;
    }

    public void input(byte[] b) {
        consumer.data(b, b.length);
    }

    public void write(byte[] data) throws IOException {
        // no op
    }

    public void write(byte[] data, int len) throws IOException {
        // no op
    }
}
