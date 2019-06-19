/*
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.web.dwr;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.apache.commons.lang3.StringUtils;
import org.springframework.dao.DuplicateKeyException;

import com.infiniteautomation.mango.util.ConfigurationExportData;
import com.serotonin.ShouldNeverHappenException;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.LicenseViolatedException;
import com.serotonin.m2m2.db.dao.DataPointDao;
import com.serotonin.m2m2.db.dao.DataSourceDao;
import com.serotonin.m2m2.db.dao.EventDao;
import com.serotonin.m2m2.db.dao.TemplateDao;
import com.serotonin.m2m2.i18n.ProcessResult;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.rt.dataSource.DataSourceRT;
import com.serotonin.m2m2.rt.event.AlarmLevels;
import com.serotonin.m2m2.rt.event.EventInstance;
import com.serotonin.m2m2.vo.DataPointNameComparator;
import com.serotonin.m2m2.vo.DataPointVO;
import com.serotonin.m2m2.vo.User;
import com.serotonin.m2m2.vo.dataSource.BasicDataSourceVO;
import com.serotonin.m2m2.vo.dataSource.DataSourceVO;
import com.serotonin.m2m2.vo.dataSource.PointLocatorVO;
import com.serotonin.m2m2.vo.event.detector.AbstractPointEventDetectorVO;
import com.serotonin.m2m2.vo.permission.PermissionException;
import com.serotonin.m2m2.vo.permission.Permissions;
import com.serotonin.m2m2.vo.template.DataPointPropertiesTemplateVO;
import com.serotonin.m2m2.web.dwr.beans.EventInstanceBean;
import com.serotonin.m2m2.web.dwr.util.DwrPermission;
import com.serotonin.m2m2.web.taglib.Functions;
import com.serotonin.validation.StringValidation;

/**
 * @author Matthew Lohbihler
 */
public class DataSourceEditDwr extends DataSourceListDwr {
    //
    //
    // Common methods
    //
    @DwrPermission(user = true)
    public ProcessResult editInit() {
        ProcessResult response = new ProcessResult();
        response.addData("points", getPoints());
        response.addData("alarms", getAlarms());
        return response;
    }

    protected void setBasicProps(DataSourceVO<?> ds, BasicDataSourceVO basic) {
        ds.setName(basic.getName());
        ds.setXid(basic.getXid());
        ds.setEditPermission(basic.getEditPermission());
        ds.setPurgeOverride(basic.isPurgeOverride());
        ds.setPurgeType(basic.getPurgeType());
        ds.setPurgePeriod(basic.getPurgePeriod());
        ds.setEnabled(basic.isEnabled());
    }

    protected ProcessResult tryDataSourceSave(DataSourceVO<?> ds) {
        ProcessResult response = new ProcessResult();

        User user = Common.getHttpUser();
        if(!Permissions.hasPermission(user, ds.getEditPermission())) {
            response.addContextualMessage("dataSource.editPermission", "validate.mustHaveEditPermission");
            return response;
        }
        ds.validate(response);

        if (!response.getHasMessages()) {
            Common.runtimeManager.saveDataSource(ds);
            response.addData("id", ds.getId());
            response.addData("vo", ds); //For new table method
            user.setEditDataSource(ds);
        }

        return response;
    }

    @DwrPermission(user = true)
    public void cancelTestingUtility() {
        Common.getHttpUser().cancelTestingUtility();
    }

    @DwrPermission(user = true)
    public List<DataPointVO> getPoints() {
        User user = Common.getHttpUser();
        if (user == null)
            return null;

        DataSourceVO<?> ds = user.getEditDataSource();
        if (ds.getId() == Common.NEW_ID)
            return null;

        List<DataPointVO> points = DataPointDao.getInstance()
                .getDataPoints(ds.getId(), DataPointNameComparator.instance, false);
        return points;
    }

    @DwrPermission(user = true)
    public DataPointVO getPoint(int pointId) {
        return getPoint(pointId, null, null);
    }

    /**
     *
     * @param pointId
     * @param setDefaults - Called when getting a new data point to fill default values
     * @param updateDefaults - Called when getting an existing data point to fill default values
     * @return
     */
    protected DataPointVO getPoint(int pointId, Consumer<DataPointVO> setDefaults, Consumer<DataPointVO> updateDefaults) {
        //Added to allow saving point settings from data point edit view, this can be called by a background thread
        // so we must be sure to get the user from there if necessary
        User user = Common.getHttpUser();
        if(user == null)
            user = Common.getBackgroundContextUser();
        DataPointVO dp = user.getEditPoint();
        DataSourceVO<?> ds = user.getEditDataSource();

        if(ds.getId() == Common.NEW_ID)
            throw new ShouldNeverHappenException("Please Save Data Source First.");

        if(pointId == Common.NEW_ID){
            String deviceName;
            String readPermission = null;
            String setPermission = null;
            if(dp != null){
                deviceName = dp.getDeviceName();
                readPermission = dp.getReadPermission();
                setPermission = dp.getSetPermission();
            }else{
                deviceName = ds.getName();
            }
            dp = new DataPointVO();
            dp.setXid(DataPointDao.getInstance().generateUniqueXid());
            dp.setDeviceName(deviceName);
            dp.setId(pointId);
            dp.setDataSourceId(ds.getId());
            dp.setDataSourceTypeName(ds.getDefinition().getDataSourceTypeName());
            dp.setReadPermission(readPermission);
            dp.setSetPermission(setPermission);

            dp.setPointLocator(ds.createPointLocator());
            dp.setEventDetectors(new ArrayList<AbstractPointEventDetectorVO<?>>(0));
            dp.defaultTextRenderer();
        }else{
            //Only get a new point if it doesn't match our editing point as there are modifications we want to
            // retain in the user's editing point
            if(dp.getId() != pointId)
                dp = DataPointDao.getInstance().getFull(pointId);
        }

        //Use the defaulter
        if (dp != null) {
            if (dp.getId() == Common.NEW_ID && setDefaults != null)
                setDefaults.accept(dp);
            else if(updateDefaults != null)
                updateDefaults.accept(dp);
        }

        //Ensure we don't assign points to the wrong data source type, basically ensure the editing source is the one we
        // should use for this point.  If not we must fail.
        if (!(dp.getDataSourceTypeName().equals(ds.getDefinition().getDataSourceTypeName()))
                || (dp.getDataSourceId() != ds.getId())) {
            throw new RuntimeException("Data point type mismatch to data source type, unable to save.  Are you working with multiple tabs open?");
        }

        user.setEditPoint(dp);
        return dp;
    }

    //These methods will save the point if it is valid.
    protected ProcessResult validatePoint(int id, String xid, String name, PointLocatorVO<?> locator) {
        return validatePoint(id, xid, name, locator, true, null, null, null);
    }
    protected ProcessResult validatePoint(int id, String xid, String name, PointLocatorVO<?> locator, boolean includePointList) {
        return validatePoint(id, xid, name, locator, includePointList, null, null, null);
    }

    protected ProcessResult validatePoint(int id, String xid, String name, PointLocatorVO<?> locator, boolean includePointList, Consumer<DataPointVO> setDefaults, Consumer<DataPointVO> updateDefaults, Consumer<DataPointVO> postSave) {
        ProcessResult response = new ProcessResult();

        //This saving of the point into the User is a bad idea, need to rework to
        // pass the point back and forth to page.
        DataPointVO dp = getPoint(id, setDefaults, updateDefaults);
        dp.setXid(xid);
        dp.setName(name);
        dp.setPointLocator(locator);

        //Confirm that we are assigning a point to the correct data source
        DataSourceVO<?> ds = DataSourceDao.getInstance().get(dp.getDataSourceId());
        PointLocatorVO<?> plvo = ds.createPointLocator();
        if (plvo.getClass() != locator.getClass()) {
            response.addGenericMessage("validate.invalidType");
            return response;
        }

        //If we are a new point then only validate the basics
        if (id == Common.NEW_ID) {
            if (StringUtils.isBlank(xid))
                response.addContextualMessage("xid", "validate.required");
            else if (StringValidation.isLengthGreaterThan(xid, 100))
                response.addMessage("xid", new TranslatableMessage("validate.notLongerThan", 100));
            else if (!DataPointDao.getInstance().isXidUnique(xid, id))
                response.addContextualMessage("xid", "validate.xidUsed");

            if (StringUtils.isBlank(name))
                response.addContextualMessage("name", "validate.required");

            //Load in the default Template
            DataPointPropertiesTemplateVO template = TemplateDao.getInstance().getDefaultDataPointTemplate(locator.getDataTypeId());
            if(template != null){
                template.updateDataPointVO(dp);
            }

            //Should really be done elsewhere
            dp.setEventDetectors(new ArrayList<AbstractPointEventDetectorVO<?>>());

        } else if (id == DataPointDwr.COPY_ID) {
            dp.setId(Common.NEW_ID);
            if (StringUtils.isBlank(xid))
                response.addContextualMessage("xid", "validate.required");
            else if (StringValidation.isLengthGreaterThan(xid, 100))
                response.addMessage("xid", new TranslatableMessage("validate.notLongerThan", 100));
            else if (!DataPointDao.getInstance().isXidUnique(xid, id))
                response.addContextualMessage("xid", "validate.xidUsed");

            if (StringUtils.isBlank(name))
                response.addContextualMessage("name", "validate.required");
        } else {
            //New validation on save for all settings on existing points
            dp.validate(response);
        }
        //Validate Locator
        locator.validate(response, dp, ds);

        if (!response.getHasMessages()) {

            try {
                Common.runtimeManager.saveDataPoint(dp);
            } catch(DuplicateKeyException e) {
                response.addGenericMessage("pointEdit.detectors.duplicateXid");
                return response;
            } catch(LicenseViolatedException e) {
                response.addMessage(e.getErrorMessage());
                return response;
            }
            if(postSave != null)
                postSave.accept(dp);
            response.addData("id", dp.getId());
            response.addData("vo", dp);
            if (includePointList)
                response.addData("points", getPoints());
            //Set the User Point, this can be called by a background thread
            // so we must be sure to get the user from there if necessary
            User user = Common.getHttpUser();
            if(user == null)
                user = Common.getBackgroundContextUser();
            user.setEditPoint(dp);
        }

        return response;
    }

    @DwrPermission(user = true)
    public List<DataPointVO> deletePoint(int id) {
        DataPointVO dp = getPoint(id);
        if (dp != null)
            Common.runtimeManager.deleteDataPoint(dp);

        return getPoints();
    }

    @DwrPermission(user = true)
    public Map<String, Object> toggleEditDataSource() {
        DataSourceVO<?> ds = Common.getHttpUser().getEditDataSource();
        Map<String, Object> result = super.toggleDataSource(ds.getId());
        ds.setEnabled((Boolean) result.get("enabled"));
        return result;
    }

    @DwrPermission(user = true)
    public ProcessResult togglePoint(int dataPointId) {
        ProcessResult response = super.toggleDataPoint(dataPointId);
        response.addData("points", getPoints());
        return response;
    }

    @DwrPermission(user = true)
    public List<EventInstanceBean> getAlarms() {
        DataSourceVO<?> ds = Common.getHttpUser().getEditDataSource();
        List<EventInstance> events = EventDao.getInstance().getPendingEventsForDataSource(ds.getId(), Common.getHttpUser().getId());
        List<EventInstanceBean> beans = new ArrayList<>();
        if (events != null) {
            for (EventInstance event : events)
                beans.add(new EventInstanceBean(event.isActive(), event.getAlarmLevel(), Functions.getTime(event
                        .getActiveTimestamp()), translate(event.getMessage())));
        }
        return beans;
    }

    @DwrPermission(user = true)
    public void updateEventAlarmLevel(int eventId, AlarmLevels alarmLevel) {
        DataSourceVO<?> ds = Common.getHttpUser().getEditDataSource();
        ds.setAlarmLevel(eventId, alarmLevel);
    }

    @DwrPermission(user = true)
    public String exportDataSource() {
        Map<String, Object> data = new LinkedHashMap<>();
        List<DataSourceVO<?>> dss = new ArrayList<>();
        dss.add(Common.getHttpUser().getEditDataSource());
        data.put(ConfigurationExportData.DATA_SOURCES, dss);
        return EmportDwr.export(data);
    }

    @DwrPermission(user = true)
    public String exportDataPoint(int dataPointId) {
        DataSourceVO<?> ds = Common.getHttpUser().getEditDataSource();
        DataPointVO dp = DataPointDao.getInstance().getDataPoint(dataPointId);
        if (dp == null)
            return null;
        if (dp.getDataSourceId() != ds.getId())
            throw new PermissionException(new TranslatableMessage("common.default", "Wrong data source"), Common.getHttpUser());

        Map<String, Object> data = new LinkedHashMap<>();
        List<DataPointVO> dss = new ArrayList<>();
        dss.add(dp);
        data.put(ConfigurationExportData.DATA_POINTS, dss);
        return EmportDwr.export(data);
    }

    @DwrPermission(user = true)
    public final ProcessResult getGeneralStatusMessages() {
        ProcessResult result = new ProcessResult();

        DataSourceVO<?> vo = Common.getHttpUser().getEditDataSource();
        DataSourceRT<?> rt = Common.runtimeManager.getRunningDataSource(vo.getId());

        List<TranslatableMessage> messages = new ArrayList<>();
        result.addData("messages", messages);
        if (rt == null)
            messages.add(new TranslatableMessage("dsEdit.notEnabled"));
        else {
            rt.addStatusMessages(messages);
            if (messages.isEmpty())
                messages.add(new TranslatableMessage("dsEdit.noStatus"));
        }

        return result;
    }

    //
    // Data purge
    //
    @DwrPermission(user = true)
    public long purgeNow(int purgeType, int purgePeriod, boolean allData) {

        DataSourceVO<?> ds = Common.getHttpUser().getEditDataSource();
        if (ds.getId() == Common.NEW_ID)
            return 0;

        List<DataPointVO> points = DataPointDao.getInstance()
                .getDataPoints(ds.getId(), DataPointNameComparator.instance, false);

        Long count = 0L;
        for (DataPointVO point : points) {
            if (allData)
                count += Common.runtimeManager.purgeDataPointValues(point.getId());
            else
                count += Common.runtimeManager.purgeDataPointValues(point.getId(), purgeType, purgePeriod);
        }
        return count;
    }

}
