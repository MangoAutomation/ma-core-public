/*
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.rt.dataSource;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.infiniteautomation.mango.io.serial.SerialPortException;
import com.serotonin.ShouldNeverHappenException;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.dao.DataSourceDao;
import com.serotonin.m2m2.db.dao.RoleDao.RoleDeletedDaoEvent;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.rt.dataImage.DataPointRT;
import com.serotonin.m2m2.rt.dataImage.PointValueTime;
import com.serotonin.m2m2.rt.dataImage.SetPointSource;
import com.serotonin.m2m2.rt.event.type.DataSourceEventType;
import com.serotonin.m2m2.vo.dataSource.DataSourceVO;
import com.serotonin.m2m2.vo.event.EventTypeVO;
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
    public static final String ATTR_UNRELIABLE_KEY = "UNRELIABLE";

    /**
     * Under the expectation that most data sources will run in their own threads, the addedPoints field is used as a
     * cache for points that have been added to the data source, so that at a convenient time for the data source they
     * can be included in the polling.
     *
     * Note that updated versions of data points that could already be running may be added here, so implementations
     * should always check for existing instances.
     */
    protected List<DataPointRT> addedChangedPoints = new ArrayList<DataPointRT>();

    /**
     * Under the expectation that most data sources will run in their own threads, the removedPoints field is used as a
     * cache for points that have been removed from the data source, so that at a convenient time for the data source
     * they can be removed from the polling.
     */
    protected List<DataPointRT> removedPoints = new ArrayList<DataPointRT>();

    /**
     * Access to either the addedPoints or removedPoints lists should be synchronized with this object's monitor.
     */
    protected final ReadWriteLock pointListChangeLock = new ReentrantReadWriteLock();

    private final List<DataSourceEventType> eventTypes;

    private boolean terminated;

    /* Thread safe set of active event types */
    private ConcurrentHashMap<Integer, Boolean> activeRtnEventTypes;

    protected final VO vo;



    public DataSourceRT(VO vo) {
        this.vo = vo;
        this.eventTypes = new ArrayList<DataSourceEventType>();
        for (EventTypeVO etvo : vo.getEventTypes()) {
            this.eventTypes.add((DataSourceEventType) etvo.getEventType());
        }
        this.activeRtnEventTypes = new ConcurrentHashMap<>();
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
     *
     * @param eventId
     * @param time
     * @param rtn - Can this event return to normal
     * @param message
     */
    protected void raiseEvent(int eventId, long time, boolean rtn, TranslatableMessage message) {
        message = new TranslatableMessage("event.ds", vo.getName(), message);
        DataSourceEventType type = getEventType(eventId);

        Map<String, Object> context = new HashMap<String, Object>();
        context.put("dataSource", vo);

        Common.eventManager.raiseEvent(type, time, rtn, type.getAlarmLevel(), message, context);
        if(rtn) {
            activeRtnEventTypes.compute(eventId, (k,v)->{
                return true;
            });
        }
    }

    protected void returnToNormal(int eventId, long time) {
        //For performance ensure we have an active event to RTN
        if(activeRtnEventTypes.compute(eventId, (k,v)->{
            if(v == null || v == false)
                return false;
            else
                return true;
        })) {
            DataSourceEventType type = getEventType(eventId);
            Common.eventManager.returnToNormal(type, time);
        }
    }

    private DataSourceEventType getEventType(int eventId) {
        for (DataSourceEventType et : eventTypes) {
            if (et.getDataSourceEventTypeId() == eventId)
                return et;
        }
        return null;
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
    /*
     * For future use if we want to allow some data sources to startup in safe mode
     *  will require RuntimeManagerChanges
     * (non-Javadoc)
     * @see com.serotonin.util.ILifecycle#initialize(boolean)
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

    @Override
    public void terminate() {
        terminated = true;

        // Remove any outstanding events.
        Common.eventManager.cancelEventsForDataSource(vo.getId());
    }

    @Override
    public void joinTermination() {
        // no op
    }

    //
    // Additional lifecycle.
    public void beginPolling() {
        // no op
    }

    /**
     * Override to handle any situations where you need to know that a role was deleted.
     *  be sure to call super for this method as it handles the edit permissions
     *
     * @param event
     */
    public void handleRoleDeletedEvent(RoleDeletedDaoEvent event) {
        if(vo.getEditPermission().containsRole(event.getRole().getRole())) {
            vo.setEditPermission(vo.getEditPermission().removeRole(event.getRole().getRole()));
        }
        if(vo.getReadPermission().containsRole(event.getRole().getRole())) {
            vo.setReadPermission(vo.getReadPermission().removeRole(event.getRole().getRole()));
        }
    }
}
