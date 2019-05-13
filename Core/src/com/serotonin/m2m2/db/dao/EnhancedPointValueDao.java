package com.serotonin.m2m2.db.dao;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import com.serotonin.m2m2.rt.dataImage.PointValueTime;
import com.serotonin.m2m2.rt.dataImage.SetPointSource;
import com.serotonin.m2m2.vo.DataPointVO;
import com.serotonin.m2m2.vo.dataSource.DataSourceVO;

public interface EnhancedPointValueDao extends PointValueDao {
    public Map<Integer, PointValueTime> getLatestPointValuesForDataSource(DataSourceVO<?> dataSource);
    public Map<Integer, List<PointValueTime>> getLatestPointValuesForDataSource(DataSourceVO<?> dataSource, int numberOfValues);
    public PointValueTime savePointValueSync(DataPointVO pointVo, DataSourceVO<?> dataSourceVo, PointValueTime pvt, SetPointSource source);
    public void savePointValueAsync(DataPointVO pointVo, DataSourceVO<?> dataSourceVo, PointValueTime pvt, SetPointSource source, Consumer<Long> savedCallback);
    public PointValueTime updatePointValueSync(DataPointVO pointVo, DataSourceVO<?> dataSourceVo, PointValueTime pvt, SetPointSource source);
    public void updatePointValueAsync(DataPointVO pointVo, DataSourceVO<?> dataSourceVo, PointValueTime pvt, SetPointSource source);
}
