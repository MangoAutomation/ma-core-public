/**
 * Copyright (C) 2013 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.db.dao;

import java.util.List;
import java.util.function.Supplier;

import com.infiniteautomation.mango.db.query.BookendQueryCallback;
import com.infiniteautomation.mango.db.query.PVTQueryCallback;
import com.serotonin.db.MappedRowCallback;
import com.serotonin.db.WideQueryCallback;
import com.serotonin.log.LogStopWatch;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.rt.dataImage.IdPointValueTime;
import com.serotonin.m2m2.rt.dataImage.PointValueTime;
import com.serotonin.m2m2.rt.dataImage.SetPointSource;
import com.serotonin.m2m2.vo.DataPointVO;
import com.serotonin.m2m2.vo.pair.LongPair;

/**
 *
 * Class to output query execution times
 *
 * INFO Level log Output is:
 * start[ts] time[exec ms] tag[functionName(param1vaoue,..,paramNvalue){num results}]
 *
 *
 * For Point Value Daos
 * @author Terry Packer
 *
 */
public class PointValueDaoMetrics implements CachingPointValueDao {

    private final PointValueDao dao;
    private final long metricsThreshold;

    public PointValueDaoMetrics(PointValueDao dao){
        this.dao = dao;
        this.metricsThreshold = Common.envProps.getLong("db.metricsThreshold", 0L);
    }

    public PointValueDao getBaseDao(){
        return this.dao;
    }

    @Override
    public PointValueTime savePointValueSync(DataPointVO vo,
            PointValueTime pointValue, SetPointSource source) {
        return dao.savePointValueSync(vo, pointValue, source);
    }

    @Override
    public void savePointValueAsync(DataPointVO vo, PointValueTime pointValue,
            SetPointSource source) {
        dao.savePointValueAsync(vo, pointValue, source);

    }

    @Override
    public List<PointValueTime> getPointValues(DataPointVO vo, long since) {
        LogStopWatch LogStopWatch = new LogStopWatch();
        List<PointValueTime> values = dao.getPointValues(vo, since);
        LogStopWatch.stop(() -> "getPointValues(vo,since) (" + vo + ", " +since + "){" + values.size() +"}", this.metricsThreshold);
        return values;
    }

    @Override
    public List<PointValueTime> getPointValuesBetween(DataPointVO vo, long from,
            long to) {
        LogStopWatch LogStopWatch = new LogStopWatch();
        List<PointValueTime> values = dao.getPointValuesBetween(vo, from,to);
        LogStopWatch.stop(() -> "getPointValuesBetween(vo, from, to)  ("+vo+", "+from+", "+to + "){" + values.size() +"}", this.metricsThreshold);
        return values;
    }

    @Override
    public List<PointValueTime> getPointValuesBetween(DataPointVO vo, long from,
            long to, int limit) {
        LogStopWatch LogStopWatch = new LogStopWatch();
        List<PointValueTime> values = dao.getPointValuesBetween(vo, from, to, limit);
        LogStopWatch.stop(() -> "getPointValuesBetween(vo, from, to)  ("+vo+", "+from+", "+to+ ", "+limit + "){" + values.size() +"}", this.metricsThreshold);
        return values;

    }

    @Override
    public List<PointValueTime> getLatestPointValues(DataPointVO vo, int limit) {
        LogStopWatch LogStopWatch = new LogStopWatch();
        List<PointValueTime> values = dao.getLatestPointValues(vo, limit);
        LogStopWatch.stop(() -> "getLatestPointValues(vo,limit) (" + vo + ", " + limit + "){" + values.size() +"}", this.metricsThreshold);
        return values;
    }

    @Override
    public List<PointValueTime> getLatestPointValues(DataPointVO vo, int limit,
            long before) {
        LogStopWatch LogStopWatch = new LogStopWatch();
        List<PointValueTime> values = dao.getLatestPointValues(vo, limit,before);
        LogStopWatch.stop(() -> "getLatestPointValues(vo,limit,before) (" + vo +", " + limit + ", " + before + "){" + values.size() +"}", this.metricsThreshold);
        return values;
    }

    @Override
    public void getLatestPointValues(List<DataPointVO> vos, long before, boolean orderById, Integer limit, PVTQueryCallback<IdPointValueTime> callback){
        LogStopWatch LogStopWatch = new LogStopWatch();
        dao.getLatestPointValues(vos, before, orderById, limit, callback);
        Supplier<String> msg;
        if(vos.size() <= 10) {
            msg = () -> {
                return "getLatestPointValues(vos,limit,before, orderById, callback) (" + vos +", " + limit + ", " + before + "," + orderById + ", callback)";
            };
        }else {
            msg = () -> {
                return "getLatestPointValues(vos,limit,before, orderById, callback) ([" + vos.size() +"], " + limit + ", " + before + "," + orderById + ", callback)";
            };
        }
        LogStopWatch.stop(msg, this.metricsThreshold);
    }

    @Override
    public PointValueTime getLatestPointValue(DataPointVO vo) {
        LogStopWatch LogStopWatch = new LogStopWatch();
        PointValueTime value = dao.getLatestPointValue(vo);
        LogStopWatch.stop(() -> "getLatestPointValue(vo) (" + vo + "){" + (value != null ? 1 : 0) + "}", this.metricsThreshold);
        return value;
    }

    @Override
    public PointValueTime getPointValueBefore(DataPointVO vo, long time) {
        LogStopWatch LogStopWatch = new LogStopWatch();
        PointValueTime value = dao.getPointValueBefore(vo,time);
        LogStopWatch.stop(() -> "getPointValuesBefore(vo,time) (" + vo + ", " + time + "){" + (value != null ? 1 : 0) + "}", this.metricsThreshold);
        return value;
    }

    @Override
    public PointValueTime getPointValueAfter(DataPointVO vo, long time) {
        LogStopWatch LogStopWatch = new LogStopWatch();
        PointValueTime value = dao.getPointValueAfter(vo,time);
        LogStopWatch.stop(() -> "getPointValueAfter(vo,time) (" + vo + ", " + time + "){" + (value != null ? 1 : 0) + "}", this.metricsThreshold);
        return value;
    }

    @Override
    public PointValueTime getPointValueAt(DataPointVO vo, long time) {
        LogStopWatch LogStopWatch = new LogStopWatch();
        PointValueTime value = dao.getPointValueAt(vo,time);
        LogStopWatch.stop(() -> "getPointValueAt(vo,time) (" + vo + ", " + time + "){" + (value != null ? 1 : 0) + "}", this.metricsThreshold);
        return value;
    }

    @Override
    public void getPointValuesBetween(DataPointVO vo, long from, long to,
            MappedRowCallback<PointValueTime> callback) {
        LogStopWatch LogStopWatch = new LogStopWatch();
        dao.getPointValuesBetween(vo,from,to,callback);
        LogStopWatch.stop(() -> "getPointValuesBetween(vo,from,to,callback) + (" + vo + ", " + from + ", " + to + ", " + callback.toString() + ")", this.metricsThreshold);
    }

    @Override
    public void getPointValuesBetween(List<DataPointVO> vos, long from,
            long to, MappedRowCallback<IdPointValueTime> callback) {
        LogStopWatch LogStopWatch = new LogStopWatch();
        dao.getPointValuesBetween(vos,from,to,callback);
        Supplier<String> msg;
        if(vos.size() <= 10) {
            msg = () -> {
                String sqlIn = "[";
                for(int i=0; i<vos.size(); i++){
                    sqlIn += vos.get(i);
                    if(i < vos.size())
                        sqlIn += ",";
                }
                sqlIn += "]";
                return "getPointValuesBetween(vos,from,to,callback) ("+ sqlIn + ", " + from + ", " + to + ", " + callback.toString() + ")";
            };
        }else {
            msg = () -> {
                String sqlIn = "[" + vos.size() + "]";
                return "getPointValuesBetween(vos,from,to,callback) ("+ sqlIn + ", " + from + ", " + to + ", " + callback.toString() + ")";
            };
        }
        LogStopWatch.stop(msg, this.metricsThreshold);

    }

    @Override
    public void wideQuery(DataPointVO vo, long from, long to, WideQueryCallback<PointValueTime> callback) {
        LogStopWatch LogStopWatch = new LogStopWatch();
        dao.wideQuery(vo, from , to, callback);
        LogStopWatch.stop(() -> "wideQuery(vo,from,to,callback) ("+ vo + ", " + from + ", " + to + ", " + callback.toString() + ")" , this.metricsThreshold);
    }

    @Override
    public long deletePointValuesBefore(DataPointVO vo, long time) {
        LogStopWatch LogStopWatch = new LogStopWatch();
        long value = dao.deletePointValuesBefore(vo,time);
        LogStopWatch.stop(() -> "deletePointValuesBefore(vo,time) (" + vo + ", " + time + ")", this.metricsThreshold);
        return value;

    }

    @Override
    public boolean deletePointValuesBeforeWithoutCount(DataPointVO vo, long time){
        LogStopWatch LogStopWatch = new LogStopWatch();
        boolean value = dao.deletePointValuesBeforeWithoutCount(vo,time);
        LogStopWatch.stop(() -> "deletePointValuesBeforeWithoutCount(vo,time) (" + vo + ", " + time + ")", this.metricsThreshold);
        return value;
    }

    @Override
    public long deletePointValuesBetween(DataPointVO vo, long startTime, long endTime){
        LogStopWatch LogStopWatch = new LogStopWatch();
        long value = dao.deletePointValuesBetween(vo,startTime,endTime);
        LogStopWatch.stop(() -> "deletePointValuesBetween(vo,startTime,endTime) (" + vo + ", " + startTime + ", " + endTime + ")", this.metricsThreshold);
        return value;
    }

    @Override
    public long deletePointValues(DataPointVO vo) {
        LogStopWatch LogStopWatch = new LogStopWatch();
        long value = dao.deletePointValues(vo);
        LogStopWatch.stop(() -> "deletePointValues(vo) (" + vo + ")", this.metricsThreshold);
        return value;
    }

    @Override
    public boolean deletePointValuesWithoutCount(DataPointVO vo) {
        LogStopWatch LogStopWatch = new LogStopWatch();
        boolean value = dao.deletePointValuesWithoutCount(vo);
        LogStopWatch.stop(() -> "deletePointValuesWithoutCount(vo) (" + vo + ")", this.metricsThreshold);
        return value;
    }

    @Override
    public long deleteAllPointData() {
        LogStopWatch LogStopWatch = new LogStopWatch();
        long value = dao.deleteAllPointData();
        LogStopWatch.stop(() -> "deleteAllPointData()", this.metricsThreshold);
        return value;
    }

    @Override
    public void deleteAllPointDataWithoutCount() {
        LogStopWatch LogStopWatch = new LogStopWatch();
        dao.deleteAllPointDataWithoutCount();
        LogStopWatch.stop(() -> "deleteAllPointDataWithoutCount()", this.metricsThreshold);
    }

    @Override
    public long deleteOrphanedPointValues() {
        LogStopWatch LogStopWatch = new LogStopWatch();
        long value = dao.deleteOrphanedPointValues();
        LogStopWatch.stop(() -> "deleteOrphanedPointValues()", this.metricsThreshold);
        return value;
    }

    @Override
    public void deleteOrphanedPointValuesWithoutCount() {
        LogStopWatch LogStopWatch = new LogStopWatch();
        dao.deleteOrphanedPointValuesWithoutCount();
        LogStopWatch.stop(() -> "deleteOrphanedPointValuesWithoutCount()", this.metricsThreshold);
    }

    @Override
    public void deleteOrphanedPointValueAnnotations() {
        LogStopWatch LogStopWatch = new LogStopWatch();
        dao.deleteOrphanedPointValueAnnotations();
        LogStopWatch.stop(() -> "deleteOrphanedPointValueAnnotations()", this.metricsThreshold);
        return;

    }

    @Override
    public long dateRangeCount(DataPointVO vo, long from, long to) {
        LogStopWatch LogStopWatch = new LogStopWatch();
        long value = dao.dateRangeCount(vo, from, to);
        LogStopWatch.stop(() -> "dateRangeCount(vo,from,to) (" + vo + ", " + from + ", " + to + ")", this.metricsThreshold);
        return value;
    }

    @Override
    public long getInceptionDate(DataPointVO vo) {
        LogStopWatch LogStopWatch = new LogStopWatch();
        long value = dao.getInceptionDate(vo);
        LogStopWatch.stop(() -> "getInceptionDate(vo) (" + vo + ")", this.metricsThreshold);
        return value;
    }

    @Override
    public long getStartTime(List<DataPointVO> vos) {
        LogStopWatch LogStopWatch = new LogStopWatch();
        long result = dao.getStartTime(vos);
        Supplier<String> msg = () -> {
            String sqlIn = "[";
            for(int i=0; i<vos.size(); i++){
                sqlIn += vos.get(i);
                if(i < vos.size())
                    sqlIn += ",";
            }
            sqlIn += "]";
            return "getStartTime(vos) (" + sqlIn + ")";
        };
        LogStopWatch.stop(msg, this.metricsThreshold);
        return result;
    }

    @Override
    public long getEndTime(List<DataPointVO> vos) {
        LogStopWatch LogStopWatch = new LogStopWatch();
        long result = dao.getEndTime(vos);
        Supplier<String> msg = () -> {
            String sqlIn = "[";
            for(int i=0; i<vos.size(); i++){
                sqlIn += vos.get(i);
                if(i < vos.size())
                    sqlIn += ",";
            }
            sqlIn += "]";
            return "getEndTime(vos) (" + sqlIn + ")";
        };
        LogStopWatch.stop(msg, this.metricsThreshold);
        return result;
    }

    @Override
    public LongPair getStartAndEndTime(List<DataPointVO> vos) {
        LogStopWatch LogStopWatch = new LogStopWatch();
        LongPair result = dao.getStartAndEndTime(vos);
        Supplier<String> msg = () -> {

            String sqlIn = "[";
            for(int i=0; i<vos.size(); i++){
                sqlIn += vos.get(i);
                if(i < vos.size())
                    sqlIn += ",";
            }
            sqlIn += "]";
            return "getStartAndEndTime(vos) + (" + sqlIn + ")";
        };
        LogStopWatch.stop(msg, this.metricsThreshold);
        return result;
    }

    @Override
    public List<Long> getFiledataIds(DataPointVO vo) {
        LogStopWatch LogStopWatch = new LogStopWatch();
        List<Long> value = dao.getFiledataIds(vo);
        LogStopWatch.stop(() -> "getFiledataIds(vo) (" + vo + ")", this.metricsThreshold);
        return value;
    }

    @Override
    public long deletePointValue(DataPointVO vo, long ts) {
        LogStopWatch LogStopWatch = new LogStopWatch();
        long value = dao.deletePointValue(vo, ts);
        LogStopWatch.stop(() -> "deletePointValue(vo, ts) + (" + vo + ", " + ts + ")", this.metricsThreshold);
        return value;
    }

    @Override
    public void wideBookendQuery(List<DataPointVO> vos, long from, long to, boolean orderById, Integer limit,
            BookendQueryCallback<IdPointValueTime> callback) {
        LogStopWatch logStopWatch = new LogStopWatch();
        dao.wideBookendQuery(vos, from, to, orderById, limit, callback);
        Supplier<String> msg;
        if(vos.size() <= 10) {
            msg = () -> {
                return "wideBookendQuery(dataPointIds, from, to, orderById, limit, callback) + (" + vos + ", " + to + ", " + from + ", " + limit + ",callback)";
            };
        }else {
            msg = () -> {
                return "wideBookendQuery(dataPointIds, from, to, orderById, limit, callback) + ([" + vos.size() + "], " + to + ", " + from + ", " + limit + ",callback)";
            };
        }
        logStopWatch.stop(msg, this.metricsThreshold);
    }

    @Override
    public void getPointValuesBetween(List<DataPointVO> vos, long from, long to, boolean orderById,
            Integer limit, PVTQueryCallback<IdPointValueTime> callback) {
        LogStopWatch logStopWatch = new LogStopWatch();
        dao.getPointValuesBetween(vos, from, to, orderById, limit, callback);
        Supplier<String> msg;
        if(vos.size() <= 10) {
            msg = () -> {
                return "getPointValuesBetween(vos, from, to, orderById, limit, callback) + (" + vos + ", " + to + ", " + from + ", " + limit + ",callback)";
            };
        }else {
            msg = () -> {
                return "getPointValuesBetween(vos, from, to, orderById, limit, callback) + ([" + vos.size() + "], " + to + ", " + from + ", " + limit + ",callback)";
            };
        }
        logStopWatch.stop(msg, this.metricsThreshold);
    }

}
