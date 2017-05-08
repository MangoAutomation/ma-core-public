/*
 * Created on 8-Mar-2006
 */
package com.serotonin.util.queue;

public class ShortQueue implements Cloneable {
    private short[] queue;
    private int head = -1;
    private int tail = 0;
    private int size = 0;

    public ShortQueue() {
        this(128);
    }

    public ShortQueue(int initialLength) {
        queue = new short[initialLength];
    }

    public ShortQueue(short[] s) {
        this(s.length);
        push(s, 0, s.length);
    }

    public ShortQueue(short[] s, int pos, int length) {
        this(length);
        push(s, pos, length);
    }

    public void push(short s) {
        if (room() == 0)
            expand();

        queue[tail] = s;

        if (head == -1)
            head = 0;
        tail = (tail + 1) % queue.length;
        size++;
    }

    public void push(short[] s) {
        push(s, 0, s.length);
    }

    public void push(short[] s, int pos, int length) {
        if (length == 0)
            return;

        while (room() < length)
            expand();

        int tailLength = queue.length - tail;
        if (tailLength > length)
            System.arraycopy(s, pos, queue, tail, length);
        else
            System.arraycopy(s, pos, queue, tail, tailLength);

        if (length > tailLength)
            System.arraycopy(s, tailLength + pos, queue, 0, length - tailLength);

        if (head == -1)
            head = 0;
        tail = (tail + length) % queue.length;
        size += length;
    }

    public void push(ShortQueue source) {
        if (source.size == 0)
            return;

        if (source == this)
            source = (ShortQueue) clone();

        int firstCopyLen = source.queue.length - source.head;
        if (source.size < firstCopyLen)
            firstCopyLen = source.size;
        push(source.queue, source.head, firstCopyLen);

        if (firstCopyLen < source.size)
            push(source.queue, 0, source.tail);
    }

    public short pop() {
        short retval = queue[head];

        if (size == 1) {
            head = -1;
            tail = 0;
        }
        else
            head = (head + 1) % queue.length;

        size--;

        return retval;
    }

    public int pop(short[] buf) {
        return pop(buf, 0, buf.length);
    }

    public int pop(short[] buf, int pos, int length) {
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

    public short[] popAll() {
        short[] data = new short[size];
        pop(data);
        return data;
    }

    public short tailPop() {
        if (size == 0)
            throw new ArrayIndexOutOfBoundsException(-1);

        tail = (tail + queue.length - 1) % queue.length;
        short retval = queue[tail];

        if (size == 1) {
            head = -1;
            tail = 0;
        }

        size--;

        return retval;
    }

    public short peek(int index) {
        if (index >= size)
            throw new IllegalArgumentException("index " + index + " is >= queue size " + size);

        index = (index + head) % queue.length;
        return queue[index];
    }

    public short[] peek(int index, int length) {
        short[] result = new short[length];
        // TODO: use System.arraycopy instead.
        for (int i = 0; i < length; i++)
            result[i] = peek(index + i);
        return result;
    }

    public int peek(short[] buf) {
        return peek(buf, 0, buf.length);
    }

    public int peek(short[] buf, int pos, int length) {
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

    public int indexOf(short s) {
        return indexOf(s, 0);
    }

    public int indexOf(short s, int start) {
        if (start >= size)
            return -1;

        int index = (head + start) % queue.length;
        for (int i = start; i < size; i++) {
            if (queue[index] == s)
                return i;
            index = (index + 1) % queue.length;
        }
        return -1;
    }

    public int indexOf(short[] s) {
        return indexOf(s, 0);
    }

    public int indexOf(short[] s, int start) {
        if (s == null || s.length == 0)
            throw new IllegalArgumentException("cannot search for empty values");

        while ((start = indexOf(s[0], start)) != -1 && start < size - s.length + 1) {
            boolean found = true;
            for (int i = 1; i < s.length; i++) {
                if (peek(start + i) != s[i]) {
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
        short[] news = new short[queue.length * 2];

        if (head == -1) {
            queue = news;
            return;
        }

        if (tail > head) {
            System.arraycopy(queue, head, news, head, tail - head);
            queue = news;
            return;
        }

        System.arraycopy(queue, head, news, head + queue.length, queue.length - head);
        System.arraycopy(queue, 0, news, 0, tail);
        head += queue.length;
        queue = news;
    }

    @Override
    public Object clone() {
        try {
            ShortQueue clone = (ShortQueue) super.clone();
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
