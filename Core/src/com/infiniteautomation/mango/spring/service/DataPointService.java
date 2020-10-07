/**
 * Copyright (C) 2019  Infinite Automation Software. All rights reserved.
 */
package com.infiniteautomation.mango.spring.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.measure.unit.SI;
import javax.measure.unit.Unit;

import org.apache.commons.lang3.StringUtils;
import org.jooq.Condition;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.Select;
import org.jooq.SelectConnectByStep;
import org.jooq.SelectHavingStep;
import org.jooq.SelectJoinStep;
import org.jooq.SelectLimitStep;
import org.jooq.SortField;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.stereotype.Service;

import com.infiniteautomation.mango.db.query.ConditionSortLimit;
import com.infiniteautomation.mango.permission.MangoPermission;
import com.infiniteautomation.mango.spring.db.DataPointTableDefinition;
import com.infiniteautomation.mango.spring.events.DaoEvent;
import com.infiniteautomation.mango.spring.events.DaoEventType;
import com.infiniteautomation.mango.util.exception.NotFoundException;
import com.infiniteautomation.mango.util.exception.ValidationException;
import com.serotonin.InvalidArgumentException;
import com.serotonin.ShouldNeverHappenException;
import com.serotonin.db.MappedRowCallback;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.Common.Rollups;
import com.serotonin.m2m2.Common.TimePeriods;
import com.serotonin.m2m2.DataTypes;
import com.serotonin.m2m2.db.dao.DataPointDao;
import com.serotonin.m2m2.db.dao.DataPointTagsDao;
import com.serotonin.m2m2.db.dao.DataSourceDao;
import com.serotonin.m2m2.db.dao.EventDetectorDao;
import com.serotonin.m2m2.i18n.ProcessMessage;
import com.serotonin.m2m2.i18n.ProcessResult;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.module.DataSourceDefinition;
import com.serotonin.m2m2.module.ModuleRegistry;
import com.serotonin.m2m2.module.definitions.dataPoint.DataPointChangeDefinition;
import com.serotonin.m2m2.rt.RTException;
import com.serotonin.m2m2.rt.dataImage.DataPointRT;
import com.serotonin.m2m2.rt.dataImage.PointValueTime;
import com.serotonin.m2m2.rt.dataImage.SetPointSource;
import com.serotonin.m2m2.rt.dataImage.types.DataValue;
import com.serotonin.m2m2.util.ColorUtils;
import com.serotonin.m2m2.vo.DataPointSummary;
import com.serotonin.m2m2.vo.DataPointVO;
import com.serotonin.m2m2.vo.DataPointVO.IntervalLoggingTypes;
import com.serotonin.m2m2.vo.DataPointVO.LoggingTypes;
import com.serotonin.m2m2.vo.DataPointVO.PlotTypes;
import com.serotonin.m2m2.vo.DataPointVO.SimplifyTypes;
import com.serotonin.m2m2.vo.dataPoint.DataPointWithEventDetectors;
import com.serotonin.m2m2.vo.dataSource.DataSourceVO;
import com.serotonin.m2m2.vo.event.detector.AbstractPointEventDetectorVO;
import com.serotonin.m2m2.vo.permission.PermissionException;
import com.serotonin.m2m2.vo.permission.PermissionHolder;
import com.serotonin.m2m2.vo.role.Role;
import com.serotonin.m2m2.vo.role.RoleVO;
import com.serotonin.validation.StringValidation;

/**
 * Service for Data Points.  Event detectors can be added to a data point via this service when updating a point,
 *  but they cannot be removed.
 *
 * @author Terry Packer
 *
 */
@Service
public class DataPointService extends AbstractVOService<DataPointVO, DataPointTableDefinition, DataPointDao> {

    private final DataSourceDao dataSourceDao;
    private final EventDetectorDao eventDetectorDao;
    private final List<DataPointChangeDefinition> changeDefinitions;

    @Autowired
    public DataPointService(DataPointDao dao, DataSourceDao dataSourceDao, EventDetectorDao eventDetectorDao, PermissionService permissionService) {
        super(dao, permissionService);
        this.dataSourceDao = dataSourceDao;
        this.eventDetectorDao = eventDetectorDao;
        this.changeDefinitions = ModuleRegistry.getDataPointChangeDefinitions();
    }

    @Override
    public boolean hasCreatePermission(PermissionHolder user, DataPointVO vo) {
        DataSourceVO ds = dataSourceDao.get(vo.getDataSourceId());
        if(ds == null) {
            return false;
        }else {
            return permissionService.hasPermission(user, ds.getEditPermission());
        }
    }

    @Override
    public boolean hasEditPermission(PermissionHolder user, DataPointVO vo) {
        return permissionService.hasPermission(user, vo.getEditPermission());
    }

    @Override
    public boolean hasReadPermission(PermissionHolder user, DataPointVO vo) {
        return permissionService.hasPermission(user, vo.getReadPermission());
    }

    /**
     * Can this user set this data point?
     * @param user
     * @param vo
     * @return
     */
    public boolean hasSetPermission(PermissionHolder user, DataPointVO vo) {
        return permissionService.hasPermission(user, vo.getSetPermission());
    }

    public void ensureSetPermission(PermissionHolder user, DataPointVO vo) {
        if(!hasSetPermission(user, vo))
            throw new PermissionException(new TranslatableMessage("permission.exception.doesNotHaveRequiredPermission", user != null ? user.getPermissionHolderName() : null), user);
    }

    @EventListener
    protected void handleRoleEvent(DaoEvent<? extends RoleVO> event) {
        if (event.getType() == DaoEventType.DELETE) {
            Role deletedRole = event.getVo().getRole();
            for (DataPointRT rt : Common.runtimeManager.getRunningDataPoints()) {
                DataPointVO point = rt.getVO();
                point.setReadPermission(point.getReadPermission().withoutRole(deletedRole));
                point.setEditPermission(point.getEditPermission().withoutRole(deletedRole));
                point.setSetPermission(point.getSetPermission().withoutRole(deletedRole));
            }
        }
    }

    @Override
    public DataPointVO insert(DataPointVO vo)
            throws PermissionException, ValidationException {
        PermissionHolder user = Common.getUser();

        //Ensure they can create
        ensureCreatePermission(user, vo);

        //Ensure id is not set
        if(vo.getId() != Common.NEW_ID) {
            ProcessResult result = new ProcessResult();
            result.addContextualMessage("id", "validate.invalidValue");
            throw new ValidationException(result);
        }

        //Generate an Xid if necessary
        if(StringUtils.isEmpty(vo.getXid()))
            vo.setXid(dao.generateUniqueXid());

        for(DataPointChangeDefinition def : changeDefinitions) {
            def.preInsert(vo);
        }

        ensureValid(vo, user);
        dao.insert(vo);

        for(DataPointChangeDefinition def : changeDefinitions) {
            def.postInsert(vo);
        }

        if(vo.isEnabled()) {
            List<AbstractPointEventDetectorVO> detectors = eventDetectorDao.getWithSource(vo.getId(), vo);
            DataPointWithEventDetectors dp = new DataPointWithEventDetectors(vo, detectors);
            Common.runtimeManager.startDataPoint(dp);
        }
        return vo;
    }

    @Override
    public DataPointVO update(DataPointVO existing, DataPointVO vo) throws PermissionException, ValidationException {
        PermissionHolder user = Common.getUser();

        ensureEditPermission(user, existing);

        vo.setId(existing.getId());

        for(DataPointChangeDefinition def : changeDefinitions) {
            def.preUpdate(vo);
        }

        ensureValid(existing, vo, user);

        Common.runtimeManager.stopDataPoint(vo.getId());
        dao.update(existing, vo);

        for(DataPointChangeDefinition def : changeDefinitions) {
            def.postUpdate(vo);
        }

        if(vo.isEnabled()) {
            List<AbstractPointEventDetectorVO> detectors = eventDetectorDao.getWithSource(vo.getId(), vo);
            DataPointWithEventDetectors dp = new DataPointWithEventDetectors(vo, detectors);
            Common.runtimeManager.startDataPoint(dp);
        }

        return vo;
    }

    @Override
    public DataPointVO delete(DataPointVO vo)
            throws PermissionException, NotFoundException {
        PermissionHolder user = Common.getUser();

        ensureDeletePermission(user, vo);

        for(DataPointChangeDefinition def : changeDefinitions) {
            def.preDelete(vo);
        }

        Common.runtimeManager.stopDataPoint(vo.getId());
        dao.delete(vo);

        for(DataPointChangeDefinition def : changeDefinitions) {
            def.postDelete(vo);
        }

        Common.eventManager.cancelEventsForDataPoint(vo.getId());
        return vo;
    }

    /**
     * Enable/Disable/Restart a data point
     * @param xid - xid of point to restart
     * @param enabled - Enable or disable the data point
     * @param restart - Restart the data point, enabled must equal true
     *
     * @throws NotFoundException
     * @throws PermissionException
     */
    public boolean setDataPointState(String xid, boolean enabled, boolean restart) throws NotFoundException, PermissionException {
        PermissionHolder user = Common.getUser();

        DataPointVO vo = get(xid);
        ensureEditPermission(user, vo);
        return setDataPointState(vo, enabled, restart);
    }

    /**
     * Reload a potentially running data point, if not running
     *  then ignore
     * @param xid
     * @return true if state changed
     */
    public boolean reloadDataPoint(String xid) {
        DataPointVO vo = get(xid);
        return reloadDataPoint(vo);
    }

    /**
     * Reload a potentially running data point, if not running
     *  then ignore
     *
     * @param id
     * @return true if state changed
     */
    public boolean reloadDataPoint(Integer id) {
        DataPointVO vo = get(id);
        return reloadDataPoint(vo);
    }

    /**
     * Reload a running point common logic
     * @param vo
     * @return true if state changed
     */
    protected boolean reloadDataPoint(DataPointVO vo) {
        ensureEditPermission(Common.getUser(), vo);
        boolean running = Common.runtimeManager.isDataPointRunning(vo.getId());
        if(running) {
            return setDataPointState(vo, true, true);
        }else {
            return false;
        }
    }

    /**
     * Set the state helper method
     * @param vo - The point to restart
     * @param enabled - Enable or disable the data point
     * @param restart - Restart the data point, enabled must equal true (will start a stopped point)
     * @return - true if the state changed
     */
    protected boolean setDataPointState(DataPointVO vo, boolean enabled, boolean restart) {
        vo.setEnabled(enabled);
        boolean dataSourceRunning = Common.runtimeManager.isDataSourceRunning(vo.getDataSourceId());

        if(!dataSourceRunning) {
            //We must check its state in the DB
            boolean enabledInDB = dao.isEnabled(vo.getId());
            if(enabledInDB && !enabled){
                dao.saveEnabledColumn(vo);
                return true;
            }else if(!enabledInDB && enabled) {
                DataPointDao.getInstance().saveEnabledColumn(vo);
                return true;
            }
        }else {
            boolean running = Common.runtimeManager.isDataPointRunning(vo.getId());
            if (running && !enabled) {
                //Running, so stop it
                Common.runtimeManager.stopDataPoint(vo.getId());
                dao.saveEnabledColumn(vo);
                return true;
            } else if (!running && enabled) {
                //Not running, so start it
                List<AbstractPointEventDetectorVO> detectors = eventDetectorDao.getWithSource(vo.getId(), vo);
                DataPointWithEventDetectors dp = new DataPointWithEventDetectors(vo, detectors);
                Common.runtimeManager.startDataPoint(dp);
                dao.saveEnabledColumn(vo);
                return true;
            }else if(enabled && restart) {
                //May be running or not, will either start or restart it (stopping a non running point will do nothing which is ok)
                Common.runtimeManager.stopDataPoint(vo.getId());
                List<AbstractPointEventDetectorVO> detectors = eventDetectorDao.getWithSource(vo.getId(), vo);
                DataPointWithEventDetectors dp = new DataPointWithEventDetectors(vo, detectors);
                Common.runtimeManager.startDataPoint(dp);
                return false;
            }
        }
        return false;
    }

    /**
     * Make a copy of the points on an existing data source, then place them onto the new data source
     *
     * @param existingDataSourceId
     * @param newDataSourceId
     * @param newDeviceName
     */
    public void copyDataSourcePoints(int existingDataSourceId, int newDataSourceId, String newDeviceName) {
        for (DataPointVO dataPoint : getDataPoints(existingDataSourceId)) {

            DataPointVO dataPointCopy = dataPoint.copy();
            dataPointCopy.setId(Common.NEW_ID);
            dataPointCopy.setXid(dao.generateUniqueXid());
            dataPointCopy.setName(dataPoint.getName());
            dataPointCopy.setDeviceName(newDeviceName);
            dataPointCopy.setDataSourceId(newDataSourceId);
            //Don't enable the point until after we add the detectors
            dataPointCopy.setEnabled(false);

            //Copy Tags
            dataPointCopy.setTags(dataPoint.getTags());

            insert(dataPointCopy);

            //Insert new event detectors
            List<AbstractPointEventDetectorVO> detectors = EventDetectorDao.getInstance().getWithSource(dataPoint.getId(), dataPointCopy);
            for (AbstractPointEventDetectorVO ped : detectors) {
                ped.setId(Common.NEW_ID);
                ped.setXid(EventDetectorDao.getInstance().generateUniqueXid());
                eventDetectorDao.insert(ped);
            }
            if(dataPoint.isEnabled()) {
                //Start him up
                setDataPointState(dataPointCopy, true, true);
            }
        }
    }

    /**
     * Query for device names on data points a user can read
     * @param conditions
     * @param sortAsc
     * @param limit
     * @param offset
     * @param callback
     */
    public void queryDeviceNames(Condition conditions, boolean sortAsc, Integer limit, Integer offset, MappedRowCallback<String> callback) {
        PermissionHolder user = Common.getUser();

        Field<Object> deviceName = this.dao.getTable().getAlias("deviceName");
        List<SortField<Object>> sort = new ArrayList<>();
        if(sortAsc) {
            sort.add(deviceName.asc());
        }else {
            sort.add(deviceName.desc());
        }

        ConditionSortLimit csl = new ConditionSortLimit(conditions, sort, limit, offset);

        SelectJoinStep<Record> select = this.dao.getSelectQuery(Collections.singletonList(deviceName));
        select = dao.joinTables(select, null);

        if(!permissionService.hasAdminRole(user)) {
            select = dao.joinPermissions(select, csl, user);
        }

        SelectConnectByStep<Record> afterWhere = conditions == null ? select : select.where(conditions);
        SelectHavingStep<Record> afterGroupBy = afterWhere.groupBy(deviceName);
        SelectLimitStep<Record> afterSort = afterGroupBy.orderBy(sort);

        Select<Record> offsetStep = afterSort;
        if (limit != null) {
            if (offset != null) {
                offsetStep = afterSort.limit(offset, limit);
            } else {
                offsetStep = afterSort.limit(limit);
            }
        }

        dao.customizedQuery(offsetStep, (ResultSetExtractor<Void>) rs -> {
            int rowNum = 0;
            while (rs.next()) {
                callback.row(rs.getString(1), rowNum);
                rowNum++;
            }
            return null;
        });
    }

    @Override
    public ProcessResult validate(DataPointVO vo, PermissionHolder user) {
        ProcessResult result = commonValidation(vo, user);

        DataSourceVO dsvo = DataSourceDao.getInstance().get(vo.getDataSourceId());
        if(dsvo == null) {
            result.addContextualMessage("dataSourceId", "validate.invalidValue");
            return result;
        }

        //Validate pl if there is one
        if(vo.getPointLocator() != null) {
            DataSourceDefinition<? extends DataSourceVO> def = ModuleRegistry.getDataSourceDefinition(vo.getPointLocator().getDataSourceType());
            if(def == null) {
                throw new ShouldNeverHappenException("No data source definition for type " + vo.getPointLocator().getDataSourceType());
            }else {
                //Validate the point locator
                def.validate(result, vo, dsvo, user);
            }
        }

        permissionService.validateVoRoles(result, "readPermission", user, false, null, vo.getReadPermission());
        permissionService.validateVoRoles(result, "editPermission", user, false, null, vo.getEditPermission());
        permissionService.validateVoRoles(result, "setPermission", user, false, null, vo.getSetPermission());
        return result;
    }

    @Override
    public ProcessResult validate(DataPointVO existing, DataPointVO vo, PermissionHolder user) {
        ProcessResult result = commonValidation(vo, user);

        //Don't allow moving to new data source
        if(existing.getDataSourceId() != vo.getDataSourceId()) {
            result.addContextualMessage("dataSourceId", "validate.dataPoint.pointChangeDataSource");
        }

        DataSourceVO dsvo = DataSourceDao.getInstance().get(vo.getDataSourceId());
        if(dsvo == null) {
            result.addContextualMessage("dataSourceId", "validate.invalidValue");
            return result;
        }

        //Validate pl if there is one
        if(vo.getPointLocator() != null) {
            DataSourceDefinition<? extends DataSourceVO> def = ModuleRegistry.getDataSourceDefinition(vo.getPointLocator().getDataSourceType());
            if(def == null) {
                throw new ShouldNeverHappenException("No data source definition for type " + vo.getPointLocator().getDataSourceType());
            }else {
                //Validate the point locator
                def.validate(result, existing, vo, dsvo, user);
            }
        }

        //Validate permissions
        permissionService.validateVoRoles(result, "readPermission", user, false, existing.getReadPermission(), vo.getReadPermission());
        permissionService.validateVoRoles(result, "editPermission", user, false, existing.getEditPermission(), vo.getEditPermission());
        permissionService.validateVoRoles(result, "setPermission", user, false, existing.getSetPermission(), vo.getSetPermission());
        return result;
    }

    /**
     * Common validation logic
     * @param vo
     * @param user
     * @return
     */
    protected ProcessResult commonValidation(DataPointVO vo, PermissionHolder user) {
        ProcessResult response = super.validate(vo, user);
        if (StringValidation.isLengthGreaterThan(vo.getDeviceName(), 255))
            response.addMessage("deviceName", new TranslatableMessage("validate.notLongerThan", 255));

        if(vo.getPointLocator() != null){
            if (vo.getPointLocator().getDataTypeId() == DataTypes.NUMERIC && (vo.getLoggingType() == DataPointVO.LoggingTypes.ON_CHANGE ||
                    vo.getLoggingType() == DataPointVO.LoggingTypes.ON_CHANGE_INTERVAL)) {
                if (vo.getTolerance() < 0)
                    response.addContextualMessage("tolerance", "validate.cannotBeNegative");
            }
        }else{
            response.addContextualMessage("pointLocator", "validate.required");
            return response;
        }

        if (!DataPointVO.LOGGING_TYPE_CODES.isValidId(vo.getLoggingType()))
            response.addContextualMessage("loggingType", "validate.invalidValue");
        if (!Common.TIME_PERIOD_CODES.isValidId(vo.getIntervalLoggingPeriodType()))
            response.addContextualMessage("intervalLoggingPeriodType", "validate.invalidValue");
        if (vo.getIntervalLoggingPeriod() <= 0)
            response.addContextualMessage("intervalLoggingPeriod", "validate.greaterThanZero");
        if (!DataPointVO.INTERVAL_LOGGING_TYPE_CODES.isValidId(vo.getIntervalLoggingType()))
            response.addContextualMessage("intervalLoggingType", "validate.invalidValue");

        if(vo.getPointLocator().getDataTypeId() == DataTypes.IMAGE || vo.getPointLocator().getDataTypeId() == DataTypes.ALPHANUMERIC ) {
            if(vo.getLoggingType() == LoggingTypes.INTERVAL && vo.getIntervalLoggingType() != IntervalLoggingTypes.INSTANT)
                response.addContextualMessage("intervalLoggingType", "validate.intervalType.incompatible",
                        DataPointVO.INTERVAL_LOGGING_TYPE_CODES.getCode(vo.getIntervalLoggingType()), DataTypes.CODES.getCode(vo.getPointLocator().getDataTypeId()));
        }

        if(vo.isPurgeOverride()) {
            if (!Common.TIME_PERIOD_CODES.isValidId(vo.getPurgeType(), TimePeriods.MILLISECONDS, TimePeriods.SECONDS, TimePeriods.MINUTES, TimePeriods.HOURS))
                response.addContextualMessage("purgeType", "validate.invalidValue");

            if (vo.getPurgePeriod() <= 0)
                response.addContextualMessage("purgePeriod", "validate.greaterThanZero");
        }

        if (vo.getTextRenderer() == null) {
            response.addContextualMessage("textRenderer", "validate.required");
        }

        if (vo.getDefaultCacheSize() < 0) {
            response.addContextualMessage("defaultCacheSize", "validate.cannotBeNegative");
        }

        if (vo.isDiscardExtremeValues() && vo.getDiscardHighLimit() <= vo.getDiscardLowLimit()) {
            response.addContextualMessage("discardHighLimit", "validate.greaterThanDiscardLow");
        }

        if(vo.getPointLocator().getDataTypeId() != DataTypes.NUMERIC && vo.getPointLocator().getDataTypeId() != DataTypes.MULTISTATE) {
            vo.setPreventSetExtremeValues(false);
        }

        if(vo.isPreventSetExtremeValues() && vo.getSetExtremeHighLimit() <= vo.getSetExtremeLowLimit()) {
            response.addContextualMessage("setExtremeHighLimit", "validate.greaterThanSetExtremeLow");
        }

        if (!StringUtils.isBlank(vo.getChartColour())) {
            try {
                ColorUtils.toColor(vo.getChartColour());
            }
            catch (InvalidArgumentException e) {
                response.addContextualMessage("chartColour", "validate.invalidValue");
            }
        }else if(vo.getChartColour() == null){
            response.addContextualMessage("chartColour", "validate.invalidValue");
        }

        if(!Common.ROLLUP_CODES.isValidId(vo.getRollup()))
            response.addContextualMessage("rollup", "validate.invalidValue");
        else if(!validateRollup(vo))
            response.addContextualMessage("rollup", "validate.rollup.incompatible", vo.getRollup());

        // Check text renderer type
        if (vo.getTextRenderer() != null) {
            ProcessResult contextResult = new ProcessResult();
            vo.getTextRenderer().validate(contextResult, vo.getPointLocator().getDataTypeId());
            for (ProcessMessage msg : contextResult.getMessages()) {
                String contextKey = msg.getContextKey();
                if (contextKey != null) {
                    msg.setContextKey("textRenderer." + contextKey);
                }
                response.addMessage(msg);
            }
        }

        // Check the plot type
        if (!DataPointVO.PLOT_TYPE_CODES.isValidId(vo.getPlotType()))
            response.addContextualMessage("plotType", "validate.invalidValue");
        if (vo.getPlotType() != PlotTypes.STEP && vo.getPointLocator().getDataTypeId() != DataTypes.NUMERIC)
            response.addContextualMessage("plotType", "validate.invalidValue");

        if(!DataPointVO.SIMPLIFY_TYPE_CODES.isValidId(vo.getSimplifyType()))
            response.addContextualMessage("simplifyType", "validate.invalidValue");
        else if(vo.getSimplifyType() == SimplifyTypes.TARGET && vo.getSimplifyTarget() < 10)
            response.addContextualMessage("simplifyTarget", "validate.greaterThan", 10);
        else if(vo.getSimplifyType() != DataPointVO.SimplifyTypes.NONE && (vo.getPointLocator().getDataTypeId() == DataTypes.ALPHANUMERIC ||
                vo.getPointLocator().getDataTypeId() == DataTypes.IMAGE))
            response.addContextualMessage("simplifyType", "validate.cannotSimplifyType", DataTypes.getDataTypeMessage(vo.getPointLocator().getDataTypeId()));

        //Validate the unit
        if(vo.getUnit() == null) {
            //For other unit validation, even though we know this is invalid
            vo.setUnit(Unit.ONE);
            response.addContextualMessage("unit", "validate.required");
        }

        if (!vo.isUseIntegralUnit()) {
            vo.setIntegralUnit(vo.getUnit().times(SI.SECOND));
        }else {
            // integral unit should have same dimensions as the default integrated unit
            if (vo.getIntegralUnit() == null) {
                response.addContextualMessage("integralUnit", "validate.required");
            }else if(!vo.getIntegralUnit().isCompatible(vo.getUnit().times(SI.SECOND))) {
                response.addContextualMessage("integralUnit", "validate.unitNotCompatible");
            }
        }

        if (!vo.isUseRenderedUnit()) {
            vo.setRenderedUnit(Unit.ONE);
        }else {
            if (vo.getRenderedUnit() == null) {
                vo.setRenderedUnit(Unit.ONE);
                response.addContextualMessage("renderedUnit", "validate.required");
            }

            if(!vo.getRenderedUnit().isCompatible(vo.getUnit())) {
                response.addContextualMessage("renderedUnit", "validate.unitNotCompatible");
            }
        }

        if (vo.isOverrideIntervalLoggingSamples()) {
            if (vo.getIntervalLoggingSampleWindowSize() <= 0) {
                response.addContextualMessage("intervalLoggingSampleWindowSize", "validate.greaterThanZero");
            }
        }

        Map<String, String> tags = vo.getTags();
        if (tags != null) {
            for (Entry<String, String> entry : tags.entrySet()) {
                String tagKey = entry.getKey();
                if (tagKey == null || entry.getValue() == null) {
                    response.addContextualMessage("tags", "validate.tagCantBeNull");
                    break;
                }
                if (DataPointTagsDao.NAME_TAG_KEY.equals(tagKey) || DataPointTagsDao.DEVICE_TAG_KEY.equals(tagKey)) {
                    response.addContextualMessage("tags", "validate.invalidTagKey");
                    break;
                }
            }
        }else {
            //ensure there is an empty map
            vo.setTags(new HashMap<>());
        }

        return response;
    }

    /**
     * Is a rollup valid based on data type?
     * @param vo
     * @return
     */
    private boolean validateRollup(DataPointVO vo) {
        boolean numeric = vo.getPointLocator().getDataTypeId() == DataTypes.NUMERIC;
        switch(vo.getRollup()) {
            case Rollups.FIRST :
            case Rollups.LAST :
            case Rollups.START :
            case Rollups.COUNT :
            case Rollups.NONE :
                return true;
            case Rollups.AVERAGE :
            case Rollups.DELTA :
            case Rollups.MINIMUM :
            case Rollups.MAXIMUM :
            case Rollups.ACCUMULATOR :
            case Rollups.SUM :
            case Rollups.INTEGRAL :
                return numeric;
            default :
                return false;
        }
    }

    /**
     *
     * @param dataSourceId
     * @return
     */
    public List<DataPointVO> getDataPoints(int dataSourceId) {
        List<DataPointVO> points = dao.getDataPoints(dataSourceId);
        PermissionHolder user = Common.getUser();
        for(DataPointVO point : points) {
            ensureReadPermission(user, point);
        }
        return points;
    }

    /**
     * Get a summary for a data point.
     *  A summary is a subset of a data point configuration.
     * @param xid
     * @return
     */
    public DataPointSummary getSummary(String xid) {
        DataPointSummary vo = dao.getSummary(xid);
        if(vo == null) {
            throw new NotFoundException();
        }
        permissionService.ensurePermission(Common.getUser(), vo.getReadPermission());
        return vo;
    }

    /**
     * Get a data point and its detectors from the database
     * @param xid
     * @return
     * @throws PermissionException
     * @throws NotFoundException
     */
    public DataPointWithEventDetectors getWithEventDetectors(String xid) throws PermissionException, NotFoundException {
        DataPointVO vo = get(xid);
        List<AbstractPointEventDetectorVO> detectors = eventDetectorDao.getWithSource(vo.getId(), vo);
        return new DataPointWithEventDetectors(vo, detectors);
    }

    /**
     * Set the value of a data point
     * @param id
     * @param valueTime
     * @param source
     * @throws NotFoundException
     * @throws PermissionException - if the setting permission holder does not have set permission
     * @throws RTException - if the point is not enabled and settable
     */
    public void setValue(int id, PointValueTime valueTime, SetPointSource source) throws NotFoundException, PermissionException, RTException {
        DataPointVO vo = get(id);
        PermissionHolder user = Common.getUser();

        ensureSetPermission(user, vo);
        Common.runtimeManager.setDataPointValue(vo.getId(), valueTime, source);
    }

    /**
     * Set the value, let Mango use now as the time of the value
     * @param id
     * @param value
     * @param source
     * @throws NotFoundException
     * @throws PermissionException - if the setting permission holder does not have set permission
     * @throws RTException - if the point is not enabled and settable
     */
    public void setValue(int id, DataValue value, SetPointSource source) throws NotFoundException, PermissionException, RTException {
        setValue(id, new PointValueTime(value, Common.timer.currentTimeMillis()), source);
    }

    /**
     * Force the point to read it's value (if supported by data source)
     * @param id
     * @throws NotFoundException
     * @throws PermissionException - if calling permission holder does not have read permission
     * @throws RTException - if point or source is disabled
     */
    public void forcePointRead(int id) throws NotFoundException, PermissionException, RTException {
        DataPointVO vo = get(id);
        Common.runtimeManager.forcePointRead(vo.getId());
    }

    /**
     * Relinquish the value of a BACnet data point
     * @param id
     * @throws NotFoundException
     * @throws PermissionException - if calling permission holder does not have read permission
     * @throws RTException - if point or source is disabled
     */
    public void reliquish(int id) throws NotFoundException, PermissionException, RTException {
        DataPointVO vo = get(id);
        Common.runtimeManager.relinquish(vo.getId());
    }

    /**
     * Get the read permission for this data point
     * @param dataPointId
     * @return
     * @throws NotFoundException
     * @throws PermissionException
     */
    public MangoPermission getReadPermission(int dataPointId) throws NotFoundException, PermissionException{
        PermissionHolder user = Common.getUser();
        Integer permissionId = dao.getReadPermissionId(dataPointId);
        if(permissionId == null) {
            throw new NotFoundException();
        }

        MangoPermission read = permissionService.get(permissionId);
        permissionService.ensurePermission(user, read);
        return read;
    }
}
