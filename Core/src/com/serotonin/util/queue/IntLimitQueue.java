package com.serotonin.util.queue;

/**
 * Limit queue for ints. A limit queue only holds a maximum number of values, discarding those at the end of the queue
 * when new values are added.
 * 
 * @author Matthew
 */
public class IntLimitQueue {
    private final int[] queue;
    private int head = -1;
    private int tail = 0;
    private int size = 0;

    public IntLimitQueue(int limit) {
        queue = new int[limit];
    }

    public void push(int i) {
        queue[tail] = i;

        tail = (tail + 1) % queue.length;

        if (head == -1) {
            head = 0;
            size = 1;
        }
        else if (size == queue.length)
            head = tail;
        else
            size++;
    }

    public int pop() {
        int retval = queue[head];

        if (size == 1) {
            head = -1;
            tail = 0;
        }
        else
            head = (head + 1) % queue.length;

        size--;

        return retval;
    }

    public int peek(int index) {
        if (index >= size)
            throw new IllegalArgumentException("index " + index + " is >= queue size " + size);

        index = (index + head) % queue.length;
        return queue[index];
    }

    public int size() {
        return size;
    }

    public void clear() {
        head = -1;
        tail = 0;
        size = 0;
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
