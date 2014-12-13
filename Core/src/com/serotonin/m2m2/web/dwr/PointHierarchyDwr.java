package com.serotonin.m2m2.web.dwr;

import com.serotonin.m2m2.db.dao.DataPointDao;
import com.serotonin.m2m2.vo.hierarchy.PointFolder;
import com.serotonin.m2m2.vo.hierarchy.PointHierarchy;
import com.serotonin.m2m2.web.dwr.util.DwrPermission;

public class PointHierarchyDwr extends BaseDwr {
    @DwrPermission(admin = true)
    public PointFolder getPointHierarchy() {
        DataPointDao dataPointDao = new DataPointDao();
        PointHierarchy ph = dataPointDao.getPointHierarchy(false);
        return ph.getRoot();
    }

    @DwrPermission(admin = true)
    public PointFolder savePointHierarchy(PointFolder rootFolder) {
        new DataPointDao().savePointHierarchy(rootFolder);
        return rootFolder;
    }
}
