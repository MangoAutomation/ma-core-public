package com.serotonin.m2m2.web.dwr;

import com.serotonin.m2m2.db.dao.DataPointDao;
import com.serotonin.m2m2.vo.hierarchy.PointFolder;
import com.serotonin.m2m2.vo.hierarchy.PointHierarchy;
import com.serotonin.m2m2.web.dwr.util.DwrPermission;

public class PointHierarchyDwr extends BaseDwr {
    @DwrPermission(admin = true)
    public PointFolder getPointHierarchy() {
        PointHierarchy ph = DataPointDao.instance.getPointHierarchy(true);
        return ph.getRoot();
    }

    @DwrPermission(admin = true)
    public PointFolder savePointHierarchy(PointFolder rootFolder) {
        DataPointDao.instance.savePointHierarchy(rootFolder);
        return rootFolder;
    }
}
