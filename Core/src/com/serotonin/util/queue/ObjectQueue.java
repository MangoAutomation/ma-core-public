/*
 * Created on 8-Mar-2006
 */
package com.serotonin.util.queue;

import java.util.Iterator;

public class ObjectQueue<T> implements Cloneable, Iterable<T> {
    private T[] queue;
    private int head = -1;
    private int tail = 0;
    int size = 0;

    public ObjectQueue() {
        this(10);
    }

    @SuppressWarnings("unchecked")
    public ObjectQueue(int initialLength) {
        queue = (T[]) new Object[initialLength];
    }

    public ObjectQueue(T[] o) {
        this(o.length);
        push(o, 0, o.length);
    }

    public ObjectQueue(T[] o, int pos, int length) {
        this(length);
        push(o, pos, length);
    }

    public void push(T o) {
        if (room() == 0)
            expand();

        queue[tail] = o;

        if (head == -1)
            head = 0;
        tail = (tail + 1) % queue.length;
        size++;
    }

    public void push(T[] o) {
        push(o, 0, o.length);
    }

    public void push(T[] o, int pos, int length) {
        if (length == 0)
            return;

        while (room() < length)
            expand();

        int tailLength = queue.length - tail;
        if (tailLength > length)
            System.arraycopy(o, pos, queue, tail, length);
        else
            System.arraycopy(o, pos, queue, tail, tailLength);

        if (length > tailLength)
            System.arraycopy(o, tailLength + pos, queue, 0, length - tailLength);

        if (head == -1)
            head = 0;
        tail = (tail + length) % queue.length;
        size += length;
    }

    public void push(ObjectQueue<T> source) {
        if (source.size == 0)
            return;

        if (source == this)
            source = clone();

        int firstCopyLen = source.queue.length - source.head;
        if (source.size < firstCopyLen)
            firstCopyLen = source.size;
        push(source.queue, source.head, firstCopyLen);

        if (firstCopyLen < source.size)
            push(source.queue, 0, source.tail);
    }

    public T pop() {
        T retval = queue[head];

        if (size == 1) {
            head = -1;
            tail = 0;
        }
        else
            head = (head + 1) % queue.length;

        size--;

        return retval;
    }

    public int pop(T[] buf) {
        return pop(buf, 0, buf.length);
    }

    public int pop(T[] buf, int pos, int length) {
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

    @SuppressWarnings("unchecked")
    public T[] popAll() {
        T[] data = (T[]) new Object[size];
        pop(data);
        return data;
    }

    public T tailPop() {
        if (size == 0)
            throw new ArrayIndexOutOfBoundsException(-1);

        tail = (tail + queue.length - 1) % queue.length;
        T retval = queue[tail];

        if (size == 1) {
            head = -1;
            tail = 0;
        }

        size--;

        return retval;
    }

    public T peek(int index) {
        if (index >= size)
            throw new IllegalArgumentException("index " + index + " is >= queue size " + size);

        index = (index + head) % queue.length;
        return queue[index];
    }

    @SuppressWarnings("unchecked")
    public T[] peek(int index, int length) {
        T[] result = (T[]) new Object[length];
        // TODO: use System.arraycopy instead.
        for (int i = 0; i < length; i++)
            result[i] = peek(index + i);
        return result;
    }

    public int peek(T[] buf) {
        return peek(buf, 0, buf.length);
    }

    public int peek(T[] buf, int pos, int length) {
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

    public int indexOf(T o) {
        return indexOf(o, 0);
    }

    public int indexOf(T o, int start) {
        if (start >= size)
            return -1;

        int index = (head + start) % queue.length;
        for (int i = start; i < size; i++) {
            if (queue[index] == o)
                return i;
            index = (index + 1) % queue.length;
        }
        return -1;
    }

    public int indexOf(T[] o) {
        return indexOf(o, 0);
    }

    public int indexOf(T[] o, int start) {
        if (o == null || o.length == 0)
            throw new IllegalArgumentException("cannot search for empty values");

        while ((start = indexOf(o[0], start)) != -1 && start < size - o.length + 1) {
            boolean found = true;
            for (int i = 1; i < o.length; i++) {
                if (peek(start + i) != o[i]) {
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

    @SuppressWarnings("unchecked")
    private void expand() {
        T[] newo = (T[]) new Object[queue.length * 2];

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
    @SuppressWarnings("unchecked")
    public ObjectQueue<T> clone() {
        try {
            ObjectQueue<T> clone = (ObjectQueue<T>) super.clone();
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

    public Iterator<T> iterator() {
        return new QueueIterator();
    }

    class QueueIterator implements Iterator<T> {
        private int currentPosition = 0;

        public boolean hasNext() {
            return currentPosition < size;
        }

        public T next() {
            return peek(currentPosition++);
        }

        public void remove() {
            throw new RuntimeException("not implemented");
        }
    }
}
