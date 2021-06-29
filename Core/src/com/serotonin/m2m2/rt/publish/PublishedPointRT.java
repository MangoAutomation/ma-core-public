/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.rt.publish;

import java.util.HashMap;
import java.util.Map;

import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.rt.dataImage.DataPointListener;
import com.serotonin.m2m2.rt.dataImage.DataPointRT;
import com.serotonin.m2m2.rt.dataImage.PointValueTime;
import com.serotonin.m2m2.rt.dataSource.DataSourceRT;
import com.serotonin.m2m2.vo.DataPointVO;
import com.serotonin.m2m2.vo.publish.PublishedPointVO;
import com.serotonin.m2m2.vo.publish.PublisherVO.PublishType;

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
        DataPointRT rt = Common.runtimeManager.getDataPoint(vo.getDataPointId());
        pointEnabled = rt != null;
        publishAttributes(rt, false);
    }

    public void terminate() {
        Common.runtimeManager.removeDataPointListener(vo.getDataPointId(), this);
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

    public boolean isPointEnabled() {
        return pointEnabled;
    }
    @Override
    public void pointInitialized() {
        pointEnabled = true;
        parent.pointInitialized(this);
        DataPointRT rt = Common.runtimeManager.getDataPoint(vo.getDataPointId());
        if(rt != null)
            publishAttributes(rt, false);
    }
    @Override
    public void pointTerminated(DataPointVO dp) {
        pointEnabled = false;
        parent.pointTerminated(this);
        //Publish that its unreliable
        Map<String, Object> attributes = new HashMap<>();
        attributes.put(DataSourceRT.ATTR_UNRELIABLE_KEY, true);
        parent.attributeChanged(vo, attributes);
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
	
    public String getListenerName(){
    	return "Published Point With Id" + vo.getDataPointId();
    }
    
    public T getVo() {
        return vo;
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
                Map<String, Object> attributes = new HashMap<>(rt.getAttributes());
                attributes.put(DataSourceRT.ATTR_UNRELIABLE_KEY, unreliable);
                parent.attributeChanged(vo, attributes);
            }else {
                parent.attributeChanged(vo, rt.getAttributes());
            }
        }
    }
}
