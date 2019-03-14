/**
 * Copyright (C) 2017 Infinite Automation Software. All rights reserved.
 *
 */
package com.serotonin.m2m2;

import java.util.List;

import com.serotonin.m2m2.db.dao.DataPointDao;
import com.serotonin.m2m2.db.dao.DataSourceDao;
import com.serotonin.m2m2.db.dao.PublisherDao;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.rt.RuntimeManager;
import com.serotonin.m2m2.rt.dataImage.DataPointListener;
import com.serotonin.m2m2.rt.dataImage.DataPointRT;
import com.serotonin.m2m2.rt.dataImage.PointValueTime;
import com.serotonin.m2m2.rt.dataImage.SetPointSource;
import com.serotonin.m2m2.rt.dataImage.types.DataValue;
import com.serotonin.m2m2.rt.dataSource.DataSourceRT;
import com.serotonin.m2m2.rt.publish.PublisherRT;
import com.serotonin.m2m2.vo.DataPointVO;
import com.serotonin.m2m2.vo.dataSource.DataSourceVO;
import com.serotonin.m2m2.vo.publish.PublishedPointVO;
import com.serotonin.m2m2.vo.publish.PublisherVO;

/**
 * A mock of the runtime manager that can optionally save data to the database.
 *   This implementation does not actually start any threads/tasks.
 *
 * @author Terry Packer
 */
public class MockRuntimeManager implements RuntimeManager {
    
    //Use the database to save data sources/points/publishers
    protected boolean useDatabase;
    
    public MockRuntimeManager() {
        
    }
    
    public MockRuntimeManager(boolean useDatabase) {
        this.useDatabase = useDatabase;
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.rt.RuntimeManager#getState()
     */
    @Override
    public int getState() {
        return RuntimeManager.RUNNING;
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.rt.RuntimeManager#initialize(boolean)
     */
    @Override
    public void initialize(boolean safe) {
        
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.rt.RuntimeManager#terminate()
     */
    @Override
    public void terminate() {
        
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.rt.RuntimeManager#joinTermination()
     */
    @Override
    public void joinTermination() {

        
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.rt.RuntimeManager#getRunningDataSource(int)
     */
    @Override
    public DataSourceRT<? extends DataSourceVO<?>> getRunningDataSource(int dataSourceId) {

        return null;
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.rt.RuntimeManager#isDataSourceRunning(int)
     */
    @Override
    public boolean isDataSourceRunning(int dataSourceId) {

        return false;
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.rt.RuntimeManager#getDataSources()
     */
    @Override
    public List<DataSourceVO<?>> getDataSources() {
        if(useDatabase)
            return DataSourceDao.getInstance().getAll();
        else 
            return null;
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.rt.RuntimeManager#getDataSource(int)
     */
    @Override
    public DataSourceVO<?> getDataSource(int dataSourceId) {
        if(useDatabase)
            return DataSourceDao.getInstance().get(dataSourceId);
        else 
            return null;
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.rt.RuntimeManager#deleteDataSource(int)
     */
    @Override
    public void deleteDataSource(int dataSourceId) {
        if(useDatabase)
            DataSourceDao.getInstance().delete(dataSourceId);
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.rt.RuntimeManager#saveDataSource(com.serotonin.m2m2.vo.dataSource.DataSourceVO)
     */
    @Override
    public void saveDataSource(DataSourceVO<?> vo) {
        if(useDatabase)
            DataSourceDao.getInstance().save(vo);
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.rt.RuntimeManager#initializeDataSourceStartup(com.serotonin.m2m2.vo.dataSource.DataSourceVO)
     */
    @Override
    public boolean initializeDataSourceStartup(DataSourceVO<?> vo) {

        return false;
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.rt.RuntimeManager#stopDataSourceShutdown(int)
     */
    @Override
    public void stopDataSourceShutdown(int id) {

        
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.rt.RuntimeManager#saveDataPoint(com.serotonin.m2m2.vo.DataPointVO)
     */
    @Override
    public void saveDataPoint(DataPointVO point) {
        if(useDatabase)
            DataPointDao.getInstance().save(point);
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.rt.RuntimeManager#deleteDataPoint(com.serotonin.m2m2.vo.DataPointVO)
     */
    @Override
    public void deleteDataPoint(DataPointVO point) {
        if(useDatabase)
            DataPointDao.getInstance().delete(point.getId());
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.rt.RuntimeManager#restartDataPoint(com.serotonin.m2m2.vo.DataPointVO)
     */
    @Override
    public void restartDataPoint(DataPointVO vo) {
        
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.rt.RuntimeManager#isDataPointRunning(int)
     */
    @Override
    public boolean isDataPointRunning(int dataPointId) {

        return false;
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.rt.RuntimeManager#getDataPoint(int)
     */
    @Override
    public DataPointRT getDataPoint(int dataPointId) {
            return null;
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.rt.RuntimeManager#addDataPointListener(int, com.serotonin.m2m2.rt.dataImage.DataPointListener)
     */
    @Override
    public void addDataPointListener(int dataPointId, DataPointListener l) {

        
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.rt.RuntimeManager#removeDataPointListener(int, com.serotonin.m2m2.rt.dataImage.DataPointListener)
     */
    @Override
    public void removeDataPointListener(int dataPointId, DataPointListener l) {

        
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.rt.RuntimeManager#getDataPointListeners(int)
     */
    @Override
    public DataPointListener getDataPointListeners(int dataPointId) {

        return null;
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.rt.RuntimeManager#setDataPointValue(int, com.serotonin.m2m2.rt.dataImage.types.DataValue, com.serotonin.m2m2.rt.dataImage.SetPointSource)
     */
    @Override
    public void setDataPointValue(int dataPointId, DataValue value, SetPointSource source) {

        
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.rt.RuntimeManager#setDataPointValue(int, com.serotonin.m2m2.rt.dataImage.PointValueTime, com.serotonin.m2m2.rt.dataImage.SetPointSource)
     */
    @Override
    public void setDataPointValue(int dataPointId, PointValueTime valueTime,
            SetPointSource source) {

        
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.rt.RuntimeManager#relinquish(int)
     */
    @Override
    public void relinquish(int dataPointId) {

        
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.rt.RuntimeManager#forcePointRead(int)
     */
    @Override
    public void forcePointRead(int dataPointId) {

        
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.rt.RuntimeManager#forceDataSourcePoll(int)
     */
    @Override
    public void forceDataSourcePoll(int dataSourceId) {

        
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.rt.RuntimeManager#purgeDataPointValues()
     */
    @Override
    public long purgeDataPointValues() {

        return 0;
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.rt.RuntimeManager#purgeDataPointValuesWithoutCount()
     */
    @Override
    public void purgeDataPointValuesWithoutCount() {

        
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.rt.RuntimeManager#purgeDataPointValues(int, int, int)
     */
    @Override
    public long purgeDataPointValues(int dataPointId, int periodType, int periodCount) {

        return 0;
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.rt.RuntimeManager#purgeDataPointValues(int)
     */
    @Override
    public long purgeDataPointValues(int dataPointId) {

        return 0;
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.rt.RuntimeManager#purgeDataPointValuesWithoutCount(int)
     */
    @Override
    public boolean purgeDataPointValuesWithoutCount(int dataPointId) {

        return false;
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.rt.RuntimeManager#purgeDataPointValue(int, long)
     */
    @Override
    public long purgeDataPointValue(int dataPointId, long ts) {

        return 0;
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.rt.RuntimeManager#purgeDataPointValues(int, long)
     */
    @Override
    public long purgeDataPointValues(int dataPointId, long before) {

        return 0;
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.rt.RuntimeManager#purgeDataPointValuesBetween(int, long, long)
     */
    @Override
    public long purgeDataPointValuesBetween(int dataPointId, long startTime, long endTime) {

        return 0;
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.rt.RuntimeManager#purgeDataPointValuesWithoutCount(int, long)
     */
    @Override
    public boolean purgeDataPointValuesWithoutCount(int dataPointId, long before) {

        return false;
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.rt.RuntimeManager#getRunningPublisher(int)
     */
    @Override
    public PublisherRT<?> getRunningPublisher(int publisherId) {

        return null;
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.rt.RuntimeManager#isPublisherRunning(int)
     */
    @Override
    public boolean isPublisherRunning(int publisherId) {

        return false;
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.rt.RuntimeManager#getPublisher(int)
     */
    @Override
    public PublisherVO<? extends PublishedPointVO> getPublisher(int publisherId) {
        if(useDatabase)
            return PublisherDao.getInstance().get(publisherId);
        else
            return null;
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.rt.RuntimeManager#deletePublisher(int)
     */
    @Override
    public void deletePublisher(int publisherId) {
        if(useDatabase)
            PublisherDao.getInstance().delete(publisherId);
        
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.rt.RuntimeManager#savePublisher(com.serotonin.m2m2.vo.publish.PublisherVO)
     */
    @Override
    public void savePublisher(PublisherVO<? extends PublishedPointVO> vo) {
        if(useDatabase)
            PublisherDao.getInstance().save(vo);
    }

    @Override
    public void enableDataPoint(DataPointVO point, boolean enabled) {
        point.setEnabled(enabled);
        if(useDatabase)
            DataPointDao.getInstance().saveEnabledColumn(point);
    }

    @Override
    public TranslatableMessage getStateMessage() {
        return new TranslatableMessage("common.default", "Mock runtime manager state message");
    }
}
