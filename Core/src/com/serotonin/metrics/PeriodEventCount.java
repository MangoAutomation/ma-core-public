package com.serotonin.metrics;

import com.serotonin.util.queue.LongQueue;

/**
 * This class provides a high-accuracy count of event occurrences over a given period of the most recent time. For
 * example, it can provide the number of events that have occurred over the past 5 minutes. An "event" is user-defined
 * (the definition of which is irrelevant to this class).
 * 
 * Care should be taken not to choose time periods that are too long, since event timestamps are individually stored.
 * This results in an array that is at least as long as the number of events that have occurred in the period, and most
 * likely longer. Depending upon your application, this could result in a great deal of memory usage.
 * 
 * @author Matthew
 */
public class PeriodEventCount {
    private final long period;
    private final LongQueue queue = new LongQueue();
    private final long hitPurgeInterval;
    private long nextHitPurge;

    public PeriodEventCount(long period) {
        this.period = period;
        hitPurgeInterval = period / 10;
        nextHitPurge = System.currentTimeMillis();
    }

    public void hit() {
        long now = System.currentTimeMillis();

        // Occasionally purge the queue in case getCount is not called very often.
        if (nextHitPurge <= now) {
            purge();
            nextHitPurge = now + hitPurgeInterval;
        }

        queue.push(now);

    }

    public int getCount() {
        purge();
        return queue.size();
    }

    private synchronized void purge() {
        // Drop expired timestamps off of the queue.
        long cutoff = System.currentTimeMillis() - period;
        while (queue.size() > 0 && queue.peek(0) < cutoff)
            queue.pop();
    }
}
