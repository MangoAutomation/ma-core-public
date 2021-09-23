package com.serotonin.m2m2.rt.dataImage;

import java.util.List;

import com.serotonin.NotImplementedException;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.dao.DataPointDao;
import com.serotonin.m2m2.db.dao.PointValueDao;
import com.serotonin.m2m2.rt.dataImage.DataPointRT.FireEvents;
import com.serotonin.m2m2.rt.script.AbstractPointWrapper;
import com.serotonin.m2m2.rt.script.DataPointWrapper;
import com.serotonin.m2m2.vo.DataPointVO;
import com.serotonin.timer.SimulationTimer;

public class HistoricalDataPoint implements IDataPointValueSource {

    private final DataPointVO vo;
    private final PointValueDao pointValueDao;
    private final SimulationTimer timer;

    public HistoricalDataPoint(DataPointVO vo, SimulationTimer timer, PointValueDao pointValueDao) {
        this.vo = vo;
        this.pointValueDao = pointValueDao;
        this.timer = timer;
    }

    public int getId() {
        return vo.getId();
    }

    @Override
    public List<PointValueTime> getLatestPointValues(int limit) {
        return pointValueDao.getLatestPointValues(vo, limit, timer.currentTimeMillis());
    }

    @Override
    public void updatePointValue(PointValueTime newValue) {
        throw new NotImplementedException();
    }

    @Override
    public void updatePointValue(PointValueTime newValue, boolean async) {
        throw new NotImplementedException();
    }

    @Override
    public void setPointValue(PointValueTime newValue, SetPointSource source) {
        //throw new NotImplementedException();
        DataPointRT dprt = Common.runtimeManager.getDataPoint(vo.getId());
        if(dprt == null) //point isn't running, we can save the value through the DAO
            pointValueDao.savePointValueAsync(vo, newValue, source);
        else //Give the point a chance to cache the new value
            dprt.savePointValueDirectToCache(newValue, source, true, true, FireEvents.NEVER);
    }

    @Override
    public PointValueTime getPointValue() {
        return pointValueDao.getPointValueBefore(vo, timer.currentTimeMillis() + 1);
    }

    @Override
    public PointValueTime getPointValueBefore(long time) {
        return pointValueDao.getPointValueBefore(vo, time);
    }

    @Override
    public PointValueTime getPointValueAfter(long time) {
        return pointValueDao.getPointValueAfter(vo, time);
    }

    @Override
    public List<PointValueTime> getPointValues(long since) {
        return pointValueDao.getPointValuesBetween(vo, since, timer.currentTimeMillis());
    }

    @Override
    public List<PointValueTime> getPointValuesBetween(long from, long to) {
        return pointValueDao.getPointValuesBetween(vo, from, to);
    }

    @Override
    public int getDataTypeId() {
        return vo.getPointLocator().getDataTypeId();
    }

    @Override
    public PointValueTime getPointValueAt(long time) {
        return Common.getBean(PointValueDao.class).getPointValueAt(vo, time);
    }

    @Override
    public DataPointWrapper getDataPointWrapper(AbstractPointWrapper rtWrapper) {
        return new DataPointWrapper(DataPointDao.getInstance().get(vo.getId()), rtWrapper);
    }

    @Override
    public DataPointVO getVO() {
        return vo;
    }
}
