/**
 * Copyright (C) 2013 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.rt.dataImage;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.infiniteautomation.mango.db.query.QueryComparison;
import com.infiniteautomation.mango.db.query.SortOption;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.dao.DataPointDao;
import com.serotonin.m2m2.i18n.Translations;
import com.serotonin.m2m2.vo.DataPointExtendedNameComparator;
import com.serotonin.m2m2.vo.DataPointSummary;
import com.serotonin.m2m2.vo.DataPointVO;
import com.serotonin.m2m2.vo.hierarchy.PointFolder;
import com.serotonin.m2m2.vo.hierarchy.PointHierarchy;
import com.serotonin.m2m2.vo.hierarchy.PointHierarchyEventDispatcher;
import com.serotonin.m2m2.vo.hierarchy.PointHierarchyListener;
import com.serotonin.m2m2.vo.permission.Permissions;

/**
 * 
 * Real Time Data Cache that contains the most recent value for all
 * data points in the Point Hierarchy.  This was previously on a per user basis
 * but now all points are registered and permissions are used to extract the data.
 *
 * Available properties defined in RealTimeDataPointValue
 * 
 * @author Terry Packer
 *
 */
public class RealTimeDataPointValueCache {
	private static final Log LOG = LogFactory.getLog(DataPointRT.class);

	private final List<RealTimeDataPointValue> realTimeData = new CopyOnWriteArrayList<RealTimeDataPointValue>();
	//Singleton Instance
	public static final RealTimeDataPointValueCache instance = new RealTimeDataPointValueCache();
	
	

	/**
	 * Singleton Constructor to create cache
	 * and register with the P.H. to capture any changes
	 */
    private RealTimeDataPointValueCache() {
    	
    	//Initially load in the PH.
    	PointHierarchy ph = createPointHierarchy(Common.getTranslations());
		PointFolder root = ph.getRoot();
		//Fill the cache now
		fillCache(root,realTimeData);
    	
		//Register so we know when it changes
        PointHierarchyEventDispatcher.addListener(new PointHierarchyListener() {
            @Override
            public void pointHierarchyCleared() {
            	realTimeData.clear();
            }

            @Override
            public void pointHierarchySaved(PointFolder root) {
            	realTimeData.clear();
    			//Fill the cache now
    			fillCache(root,realTimeData);

            }
        });
    }
	
	/**
	 * Query the cache
	 * 
	 * properties that are queryable:
	 * deviceName
	 * pointName
	 * pointValue
	 * unit
	 * renderedValue
	 * timestamp
	 * pointType
	 * status
	 * path
	 * xid
	 * 
	 * 
	 * 
	 * @param andComparisons
	 * @param orComparisons
	 * @param sort
	 * @param permissions - String of comma separated permissions used to limit the sort
	 * @return
	 */
	public List<RealTimeDataPointValue> query(List<QueryComparison> andComparisons, List<QueryComparison> orComparisons, 
			List<SortOption> sorts, Integer limit, String permissions){
		List<RealTimeDataPointValue> results = new ArrayList<RealTimeDataPointValue>();
		
		boolean keep = true;
		for(RealTimeDataPointValue rtdpv : this.realTimeData){
			if(!Permissions.hasPermission(permissions, rtdpv.getReadPermission()))
				continue; //Only query those points we can keep
			
			//All must be true to keep
			for(QueryComparison comparison : andComparisons){
				try {
					keep = comparison.apply(rtdpv);
				} catch (IllegalArgumentException | IllegalAccessException
						| NoSuchFieldException | NoSuchMethodException | InvocationTargetException | SecurityException e) {
					LOG.warn("Bad Query attribute", e);
					keep = false;
				}
				if(!keep)
					break; //We failed to pass our criteria
			}
			
			if(!keep)
				continue; //We failed so move on to next
			
			//Only one must be true to keep
			if(orComparisons.size() > 0)
				keep = false;
			for(QueryComparison comparison : orComparisons){
				try {
					keep = comparison.apply(rtdpv);
				} catch (IllegalArgumentException | IllegalAccessException
						| NoSuchFieldException | SecurityException | InvocationTargetException | NoSuchMethodException e) {
					LOG.warn("Bad Query attribute", e);
					keep = false;
				}
			}
			
			
			if(keep)
				results.add(rtdpv);
			else
				keep = true; //reset 
		}
		
		//Perform the sort
		for(SortOption sort : sorts){
			Collections.sort(results, new RealTimeDataComparator(sort));
		}
		
		if(limit!=null){
			if(results.size() > limit)
				return results.subList(0, limit);
		}
		
		
		return results;
	}
	
	/**
	 * Create a point hierarchy for this user out of all points they can read
	 * @param translations
	 * @param user
	 * @return
	 */
    private static PointHierarchy createPointHierarchy(Translations translations) {

        // Create a point hierarchy for the user.
		PointHierarchy ph = DataPointDao.instance.getPointHierarchy(true).copyFoldersOnly();
        List<DataPointVO> points = DataPointDao.instance.getDataPoints(DataPointExtendedNameComparator.instance, false);
        for (DataPointVO point : points){
        	ph.addDataPoint(point.getPointFolderId(), new DataPointSummary(point));
        }
        ph.parseEmptyFolders();
        
        return ph;
    }
	
    private static void fillCache(PointFolder root, List<RealTimeDataPointValue> cache){
    	recursivelyFillCache(root, root, cache);
    }
    
	/**
	 * Fill the real time cache from the point hierarchy
	 * 
	 * @param hierarchy
	 * @param folder
	 */
	private static void recursivelyFillCache(PointFolder root, PointFolder folder,
			List<RealTimeDataPointValue> cache) {
		for(DataPointSummary summary : folder.getPoints()){
			//Here we can add all points or just running ones
			DataPointRT runningPoint = Common.runtimeManager.getDataPoint(summary.getId());
			if(runningPoint != null)
				cache.add(new RealTimeDataPointValue(runningPoint,PointHierarchy.getPath(summary.getId(), root)));
		}
		for(PointFolder subFolder : folder.getSubfolders()){
			recursivelyFillCache(root, subFolder, cache);
		}
	}
	
	class RealTimeDataComparator implements Comparator<RealTimeDataPointValue>{
		
		public RealTimeDataComparator(SortOption sort){
			this.sort = sort;
		}

		private SortOption sort;
		
		/* (non-Javadoc)
		 * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
		 */
		@SuppressWarnings("unchecked")
		@Override
		public int compare(RealTimeDataPointValue o1, RealTimeDataPointValue o2) {
			
			try {
				Object v1 = PropertyUtils.getProperty(o1, sort.getAttribute());
				Comparable<Comparable<?>> c1;
				if(v1 instanceof Comparable)
					c1 = (Comparable<Comparable<?>>)v1;
				else
					return 0;
				Object v2 = PropertyUtils.getProperty(o2, sort.getAttribute());
				Comparable<?> c2;
				if(v2 instanceof Comparable)
					c2 = (Comparable<?>)v2;
				else
					return 0;
				return c1.compareTo(c2);
			} catch (SecurityException | IllegalArgumentException | 
					IllegalAccessException | InvocationTargetException | 
					NoSuchMethodException e) {
				LOG.warn("Bad Sort Parameter.", e);
			}
			return 0;
		}
	}
	
}


	
