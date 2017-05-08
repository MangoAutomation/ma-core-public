/*
 * Created on 8-Mar-2006
 */
package com.serotonin.util.queue;

public class LongQueue implements Cloneable {
    private long[] queue;
    private int head = -1;
    private int tail = 0;
    private int size = 0;

    public LongQueue() {
        this(128);
    }

    public LongQueue(int initialLength) {
        queue = new long[initialLength];
    }

    public LongQueue(long[] i) {
        this(i.length);
        push(i, 0, i.length);
    }

    public LongQueue(long[] i, int pos, int length) {
        this(length);
        push(i, pos, length);
    }

    public void push(long i) {
        if (room() == 0)
            expand();

        queue[tail] = i;

        if (head == -1)
            head = 0;
        tail = (tail + 1) % queue.length;
        size++;
    }

    public void push(long[] i) {
        push(i, 0, i.length);
    }

    public void push(long[] i, int pos, int length) {
        if (length == 0)
            return;

        while (room() < length)
            expand();

        int tailLength = queue.length - tail;
        if (tailLength > length)
            System.arraycopy(i, pos, queue, tail, length);
        else
            System.arraycopy(i, pos, queue, tail, tailLength);

        if (length > tailLength)
            System.arraycopy(i, tailLength + pos, queue, 0, length - tailLength);

        if (head == -1)
            head = 0;
        tail = (tail + length) % queue.length;
        size += length;
    }

    public void push(LongQueue source) {
        if (source.size == 0)
            return;

        if (source == this)
            source = (LongQueue) clone();

        int firstCopyLen = source.queue.length - source.head;
        if (source.size < firstCopyLen)
            firstCopyLen = source.size;
        push(source.queue, source.head, firstCopyLen);

        if (firstCopyLen < source.size)
            push(source.queue, 0, source.tail);
    }

    public long pop() {
        long retval = queue[head];

        if (size == 1) {
            head = -1;
            tail = 0;
        }
        else
            head = (head + 1) % queue.length;

        size--;

        return retval;
    }

    public int pop(long[] buf) {
        return pop(buf, 0, buf.length);
    }

    public int pop(long[] buf, int pos, int length) {
        length = peek(buf, pos, length);

        size -= length;

        if (size == 0) {
            head = -1;
            tail = 0;
        }
        else
            head = (head + length) % queue.length;

        return length;
    }

    public int pop(int length) {
        if (length == 0)
            return 0;
        if (size == 0)
            throw new ArrayIndexOutOfBoundsException(-1);

        if (length > size)
            length = size;

        size -= length;

        if (size == 0) {
            head = -1;
            tail = 0;
        }
        else
            head = (head + length) % queue.length;

        return length;
    }

    public long[] popAll() {
        long[] data = new long[size];
        pop(data);
        return data;
    }

    public long tailPop() {
        if (size == 0)
            throw new ArrayIndexOutOfBoundsException(-1);

        tail = (tail + queue.length - 1) % queue.length;
        long retval = queue[tail];

        if (size == 1) {
            head = -1;
            tail = 0;
        }

        size--;

        return retval;
    }

    public long peek(int index) {
        if (index >= size)
            throw new IllegalArgumentException("index " + index + " is >= queue size " + size);

        index = (index + head) % queue.length;
        return queue[index];
    }

    public long[] peek(int index, int length) {
        long[] result = new long[length];
        // TODO: use System.arraycopy instead.
        for (int i = 0; i < length; i++)
            result[i] = peek(index + i);
        return result;
    }

    public int peek(long[] buf) {
        return peek(buf, 0, buf.length);
    }

    public int peek(long[] buf, int pos, int length) {
        if (length == 0)
            return 0;
        if (size == 0)
            throw new ArrayIndexOutOfBoundsException(-1);

        if (length > size)
            length = size;

        int firstCopyLen = queue.length - head;
        if (length < firstCopyLen)
            firstCopyLen = length;

        System.arraycopy(queue, head, buf, pos, firstCopyLen);
        if (firstCopyLen < length)
            System.arraycopy(queue, 0, buf, pos + firstCopyLen, length - firstCopyLen);

        return length;
    }

    public int indexOf(long i) {
        return indexOf(i, 0);
    }

    public int indexOf(long value, int start) {
        if (start >= size)
            return -1;

        int index = (head + start) % queue.length;
        for (int i = start; i < size; i++) {
            if (queue[index] == value)
                return i;
            index = (index + 1) % queue.length;
        }
        return -1;
    }

    public int indexOf(long[] values) {
        return indexOf(values, 0);
    }

    public int indexOf(long[] values, int start) {
        if (values == null || values.length == 0)
            throw new IllegalArgumentException("cannot search for empty values");

        while ((start = indexOf(values[0], start)) != -1 && start < size - values.length + 1) {
            boolean found = true;
            for (int i = 1; i < values.length; i++) {
                if (peek(start + i) != values[i]) {
                    found = false;
                    break;
                }
            }

            if (found) {
                return start;
            }

            start++;
        }

        return -1;
    }

    public int size() {
        return size;
    }

    public void clear() {
        size = 0;
        head = -1;
        tail = 0;
    }

    private int room() {
        return queue.length - size;
    }

    private void expand() {
        long[] newq = new long[queue.length * 2];

        if (head == -1) {
            queue = newq;
            return;
        }

        if (tail > head) {
            System.arraycopy(queue, head, newq, head, tail - head);
            queue = newq;
            return;
        }

        System.arraycopy(queue, head, newq, head + queue.length, queue.length - head);
        System.arraycopy(queue, 0, newq, 0, tail);
        head += queue.length;
        queue = newq;
    }

    @Override
    public Object clone() {
        try {
            LongQueue clone = (LongQueue) super.clone();
            // Array is mutable, so make a copy of it too.
            clone.queue = queue.clone();
            return clone;
        }
        catch (CloneNotSupportedException e) { /* Will never happen because we're Cloneable */
        }
        return null;
    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();

        if (queue.length == 0)
            sb.append("[]");
        else {
            sb.append('[');
            sb.append(queue[0]);
            for (int i = 1; i < queue.length; i++) {
                sb.append(", ");
                sb.append(queue[i]);
            }
            sb.append("]");
        }

        sb.append(", h=").append(head).append(", t=").append(tail).append(", s=").append(size);
        return sb.toString();
    }
}
