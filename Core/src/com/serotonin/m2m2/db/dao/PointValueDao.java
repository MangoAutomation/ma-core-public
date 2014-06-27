/*
    Mango - Open Source M2M - http://mango.serotoninsoftware.com
    Copyright (C) 2006-2011 Serotonin Software Technologies Inc.
    @author Matthew Lohbihler
    
    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.serotonin.m2m2.db.dao;

import java.util.List;

import com.serotonin.db.MappedRowCallback;
import com.serotonin.m2m2.rt.dataImage.IdPointValueTime;
import com.serotonin.m2m2.rt.dataImage.PointValueTime;
import com.serotonin.m2m2.rt.dataImage.SetPointSource;
import com.serotonin.m2m2.vo.pair.LongPair;

public interface PointValueDao {
	
	
	
	
    /**
     * Only the PointValueCache should call this method during runtime. Do not use.
     */
    public PointValueTime savePointValueSync(int pointId, PointValueTime pointValue, SetPointSource source);

    /**
     * Only the PointValueCache should call this method during runtime. Do not use.
     */
    public void savePointValueAsync(int pointId, PointValueTime pointValue, SetPointSource source);

    public List<PointValueTime> getPointValues(int pointId, long since);

    public List<PointValueTime> getPointValuesBetween(int pointId, long from, long to);

    public List<PointValueTime> getLatestPointValues(int pointId, int limit);

    public List<PointValueTime> getLatestPointValues(int pointId, int limit, long before);

    public PointValueTime getLatestPointValue(int pointId);

    public PointValueTime getPointValueBefore(int pointId, long time);

    public PointValueTime getPointValueAfter(int pointId, long time);

    public PointValueTime getPointValueAt(int pointId, long time);

    public void getPointValuesBetween(int pointId, long from, long to, MappedRowCallback<PointValueTime> callback);

    public void getPointValuesBetween(List<Integer> pointIds, long from, long to,
            MappedRowCallback<IdPointValueTime> callback);

    public long deletePointValuesBefore(int pointId, long time);

    public long deletePointValues(int pointId);

    public long deleteAllPointData();

    /**
     * Delete any point values that are no longer tied to a point in the Data Points table
     * @return
     */
    public long deleteOrphanedPointValues();

    public void deleteOrphanedPointValueAnnotations();

    //NOT USING in Mango (legacy from M2M)
    //public void compressTables();

    public long dateRangeCount(int pointId, long from, long to);

    public long getInceptionDate(int pointId);

    public long getStartTime(List<Integer> pointIds);

    public long getEndTime(List<Integer> pointIds);

    public LongPair getStartAndEndTime(List<Integer> pointIds);

    public List<Long> getFiledataIds(int pointId);

	/**
	 * Delete any point values where data type doesn't match the vo,
	 * just in case the data type was changed.
	 * Only do this if the data type has actually changed because it is
	 * just really slow if the database is big or busy.
	 * 
	 * @param id
	 * @param dataTypeId
	 */
	public long deletePointValuesWithMismatchedType(int id, int dataTypeId);

	/**
	 * Update a given point value at some time by queueing up a work item
	 * 
	 * @param id
	 * @param pvt
	 * @param object
	 */
	public void updatePointValueAsync(int dataPointId, PointValueTime pvt, SetPointSource source);

	/**
	 * Update a given point value at some time directly
	 * @param dataPointId
	 * @param pvt
	 * @param source
	 * @return
	 */
	public PointValueTime updatePointValueSync(int dataPointId, PointValueTime pvt, SetPointSource source);

	/**
	 * Delete all data point values at a time
	 * @param dataPointId
	 * @param ts
	 * @return
	 */
	public long deletePointValue(int dataPointId, long ts);
}
