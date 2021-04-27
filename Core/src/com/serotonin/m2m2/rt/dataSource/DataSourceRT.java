/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.rt.dataSource;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.checkerframework.checker.nullness.qual.NonNull;

import com.infiniteautomation.mango.io.serial.SerialPortException;
import com.infiniteautomation.mango.spring.events.DaoEvent;
import com.infiniteautomation.mango.spring.events.DaoEventType;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.dao.DataPointDao;
import com.serotonin.m2m2.db.dao.DataSourceDao;
import com.serotonin.m2m2.db.dao.PointValueCacheDao;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.rt.DataPointGroupInitializer;
import com.serotonin.m2m2.rt.RuntimeManager;
import com.serotonin.m2m2.rt.dataImage.DataPointRT;
import com.serotonin.m2m2.rt.dataImage.PointValueTime;
import com.serotonin.m2m2.rt.dataImage.SetPointSource;
import com.serotonin.m2m2.rt.event.type.DataSourceEventType;
import com.serotonin.m2m2.vo.dataPoint.DataPointWithEventDetectors;
import com.serotonin.m2m2.vo.dataSource.DataSourceVO;
import com.serotonin.m2m2.vo.dataSource.PollingDataSourceVO;
import com.serotonin.m2m2.vo.role.Role;
import com.serotonin.m2m2.vo.role.RoleVO;
import com.serotonin.util.ILifecycle;
import com.serotonin.util.ILifecycleState;

/**
 * Data sources are things that produce data for consumption of this system. Anything that houses, creates, manages, or
 * otherwise can get data to Mango can be considered a data source. As such, this interface can more precisely be
 * considered a proxy of the real thing.
 *
 * Mango contains multiple objects that carry the name data source. This interface represents those types of objects
 * that execute and perform the actual task of getting information one way or another from the external data source and
 * into the system, and is known as the "run-time" (RT) data source. (Another type is the data source VO, which
 * represents the configuration of a data source RT, a subtle but useful distinction. In particular, a VO is
 * serializable, while an RT is not.)
 *
 * @author Matthew Lohbihler
 */
abstract public class DataSourceRT<VO extends DataSourceVO> implements ILifecycle {
    private final Log log = LogFactory.getLog(DataSourceRT.class);

    public static final String DATA_SOURCE_EVENT_CONTEXT_KEY = "dataSource";
    public static final String ATTR_UNRELIABLE_KEY = "UNRELIABLE";

    /**
     * Protects access to {@link #dataPointsMap} and {@link #dataPoints}
     */
    protected final ReadWriteLock pointListChangeLock = new ReentrantReadWriteLock();

    /**
     * The implementor of the data source is responsible for resetting this back to false after reading the points.
     * It is set to true every time {@link #addDataPoint(DataPointRT)} and {@link #removeDataPoint(DataPointRT)} are called.
     */
    protected boolean pointListChanged = false;

    private final Map<Integer, DataPointRT> dataPointsMap = new HashMap<>();

    /**
     *  You must hold the read lock of {@link #pointListChangeLock} e.g. inside {@link PollingDataSource#doPoll(long)}
     *  when accessing this collection.
     */
    protected final Collection<DataPointRT> dataPoints = Collections.unmodifiableCollection(dataPointsMap.values());

    /**
     * Stores pending data points which are waiting to be added or removed from {@link #dataPointsMap}
     * (usually on the next poll)
     */
    private final Queue<PendingPoint> pendingPoints = new ConcurrentLinkedQueue<>();

    private static class PendingPoint {
        private final DataPointRT point;
        private final boolean remove;

        private PendingPoint(DataPointRT point, boolean remove) {
            this.point = point;
            this.remove = remove;
        }
    }

    /**
     * Stores a map of data source event type ids to the {@link EventStatus}
     */
    private final Map<Integer, EventStatus> eventTypes;

    private volatile ILifecycleState state = ILifecycleState.PRE_INITIALIZE;

    protected final VO vo;

    public DataSourceRT(VO vo) {
        this.vo = vo;

        this.eventTypes = Collections.unmodifiableMap(vo.getEventTypes().stream()
                .map(e -> (DataSourceEventType) e.getEventType())
                .collect(Collectors.toMap(DataSourceEventType::getDataSourceEventTypeId, EventStatus::new)));
    }

    public int getId() {
        return vo.getId();
    }

    public String getName() {
        return vo.getName();
    }

    public VO getVo() {
        return vo;
    }

    /**
     * This method is usable by subclasses to retrieve serializable data stored using the setPersistentData method.
     */
    public Object getPersistentData() {
        return DataSourceDao.getInstance().getPersistentData(vo.getId());
    }

    /**
     * This method is usable by subclasses to store any type of serializable data. This intention is to provide a
     * mechanism for data source RTs to be able to persist data between runs. Normally this method would at least be
     * called in the terminate method, but may also be called regularly for failover purposes.
     */
    protected void setPersistentData(Object persistentData) {
        DataSourceDao.getInstance().savePersistentData(vo.getId(), persistentData);
    }

    /**
     * Queues a data point for addition to the data source's current points.
     * @param dataPoint point to add
     */
    public void addDataPoint(DataPointRT dataPoint) {
        ensureState(ILifecycleState.RUNNING, ILifecycleState.INITIALIZING);
        dataPoint.ensureState(ILifecycleState.INITIALIZING);
        pendingPoints.add(new PendingPoint(dataPoint, false));
    }

    /**
     * Queues a data point for removal from the data source's current points.
     * @param dataPoint point to remove
     */
    public void removeDataPoint(DataPointRT dataPoint) {
        ensureState(ILifecycleState.RUNNING);
        dataPoint.ensureState(ILifecycleState.TERMINATING);
        pendingPoints.add(new PendingPoint(dataPoint, true));
    }

    /**
     * Immediately adds the data point to the current points. Must hold write lock of {@link #pointListChangeLock}.
     *
     * @param time usually the current poll time
     * @param dataPoint data point to add
     */
    protected void addDataPointInternal(long time, DataPointRT dataPoint) {
        if (dataPointsMap.putIfAbsent(dataPoint.getId(), dataPoint) != null) {
            throw new IllegalStateException("Data point with ID " + dataPoint.getId() + " is already present on this data source");
        }
        pointListChanged = true;
    }

    /**
     * Immediately removes the data point from the current points. Must hold write lock of {@link #pointListChangeLock}.
     *
     * @param time usually the current poll time
     * @param dataPoint data point to remove
     */
    protected void removeDataPointInternal(long time, DataPointRT dataPoint) {
        if (!dataPointsMap.remove(dataPoint.getId(), dataPoint)) {
            throw new IllegalStateException("Data point with ID " + dataPoint.getId() + " was not present on this data source");
        }
        pointListChanged = true;
    }

    /**
     * Drains the pending points queue and adds/removes them from the current points.
     *
     * @param time usually the current poll time
     */
    protected void drainPendingPointsQueue(long time) {
        pointListChangeLock.writeLock().lock();
        try {
            PendingPoint pending;
            while ((pending = pendingPoints.poll()) != null) {
                if (pending.remove) {
                    removeDataPointInternal(time, pending.point);
                } else {
                    addDataPointInternal(time, pending.point);
                }
            }
        } finally {
            pointListChangeLock.writeLock().unlock();
        }
    }

    public void setPointValue(DataPointRT dataPoint, PointValueTime valueTime, SetPointSource source) {
        if(dataPoint.getVO().isPreventSetExtremeValues()) {
            double transformedValue = valueTime.getDoubleValue();
            if(transformedValue > dataPoint.getVO().getSetExtremeHighLimit()
                    || transformedValue < dataPoint.getVO().getSetExtremeLowLimit())
                return;
        }
        setPointValueImpl(dataPoint, valueTime, source);
    }

    abstract public void setPointValueImpl(DataPointRT dataPoint, PointValueTime valueTime, SetPointSource source);

    public void relinquish(DataPointRT dataPoint) {
        throw new UnsupportedOperationException("Not implemented for " + getClass());
    }

    public void forcePointRead(DataPointRT dataPoint) {
        throw new UnsupportedOperationException("Not implemented for " + getClass());
    }

    public void forcePoll() {
        throw new UnsupportedOperationException("Not implemented for " + getClass());
    }

    /**
     * Raises a data source event.
     *
     * @param dataSourceEventTypeId Must be registered via {@link PollingDataSourceVO#addEventTypes(java.util.List)}
     * @param time time at which the event will be raised
     * @param rtn true if the event can return to normal (become inactive) at some point in the future
     * @param message translatable event message
     * @throws IllegalStateException if data source has been terminated
     */
    protected void raiseEvent(int dataSourceEventTypeId, long time, boolean rtn, TranslatableMessage message) {
        // Persistent TCP data source raises events while initializing
        ensureState(ILifecycleState.INITIALIZING, ILifecycleState.RUNNING);
        message = new TranslatableMessage("event.ds", vo.getName(), message);
        EventStatus status = getEventStatus(dataSourceEventTypeId);
        Map<String, Object> context = Collections.singletonMap(DATA_SOURCE_EVENT_CONTEXT_KEY, vo);
        DataSourceEventType type = status.eventType;

        if (rtn) {
            synchronized (status.lock) {
                status.active = true;
                Common.eventManager.raiseEvent(type, time, true, type.getAlarmLevel(), message, context);
            }
        } else {
            Common.eventManager.raiseEvent(type, time, false, type.getAlarmLevel(), message, context);
        }
    }

    /**
     * Returns a data source event to normal.
     *
     * @param dataSourceEventTypeId Must be registered via {@link PollingDataSourceVO#addEventTypes(java.util.List)}
     * @param time time at which the event will be returned to normal (made inactive)
     * @throws IllegalStateException if data source has been terminated
     */
    protected void returnToNormal(int dataSourceEventTypeId, long time) {
        // Persistent TCP data source deactivates events while initializing
        ensureState(ILifecycleState.INITIALIZING, ILifecycleState.RUNNING);
        EventStatus status = getEventStatus(dataSourceEventTypeId);
        synchronized (status.lock) {
            //For performance ensure we have an active event to RTN
            if (status.active) {
                Common.eventManager.returnToNormal(status.eventType, time);
                // only remove afterwards in case returnToNormal throws an exception
                status.active = false;
            }
        }
    }

    private @NonNull EventStatus getEventStatus(int eventId) {
        EventStatus status = eventTypes.get(eventId);
        if (status == null) {
            throw new IllegalArgumentException(String.format("Event type ID %s is not registered for this data source", eventId));
        }
        return status;
    }

    protected TranslatableMessage getSerialExceptionMessage(Exception e, String portId) {
        if(e instanceof SerialPortException)
            return new TranslatableMessage("event.serial.portError", portId, e.getLocalizedMessage());
        return getExceptionMessage(e);
    }

    protected static TranslatableMessage getExceptionMessage(Exception e) {
        return new TranslatableMessage("event.exception2", e.getClass().getName(), e.getMessage());
    }

    /**
     * Override as required to add messages to the runtime status section of the data source edit page.
     *
     * @param messages
     *            the list to which to add messages
     */
    public void addStatusMessages(List<TranslatableMessage> messages) {
        // Override as required
    }

    //
    //
    // Lifecycle
    //
    /* TODO Mango 4.0
     * For future use if we want to allow some data sources to startup in safe mode
     *  will require RuntimeManagerChanges
     */
    @Override
    public final synchronized void initialize(boolean safe) {
        ensureState(ILifecycleState.PRE_INITIALIZE);
        this.state = ILifecycleState.INITIALIZING;
        if(!safe)
            initialize();
        initializePoints();
        this.state = ILifecycleState.RUNNING;
    }

    /**
     * The {@link DataPointGroupInitializer} calls
     * {@link RuntimeManager#startDataPoint(com.serotonin.m2m2.vo.dataPoint.DataPointWithEventDetectors, java.util.List) startDataPoint()}
     * which adds the data points to the cache in the RTM and initializes them.
     */
    private void initializePoints() {
        DataPointDao dataPointDao = Common.getBean(DataPointDao.class);
        ExecutorService executorService = Common.getBean(ExecutorService.class);

        // Add the enabled points to the data source.
        List<DataPointWithEventDetectors> dataSourcePoints = dataPointDao.getDataPointsForDataSourceStart(getId());

        //Startup multi threaded
        int pointsPerThread = Common.envProps.getInt("runtime.datapoint.startupThreads.pointsPerThread", 1000);
        int startupThreads = Common.envProps.getInt("runtime.datapoint.startupThreads", Runtime.getRuntime().availableProcessors());
        PointValueCacheDao pointValueCacheDao = Common.databaseProxy.getPointValueCacheDao();
        DataPointGroupInitializer pointInitializer = new DataPointGroupInitializer(executorService, startupThreads, pointValueCacheDao);
        pointInitializer.initialize(dataSourcePoints, pointsPerThread);

        //Signal to the data source that all points are added.
        initialized();
    }

    /**
     * Initialize this data source
     */
    protected void initialize() {
        // no op
    }

    /**
     * Hook to know when all points are added, the source is fully initialized
     */
    protected void initialized() {

    }

    /**
     * Hook for just before points get removed from the data source
     */
    protected void terminating() {

    }

    @Override
    public final synchronized void terminate() {
        ensureState(ILifecycleState.RUNNING);
        this.state = ILifecycleState.TERMINATING;

        try {
            //Signal we are going down
            terminating();
        } catch (Exception e) {
            log.error("Failed to signal termination to " + readableIdentifier(), e);
        }

        try {
            terminatePoints();
        } catch (Exception e) {
            log.error("Failed to terminate all points for " + readableIdentifier(), e);
        }
        terminateImpl();
    }

    /**
     * Stop the data points.
     */
    private void terminatePoints() {
        List<Integer> pointIds = new ArrayList<>();

        pointListChangeLock.writeLock().lock();
        try {
            drainPendingPointsQueue(Common.timer.currentTimeMillis());
            for (DataPointRT p : dataPoints) {
                Common.runtimeManager.stopDataPoint(p.getId());
                pointIds.add(p.getId());
            }
            // clear all points out at once
            dataPointsMap.clear();
            pointListChanged = true;
        } finally {
            pointListChangeLock.writeLock().unlock();
        }

        //Terminate all events at once
        Common.eventManager.cancelEventsForDataPoints(pointIds);
    }

    /**
     * Waits for the data source to terminate.
     */
    @Override
    public final synchronized void joinTermination() {
        if (getLifecycleState() == ILifecycleState.TERMINATED) return;
        ensureState(ILifecycleState.TERMINATING);
        try {
            joinTerminationImpl();
            this.state = ILifecycleState.TERMINATED;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for data source to stop: " + readableIdentifier(), e);
        } finally {
            postTerminate();
        }
    }

    protected void joinTerminationImpl() throws InterruptedException {
    }
    protected void terminateImpl() {
    }

    /**
     * Hook for after termination is complete.
     */
    protected void postTerminate() {
        boolean anyActive = false;
        for (EventStatus status : eventTypes.values()) {
            if (status.active) {
                anyActive = true;
                break;
            }
        }

        if (anyActive) {
            // Remove any outstanding events after polling has stopped
            Common.eventManager.cancelEventsForDataSource(vo.getId());
        }
    }

    @Override
    public ILifecycleState getLifecycleState() {
        return state;
    }

    //
    // Additional lifecycle.
    public void beginPolling() {
        // no op
    }

    /**
     * Override to handle any situations where you need to know that a role was modified.
     *  be sure to call super for this method as it handles the edit and read permissions
     *
     * @param event dao event
     */
    public void handleRoleEvent(DaoEvent<? extends RoleVO> event) {
        if (event.getType() == DaoEventType.DELETE) {
            Role deletedRole = event.getVo().getRole();
            vo.setEditPermission(vo.getEditPermission().withoutRole(deletedRole));
            vo.setReadPermission(vo.getReadPermission().withoutRole(deletedRole));
        }
    }

    /**
     * Stores the event type and if it is active or not
     */
    private static class EventStatus {
        private final Object lock = new Object();
        private final DataSourceEventType eventType;
        private boolean active = false;

        private EventStatus(DataSourceEventType eventType) {
            this.eventType = eventType;
        }
    }

    public String readableIdentifier() {
        return String.format("Data source (name=%s, id=%d, type=%s)", getName(), getId(), getClass().getSimpleName());
    }

    protected final void forEachDataPoint(Consumer<? super DataPointRT> consumer) {
        pointListChangeLock.readLock().lock();
        try {
            dataPoints.forEach(consumer);
        } finally {
            pointListChangeLock.readLock().unlock();
        }
    }

    protected final DataPointRT getPointById(int id) {
        pointListChangeLock.readLock().lock();
        try {
            return dataPointsMap.get(id);
        } finally {
            pointListChangeLock.readLock().unlock();
        }
    }

    protected final void setAttribute(String key, Object value) {
        forEachDataPoint(point -> point.setAttribute(key, value));
    }

    /**
     * @return true if interval logging initialization should be inhibited when a data point is initialized.
     */
    public boolean inhibitIntervalLoggingInitialization() {
        return false;
    }
}
