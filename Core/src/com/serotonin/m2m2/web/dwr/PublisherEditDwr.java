/*
    Copyright (C) 2006-2011 Serotonin Software Technologies Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.web.dwr;

import java.util.Iterator;
import java.util.List;

import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.DataTypes;
import com.serotonin.m2m2.db.dao.DataPointDao;
import com.serotonin.m2m2.i18n.ProcessResult;
import com.serotonin.m2m2.module.license.PublisherTypePointsLimit;
import com.serotonin.m2m2.vo.DataPointExtendedNameComparator;
import com.serotonin.m2m2.vo.DataPointVO;
import com.serotonin.m2m2.vo.publish.PublishedPointVO;
import com.serotonin.m2m2.vo.publish.PublisherVO;
import com.serotonin.m2m2.web.dwr.util.DwrPermission;

/**
 * @author Matthew Lohbihler
 */
public class PublisherEditDwr extends BaseDwr {
    protected ProcessResult trySave(PublisherVO<? extends PublishedPointVO> p) {
        ProcessResult response = new ProcessResult();

        // Limit enforcement.
        PublisherTypePointsLimit.checkLimit(p, response);

        p.validate(response);

        if (!response.getHasMessages()) {
            Common.runtimeManager.savePublisher(p);
            response.addData("id", p.getId());
        }

        return response;
    }

    @DwrPermission(admin = true)
    public void cancelTestingUtility() {
        Common.getUser().cancelTestingUtility();
    }

    @DwrPermission(admin = true)
    public ProcessResult initSender() {
        List<DataPointVO> allPoints = new DataPointDao().getDataPoints(DataPointExtendedNameComparator.instance, false);

        // Remove image points
        Iterator<DataPointVO> iter = allPoints.iterator();
        while (iter.hasNext()) {
            DataPointVO dp = iter.next();
            if (dp.getPointLocator().getDataTypeId() == DataTypes.IMAGE)
                iter.remove();
        }

        ProcessResult response = new ProcessResult();
        response.addData("publisher", Common.getUser().getEditPublisher());
        response.addData("allPoints", allPoints);
        return response;
    }
}
