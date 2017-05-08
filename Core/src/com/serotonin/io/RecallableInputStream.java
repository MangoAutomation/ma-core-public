package com.serotonin.io;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import com.serotonin.util.queue.ByteQueue;

public class RecallableInputStream extends InputStream {
    private final InputStream in;
    private final int length;
    private final ByteQueue queue;

    public RecallableInputStream(InputStream in, int length) {
        if (length < 1)
            throw new IllegalArgumentException("length cannot be less than 1");

        this.in = in;
        this.length = length;
        queue = new ByteQueue(length);
    }

    @Override
    public int read() throws IOException {
        int i = in.read();
        if (queue.size() > length)
            queue.pop();
        queue.push(i);
        return i;
    }

    public byte[] recall() {
        return queue.peekAll();
    }

    public String recallToString() {
        return queue.toString();
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        int count = in.read(b, off, len);

        if (count <= 0)
            return 0;

        // We may have been given more than we care to save, so take the min of read count and queue length.
        int toSave = count > length ? length : count;

        // The queue may not be full yet, so only pop what we need to pop.
        int topop = toSave - (length - queue.size());
        if (topop > 0)
            queue.pop(topop);

        queue.push(b, off + (count - toSave), toSave);

        return count;
    }

    @Override
    public long skip(long n) throws IOException {
        return in.skip(n);
    }

    @Override
    public int available() throws IOException {
        return in.available();
    }

    @Override
    public void close() throws IOException {
        in.close();
    }

    @Override
    public synchronized void mark(int readlimit) {
        in.mark(readlimit);
    }

    @Override
    public synchronized void reset() throws IOException {
        in.reset();
    }

    @Override
    public boolean markSupported() {
        return in.markSupported();
    }

    public static void main(String[] args) throws Exception {
        byte[] b = new byte[100];
        for (int i = 0; i < b.length; i++)
            b[i] = (byte) i;
        ByteArrayInputStream bais = new ByteArrayInputStream(b);

        RecallableInputStream ris = new RecallableInputStream(bais, 15);
        ris.read();
        System.out.println(Arrays.toString(ris.recall()));

        ris.read(new byte[5]);
        System.out.println(Arrays.toString(ris.recall()));

        ris.read(new byte[12]);
        System.out.println(Arrays.toString(ris.recall()));

        ris.read(new byte[20]);
        System.out.println(Arrays.toString(ris.recall()));

        ris.read();
        System.out.println(Arrays.toString(ris.recall()));

        ris.read(new byte[10]);
        System.out.println(Arrays.toString(ris.recall()));

        ris.read(new byte[100]);
        System.out.println(Arrays.toString(ris.recall()));

        ris.read(new byte[3]);
        System.out.println(Arrays.toString(ris.recall()));
    }
}
