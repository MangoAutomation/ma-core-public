/*
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.rt.publish;

import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.rt.dataImage.DataPointListener;
import com.serotonin.m2m2.rt.dataImage.PointValueTime;
import com.serotonin.m2m2.vo.publish.PublishedPointVO;

/**
 * @author Matthew Lohbihler
 */
public class PublishedPointRT<T extends PublishedPointVO> implements DataPointListener {
    private final T vo;
    private final PublisherRT<T> parent;
    private boolean pointEnabled;

    public PublishedPointRT(T vo, PublisherRT<T> parent) {
        this.vo = vo;
        this.parent = parent;
        Common.runtimeManager.addDataPointListener(vo.getDataPointId(), this);
        pointEnabled = Common.runtimeManager.isDataPointRunning(vo.getDataPointId());
    }

    public void terminate() {
        Common.runtimeManager.removeDataPointListener(vo.getDataPointId(), this);
    }

    public void pointChanged(PointValueTime oldValue, PointValueTime newValue) {
        if (parent.getVo().isChangesOnly())
            parent.publish(vo, newValue);
    }

    public void pointSet(PointValueTime oldValue, PointValueTime newValue) {
        // no op. Everything gets handled in the other methods.
    }

    public void pointUpdated(PointValueTime newValue) {
        if (!parent.getVo().isChangesOnly())
            parent.publish(vo, newValue);
    }

    public void pointBackdated(PointValueTime value) {
        // no op
    }

    public boolean isPointEnabled() {
        return pointEnabled;
    }

    public void pointInitialized() {
        pointEnabled = true;
        parent.pointInitialized(this);
    }

    public void pointTerminated() {
        pointEnabled = false;
        parent.pointTerminated(this);
    }

    public T getVo() {
        return vo;
    }
}
