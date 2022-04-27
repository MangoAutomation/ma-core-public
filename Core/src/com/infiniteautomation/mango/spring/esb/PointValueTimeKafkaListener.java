/*
 * Copyright (C) 2022 Radix IoT LLC. All rights reserved.
 *
 *
 */

package com.infiniteautomation.mango.spring.esb;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

import com.infiniteautomation.mango.spring.components.RunAs;
import com.radixiot.pi.grpc.MangoPointValueTime;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.rt.dataImage.DataPointEventMulticaster;
import com.serotonin.m2m2.rt.dataImage.DataPointListener;
import com.serotonin.m2m2.rt.dataImage.PointValueTime;

@Component
public class PointValueTimeKafkaListener {

    /**
     * The list of point listeners, kept here such that listeners can be notified of point initializations (i.e. a
     * listener can register itself before the point is enabled).
     */
    private final ConcurrentMap<Integer, DataPointListener> dataPointListeners = new ConcurrentHashMap<>();


    @KafkaListener(topics = PointValueTimeTopic.TOPIC, groupId = "mango")
    protected void handleValue(Message<MangoPointValueTime> value) {
        MangoPointValueTime pvt = value.getPayload();
        DataPointListener listener = getDataPointListeners(pvt.getDataPointId());
        if (listener != null) {
            RunAs ra = Common.getBean(RunAs.class);
            ra.runAs(ra.systemSuperadmin(), () -> {
                PointValueTime newValue = new PointValueTime(pvt.getValue(), pvt.getTimestamp());
                // Updated
                listener.pointUpdated(newValue);

                // Fire if the point has changed.
                //if (!PointValueTime.equalValues(oldValue, newValue))
                listener.pointChanged(new PointValueTime(0, 0), newValue);

                // Fire if the point was set.
                //if (set)
                //    listener.pointSet(oldValue, newValue);
                // Was this value actually logged
                //if (logged)
                //    listener.pointLogged(newValue);
            });
        }
    }

    public void addDataPointListener(int dataPointId, DataPointListener l) {
        dataPointListeners.compute(dataPointId, (k, v) -> DataPointEventMulticaster.add(v, l));
    }

    public void removeDataPointListener(int dataPointId, DataPointListener l) {
        dataPointListeners.compute(dataPointId, (k, v) -> DataPointEventMulticaster.remove(v, l));
    }

    public DataPointListener getDataPointListeners(int dataPointId) {
        return dataPointListeners.get(dataPointId);
    }

}
