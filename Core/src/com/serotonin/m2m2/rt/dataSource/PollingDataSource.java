/*
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.rt.dataSource;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.infiniteautomation.mango.monitor.ValueMonitor;
import com.serotonin.ShouldNeverHappenException;
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

abstract public class PollingDataSource<T extends PollingDataSourceVO<?>> extends DataSourceRT<T> {

    private final Log LOG = LogFactory.getLog(PollingDataSource.class);
    private static final String prefix = "POLLINGDS-";
    private Object terminationLock;

    protected List<DataPointRT> dataPoints = new ArrayList<>();
    protected boolean pointListChanged = false;

    // If polling is done with millis
    protected long pollingPeriodMillis = 300000; // Default to 5 minutes just to
    // have something here
    protected boolean quantize;

    // If polling is done with cron
    protected String cronPattern;

    private final TimeoutClient timeoutClient;
    private TimerTask timerTask;
    protected volatile Thread jobThread;

    private final AtomicBoolean lastPollSuccessful = new AtomicBoolean();
    private final AtomicLong successfulPolls = new AtomicLong();
    private final AtomicLong unsuccessfulPolls = new AtomicLong();
    private final AtomicLong currentSuccessfulPolls = new AtomicLong();
    private final ValueMonitor<Long> currentSuccessfulPollsMonitor;
    private final ValueMonitor<Long> lastPollDurationMonitor;
    private final ValueMonitor<Double> successfulPollsPercentageMonitor;
    private final ConcurrentLinkedQueue<LongLongPair> latestPollTimes;
    private final ConcurrentLinkedQueue<Long> latestAbortedPollTimes;
    private long nextAbortedPollMessageTime = 0l;
    private final long abortedPollLogDelay;

    public PollingDataSource(T vo) {
        super(vo);
        if(vo.isUseCron())
            this.cronPattern = vo.getCronPattern();
        else
            pollingPeriodMillis = Common.getMillis(vo.getUpdatePeriodType(), vo.getUpdatePeriods());

        this.quantize = vo.isQuantize();

        this.latestPollTimes = new ConcurrentLinkedQueue<LongLongPair>();
        this.latestAbortedPollTimes = new ConcurrentLinkedQueue<Long>();
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

    public void incrementSuccessfulPolls(long time) {
        successfulPolls.incrementAndGet();
        currentSuccessfulPolls.incrementAndGet();
        this.lastPollSuccessful.getAndSet(true);
    }

    public long getUnsuccessfulPolls() {
        return unsuccessfulPolls.get();
    }

    /**
     * Increment the unsuccessful polls
     * and fire event if necessary
     * @param time
     */
    public void incrementUnsuccessfulPolls(long time) {
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

    public synchronized void scheduleTimeoutImpl(long fireTime) {
        try {
            jobThread = Thread.currentThread();

            long startTs = Common.timer.currentTimeMillis();

            // Check to see if this poll is running after it's next poll time, i.e. polls are
            // backing up
            if ((cronPattern == null) && ((startTs - fireTime) > pollingPeriodMillis)) {
                incrementUnsuccessfulPolls(fireTime);
                return;
            }

            incrementSuccessfulPolls(fireTime);

            // Check if there were changes to the data points list.
            updateChangedPoints(fireTime);

            doPollNoSync(fireTime);

            // Save the poll time and duration
            long pollDuration = Common.timer.currentTimeMillis() - startTs;
            this.latestPollTimes.add(new LongLongPair(fireTime, pollDuration));
            this.lastPollDurationMonitor.setValue(pollDuration);
            // Trim the Queue
            while (this.latestPollTimes.size() > 10)
                this.latestPollTimes.poll();
        } finally {
            if (terminationLock != null) {
                synchronized (terminationLock) {
                    terminationLock.notifyAll();
                }
            }
            updateSuccessfulPollQuotient();
            jobThread = null;
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
     * @param time
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

    protected void updateChangedPoints(long fireTime) {
        pointListChangeLock.writeLock().lock();
        try {
            if (addedChangedPoints.size() > 0) {

                // Remove any existing instances of the points.
                dataPoints.removeAll(addedChangedPoints);

                // Add the changed points and start the interval logging
                for(DataPointRT rt : addedChangedPoints){
                    rt.initializeIntervalLogging(fireTime, quantize);
                    dataPoints.add(rt);
                }
                addedChangedPoints.clear();
                pointListChanged = true;
            }
            if (removedPoints.size() > 0) {
                dataPoints.removeAll(removedPoints);
                removedPoints.clear();
                pointListChanged = true;
            }
        } finally {
            pointListChangeLock.writeLock().unlock();
        }
    }

    //
    //
    // Data source interface
    //
    @Override
    public void beginPolling() {
        if (cronPattern == null) {
            long delay = 0;
            if (quantize){
                // Quantize the start.
                long now = Common.timer.currentTimeMillis();
                delay = pollingPeriodMillis - (now % pollingPeriodMillis);
                if(LOG.isDebugEnabled())
                    LOG.debug("First poll should be at: " + (now + delay));
                timerTask = new TimeoutTask(new FixedRateTrigger(new Date(now + delay), pollingPeriodMillis), this.timeoutClient);
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
    public void terminate() {
        if (timerTask != null)
            timerTask.cancel();

        Common.MONITORED_VALUES.remove(currentSuccessfulPollsMonitor.getId());
        Common.MONITORED_VALUES.remove(lastPollDurationMonitor.getId());
        Common.MONITORED_VALUES.remove(successfulPollsPercentageMonitor.getId());

        super.terminate();
    }

    @Override
    public void joinTermination() {
        super.joinTermination();

        if (jobThread == null)
            return;

        terminationLock = new Object();

        int tries = 10;
        while (true) {
            synchronized (terminationLock) {
                Thread localThread = jobThread;
                if (localThread == null)
                    break;

                try {
                    terminationLock.wait(30000);
                }
                catch (InterruptedException e) {
                    // no op
                }

                if (jobThread != null) {
                    if (tries-- > 0)
                        LOG.warn("Waiting for data source to stop: id=" + getId() + ", type=" + getClass());
                    else
                        throw new ShouldNeverHappenException("Timeout waiting for data source to stop: id=" + getId()
                        + ", type=" + getClass() + ", stackTrace="
                        + Arrays.toString(localThread.getStackTrace()));
                }
            }
        }
    }

    /**
     * Get the latest poll times and durations.  Use sparingly as this will block the polling thread
     * @return
     */
    public List<LongLongPair> getLatestPollTimes(){
        List<LongLongPair> latestTimes = new ArrayList<LongLongPair>();
        Iterator<LongLongPair> it = this.latestPollTimes.iterator();
        while(it.hasNext()){
            latestTimes.add(it.next());
        }
        return latestTimes;
    }

    /**
     * Get the latest times for Aborted polls.
     * @return
     */
    public List<Long> getLatestAbortedPollTimes(){
        List<Long> latestTimes = new ArrayList<Long>();
        Iterator<Long> it = this.latestAbortedPollTimes.iterator();
        while(it.hasNext()){
            latestTimes.add(it.next());
        }
        return latestTimes;
    }

}
