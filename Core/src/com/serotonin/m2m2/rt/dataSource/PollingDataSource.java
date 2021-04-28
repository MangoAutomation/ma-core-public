/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */

package com.serotonin.m2m2.rt.dataSource;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.infiniteautomation.mango.monitor.ValueMonitor;
import com.serotonin.db.pair.LongLongPair;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.rt.dataImage.DataPointRT;
import com.serotonin.m2m2.util.timeout.TimeoutClient;
import com.serotonin.m2m2.util.timeout.TimeoutTask;
import com.serotonin.m2m2.vo.dataSource.PollingDataSourceVO;
import com.serotonin.timer.CronTimerTrigger;
import com.serotonin.timer.FixedRateTrigger;
import com.serotonin.timer.RejectedTaskReason;
import com.serotonin.timer.TimerTask;
import com.serotonin.util.ILifecycleState;

abstract public class PollingDataSource<T extends PollingDataSourceVO> extends DataSourceRT<T> {

    private final Log LOG = LogFactory.getLog(PollingDataSource.class);
    private static final String prefix = "POLLINGDS-";
    private final Lock pollLock = new ReentrantLock();

    private enum PendingPointOperation {
        ADD, REMOVE
    }

    private static class PendingPoint {
        private final DataPointRT point;
        private final PendingPointOperation operation;

        private PendingPoint(DataPointRT point, PendingPointOperation operation) {
            this.point = point;
            this.operation = operation;
        }
    }

    /**
     * Stores pending data points which are waiting to be added or removed from {@link #dataPointsMap}
     * (usually on the next poll)
     */
    private final Queue<PendingPoint> pendingPoints = new ConcurrentLinkedQueue<>();


    // If polling is done with millis
    protected long pollingPeriodMillis = 300000; // Default to 5 minutes just to
    // have something here
    protected boolean quantize;

    // If polling is done with cron
    protected String cronPattern;

    private final TimeoutClient timeoutClient;
    private TimerTask timerTask;

    private final AtomicBoolean lastPollSuccessful = new AtomicBoolean();
    private final AtomicLong successfulPolls = new AtomicLong();
    private final AtomicLong unsuccessfulPolls = new AtomicLong();
    private final AtomicLong currentSuccessfulPolls = new AtomicLong();
    private final ValueMonitor<Long> currentSuccessfulPollsMonitor;
    private final ValueMonitor<Long> lastPollDurationMonitor;
    private final ValueMonitor<Double> successfulPollsPercentageMonitor;
    private final ConcurrentLinkedQueue<LongLongPair> latestPollTimes;
    private final ConcurrentLinkedQueue<Long> latestAbortedPollTimes;
    private long nextAbortedPollMessageTime = 0L;
    private final long abortedPollLogDelay;

    public PollingDataSource(T vo) {
        super(vo);
        if(vo.isUseCron())
            this.cronPattern = vo.getCronPattern();
        else
            pollingPeriodMillis = Common.getMillis(vo.getUpdatePeriodType(), vo.getUpdatePeriods());

        this.quantize = vo.isQuantize();

        this.latestPollTimes = new ConcurrentLinkedQueue<>();
        this.latestAbortedPollTimes = new ConcurrentLinkedQueue<>();
        this.abortedPollLogDelay = Common.envProps.getLong("runtime.datasource.pollAbortedLogFrequency", 3600000);
        this.timeoutClient = new TimeoutClient(){

            @Override
            public void scheduleTimeout(long fireTime) {
                scheduleTimeoutImpl(fireTime);
            }

            @Override
            public String getTaskId() {
                return prefix + vo.getXid();
            }

            @Override
            public String getThreadName() {
                return "Polling Data Source: " + vo.getXid();
            }

            @Override
            public void rejected(RejectedTaskReason reason) {
                incrementUnsuccessfulPolls(reason.getScheduledExecutionTime());
                updateSuccessfulPollQuotient();
                Common.backgroundProcessing.rejectedHighPriorityTask(reason);
            }

        };

        //Set it to -1 so that if it never aborts we can distinguish from always aborting
        this.currentSuccessfulPollsMonitor = Common.MONITORED_VALUES.<Long>create("com.serotonin.m2m2.rt.dataSource.PollingDataSource_" + vo.getXid() + "_SUCCESS")
                .name(new TranslatableMessage("internal.monitor.pollingDataSource.SUCCESS", vo.getName()))
                .value(-1L)
                .build();

        this.lastPollDurationMonitor = Common.MONITORED_VALUES.<Long>create("com.serotonin.m2m2.rt.dataSource.PollingDataSource_" + vo.getXid() + "_DURATION")
                .name(new TranslatableMessage("internal.monitor.pollingDataSource.DURATION", vo.getName()))
                .value(0L)
                .build();

        this.successfulPollsPercentageMonitor = Common.MONITORED_VALUES.<Double>create("com.serotonin.m2m2.rt.dataSource.PollingDataSource_" + vo.getXid() + "_PERCENTAGE")
                .name(new TranslatableMessage("internal.monitor.pollingDataSource.PERCENTAGE", vo.getName()))
                .value(0D)
                .build();
    }

    public long getSuccessfulPolls() {
        return successfulPolls.get();
    }

    protected void incrementSuccessfulPolls() {
        successfulPolls.incrementAndGet();
        currentSuccessfulPolls.incrementAndGet();
        this.lastPollSuccessful.getAndSet(true);
    }

    public long getUnsuccessfulPolls() {
        return unsuccessfulPolls.get();
    }

    /**
     * Increment the unsuccessful polls and fire event if necessary
     * @param time time at which the poll was supposed to occur
     */
    protected void incrementUnsuccessfulPolls(long time) {
        long consecutiveSuccesses = currentSuccessfulPolls.getAndSet(0);
        currentSuccessfulPollsMonitor.setValue(consecutiveSuccesses);

        long unsuccessful = unsuccessfulPolls.incrementAndGet();

        lastPollSuccessful.set(false);
        latestAbortedPollTimes.add(time);
        //Trim the Queue
        while(latestAbortedPollTimes.size() > 10)
            latestAbortedPollTimes.poll();

        //Log A Message Every 5 Minutes
        if(LOG.isWarnEnabled() && (nextAbortedPollMessageTime <= time)){
            nextAbortedPollMessageTime = time + abortedPollLogDelay;
            LOG.warn("Data Source " + vo.getName() + " aborted " + unsuccessful + " polls since it started.");
        }

        //Raise No RTN Event On First aborted poll
        int eventId = vo.getPollAbortedExceptionEventId();
        if((eventId >= 0) && (unsuccessful == 1))
            this.raiseEvent(eventId, time, false, new TranslatableMessage("event.pollAborted", vo.getXid(), vo.getName()));
    }

    protected void updateSuccessfulPollQuotient() {
        long unsuccessful = unsuccessfulPolls.get();
        long successful = successfulPolls.get();
        successfulPollsPercentageMonitor.setValue(((double)successful/(double)(successful + unsuccessful))*100);
    }

    protected final void scheduleTimeoutImpl(long fireTime) {
        pollLock.lock();
        try {
            // terminating is unlikely as the task task is cancelled, but can occur
            ensureState(ILifecycleState.RUNNING, ILifecycleState.TERMINATING);

            try {
                long startTs = Common.timer.currentTimeMillis();

                // Check to see if this poll is running after it's next poll time, i.e. polls are
                // backing up
                if ((cronPattern == null) && ((startTs - fireTime) > pollingPeriodMillis)) {
                    incrementUnsuccessfulPolls(fireTime);
                    return;
                }

                incrementSuccessfulPolls();

                flushPoints(fireTime);
                doPollNoSync(fireTime);

                // Save the poll time and duration
                long pollDuration = Common.timer.currentTimeMillis() - startTs;
                this.latestPollTimes.add(new LongLongPair(fireTime, pollDuration));
                this.lastPollDurationMonitor.setValue(pollDuration);
                // Trim the Queue
                while (this.latestPollTimes.size() > 10) {
                    this.latestPollTimes.poll();
                }
            } finally {
                updateSuccessfulPollQuotient();
            }
        } finally {
            pollLock.unlock();
        }
    }

    @Override
    public void addStatusMessages(List<TranslatableMessage> messages) {
        super.addStatusMessages(messages);
        long sum = unsuccessfulPolls.longValue() + successfulPolls.longValue();
        messages.add(new TranslatableMessage("dsEdit.discardedPolls", unsuccessfulPolls, sum, (int) (unsuccessfulPolls
                .doubleValue() / sum * 100)));
    }

    @Override
    public void forcePoll() {
        //After discussion this is left as is but does not guarantee any ordering of multiple calls.
        // Potentially add a poll to the front of this tasks queue if a poll is already running or execute now.  This would
        // allow rejection of multiple force polls in the scenario that they are called too fast.
        scheduleTimeoutImpl(Common.timer.currentTimeMillis());
    }

    /**
     * Override this method if you do not want the poll to synchronize on
     * pointListChangeLock
     *
     * @param time timestamp at which the poll will be performed
     */
    protected void doPollNoSync(long time) {
        pointListChangeLock.readLock().lock();
        try{
            doPoll(time);
        } finally {
            pointListChangeLock.readLock().unlock();
        }
    }

    abstract protected void doPoll(long time);

    //
    //
    // Data source interface
    //
    @Override
    public synchronized void beginPolling() {
        ensureState(ILifecycleState.RUNNING);
        if (timerTask != null) {
            throw new IllegalStateException("Polling was already started");
        }

        if (cronPattern == null) {
            long delay = 0;
            if (quantize){
                // Quantize the start.
                long now = Common.timer.currentTimeMillis();
                delay = pollingPeriodMillis - (now % pollingPeriodMillis);
                long firstPollTime = now + delay;
                if(LOG.isDebugEnabled())
                    LOG.debug("First poll should be at: " + firstPollTime);
                timerTask = new TimeoutTask(new FixedRateTrigger(new Date(firstPollTime), pollingPeriodMillis), this.timeoutClient);
            } else
                timerTask = new TimeoutTask(new FixedRateTrigger(delay, pollingPeriodMillis), this.timeoutClient);
        }
        else {
            try {
                timerTask = new TimeoutTask(new CronTimerTrigger(cronPattern), this.timeoutClient);
            }
            catch (ParseException e) {
                // Should not happen
                throw new RuntimeException(e);
            }
        }

        super.beginPolling();
    }

    @Override
    public boolean inhibitIntervalLoggingInitialization() {
        return true;
    }

    @Override
    public void terminating() {
        if (timerTask != null)
            timerTask.cancel();
    }

    @Override
    protected final void terminateImpl() {
        Common.MONITORED_VALUES.remove(currentSuccessfulPollsMonitor.getId());
        Common.MONITORED_VALUES.remove(lastPollDurationMonitor.getId());
        Common.MONITORED_VALUES.remove(successfulPollsPercentageMonitor.getId());
        pollingTerminate();
    }

    public void pollingTerminate() {
    }

    /**
     * Waits for the current poll (if any) to complete.
     */
    @Override
    public void joinTerminationImpl() throws InterruptedException {
        int tries = 0;
        while (++tries <= 10) {
            if (pollLock.tryLock(30, TimeUnit.SECONDS)) {
                pollLock.unlock();
                return;
            }
            LOG.warn("Waiting for data source to stop: id=" + getId() + ", type=" + getClass());
        }

        throw new IllegalStateException(String.format("Timeout waiting for data source to stop: id=%d, type=%s",
                getId(), getClass()));
    }

    /**
     * Get the latest poll times and durations.
     * @return list of poll times and durations
     */
    public List<LongLongPair> getLatestPollTimes(){
        return new ArrayList<>(this.latestPollTimes);
    }

    /**
     * Get the latest times for Aborted polls.
     * @return list of timestamps for which the poll was aborted
     */
    public List<Long> getLatestAbortedPollTimes(){
        return new ArrayList<>(this.latestAbortedPollTimes);
    }

    @Override
    public void addDataPoint(DataPointRT dataPoint) {
        ensureState(ILifecycleState.RUNNING, ILifecycleState.INITIALIZING);
        dataPoint.ensureState(ILifecycleState.INITIALIZING);
        pendingPoints.add(new PendingPoint(dataPoint, PendingPointOperation.ADD));
    }

    @Override
    public void removeDataPoint(DataPointRT dataPoint) {
        ensureState(ILifecycleState.RUNNING);
        dataPoint.ensureState(ILifecycleState.TERMINATING);
        pendingPoints.add(new PendingPoint(dataPoint, PendingPointOperation.REMOVE));
    }

    @Override
    protected void flushPoints() {
        flushPoints(null);
    }

    /**
     * Drains the pending points queue and adds/removes them from the current points.
     * @param pollTime the time of the poll
     */
    protected void flushPoints(Long pollTime) {
        pointListChangeLock.writeLock().lock();
        try {
            PendingPoint pending;
            while ((pending = pendingPoints.poll()) != null) {
                try {
                    switch (pending.operation) {
                        case ADD:
                            addDataPointInternal(pending.point);
                            if (pollTime != null) {
                                pointAddedToPoll(pending.point, pollTime);
                            }
                            break;
                        case REMOVE:
                            removeDataPointInternal(pending.point);
                            if (pollTime != null) {
                                pointRemovedFromPoll(pending.point, pollTime);
                            }
                            break;
                    }
                } catch (Exception e) {
                    LOG.error("Failed to " + pending.operation + " point to list", e);
                }
            }
        } finally {
            pointListChangeLock.writeLock().unlock();
        }
    }

    /**
     * Hook for when a data point is added before a poll executes
     * @param point added point
     * @param pollTime poll time for which the point will be added
     */
    protected void pointAddedToPoll(DataPointRT point, long pollTime) {
        point.initializeIntervalLogging(pollTime, vo.isQuantize());
    }

    /**
     * Hook for when a data point is removed before a poll executes
     * @param point removed point
     * @param pollTime poll time for which the point will be removed
     */
    protected void pointRemovedFromPoll(DataPointRT point, long pollTime) {
    }
}
