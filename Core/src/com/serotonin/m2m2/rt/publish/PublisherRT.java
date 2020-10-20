/*
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.rt.publish;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.infiniteautomation.mango.spring.events.DaoEvent;
import com.serotonin.ShouldNeverHappenException;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.dao.DataPointDao;
import com.serotonin.m2m2.db.dao.PublisherDao;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.rt.dataImage.DataPointRT;
import com.serotonin.m2m2.rt.dataImage.PointValueTime;
import com.serotonin.m2m2.rt.event.AlarmLevels;
import com.serotonin.m2m2.rt.event.type.EventType;
import com.serotonin.m2m2.rt.event.type.PublisherEventType;
import com.serotonin.m2m2.util.timeout.TimeoutClient;
import com.serotonin.m2m2.util.timeout.TimeoutTask;
import com.serotonin.m2m2.vo.publish.PublishedPointVO;
import com.serotonin.m2m2.vo.publish.PublisherVO;
import com.serotonin.m2m2.vo.role.RoleVO;
import com.serotonin.timer.FixedRateTrigger;
import com.serotonin.timer.TimerTask;

/**
 * @author Matthew Lohbihler
 */
abstract public class PublisherRT<T extends PublishedPointVO> extends TimeoutClient {
    public static final int POINT_DISABLED_EVENT = 1;
    public static final int QUEUE_SIZE_WARNING_EVENT = 2;

    private final Object persistentDataLock = new Object();

    private final EventType pointDisabledEventType;
    private final EventType queueSizeWarningEventType;

    private final PublisherVO<T> vo;
    protected final List<PublishedPointRT<T>> pointRTs = new ArrayList<PublishedPointRT<T>>();
    protected final PublishQueue<T, PointValueTime> queue;
    protected final AttributePublishQueue<T> attributesChangedQueue;
    private boolean pointEventActive;
    private volatile Thread jobThread;
    private SendThread sendThread;
    private TimerTask snapshotTask;

    public PublisherRT(PublisherVO<T> vo) {
        this.vo = vo;
        queue = createPublishQueue(vo);
        attributesChangedQueue = createAttirbutesChangedQueue();
        pointDisabledEventType = new PublisherEventType(this.getVo(), POINT_DISABLED_EVENT);
        queueSizeWarningEventType = new PublisherEventType(this.getVo(), QUEUE_SIZE_WARNING_EVENT);
    }

    public int getId() {
        return vo.getId();
    }

    protected PublishQueue<T, PointValueTime> createPublishQueue(PublisherVO<T> vo) {
        return new PublishQueue<T, PointValueTime>(this, vo.getCacheWarningSize(), vo.getCacheDiscardSize());
    }

    protected AttributePublishQueue<T> createAttirbutesChangedQueue() {
        return new AttributePublishQueue<T>(vo.getPoints().size());
    }

    public PublisherVO<T> getVo() {
        return vo;
    }

    /**
     * Override to handle any situations where you need to know that a role was modified.
     *
     * @param event
     */
    public void handleRoleEvent(DaoEvent<? extends RoleVO> event) {

    }

    /**
     * This method is usable by subclasses to retrieve serializable data stored using the setPersistentData method.
     */
    public Object getPersistentData(String key) {
        synchronized (persistentDataLock) {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>)  PublisherDao.getInstance().getPersistentData(vo.getId());
            if (map != null)
                return map.get(key);
            return null;
        }
    }

    /**
     * This method is usable by subclasses to store any type of serializable data. This intention is to provide a
     * mechanism for publisher RTs to be able to persist data between runs. Normally this method would at least be
     * called in the terminate method, but may also be called regularly for failover purposes.
     */
    public void setPersistentData(String key, Object persistentData) {
        synchronized (persistentDataLock) {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) PublisherDao.getInstance().getPersistentData(vo.getId());
            if (map == null)
                map = new HashMap<String, Object>();

            map.put(key, persistentData);

            PublisherDao.getInstance().savePersistentData(vo.getId(), map);
        }
    }

    void publish(T vo, PointValueTime newValue) {
        queue.add(vo, newValue);

        synchronized (sendThread) {
            sendThread.notify();
        }
    }

    public void publish(T vo, List<PointValueTime> newValues) {
        queue.add(vo, newValues);

        synchronized (sendThread) {
            sendThread.notify();
        }
    }

    protected void pointInitialized(PublishedPointRT<T> rt) {
        checkForDisabledPoints();
    }

    protected void pointTerminated(PublishedPointRT<T> rt) {
        checkForDisabledPoints();
    }

    protected void attributeChanged(T vo, Map<String, Object> attributes) {
        if(this.vo.isPublishAttributeChanges()) {
            attributesChangedQueue.add(vo, attributes);
            synchronized (sendThread) {
                sendThread.notify();
            }
        }
    }


    synchronized private void checkForDisabledPoints() {
        int badPointId = -1;
        String disabledPoint = null;
        for (PublishedPointRT<T> rt : pointRTs) {
            if (!rt.isPointEnabled()) {
                badPointId = rt.getVo().getDataPointId();
                disabledPoint = DataPointDao.getInstance().getXidById(badPointId);
                break;
            }
        }

        boolean foundBadPoint = badPointId != -1;
        if (pointEventActive != foundBadPoint) {
            pointEventActive = foundBadPoint;
            if (pointEventActive) {
                // A published point has been terminated, was never enabled, or no longer exists.
                TranslatableMessage lm;
                if (disabledPoint == null)
                    // The point is missing
                    lm = new TranslatableMessage("event.publish.pointMissing", badPointId);
                else
                    lm = new TranslatableMessage("event.publish.pointDisabled", disabledPoint);
                Common.eventManager.raiseEvent(pointDisabledEventType, Common.timer.currentTimeMillis(), true,
                        vo.getAlarmLevel(POINT_DISABLED_EVENT, AlarmLevels.URGENT), lm, createEventContext());
            }
            else
                // Everything is good
                Common.eventManager.returnToNormal(pointDisabledEventType, Common.timer.currentTimeMillis());
        }
    }

    void fireQueueSizeWarningEvent() {
        Common.eventManager.raiseEvent(queueSizeWarningEventType, Common.timer.currentTimeMillis(), true,
                vo.getAlarmLevel(QUEUE_SIZE_WARNING_EVENT, AlarmLevels.URGENT),
                new TranslatableMessage("event.publish.queueSize", vo.getCacheWarningSize()), createEventContext());
    }

    void deactivateQueueSizeWarningEvent() {
        Common.eventManager.returnToNormal(queueSizeWarningEventType, Common.timer.currentTimeMillis());
    }

    protected Map<String, Object> createEventContext() {
        Map<String, Object> context = new HashMap<String, Object>();
        context.put("publisher", vo);
        return context;
    }

    //
    //
    // Lifecycle
    //
    abstract public void initialize();

    protected void initialize(SendThread sendThread) {
        this.sendThread = sendThread;
        sendThread.initialize(false);

        for (T p : vo.getPoints())
            pointRTs.add(new PublishedPointRT<T>(p, this));

        if (vo.isSendSnapshot()) {
            // Add a schedule to send the snapshot
            long snapshotPeriodMillis = Common.getMillis(vo.getSnapshotSendPeriodType(), vo.getSnapshotSendPeriods());
            snapshotTask = new TimeoutTask(new FixedRateTrigger(0, snapshotPeriodMillis), this);
        }

        checkForDisabledPoints();
    }

    public void terminate() {
        if(sendThread != null){
            sendThread.terminate();
            sendThread.joinTermination();
        }

        // Unschedule any job that is running.
        if (snapshotTask != null)
            snapshotTask.cancel();

        // Terminate the point listeners
        for (PublishedPointRT<T> rt : pointRTs)
            rt.terminate();

        // Remove any outstanding events.
        Common.eventManager.cancelEventsForPublisher(getId());
    }

    public void joinTermination() {
        Thread localThread = jobThread;
        if (localThread != null) {
            try {
                localThread.join(30000); // 30 seconds
            }
            catch (InterruptedException e) { /* no op */
            }
            if (jobThread != null)
                throw new ShouldNeverHappenException("Timeout waiting for publisher to stop: id=" + getId());
        }
    }

    //
    //
    // Scheduled snapshot send stuff
    //
    @Override
    public void scheduleTimeout(long fireTime) {
        if (jobThread != null)
            return;

        jobThread = Thread.currentThread();

        try {
            synchronized (this) {
                for (PublishedPointRT<T> rt : pointRTs) {
                    if (rt.isPointEnabled()) {
                        DataPointRT dp = Common.runtimeManager.getDataPoint(rt.getVo().getDataPointId());
                        if (dp != null) {
                            PointValueTime pvt = dp.getPointValue();
                            if (pvt != null)
                                publish(rt.getVo(), pvt);
                            //Publish snapshot of attributes
                            if(vo.isPublishAttributeChanges()) {
                                rt.publishAttributes(dp, false);
                            }
                        }
                    }
                }
            }
        }
        finally {
            jobThread = null;
        }
    }

    @Override
    public String getThreadName() {
        return "Pubisher: " + vo.getXid();
    }

    @Override
    public String getTaskId() {
        return "PUB-" + vo.getXid();
    }
}
