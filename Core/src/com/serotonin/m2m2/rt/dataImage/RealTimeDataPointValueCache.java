/**
 * Copyright (C) 2013 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.rt.dataImage;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.infiniteautomation.mango.db.query.QueryComparison;
import com.infiniteautomation.mango.db.query.SortOption;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.dao.DataPointDao;
import com.serotonin.m2m2.i18n.Translations;
import com.serotonin.m2m2.vo.DataPointExtendedNameComparator;
import com.serotonin.m2m2.vo.DataPointSummary;
import com.serotonin.m2m2.vo.DataPointVO;
import com.serotonin.m2m2.vo.User;
import com.serotonin.m2m2.vo.hierarchy.PointFolder;
import com.serotonin.m2m2.vo.hierarchy.PointHierarchy;
import com.serotonin.m2m2.vo.hierarchy.PointHierarchyEventDispatcher;
import com.serotonin.m2m2.vo.hierarchy.PointHierarchyListener;
import com.serotonin.m2m2.vo.permission.Permissions;

/**
 * 
 * Real Time Data Cache that contains the most recent value for all
 * data points in the Point Hierarchy on a Per-User basis
 *
 * Available properties defined in RealTimeDataPointValue
 * 
 * @author Terry Packer
 *
 */
public class RealTimeDataPointValueCache {

	//List for real time data on a per user basis
	private final Map<User, List<RealTimeDataPointValue>> userRealTimeData = new ConcurrentHashMap<User, List<RealTimeDataPointValue>>();
	
	//Singleton Instance
	public static final RealTimeDataPointValueCache instance = new RealTimeDataPointValueCache();
	
	

	/**
	 * Singleton Constructor to create cache
	 * and register with the P.H. to 
	 */
    private RealTimeDataPointValueCache() {
    	
        PointHierarchyEventDispatcher.addListener(new PointHierarchyListener() {
            @Override
            public void pointHierarchyCleared() {
                userRealTimeData.clear();
            }

            @Override
            public void pointHierarchySaved(PointFolder root) {
                userRealTimeData.clear();
            }
        });
    }
	
	/**
	 * Get all points the user can read
	 * 
	 * @param user 
	 * @return
	 */
	public List<RealTimeDataPointValue> getAll(User user) {
		List<RealTimeDataPointValue> cache = getCache(user);
		
		return cache;
	}
    
	/**
	 * Get a point matching this xid
	 * 
	 * @param xid
	 * @param user
	 * @return
	 */
	public RealTimeDataPointValue get(
			String xid, User user) {
		List<RealTimeDataPointValue> cache = getCache(user);
		
		for(RealTimeDataPointValue rtpv : cache){
			if(rtpv.getXid().equals(xid))
				return rtpv;
		}
		
		return null;
	}
	
	/**
	 * Query a User's cache of points 
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
	 * @param user
	 * @return
	 */
	public List<RealTimeDataPointValue> query(List<QueryComparison> andComparisons, List<QueryComparison> orComparisons, List<SortOption> sort, User user){
		List<RealTimeDataPointValue> results = new ArrayList<RealTimeDataPointValue>();
		
		List<RealTimeDataPointValue> cache = getCache(user);
		boolean keep = false;
		for(RealTimeDataPointValue rtdpv : cache){
			
			//All must be true to keep
			for(QueryComparison comparison : andComparisons){
				switch(comparison.getComparisonType()){
				case QueryComparison.CONTAINS:
				case QueryComparison.EQUAL_TO:
				case QueryComparison.GREATER_THAN:
				case QueryComparison.GREATER_THAN_EQUAL_TO:
				case QueryComparison.IN:
				case QueryComparison.LESS_THAN:
				case QueryComparison.LESS_THAN_EQUAL_TO:
				case QueryComparison.LIKE:
				}
			}
			
			//Only one must be true to keep
			for(QueryComparison comparison : orComparisons){
				
			}
			
			
			if(keep)
				results.add(rtdpv);
		}
		
		//TODO Sort list
		
		return results;
	}
	
	/**
	 * 
	 */
	private List<RealTimeDataPointValue> getCache(User user) {
		//First check to see if we have a cache for this user, if not then create one
		List<RealTimeDataPointValue> cache = this.userRealTimeData.get(user);
		if(cache == null){
			PointHierarchy ph = createUserPointHierarchy(Common.getTranslations(),user);
			PointFolder root = ph.getRoot();
			//Fill the cache now
			cache = new ArrayList<RealTimeDataPointValue>();
			recursivelyFillCache(ph,root,cache);
			this.userRealTimeData.put(user, cache);
		}
		return cache;
	}
	
	/**
	 * Create a point hierarchy for this user out of all points they can read
	 * @param translations
	 * @param user
	 * @return
	 */
    private PointHierarchy createUserPointHierarchy(Translations translations, User user) {

        // Create a point hierarchy for the user.
		PointHierarchy ph = DataPointDao.instance.getPointHierarchy(true).copyFoldersOnly();
        List<DataPointVO> points = DataPointDao.instance.getDataPoints(DataPointExtendedNameComparator.instance, false);
        for (DataPointVO point : points) {
            if (Permissions.hasDataPointReadPermission(user, point))
                ph.addDataPoint(point.getPointFolderId(), new DataPointSummary(point));
        }
        ph.parseEmptyFolders();
        
        return ph;
    }
	
	/**
	 * Fill the real time cache from the point hierarchy
	 * 
	 * @param hierarchy
	 * @param folder
	 */
	private void recursivelyFillCache(PointHierarchy hierarchy, PointFolder folder,
			List<RealTimeDataPointValue> cache) {
		for(DataPointSummary summary : folder.getPoints()){
			//Here we can add all points or just running ones
			DataPointRT runningPoint = Common.runtimeManager.getDataPoint(summary.getId());
			if(runningPoint != null)
				cache.add(new RealTimeDataPointValue(runningPoint,hierarchy.getPath(summary.getId())));
		}
		for(PointFolder subFolder : folder.getSubfolders()){
			recursivelyFillCache(hierarchy, subFolder, cache);
		}
	}



}
