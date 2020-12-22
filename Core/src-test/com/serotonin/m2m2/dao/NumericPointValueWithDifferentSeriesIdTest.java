/**
 * Copyright (C) 2020  Infinite Automation Software. All rights reserved.
 */

package com.serotonin.m2m2.dao;

import static org.junit.Assert.fail;

import java.util.List;

import com.infiniteautomation.mango.spring.service.DataPointService;
import com.infiniteautomation.mango.util.exception.ValidationException;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.dao.DataPointDao;
import com.serotonin.m2m2.i18n.ProcessMessage;
import com.serotonin.m2m2.vo.DataPointVO;
import com.serotonin.m2m2.vo.IDataPoint;

/**
 *
 * @author Terry Packer
 */
public class NumericPointValueWithDifferentSeriesIdTest extends NumericPointValueDaoTest {


    @Override
    protected List<IDataPoint> createMockDataPoints(int count) {
        List<IDataPoint> points = super.createMockDataPoints(count);

        //Change series id
        DataPointService service = Common.getBean(DataPointService.class);
        DataPointDao dao = Common.getBean(DataPointDao.class);
        for(IDataPoint point : points) {
            DataPointVO vo = (DataPointVO)point;
            vo.setSeriesId(dao.insertNewTimeSeries());
            service.getPermissionService().runAsSystemAdmin(() -> {
                try {
                    service.update(vo.getId(), vo);
                } catch(ValidationException e) {
                    String failureMessage = "";
                    for(ProcessMessage m : e.getValidationResult().getMessages()){
                        String messagePart = m.getContextKey() + " -> " + m.getContextualMessage().translate(Common.getTranslations()) + "\n";
                        failureMessage += messagePart;
                    }
                    fail(failureMessage);
                }
            });
        }

        return points;
    }

}
