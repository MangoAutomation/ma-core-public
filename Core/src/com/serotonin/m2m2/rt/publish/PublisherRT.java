/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.rt.publish;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.infiniteautomation.mango.spring.events.DaoEvent;
import com.serotonin.ShouldNeverHappenException;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.dao.DataPointDao;
import com.serotonin.m2m2.db.dao.PublishedPointDao;
import com.serotonin.m2m2.db.dao.PublisherDao;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.rt.PublishedPointGroupInitializer;
import com.serotonin.m2m2.rt.RuntimeManager;
import com.serotonin.m2m2.rt.dataImage.DataPointRT;
import com.serotonin.m2m2.rt.dataImage.PointValueTime;
import com.serotonin.m2m2.rt.event.AlarmLevels;
import com.serotonin.m2m2.rt.event.type.EventType;
import com.serotonin.m2m2.rt.event.type.PublisherEventType;
import com.serotonin.m2m2.util.timeout.TimeoutClient;
import com.serotonin.m2m2.util.timeout.TimeoutTask;
import com.serotonin.m2m2.vo.DataPointVO;
import com.serotonin.m2m2.vo.publish.PublishedPointVO;
import com.serotonin.m2m2.vo.publish.PublisherVO;
import com.serotonin.m2m2.vo.role.RoleVO;
import com.serotonin.timer.FixedRateTrigger;
import com.serotonin.timer.TimerTask;
import com.serotonin.util.ILifecycle;
import com.serotonin.util.ILifecycleState;

/**
 * @author Matthew Lohbihler
 */
abstract public class PublisherRT<T extends PublisherVO, POINT extends PublishedPointVO> extends TimeoutClient implements ILifecycle {
    private final Logger log = LoggerFactory.getLogger(PublisherRT.class);
    public static final int POINT_DISABLED_EVENT = 1;
    public static final int QUEUE_SIZE_WARNING_EVENT = 2;

    private final Object persistentDataLock = new Object();

    private final EventType pointDisabledEventType;
    private final EventType queueSizeWarningEventType;

    protected final T vo;

    /**
     * The implementor of the publisher is responsible for resetting this back to false after reading the points.
     * It is set to true every time {@link #addPublishedPoint(PublishedPointRT)} and {@link #removePublishedPoint(PublishedPointRT)} are called.
     */
    protected boolean pointListChanged = false;

    /**
     * Protects access to {@link #publishedPoints}
     */
    protected final ReadWriteLock pointListChangeLock = new ReentrantReadWriteLock();
    /**
     *  Protected by {@link #pointListChangeLock}.
     *  <p>Note: We could potentially remove explicit locking and use a ConcurrentHashMap. However this presents two problems -</p>
     *  <ul>
     *      <li>Potential race condition when a point is added while terminating, we must iterate over every point to terminate them</li>
     *      <li>There is no linked version of ConcurrentHashMap so iteration will be slower</li>
     *  </ul>
     */
    private final Map<Integer, PublishedPointRT<POINT>> publishedPointsMap = createPublishedPointsMap();

    /**
     *  You must hold the read lock of {@link #pointListChangeLock} when accessing this collection.
     */
    protected final Collection<PublishedPointRT<POINT>> publishedPoints = Collections.unmodifiableCollection(publishedPointsMap.values());

    protected final PublishQueue<T, POINT, PointValueTime> queue;
    protected final AttributePublishQueue<POINT> attributesChangedQueue;
    private boolean pointEventActive;
    private volatile Thread jobThread;
    private SendThread sendThread;
    private TimerTask snapshotTask;
    private volatile ILifecycleState state = ILifecycleState.PRE_INITIALIZE;
    protected final DataPointDao dataPointDao;
    protected final PublishedPointDao publishedPointDao;

    public PublisherRT(T vo) {
        this.vo = vo;
        this.queue = createPublishQueue(vo);
        this.attributesChangedQueue = createAttirbutesChangedQueue();
        this.pointDisabledEventType = new PublisherEventType(vo, POINT_DISABLED_EVENT);
        this.queueSizeWarningEventType = new PublisherEventType(vo, QUEUE_SIZE_WARNING_EVENT);
        this.dataPointDao = Common.getBean(DataPointDao.class);
        this.publishedPointDao = Common.getBean(PublishedPointDao.class);
    }

    public int getId() {
        return vo.getId();
    }

    protected PublishQueue<T, POINT, PointValueTime> createPublishQueue(PublisherVO vo) {
        return new PublishQueue<>(this, vo.getCacheWarningSize(), vo.getCacheDiscardSize());
    }

    protected AttributePublishQueue<POINT> createAttirbutesChangedQueue() {
        return new AttributePublishQueue<>();
    }

    public final PublisherVO getVo() {
        return vo;
    }

    /**
     * Can be used to return a different implementation, or empty map if you are going to override
     * {@link #publishedPointAdded(PublishedPointRT)} etc.
     * @return map to use to store data points
     */
    protected Map<Integer, PublishedPointRT<POINT>> createPublishedPointsMap() {
        return new LinkedHashMap<>();
    }

    /**
     * Override to handle any situations where you need to know that a role was modified.
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
                map = new HashMap<>();

            map.put(key, persistentData);

            PublisherDao.getInstance().savePersistentData(vo.getId(), map);
        }
    }

    void publish(POINT vo, PointValueTime newValue) {
        queue.add(vo, newValue);

        synchronized (sendThread) {
            sendThread.notify();
        }
    }

    public void publish(POINT vo, List<PointValueTime> newValues) {
        queue.add(vo, newValues);

        synchronized (sendThread) {
            sendThread.notify();
        }
    }

    /**
     * A data point that is being published was just initialized
     * @param rt
     */
    protected void dataPointInitialized(PublishedPointRT<POINT> rt) {
        checkForDisabledPoints();
    }

    /**
     * A data point that is being published was just terminated
     * @param rt
     * @param dp
     */
    protected void dataPointTerminated(PublishedPointRT<POINT> rt, DataPointVO dp) {
        checkForDisabledPoints();
    }

    /**
     * An attribute for a published point was just changed
     * @param vo
     * @param attributes
     */
    protected void attributeChanged(POINT vo, Map<String, Object> attributes) {
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

        pointListChangeLock.readLock().lock();
        try {
            for (PublishedPointRT<POINT> rt : publishedPoints) {
                if (!rt.isPointEnabled()) {
                    badPointId = rt.getVo().getDataPointId();
                    disabledPoint = dataPointDao.getXidById(badPointId);
                    break;
                }
            }
        } finally {
            pointListChangeLock.readLock().unlock();
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
        Map<String, Object> context = new HashMap<>();
        context.put("publisher", vo);
        return context;
    }

    //
    //
    // Lifecycle
    //

    /**
     * Initialize this publisher
     */
    protected void initialize() { }

    /**
     * Create a new send thread
     * @return
     */
    protected abstract SendThread createSendThread();

    /**
     * Hook to know when all points are added, the publisher is fully initialized
     */
    protected void initialized() { }

    /**
     * Hook for just before beginning terminating the publisher
     */
    protected void terminating() { }

    /**
     * Terminate the publisher, next will be the send thread, snapshot ,
     *  queue and finally the points
     */
    protected void terminateImpl() { }

    @Override
    public final synchronized void initialize(boolean safe) {
        ensureState(ILifecycleState.PRE_INITIALIZE);
        this.state = ILifecycleState.INITIALIZING;
        try {
            initialize();
            initializeSendThread();
            initializePoints();
            checkForDisabledPoints();
            initializeSnapshot();
            //Signal to the publisher that all points are added.
            initialized();
        } catch (Exception e) {
            terminate();
            joinTermination();
            throw e;
        }
        this.state = ILifecycleState.RUNNING;
    }

    protected void initializeSendThread() {
        this.sendThread = createSendThread();
        this.sendThread.initialize(false);
    }

    /**
     * The {@link PublishedPointGroupInitializer} calls
     * {@link RuntimeManager#startPublishedPoint(PublishedPointVO) startPublishedPoint()}
     * which adds the points to the cache in the RTM and initializes them.
     */
    private void initializePoints() {
        ExecutorService executorService = Common.getBean(ExecutorService.class);

        // Add the enabled points to the data source.
        List<PublishedPointVO> points = publishedPointDao.getPublishedPoints(getId());

        //Startup multi threaded
        int pointsPerThread = Common.envProps.getInt("runtime.publishedPoint.startupThreads.pointsPerThread", 1000);
        int startupThreads = Common.envProps.getInt("runtime.publishedPoint.startupThreads", Runtime.getRuntime().availableProcessors());
        PublishedPointGroupInitializer pointInitializer = new PublishedPointGroupInitializer(executorService, startupThreads);
        pointInitializer.initialize(points, pointsPerThread);
    }

    protected void initializeSnapshot() {
        if (vo.isSendSnapshot()) {
            // Add a schedule to send the snapshot
            long snapshotPeriodMillis = Common.getMillis(vo.getSnapshotSendPeriodType(), vo.getSnapshotSendPeriods());
            snapshotTask = new TimeoutTask(new FixedRateTrigger(0, snapshotPeriodMillis), this);
        }
    }

    @Override
    public final synchronized void terminate() {
        ensureState(ILifecycleState.INITIALIZING, ILifecycleState.RUNNING);
        this.state = ILifecycleState.TERMINATING;

        try {
            //Signal we are going down
            terminating();
        } catch (Exception e) {
            log.error("Failed to signal termination to " + readableIdentifier(), e);
        }

        try {
            terminateImpl();
        } catch (Exception e) {
            log.error("Failed to terminate " + readableIdentifier(), e);
        }

        try {
            if (sendThread != null) {
                sendThread.terminate();
                sendThread.joinTermination();
            }
        } catch (Exception e) {
            log.error("Failed to terminate send thread for " + readableIdentifier(), e);
        }

        try {
            // Unschedule any job that is running.
            if (snapshotTask != null) {
                snapshotTask.cancel();
            }
        } catch (Exception e) {
            log.error("Failed to cancel snapshot task for " + readableIdentifier(), e);
        }

        try {
            terminatePoints();
        } catch (Exception e) {
            log.error("Failed to terminate points for " + readableIdentifier(), e);
        }

        try {
            // Terminate the queue
            queue.terminate();
        } catch (Exception e) {
            log.error("Failed to terminate queue for " + readableIdentifier(), e);
        }

        try {
            // Remove any outstanding events.
            Common.eventManager.cancelEventsForPublisher(getId());
        } catch (Exception e) {
            log.error("Failed to cancel events for " + readableIdentifier(), e);
        }
    }

    @Override
    public final synchronized void joinTermination() {
        if (getLifecycleState() == ILifecycleState.TERMINATED) return;
        ensureState(ILifecycleState.TERMINATING);

        try {
            Thread localThread = jobThread;
            if (localThread != null) {
                try {
                    localThread.join(30000); // 30 seconds
                }
                catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                if (jobThread != null) {
                    throw new ShouldNeverHappenException("Timeout waiting for publisher to stop: id=" + getId());
                }
            }
        } catch (Exception e) {
            log.error("Error while waiting for " + readableIdentifier() + " to stop", e);
        }

        this.state = ILifecycleState.TERMINATED;
        Common.runtimeManager.removePublisher(this);
    }

    /**
     * Stop the data points.
     */
    private void terminatePoints() {
        forEachPoint(p -> {
            p.terminate();
            p.joinTermination();
        });
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
            forEachPoint(rt -> {
                if (rt.isPointEnabled()) {
                    DataPointRT dp = Common.runtimeManager.getDataPoint(rt.getVo().getDataPointId());
                    if (dp != null) {
                        PointValueTime pvt = dp.getPointValue();
                        if (pvt != null)
                            publish((POINT)rt.getVo(), pvt);
                        //Publish snapshot of attributes
                        if(vo.isPublishAttributeChanges()) {
                            rt.publishAttributes(dp, false);
                        }
                    }
                }
            });
        }
        finally {
            jobThread = null;
        }
    }

    @Override
    public String getThreadName() {
        return "Publisher: " + vo.getXid();
    }

    @Override
    public String getTaskId() {
        return "PUB-" + vo.getXid();
    }

    @Override
    public ILifecycleState getLifecycleState() {
        return state;
    }

    @Override
    public String readableIdentifier() {
        return String.format("Publisher (name=%s, id=%d, type=%s)", getVo().getName(), getId(), getClass().getSimpleName());
    }

    /**
     * Add a point during its initialization
     * @param rt
     */
    public void addPublishedPoint(PublishedPointRT<POINT> rt) {
        ensureState(ILifecycleState.RUNNING, ILifecycleState.INITIALIZING);
        rt.ensureState(ILifecycleState.INITIALIZING);
        addPublishedPointImpl(rt);
        publishedPointAdded(rt);
    }

    /**
     * Remove a point during its termination
     * @param rt
     */
    public void removePublishedPoint(PublishedPointRT<POINT> rt) {
        ensureState(ILifecycleState.RUNNING, ILifecycleState.INITIALIZING);
        rt.ensureState(ILifecycleState.TERMINATING);
        removePublishedPointImpl(rt);
        publishedPointRemoved(rt);
    }

    /**
     * Adds the data point to the current points. If you override this method you should also override
     * {@link PublisherRT#streamPoints()}, {@link #forEachPoint(Consumer)}.
     *
     * @param point to add
     */
    protected void addPublishedPointImpl(PublishedPointRT<POINT> point) {
        pointListChangeLock.writeLock().lock();
        try {
            if (publishedPointsMap.putIfAbsent(point.getId(), point) != null) {
                throw new IllegalStateException("Published point with ID " + point.getId() + " is already present on this data source");
            }
            pointListChanged = true;
        } finally {
            pointListChangeLock.writeLock().unlock();
        }
    }

    /**
     * Removes the published point from the current points. If you override this method you should also override
     * {@link PublisherRT#streamPoints()}, {@link #forEachPoint(Consumer)}, and {@link #getPointById(int)}.
     *
     * @param point to remove
     */
    protected void removePublishedPointImpl(PublishedPointRT<POINT> point) {
        pointListChangeLock.writeLock().lock();
        try {
            if (!publishedPointsMap.remove(point.getId(), point)) {
                throw new IllegalStateException("Published point with ID " + point.getId() + " was not present on this data source");
            }
            pointListChanged = true;
        } finally {
            pointListChangeLock.writeLock().unlock();
        }
    }

    /**
     * Hook that is run when a published point is added.
     * @param point that was added
     */
    protected void publishedPointAdded(PublishedPointRT<POINT> point) {
    }

    /**
     * Hook that is run when a published point is removed.
     * @param point that was removed
     */
    protected void publishedPointRemoved(PublishedPointRT<POINT> point) {
    }

    /**
     * Care must be taken to close the stream. Always use a try-with-resources statement e.g.
     * <pre>{@code
     * try (Stream<PublishedPointRT> stream = streamPoints()) {
     *     return stream.mapToInt(PublishedPointRT::getId).toArray();
     * }
     * }</pre>
     * @return stream of points
     */
    protected Stream<PublishedPointRT<POINT>> streamPoints() {
        pointListChangeLock.readLock().lock();
        return publishedPoints.stream().onClose(() -> pointListChangeLock.readLock().unlock());
    }

    protected void forEachPoint(Consumer<PublishedPointRT<? extends PublishedPointVO>> consumer) {
        pointListChangeLock.readLock().lock();
        try {
            publishedPoints.forEach(consumer);
        } finally {
            pointListChangeLock.readLock().unlock();
        }
    }

    protected PublishedPointRT<POINT> getPointById(int id) {
        pointListChangeLock.readLock().lock();
        try {
            return publishedPointsMap.get(id);
        } finally {
            pointListChangeLock.readLock().unlock();
        }
    }
}
