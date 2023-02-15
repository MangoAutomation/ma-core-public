/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.rt.publish;

import java.util.HashMap;
import java.util.Map;

import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.dao.PublishedPointDao;
import com.serotonin.m2m2.rt.dataImage.DataPointListener;
import com.serotonin.m2m2.rt.dataImage.DataPointRT;
import com.serotonin.m2m2.rt.dataImage.PointValueTime;
import com.serotonin.m2m2.rt.dataSource.DataSourceRT;
import com.serotonin.m2m2.vo.DataPointVO;
import com.serotonin.m2m2.vo.publish.PublishedPointVO;
import com.serotonin.m2m2.vo.publish.PublisherVO.PublishType;
import com.serotonin.util.ILifecycle;
import com.serotonin.util.ILifecycleState;

/**
 * @author Matthew Lohbihler
 */
public class PublishedPointRT<T extends PublishedPointVO> implements DataPointListener, ILifecycle {
    private final T vo;
    private final PublisherRT parent;
    private boolean pointEnabled;
    private final PublishedPointDao publishedPointDao;
    private volatile ILifecycleState state = ILifecycleState.PRE_INITIALIZE;

    public PublishedPointRT(T vo, PublisherRT parent) {
        this.vo = vo;
        this.parent = parent;
        this.publishedPointDao = Common.getBean(PublishedPointDao.class);
    }

    /**
     * Initialize by adding a data point listener and
     *   publishing the initial attributes
     */
    @Override
    public final synchronized void initialize(boolean safe) {
        ensureState(ILifecycleState.PRE_INITIALIZE);
        this.state = ILifecycleState.INITIALIZING;
        notifyStateChanged();

        try {
            //Add ourselves to the runtime
            parent.addPublishedPoint(this);

            Common.runtimeManager.addDataPointListener(vo.getDataPointId(), this);
            DataPointRT rt = Common.runtimeManager.getDataPoint(vo.getDataPointId());
            pointEnabled = rt != null;
            publishAttributes(rt, false);
        }catch(Exception e) {
            try {
                terminate();
                joinTermination();
            }catch(Exception e1) {
                e.addSuppressed(e1);
            }
            throw e;
        }

        this.state = ILifecycleState.RUNNING;
        notifyStateChanged();
    }

    @Override
    public final synchronized void terminate() {
        ensureState(ILifecycleState.INITIALIZING, ILifecycleState.RUNNING);
        this.state = ILifecycleState.TERMINATING;
        notifyStateChanged();

        boolean parentTerminating = parent.getLifecycleState() == ILifecycleState.TERMINATING;
        if (!parentTerminating) {
            // Publisher clears all its points at once when it is terminating
            parent.removePublishedPoint(this);
        }

        Common.runtimeManager.removeDataPointListener(vo.getDataPointId(), this);

        this.state = ILifecycleState.TERMINATED;
        notifyStateChanged();
        Common.runtimeManager.removePublishedPoint(this);
    }

    @Override
    public void joinTermination() {

    }

    @Override
    public void pointChanged(PointValueTime oldValue, PointValueTime newValue) {
        if (parent.getVo().getPublishType() == PublishType.CHANGES_ONLY)
            parent.publish(vo, newValue);
    }

    @Override
    public void pointSet(PointValueTime oldValue, PointValueTime newValue) {
        // no op. Everything gets handled in the other methods.
    }

    @Override
    public void pointUpdated(PointValueTime newValue) {
        if (parent.getVo().getPublishType() == PublishType.ALL)
            parent.publish(vo, newValue);
    }

    @Override
    public void pointBackdated(PointValueTime value) {
        // no op
    }

    @Override
    public void pointInitialized() {
        pointEnabled = true;
        parent.dataPointInitialized(this);
        DataPointRT rt = Common.runtimeManager.getDataPoint(vo.getDataPointId());
        if(rt != null)
            publishAttributes(rt, false);
    }
    @Override
    public void pointTerminated(DataPointVO dp) {
        pointEnabled = false;
        parent.dataPointTerminated(this, dp);
        //Publish that its unreliable
        parent.attributeChanged(vo, Map.of(DataSourceRT.ATTR_UNRELIABLE_KEY, true));
    }

    @Override
    public void attributeChanged(Map<String, Object> attributes) {
        parent.attributeChanged(vo, attributes);
    }
    
	@Override
	public void pointLogged(PointValueTime value) {
		if(parent.getVo().getPublishType() == PublishType.LOGGED_ONLY)
			parent.publish(vo, value);
	}

    public boolean isPointEnabled() {
        return pointEnabled;
    }

    public String getListenerName(){
    	return "Published Point With Id" + vo.getDataPointId();
    }
    
    public T getVo() {
        return vo;
    }

    public int getId() {
        return vo.getId();
    }
    
    /**
     * Helper to publish attributes ensuring that the UNRELIABLE flag exists
     *
     * @param rt - can be null (nothing done if null)
     * @param unreliable - if missing from rt's attributes what state to use
     */
    protected void publishAttributes(DataPointRT rt, boolean unreliable) {
        if(rt != null && parent.getVo().isPublishAttributeChanges()) {
            //Ensure that the reliability attribute exists, if DNE then assume reliable.
            if(rt.getAttribute(DataSourceRT.ATTR_UNRELIABLE_KEY) == null) {
                Map<String, Object> attributes;
                if (rt.getAttributes().isEmpty()) {
                    attributes = Map.of(DataSourceRT.ATTR_UNRELIABLE_KEY, unreliable);
                } else {
                    attributes = new HashMap<>(rt.getAttributes());
                    attributes.put(DataSourceRT.ATTR_UNRELIABLE_KEY, unreliable);
                }
                parent.attributeChanged(vo, attributes);
            }else {
                parent.attributeChanged(vo, rt.getAttributes());
            }
        }
    }

    private void notifyStateChanged() {
        if (parent.getLifecycleState() == ILifecycleState.RUNNING) {
            publishedPointDao.notifyStateChanged(vo, this.state);
        }
    }

    @Override
    public String readableIdentifier() {
        return String.format("Published point (name=%s, id=%d, type=%s)", vo.getName(), getId(), getClass().getSimpleName());
    }

    @Override
    public ILifecycleState getLifecycleState() {
        return state;
    }
}
