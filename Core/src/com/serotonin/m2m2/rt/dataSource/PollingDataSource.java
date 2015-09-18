/*
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.rt.dataSource;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.serotonin.ShouldNeverHappenException;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.rt.dataImage.DataPointRT;
import com.serotonin.m2m2.util.timeout.TimeoutClient;
import com.serotonin.m2m2.util.timeout.TimeoutTask;
import com.serotonin.m2m2.vo.dataSource.DataSourceVO;
import com.serotonin.m2m2.web.taglib.Functions;
import com.serotonin.timer.CronTimerTrigger;
import com.serotonin.timer.FixedRateTrigger;
import com.serotonin.timer.TimerTask;

abstract public class PollingDataSource extends DataSourceRT implements TimeoutClient {
	
    private final Log LOG = LogFactory.getLog(PollingDataSource.class);
    private Object terminationLock;

    private final DataSourceVO<?> vo;
    protected List<DataPointRT> dataPoints = new ArrayList<DataPointRT>();
    protected boolean pointListChanged = false;

    // If polling is done with millis
    private long pollingPeriodMillis = 300000; // Default to 5 minutes just to
                                               // have something here
    private boolean quantize;

    // If polling is done with cron
    private String cronPattern;

    private TimerTask timerTask;
    private volatile Thread jobThread;
    private long jobThreadStartTime;

    private final AtomicBoolean lastPollSuccessful = new AtomicBoolean();
    private final AtomicLong successfulPolls = new AtomicLong();
    private final AtomicLong unsuccessfulPolls = new AtomicLong();

    public PollingDataSource(DataSourceVO<?> vo) {
        super(vo);
        this.vo = vo;
    }

    public void setCronPattern(String cronPattern) {
        this.cronPattern = cronPattern;
    }

    public void setPollingPeriod(int periodType, int periods, boolean quantize) {
        pollingPeriodMillis = Common.getMillis(periodType, periods);
        this.quantize = quantize;
    }

    public long getSuccessfulPolls() {
        return successfulPolls.get();
    }

    public void incrementSuccessfulPolls(long time) {
        successfulPolls.incrementAndGet();
        boolean lastPollSuccessful = this.lastPollSuccessful.getAndSet(true);
        if (lastPollSuccessful) {
	        // Return event to normal
	        int eventId = vo.getPollAbortedExceptionEventId();
	        if(eventId >= 0) {
	        	returnToNormal(eventId, time);
	        }
        }
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
        long unsuccessful = unsuccessfulPolls.incrementAndGet();
        lastPollSuccessful.set(false);
        //Raise Event
        int eventId = vo.getPollAbortedExceptionEventId();
        if(eventId >= 0)
        	this.raiseEvent(eventId, time, true, new TranslatableMessage("event.pollAborted", vo.getXid(), vo.getName(), unsuccessful));
    }

    @Override
    public void scheduleTimeout(long fireTime) {
        if (jobThread != null) {
            // There is another poll still running, so abort this one.
            LOG.warn(vo.getName() + ": poll at " + Functions.getFullSecondTime(fireTime)
                    + " aborted because a previous poll started at " + Functions.getFullSecondTime(jobThreadStartTime)
                    + " is still running");
            incrementUnsuccessfulPolls(fireTime);
            return;
        }
        incrementSuccessfulPolls(fireTime);

        try {
            jobThread = Thread.currentThread();
            jobThreadStartTime = fireTime;
            
            // Check if there were changes to the data points list.
            updateChangedPoints(fireTime);

            doPollNoSync(fireTime);
        }
        finally {
            if (terminationLock != null) {
                synchronized (terminationLock) {
                    terminationLock.notifyAll();
                }
            }
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

    /**
     * Override this method if you do not want the poll to synchronize on
     * pointListChangeLock
     * 
     * @param time
     */
    protected void doPollNoSync(long time) {
        synchronized (pointListChangeLock) {
            doPoll(time);
        }
    }

    abstract protected void doPoll(long time);

    protected void updateChangedPoints(long fireTime) {
        synchronized (pointListChangeLock) {
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
            	long now = System.currentTimeMillis();
                delay = pollingPeriodMillis - (now % pollingPeriodMillis);
                LOG.debug("First poll should be at: " + (now + delay));
            }
            timerTask = new TimeoutTask(new FixedRateTrigger(delay, pollingPeriodMillis), this);
        }
        else {
            try {
                timerTask = new TimeoutTask(new CronTimerTrigger(cronPattern), this);
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
}
