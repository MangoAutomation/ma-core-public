/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.rt.dataSource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

import org.checkerframework.checker.nullness.qual.NonNull;

import com.infiniteautomation.mango.io.serial.SerialPortException;
import com.infiniteautomation.mango.spring.events.DaoEvent;
import com.infiniteautomation.mango.spring.events.DaoEventType;
import com.serotonin.ShouldNeverHappenException;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.dao.DataSourceDao;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.rt.dataImage.DataPointRT;
import com.serotonin.m2m2.rt.dataImage.PointValueTime;
import com.serotonin.m2m2.rt.dataImage.SetPointSource;
import com.serotonin.m2m2.rt.event.type.DataSourceEventType;
import com.serotonin.m2m2.vo.dataSource.DataSourceVO;
import com.serotonin.m2m2.vo.dataSource.PollingDataSourceVO;
import com.serotonin.m2m2.vo.role.Role;
import com.serotonin.m2m2.vo.role.RoleVO;
import com.serotonin.util.ILifecycle;

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
    public static final String DATA_SOURCE_EVENT_CONTEXT_KEY = "dataSource";
    public static final String ATTR_UNRELIABLE_KEY = "UNRELIABLE";

    /**
     * Under the expectation that most data sources will run in their own threads, the addedPoints field is used as a
     * cache for points that have been added to the data source, so that at a convenient time for the data source they
     * can be included in the polling.
     *
     * Note that updated versions of data points that could already be running may be added here, so implementations
     * should always check for existing instances.
     */
    protected List<DataPointRT> addedChangedPoints = new ArrayList<>();

    /**
     * Under the expectation that most data sources will run in their own threads, the removedPoints field is used as a
     * cache for points that have been removed from the data source, so that at a convenient time for the data source
     * they can be removed from the polling.
     */
    protected List<DataPointRT> removedPoints = new ArrayList<>();

    /**
     * Access to either the addedPoints or removedPoints lists should be synchronized with this object's monitor.
     */
    protected final ReadWriteLock pointListChangeLock = new ReentrantReadWriteLock();

    /**
     * Stores a map of data source event type ids to the {@link EventStatus}
     */
    private final Map<Integer, EventStatus> eventTypes;

    private boolean terminated;

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

    protected boolean isTerminated() {
        return terminated;
    }

    public void addDataPoint(DataPointRT dataPoint) {
        pointListChangeLock.readLock().lock();
        try {
            //Further synchronize with other readers
            synchronized(pointListChangeLock) {
                addedChangedPoints.remove(dataPoint);
                addedChangedPoints.add(dataPoint);
                removedPoints.remove(dataPoint);
            }
        } finally {
            pointListChangeLock.readLock().unlock();
        }
    }

    public void removeDataPoint(DataPointRT dataPoint) {
        pointListChangeLock.readLock().lock();
        try {
            //Further synchronize with other readers
            synchronized(pointListChangeLock) {
                addedChangedPoints.remove(dataPoint);
                removedPoints.add(dataPoint);
            }
        } finally {
            pointListChangeLock.readLock().unlock();
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
        throw new ShouldNeverHappenException("not implemented in " + getClass());
    }

    public void forcePointRead(DataPointRT dataPoint) {
        // No op by default. Override as required.
    }

    public void forcePoll() {
        // No op by default. Override as required.
    }

    /**
     * Raises a data source event.
     *
     * @param dataSourceEventTypeId Must be registered via {@link PollingDataSourceVO#addEventTypes(java.util.List)}
     * @param time time at which the event will be raised
     * @param rtn true if the event can return to normal (become inactive) at some point in the future
     * @param message translatable event message
     */
    protected void raiseEvent(int dataSourceEventTypeId, long time, boolean rtn, TranslatableMessage message) {
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
     */
    protected void returnToNormal(int dataSourceEventTypeId, long time) {
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
    public void initialize(boolean safe) {
        if(!safe)
            initialize();
    }

    /**
     * Initialize this data source
     */
    public void initialize(){
        // no op
    }

    /**
     * Hook to know when all points are added, the source is fully initialized
     */
    public void initialized() {

    }

    /**
     * Hook for just before points get removed from the data source
     */
    public void terminating() {

    }

    @Override
    public synchronized void terminate() {
        terminated = true;
    }

    /**
     * Waits for the data source to terminate.
     * @throws IllegalStateException if the data source never terminates
     */
    @Override
    public void joinTermination() {
        // no op
    }

    /**
     * Hook for after termination is complete.
     * @param gracefullyTerminated true if the data source terminated gracefully
     */
    public synchronized void postTerminate(boolean gracefullyTerminated) {
        // Remove any outstanding events after polling has stopped
        Common.eventManager.cancelEventsForDataSource(vo.getId());
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
}
