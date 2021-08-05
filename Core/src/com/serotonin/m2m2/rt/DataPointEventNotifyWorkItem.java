/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 * @Author Terry Packer
 *
 */

package com.serotonin.m2m2.rt;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.serotonin.m2m2.rt.dataImage.DataPointListener;
import com.serotonin.m2m2.rt.dataImage.PointValueTime;
import com.serotonin.m2m2.rt.maint.work.WorkItem;
import com.serotonin.m2m2.util.ExceptionListWrapper;
import com.serotonin.timer.RejectedTaskReason;
import com.serotonin.timer.Task;

public class DataPointEventNotifyWorkItem implements WorkItem {

    private static final Logger LOG = LoggerFactory.getLogger(DataPointEventNotifyWorkItem.class);

    private static final String descriptionPrefix = "Point event for: ";
    private static final String prefix = "EN-";
    private final String sourceXid;
    private final DataPointListener listener;
    private final PointValueTime oldValue;
    private final PointValueTime newValue;
    private final Map<String, Object> attributes;
    private final boolean set;
    private final boolean backdate;
    private final boolean logged;
    private final boolean updated;
    private final boolean attributesChanged;
    private final String taskId;

    public DataPointEventNotifyWorkItem(String xid, DataPointListener listener, PointValueTime oldValue, PointValueTime newValue, Map<String, Object> attributes, boolean set,
                                        boolean backdate, boolean logged, boolean updated, boolean attributesChanged) {
        this.sourceXid = xid;
        this.listener = listener;
        this.oldValue = oldValue;
        this.newValue = newValue;
        this.attributes = attributes;
        this.set = set;
        this.backdate = backdate;
        this.logged = logged;
        this.updated = updated;
        this.attributesChanged = attributesChanged;
        this.taskId = prefix + sourceXid + "-" + listener.hashCode();
    }

    @Override
    public void execute() {
        try {
            if (attributesChanged) {
                listener.attributeChanged(attributes);
                return;
            }

            if (backdate)
                listener.pointBackdated(newValue);
            else if (updated) {
                // Updated
                listener.pointUpdated(newValue);

                // Fire if the point has changed.
                if (!PointValueTime.equalValues(oldValue, newValue))
                    listener.pointChanged(oldValue, newValue);

                // Fire if the point was set.
                if (set)
                    listener.pointSet(oldValue, newValue);
            }

            // Was this value actually logged
            if (logged)
                listener.pointLogged(newValue);
        } catch (ExceptionListWrapper e) {
            LOG.warn("Exceptions in event notify work item.");
            for (Exception e2 : e.getExceptions())
                LOG.warn("Listener exception: " + e2.getMessage(), e2);
        }
    }

    @Override
    public int getPriority() {
        return WorkItem.PRIORITY_MEDIUM;
    }

    @Override
    public String getDescription() {
        return descriptionPrefix + sourceXid;
    }

    @Override
    public String getTaskId() {
        //So there is one task for each listener
        return taskId;
    }

    @Override
    public int getQueueSize() {
        return Task.UNLIMITED_QUEUE_SIZE;
    }

    @Override
    public void rejected(RejectedTaskReason reason) {
        //No special handling, tracking/logging is handled by the WorkItemRunnable
    }

}
