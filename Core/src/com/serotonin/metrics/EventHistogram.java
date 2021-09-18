package com.serotonin.metrics;

import java.util.concurrent.atomic.AtomicLong;

import com.serotonin.m2m2.Common;

/**
 * This class counts temporal occurrences of some user-defined event (the definition of which is irrelevant to this
 * class), and maintains the counts in an array representing a histogram of the events. The expected use case for this
 * class is where there are a great many "hits" (i.e. recordings of events, or writes), and relatively fews reads. The
 * hit method is optimized for this (although the read method is pretty fast too).
 *
 * This class is thread-safe, but not guaranteed to produce 100% accurate results in a heavily multi-threaded or
 * very busy environment. It is conceivable, for example, that a hit could be attributed to the next period if a
 * separate thread changes the current period before the first has a change to increment the period counter. Also,
 * since counts are stored in int arrays, and ints are not guaranteed to be thread-safe, discrepancies may occur in
 * the counts themselves as well. Overall though, this class will likely provide very good accuracy in a
 * high-performance, low footprint manner.
 *
 * @author Matthew
 */
public class EventHistogram {
    private final int bucketSize;
    private final int[] buckets;
    private int position;
    private final AtomicLong lastTime = new AtomicLong();

    /**
     * @param bucketSize
     *            The size of the time period in which events are counted. 1000 is 1 second.
     * @param buckets
     *            The number of periods that are stored. After the period of buckets * bucketSize the events are rolled
     *            off the end of the queue.
     */
    public EventHistogram(int bucketSize, int buckets) {
        this.bucketSize = bucketSize;
        this.buckets = new int[buckets];
        position = 0;
        lastTime.set(Common.timer.currentTimeMillis() / bucketSize);
    }

    public void hit() {
        update();
        buckets[position]++;
    }

    public void hitMultiple(int count){
        update();
        buckets[position] = buckets[position] + count;
    }

    /**
     * Returns a snapshot of the event count array. The value at 0 is the oldest count. The value at length-1 is the
     * current count, which typically will be understated because the period will probably be incomplete.
     *
     * @return
     */
    public int[] getEventCounts() {
        update();

        int[] result = new int[buckets.length];
        int pos = (position % buckets.length) + 1;

        System.arraycopy(buckets, pos, result, 0, buckets.length - pos);
        System.arraycopy(buckets, 0, result, buckets.length - pos, pos);

        return result;
    }

    private void update() {
        long thisTime = Common.timer.currentTimeMillis() / bucketSize;
        int diff = (int) (thisTime - lastTime.getAndSet(thisTime));

        while (diff > 0) {
            position = (position + 1) % buckets.length;
            buckets[position] = 0;
            diff--;
        }
    }

}
