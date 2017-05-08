package com.serotonin.metrics;

import java.util.concurrent.atomic.AtomicLong;

import com.serotonin.util.queue.ObjectQueue;

/**
 * This class provides a high-accuracy sum of some number over a given period of the most recent time. For example, it
 * can provide the number of CPU cycles that a thread has consumed over some period of time by providing the delta at
 * least one during the period.
 * 
 * Care should be taken not to choose time periods that are too long, since event timestamps are individually stored.
 * This results in an array that is at least as long as the number of events that have occurred in the period, and most
 * likely longer. Depending upon your application, this could result in a great deal of memory usage.
 * 
 * @author Matthew
 */
public class PeriodSum {
    private final long period;
    private final ObjectQueue<TimeAmount> queue = new ObjectQueue<TimeAmount>();
    private final long hitPurgeInterval;
    private long nextHitPurge;
    private final AtomicLong sum = new AtomicLong();

    public PeriodSum(long period) {
        this.period = period;
        hitPurgeInterval = period / 10;
        nextHitPurge = System.currentTimeMillis();
    }

    public void hit(long delta) {
        long now = System.currentTimeMillis();

        // Occasionally purge the queue in case getSum is not called very often.
        if (nextHitPurge <= now) {
            purge();
            nextHitPurge = now + hitPurgeInterval;
        }

        queue.push(new TimeAmount(now, delta));
        sum.addAndGet(delta);
    }

    public long getSum() {
        purge();
        return sum.get();
    }

    private synchronized void purge() {
        // Drop expired timestamps off of the queue.
        long cutoff = System.currentTimeMillis() - period;
        while (queue.size() > 0 && queue.peek(0).time < cutoff)
            sum.addAndGet(-queue.pop().amount);
    }

    class TimeAmount {
        final long time;
        final long amount;

        public TimeAmount(long time, long amount) {
            this.time = time;
            this.amount = amount;
        }
    }
}
