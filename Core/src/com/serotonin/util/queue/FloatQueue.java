/*
 * Created on 9-Mar-2006
 */
package com.serotonin.util.queue;

public class FloatQueue implements Cloneable {
    private float[] queue;
    private int head = -1;
    private int tail = 0;
    private int size = 0;

    public FloatQueue() {
        this(10);
    }

    public FloatQueue(int initialLength) {
        queue = new float[initialLength];
    }

    public FloatQueue(float[] values) {
        this(values.length);
        push(values, 0, values.length);
    }

    public FloatQueue(float[] values, int pos, int length) {
        this(length);
        push(values, pos, length);
    }

    public void push(float value) {
        if (room() == 0)
            expand();

        queue[tail] = value;

        if (head == -1)
            head = 0;
        tail = (tail + 1) % queue.length;
        size++;
    }

    public void push(float[] values) {
        push(values, 0, values.length);
    }

    public void push(float[] values, int pos, int length) {
        if (length == 0)
            return;

        while (room() < length)
            expand();

        int tailLength = queue.length - tail;
        if (tailLength > length)
            System.arraycopy(values, pos, queue, tail, length);
        else
            System.arraycopy(values, pos, queue, tail, tailLength);

        if (length > tailLength)
            System.arraycopy(values, tailLength + pos, queue, 0, length - tailLength);

        if (head == -1)
            head = 0;
        tail = (tail + length) % queue.length;
        size += length;
    }

    public void push(FloatQueue source) {
        if (source.size == 0)
            return;

        if (source == this)
            source = (FloatQueue) clone();

        int firstCopyLen = source.queue.length - source.head;
        if (source.size < firstCopyLen)
            firstCopyLen = source.size;
        push(source.queue, source.head, firstCopyLen);

        if (firstCopyLen < source.size)
            push(source.queue, 0, source.tail);
    }

    public float pop() {
        float retval = queue[head];

        if (size == 1) {
            head = -1;
            tail = 0;
        }
        else
            head = (head + 1) % queue.length;

        size--;

        return retval;
    }

    public int pop(float[] values) {
        return pop(values, 0, values.length);
    }

    public int pop(float[] values, int pos, int length) {
        length = peek(values, pos, length);

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

    public float[] popAll() {
        float[] data = new float[size];
        pop(data);
        return data;
    }

    public float tailPop() {
        if (size == 0)
            throw new ArrayIndexOutOfBoundsException(-1);

        tail = (tail + queue.length - 1) % queue.length;
        float retval = queue[tail];

        if (size == 1) {
            head = -1;
            tail = 0;
        }

        size--;

        return retval;
    }

    public float peek(int index) {
        if (index >= size)
            throw new IllegalArgumentException("index " + index + " is >= queue size " + size);

        index = (index + head) % queue.length;
        return queue[index];
    }

    public float[] peek(int index, int length) {
        float[] result = new float[length];
        // TODO: use System.arraycopy instead.
        for (int i = 0; i < length; i++)
            result[i] = peek(index + i);
        return result;
    }

    public int peek(float[] values) {
        return peek(values, 0, values.length);
    }

    public int peek(float[] values, int pos, int length) {
        if (length == 0)
            return 0;
        if (size == 0)
            throw new ArrayIndexOutOfBoundsException(-1);

        if (length > size)
            length = size;

        int firstCopyLen = queue.length - head;
        if (length < firstCopyLen)
            firstCopyLen = length;

        System.arraycopy(queue, head, values, pos, firstCopyLen);
        if (firstCopyLen < length)
            System.arraycopy(queue, 0, values, pos + firstCopyLen, length - firstCopyLen);

        return length;
    }

    public int indexOf(float value) {
        return indexOf(value, 0);
    }

    public int indexOf(float value, int start) {
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

    public int indexOf(float[] values) {
        return indexOf(values, 0);
    }

    public int indexOf(float[] values, int start) {
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

            if (found)
                return start;

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
        float[] newo = new float[queue.length * 2];

        if (head == -1) {
            queue = newo;
            return;
        }

        if (tail > head) {
            System.arraycopy(queue, head, newo, head, tail - head);
            queue = newo;
            return;
        }

        System.arraycopy(queue, head, newo, head + queue.length, queue.length - head);
        System.arraycopy(queue, 0, newo, 0, tail);
        head += queue.length;
        queue = newo;
    }

    @Override
    public Object clone() {
        try {
            FloatQueue clone = (FloatQueue) super.clone();
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
