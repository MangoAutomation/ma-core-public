/*
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.rt.dataImage;

import java.util.List;

import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.dao.PointValueDao;

/**
 * @author Matthew Lohbihler
 */
public class PointValueFacade {
    private final int dataPointId;
    private final DataPointRT point;
    private final PointValueDao pointValueDao;
    private boolean useCache;
    
    public PointValueFacade(int dataPointId, boolean useCache) {
        this.dataPointId = dataPointId;
        point = Common.runtimeManager.getDataPoint(dataPointId);
        pointValueDao = Common.databaseProxy.newPointValueDao();
        this.useCache = useCache;
    }

    public PointValueFacade(int dataPointId) {
        this.dataPointId = dataPointId;
        point = Common.runtimeManager.getDataPoint(dataPointId);
        pointValueDao = Common.databaseProxy.newPointValueDao();
        this.useCache = true;
    }
    
    //
    //
    // Single point value
    //
    public PointValueTime getPointValueBefore(long time) {
        if ((point != null)&&(useCache))
            return point.getPointValueBefore(time);
        return pointValueDao.getPointValueBefore(dataPointId, time);
    }

    public PointValueTime getPointValueAt(long time) {
    	if ((point != null)&&(useCache))
            return point.getPointValueAt(time);
        return pointValueDao.getPointValueAt(dataPointId, time);
    }

    public PointValueTime getPointValueAfter(long time) {
    	if ((point != null)&&(useCache))
            return point.getPointValueAfter(time);
        return pointValueDao.getPointValueAfter(dataPointId, time);
    }

    public PointValueTime getPointValue() {
    	if ((point != null)&&(useCache))
            return point.getPointValue();
        return pointValueDao.getLatestPointValue(dataPointId);
    }

    //
    //
    // Point values lists
    //
    public List<PointValueTime> getPointValues(long since) {
    	if ((point != null)&&(useCache))
            return point.getPointValues(since);
        return pointValueDao.getPointValues(dataPointId, since);
    }

    /**
     * Gets point values since a certain time, can add an initial value and final value to the returned
     * results. Useful for creating graphs.
     * @param since
     * 			epoch time in ms
     * @param insertInitial
     * 			fetch the previous point value before 'since', and insert it at time 'since' 
     * @param insertFinal
     * 			take the last point value from the list and insert it at the current time
     * @return
     */
    public List<PointValueTime> getPointValues(long since, boolean insertInitial, boolean insertFinal) {
        List<PointValueTime> list = getPointValues(since);
        
        if (insertInitial && !list.isEmpty()) {
            PointValueTime prevValue = getPointValueBefore(since);
            
            // don't insert the initial value if it already exists
            if (prevValue != null && list.get(0).getTime() != since) {
                list.add(0, new PointValueTime(prevValue.getValue(), since));
            }
        }
        
        if (insertFinal && !list.isEmpty()) {
        	PointValueTime finalValue = list.get(list.size()-1);
        	long endTime = System.currentTimeMillis();
        	
        	// don't insert the final value if it already exists
        	if (finalValue != null && finalValue.getTime() != endTime) {
                list.add(new PointValueTime(finalValue.getValue(), endTime));
        	}
        }
        
        return list;
    }
    
    public List<PointValueTime> getPointValuesBetween(long from, long to) {
    	if ((point != null)&&(useCache))
            return point.getPointValuesBetween(from, to);
        return pointValueDao.getPointValuesBetween(dataPointId, from, to);
    }

    /**
     * Gets point values in a certain time period, can add an initial value and final value to the returned
     * results. Useful for creating graphs.
     * @param from
     * 			epoch time in ms
     * @param to
     * 			epoch time in ms
     * @param insertInitial
     * 			fetch the previous point value before 'from', and insert it at time 'from' 
     * @param insertFinal
     * 			take the last point value from the list and insert it at time 'to'
     * @return
     */
    public List<PointValueTime> getPointValuesBetween(long from, long to, boolean insertInitial, boolean insertFinal) {
        List<PointValueTime> list = getPointValuesBetween(from, to);
        
        if (insertInitial && !list.isEmpty()) {
            PointValueTime prevValue = getPointValueBefore(from);
            
            // don't insert the initial value if it already exists
            if (prevValue != null && list.get(0).getTime() != from) {
                list.add(0, new PointValueTime(prevValue.getValue(), from));
            }
        }
        
        if (insertFinal && !list.isEmpty()) {
        	PointValueTime finalValue = list.get(list.size()-1);
        	// don't insert final value in the future
        	long endTime = to <= System.currentTimeMillis() ? to : System.currentTimeMillis();
        	
        	// don't insert the final value if it already exists
        	if (finalValue != null && finalValue.getTime() != endTime) {
                list.add(new PointValueTime(finalValue.getValue(), endTime));
        	}
        }
        
        return list;
    }

    public List<PointValueTime> getLatestPointValues(int limit) {
    	if ((point != null)&&(useCache))
            return point.getLatestPointValues(limit);
        return pointValueDao.getLatestPointValues(dataPointId, limit);
    }
    
    /**
	 * return the give value interval List,don't use this at exactly place
	 * 
	 * @param since
	 * @param interval
	 * @return
	 */
	public List<PointValueTime> getPointValues(long since, long interval) {
		List<PointValueTime> originalValues = getPointValues(since);
		return InsertIntervalValue(originalValues, since, System.currentTimeMillis(), interval);
	}
	
	private List<PointValueTime> InsertIntervalValue(List<PointValueTime> sources, long from, long to, long interval) {
		List<Long> PVtimes = new ArrayList<Long>();
		List<PointValueTime> resultPvt = new ArrayList<PointValueTime>();
		int originalSize = sources.size();

		// have the value
		if (originalSize > 0) {
			for (Long l = from; l <= to; l = l + interval) {
				PVtimes.add(l);
			}

			int PvtCursor = PVtimes.size() - 1;
			for (int ioriginal = originalSize - 1; ioriginal >= 0; ioriginal--) {
				while (PvtCursor >= 0 && sources.get(ioriginal).getTime() <= PVtimes.get(PvtCursor)) {
					resultPvt.add(new PointValueTime(sources.get(ioriginal).getValue(), PVtimes.get(PvtCursor)));
					PvtCursor--;
				}
			}
			return ChangeUpDown(resultPvt);
		} else {
			// actually,don't have the value
			return null;
		}
	}

    /**
	 * Fore and aft switch one list 
	 * @param resultPvt
	 * @return
	 */
	private <T> List<T> ChangeUpDown(List<T> resultPvt) {
		if (resultPvt != null && resultPvt.size() > 0) {
			int isize = resultPvt.size();
			List<T> resultList = new ArrayList<T>();
			for (int i = isize - 1; i >= 0; i--) {
				resultList.add(resultPvt.get(i));
			}
			return resultList;
		} else
			return null;
	}

}
