/*
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.web.dwr;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.serotonin.db.pair.StringStringPair;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.dao.DataPointDao;
import com.serotonin.m2m2.db.dao.DataSourceDao;
import com.serotonin.m2m2.db.dao.UserDao;
import com.serotonin.m2m2.i18n.ProcessResult;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.module.ModuleRegistry;
import com.serotonin.m2m2.vo.DataPointNameComparator;
import com.serotonin.m2m2.vo.DataPointVO;
import com.serotonin.m2m2.vo.User;
import com.serotonin.m2m2.vo.dataSource.DataSourceVO;
import com.serotonin.m2m2.vo.dataSource.MockDataSource;
import com.serotonin.m2m2.vo.permission.Permissions;
import com.serotonin.m2m2.web.comparators.StringStringPairComparator;
import com.serotonin.m2m2.web.dwr.util.DwrPermission;

/**
 * @author Matthew Lohbihler
 */
public class DataSourceListDwr extends BaseDwr {
    @DwrPermission(user = true)
    public ProcessResult init() {
        ProcessResult response = new ProcessResult();

        User user = Common.getUser();
        List<DataSourceVO<?>> allDataSources = Common.runtimeManager.getDataSources();

        if (user.isAdmin()) {
            response.addData("dataSources", allDataSources);

            List<StringStringPair> translatedTypes = new ArrayList<StringStringPair>();
            for (String type : ModuleRegistry.getDataSourceDefinitionTypes())
                translatedTypes.add(new StringStringPair(type, translate(ModuleRegistry.getDataSourceDefinition(type)
                        .getDescriptionKey())));
            StringStringPairComparator.sort(translatedTypes);
            response.addData("types", translatedTypes);
        }
        else {
            List<DataSourceVO<?>> dataSources = new ArrayList<DataSourceVO<?>>();
            for (DataSourceVO<?> ds : allDataSources) {
                if (Permissions.hasDataSourcePermission(user, ds.getId()))
                    dataSources.add(ds);
            }

            response.addData("dataSources", dataSources);
        }

        return response;
    }

    @DwrPermission(user = true)
    public Map<String, Object> toggleDataSource(int dataSourceId) {
        Permissions.ensureDataSourcePermission(Common.getUser(), dataSourceId);

        DataSourceVO<?> dataSource = Common.runtimeManager.getDataSource(dataSourceId);
        Map<String, Object> result = new HashMap<String, Object>();

        dataSource.setEnabled(!dataSource.isEnabled());
        Common.runtimeManager.saveDataSource(dataSource);

        result.put("enabled", dataSource.isEnabled());
        result.put("id", dataSourceId);
        return result;
    }

    public List<DataPointVO> getPointsForDataSource(int dataSourceId) {
        return new DataPointDao().getDataPoints(dataSourceId, DataPointNameComparator.instance);
    }

    @DwrPermission(user = true)
    public int deleteDataSource(int dataSourceId) {
        Permissions.ensureDataSourcePermission(Common.getUser(), dataSourceId);
        Common.runtimeManager.deleteDataSource(dataSourceId);
        return dataSourceId;
    }

    @DwrPermission(user = true)
    public ProcessResult toggleDataPoint(int dataPointId) {
        DataPointVO dataPoint = new DataPointDao().getDataPoint(dataPointId);
        Permissions.ensureDataSourcePermission(Common.getUser(), dataPoint.getDataSourceId());

        dataPoint.setEnabled(!dataPoint.isEnabled());
        Common.runtimeManager.saveDataPoint(dataPoint);

        ProcessResult response = new ProcessResult();
        response.addData("id", dataPointId);
        response.addData("enabled", dataPoint.isEnabled());
        return response;
    }

    @DwrPermission(user = true)
    public ProcessResult dataSourceInfo(int dataSourceId) {
        Permissions.ensureDataSourcePermission(Common.getUser(), dataSourceId);
        DataSourceDao dataSourceDao = new DataSourceDao();
        DataSourceVO<?> ds = dataSourceDao.getDataSource(dataSourceId);
        ProcessResult response = new ProcessResult();

        String name = StringUtils.abbreviate(
                TranslatableMessage.translate(getTranslations(), "common.copyPrefix", ds.getName()), 40);

        response.addData("name", name);
        response.addData("xid", dataSourceDao.generateUniqueXid());
        response.addData("deviceName", name);

        return response;
    }

    @DwrPermission(user = true)
    public ProcessResult copyDataSource(int dataSourceId, String name, String xid, String deviceName) {
        Permissions.ensureDataSourcePermission(Common.getUser(), dataSourceId);
        ProcessResult response = new ProcessResult();

        // Create a dummy data source with which to do the validation.
        DataSourceVO<?> ds = new MockDataSource();
        ds.setName(name);
        ds.setXid(xid);
        ds.validate(response);

        if (!response.getHasMessages()) {
            int dsId = new DataSourceDao().copyDataSource(dataSourceId, name, xid, deviceName);
            new UserDao().populateUserPermissions(Common.getUser());
            response.addData("newId", dsId);
        }

        return response;
    }

    @DwrPermission(user = true)
    public String exportDataSourceAndPoints(int dataSourceId) {
        Map<String, Object> data = new LinkedHashMap<String, Object>();
        List<DataSourceVO<?>> dss = new ArrayList<DataSourceVO<?>>();
        dss.add(new DataSourceDao().getDataSource(dataSourceId));
        data.put(EmportDwr.DATA_SOURCES, dss);
        data.put(EmportDwr.DATA_POINTS, new DataPointDao().getDataPoints(dataSourceId, null));
        return EmportDwr.export(data, 3);
    }
}
