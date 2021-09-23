/*
 * Copyright (C) 2021 RadixIot LLC. All rights reserved.
 */

package com.serotonin.m2m2.dao;

import org.junit.Assert;
import org.junit.Test;

import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.MangoTestBase;
import com.serotonin.m2m2.db.dao.PointValueDao;
import com.serotonin.m2m2.db.dao.PointValueDaoSQL;
import com.serotonin.m2m2.rt.dataImage.PointValueTime;
import com.serotonin.m2m2.vo.DataPointVO;
import com.serotonin.m2m2.vo.IDataPoint;

public class PointValueDaoWriteMetricsTest extends MangoTestBase {

    final int events = 100;
    final int period = 5000;


    @Test
    public void testAsyncWrite() {
        int eventsPerSecond = events/(period/1000);
        IDataPoint vo = this.createMockDataPoints(1).get(0);
        PointValueDao pointValueDao = Common.getBean(PointValueDao.class);
        int i = 0;

        while (i <= events) {
            if (i != 0) {
                this.timer.fastForwardTo(this.timer.currentTimeMillis() + period / events);
            }
            PointValueTime p2vt = new PointValueTime(-2.0, this.timer.currentTimeMillis());
            pointValueDao.savePointValueAsync((DataPointVO) vo, p2vt, null);
            i++;

        }

        Assert.assertEquals(eventsPerSecond, Common.MONITORED_VALUES.getMonitor(PointValueDaoSQL.ASYNC_INSERTS_SPEED_COUNTER_ID).getValue());
    }

    @Test
    public void testSyncWrite() {
        int eventsPerSecond = events/(period/1000);
        IDataPoint vo = this.createMockDataPoints(1).get(0);
        PointValueDao pointValueDao = Common.getBean(PointValueDao.class);
        int i = 0;

        while (i <= events) {
            if (i != 0) {
                this.timer.fastForwardTo(this.timer.currentTimeMillis() + period / events);
            }
            PointValueTime p2vt = new PointValueTime(-2.0, this.timer.currentTimeMillis());
            pointValueDao.savePointValueSync((DataPointVO) vo, p2vt, null);
            i++;

        }


        Assert.assertEquals(eventsPerSecond, Common.MONITORED_VALUES.getMonitor(PointValueDaoSQL.SYNC_INSERTS_SPEED_COUNTER_ID).getValue());
    }

}
