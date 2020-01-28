/**
 * Copyright (C) 2019  Infinite Automation Software. All rights reserved.
 */
package com.infiniteautomation.mango.spring.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.measure.unit.SI;
import javax.measure.unit.Unit;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import com.infiniteautomation.mango.spring.db.DataPointTableDefinition;
import com.infiniteautomation.mango.util.exception.NotFoundException;
import com.infiniteautomation.mango.util.exception.ValidationException;
import com.serotonin.InvalidArgumentException;
import com.serotonin.ShouldNeverHappenException;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.Common.Rollups;
import com.serotonin.m2m2.Common.TimePeriods;
import com.serotonin.m2m2.DataTypes;
import com.serotonin.m2m2.db.dao.DataPointDao;
import com.serotonin.m2m2.db.dao.DataPointTagsDao;
import com.serotonin.m2m2.db.dao.DataSourceDao;
import com.serotonin.m2m2.db.dao.RoleDao.RoleDeletedDaoEvent;
import com.serotonin.m2m2.i18n.ProcessMessage;
import com.serotonin.m2m2.i18n.ProcessResult;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.module.DataSourceDefinition;
import com.serotonin.m2m2.module.ModuleRegistry;
import com.serotonin.m2m2.rt.dataImage.DataPointRT;
import com.serotonin.m2m2.util.ColorUtils;
import com.serotonin.m2m2.vo.DataPointSummary;
import com.serotonin.m2m2.vo.DataPointVO;
import com.serotonin.m2m2.vo.DataPointVO.IntervalLoggingTypes;
import com.serotonin.m2m2.vo.DataPointVO.LoggingTypes;
import com.serotonin.m2m2.vo.DataPointVO.PlotTypes;
import com.serotonin.m2m2.vo.DataPointVO.SimplifyTypes;
import com.serotonin.m2m2.vo.dataSource.DataSourceVO;
import com.serotonin.m2m2.vo.event.detector.AbstractEventDetectorVO;
import com.serotonin.m2m2.vo.event.detector.AbstractPointEventDetectorVO;
import com.serotonin.m2m2.vo.permission.PermissionException;
import com.serotonin.m2m2.vo.permission.PermissionHolder;
import com.serotonin.m2m2.vo.role.Role;
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

    @Autowired
    public DataPointService(DataPointDao dao, PermissionService permissionService) {
        super(dao, permissionService);
    }

    @Override
    public boolean hasCreatePermission(PermissionHolder user, DataPointVO vo) {
        return permissionService.hasDataSourcePermission(user, vo.getDataSourceId());
    }

    @Override
    public boolean hasEditPermission(PermissionHolder user, DataPointVO vo) {
        return permissionService.hasDataSourcePermission(user, vo.getDataSourceId());
    }

    @Override
    public boolean hasReadPermission(PermissionHolder user, DataPointVO vo) {
        return permissionService.hasDataPointReadPermission(user, vo);
    }

    /**
     * Can this user set this data point?
     * @param user
     * @param vo
     * @return
     */
    public boolean hasSetPermission(PermissionHolder user, DataPointVO vo) {
        return permissionService.hasDataPointSetPermission(user, vo);
    }

    @Override
    @EventListener
    protected void handleRoleDeletedEvent(RoleDeletedDaoEvent event) {
        for(DataPointRT rt : Common.runtimeManager.getRunningDataPoints()) {
            if(rt.getVO().getReadRoles().contains(event.getRole().getRole())) {
                Set<Role> newReadRoles = new HashSet<>(rt.getVO().getReadRoles());
                newReadRoles.remove(event.getRole().getRole());
                rt.getVO().setReadRoles(Collections.unmodifiableSet(newReadRoles));
            }
            if(rt.getVO().getSetRoles().contains(event.getRole().getRole())) {
                Set<Role> newSetRoles = new HashSet<>(rt.getVO().getSetRoles());
                newSetRoles.remove(event.getRole().getRole());
                rt.getVO().setSetRoles(Collections.unmodifiableSet(newSetRoles));
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

        ensureValid(vo, user);
        Common.runtimeManager.insertDataPoint(vo);
        return vo;
    }

    @Override
    public DataPointVO update(DataPointVO existing, DataPointVO vo) throws PermissionException, ValidationException {
        PermissionHolder user = Common.getUser();
        ensureEditPermission(user, existing);

        vo.setId(existing.getId());
        ensureValid(existing, vo, user);

        //The existing point will have its event detectors set from the db, we want to merge those with the new ones
        if(vo.getEventDetectors() == null || vo.getEventDetectors().isEmpty()) {
            vo.setEventDetectors(existing.getEventDetectors());
        }else {
            List<AbstractPointEventDetectorVO> toKeep = new ArrayList<>(existing.getEventDetectors());
            for(AbstractEventDetectorVO ed : existing.getEventDetectors()) {
                Iterator<AbstractPointEventDetectorVO> it = vo.getEventDetectors().iterator();
                while(it.hasNext()) {
                    AbstractPointEventDetectorVO ped = it.next();
                    if(ped.getId() == ed.getId()) {
                        //same so we only keep the existing one
                        it.remove();
                    }
                }
            }
            vo.getEventDetectors().addAll(toKeep);
        }
        Common.runtimeManager.updateDataPoint(existing, vo);
        return vo;
    }

    @Override
    public DataPointVO delete(DataPointVO vo)
            throws PermissionException, NotFoundException {
        PermissionHolder user = Common.getUser();
        ensureDeletePermission(user, vo);
        Common.runtimeManager.deleteDataPoint(vo);
        return vo;
    }

    /**
     * Enable/Disable/Restart a data point
     * @param xid - xid of point to restart
     * @param enabled - Enable or disable the data point
     * @param restart - Restart the data point, enabled must equal true
     * @param user
     *
     * @throws NotFoundException
     * @throws PermissionException
     */
    public void enableDisable(String xid, boolean enabled, boolean restart) throws NotFoundException, PermissionException {
        PermissionHolder user = Common.getUser();
        DataPointVO vo = get(xid);
        permissionService.ensureDataSourcePermission(user, vo.getDataSourceId());
        if (enabled && restart) {
            Common.runtimeManager.restartDataPoint(vo);
        } else {
            Common.runtimeManager.enableDataPoint(vo, enabled);
        }
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

        permissionService.validateVoRoles(result, "readPermission", user, false, null, vo.getReadRoles());
        permissionService.validateVoRoles(result, "setPermission", user, false, null, vo.getSetRoles());
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
        permissionService.validateVoRoles(result, "readPermission", user, false, existing.getReadRoles(), vo.getReadRoles());
        permissionService.validateVoRoles(result, "setPermission", user, false, existing.getSetRoles(), vo.getSetRoles());
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
     * @param full
     * @param holder
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
        PermissionHolder user = Common.getUser();
        this.permissionService.ensureDataPointReadPermission(user, vo);
        return vo;
    }

}
