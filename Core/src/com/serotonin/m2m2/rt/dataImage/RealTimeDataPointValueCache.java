/**
 * Copyright (C) 2013 Infinite Automation Software. All rights reserved.
 *
 * @author Terry Packer
 */
package com.serotonin.m2m2.rt.dataImage;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.infiniteautomation.mango.db.query.SortOption;
import com.infiniteautomation.mango.spring.service.PermissionService;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.vo.User;

/**
 *
 * TODO Mango 4.0 This will require a re-design to track existing and new data points so 
 *  we know when they are started/stopped/etc.
 * 
 * Real Time Data Cache that contains the most recent value for all data points in the Point
 * Hierarchy. This was previously on a per user basis but now all points are registered and
 * permissions are used to extract the data.
 *
 * Available properties defined in RealTimeDataPointValue
 *
 * Changes to data point configurations are only picked up when the point hierarchy is saved.
 *
 * @author Terry Packer
 *
 */
public class RealTimeDataPointValueCache {
    private static final Log LOG = LogFactory.getLog(DataPointRT.class);

    private final List<RealTimeDataPointValue> realTimeData =
            new CopyOnWriteArrayList<RealTimeDataPointValue>();
    private final PermissionService permissionService;
    // Singleton Instance
    public static final RealTimeDataPointValueCache instance = new RealTimeDataPointValueCache();


    /**
     * Singleton Constructor to create cache and register with the P.H. to capture any changes
     */
    private RealTimeDataPointValueCache() {
        this.permissionService = Common.getBean(PermissionService.class);
    }

    /**
     * Get all the points a user can see based on permissions
     * @param user
     * @return
     */
    public List<RealTimeDataPointValue> getUserView(User user) {
        List<RealTimeDataPointValue> results = new ArrayList<RealTimeDataPointValue>();
        
        //Filter on permissions
        for (RealTimeDataPointValue rtdpv : this.realTimeData) {
            if(permissionService.hasDataPointReadPermission(user, rtdpv.vo))
                results.add(rtdpv);
        }

        return results;
    }


    class RealTimeDataComparator implements Comparator<RealTimeDataPointValue> {

        public RealTimeDataComparator(SortOption sort) {
            this.sort = sort;
        }

        private SortOption sort;

        @SuppressWarnings("unchecked")
        @Override
        public int compare(RealTimeDataPointValue o1, RealTimeDataPointValue o2) {

            try {
                Object v1 = PropertyUtils.getProperty(o1, sort.getAttribute());
                Comparable<Comparable<?>> c1;
                if (v1 instanceof Comparable)
                    c1 = (Comparable<Comparable<?>>) v1;
                else
                    return 0;
                Object v2 = PropertyUtils.getProperty(o2, sort.getAttribute());
                Comparable<?> c2;
                if (v2 instanceof Comparable)
                    c2 = (Comparable<?>) v2;
                else
                    return 0;
                return c1.compareTo(c2);
            } catch (SecurityException | IllegalArgumentException | IllegalAccessException
                    | InvocationTargetException | NoSuchMethodException e) {
                LOG.warn("Bad Sort Parameter.", e);
            }
            return 0;
        }
    }

}


