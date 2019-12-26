/**
 * Copyright (C) 2019  Infinite Automation Software. All rights reserved.
 */
package com.infiniteautomation.mango.spring.service;

import java.util.Map;
import java.util.Map.Entry;

import javax.measure.unit.SI;
import javax.measure.unit.Unit;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.infiniteautomation.mango.util.exception.NotFoundException;
import com.infiniteautomation.mango.util.exception.ValidationException;
import com.serotonin.InvalidArgumentException;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.Common.Rollups;
import com.serotonin.m2m2.Common.TimePeriods;
import com.serotonin.m2m2.DataTypes;
import com.serotonin.m2m2.db.dao.DataPointDao;
import com.serotonin.m2m2.db.dao.DataPointTagsDao;
import com.serotonin.m2m2.db.dao.DataSourceDao;
import com.serotonin.m2m2.db.dao.TemplateDao;
import com.serotonin.m2m2.i18n.ProcessMessage;
import com.serotonin.m2m2.i18n.ProcessResult;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.util.ColorUtils;
import com.serotonin.m2m2.vo.DataPointVO;
import com.serotonin.m2m2.vo.DataPointVO.IntervalLoggingTypes;
import com.serotonin.m2m2.vo.DataPointVO.LoggingTypes;
import com.serotonin.m2m2.vo.DataPointVO.PlotTypes;
import com.serotonin.m2m2.vo.DataPointVO.SimplifyTypes;
import com.serotonin.m2m2.vo.dataSource.DataSourceVO;
import com.serotonin.m2m2.vo.permission.PermissionException;
import com.serotonin.m2m2.vo.permission.PermissionHolder;
import com.serotonin.m2m2.vo.template.DataPointPropertiesTemplateVO;
import com.serotonin.validation.StringValidation;

/**
 * @author Terry Packer
 *
 */
@Service
public class DataPointService extends AbstractVOService<DataPointVO, DataPointDao> {

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
    
    @Override
    protected DataPointVO insert(DataPointVO vo, PermissionHolder user, boolean full)
            throws PermissionException, ValidationException {
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
    protected DataPointVO update(DataPointVO existing, DataPointVO vo,
            PermissionHolder user, boolean full) throws PermissionException, ValidationException {
        ensureEditPermission(user, existing);
        
        vo.setId(existing.getId());
        ensureValid(existing, vo, user);
        Common.runtimeManager.updateDataPoint(existing, vo);
        return vo;
    }
    
    @Override
    protected DataPointVO delete(DataPointVO vo, PermissionHolder user)
            throws PermissionException, NotFoundException {
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
    public void enableDisable(String xid, boolean enabled, boolean restart, PermissionHolder user) throws NotFoundException, PermissionException {
        DataPointVO vo = get(xid, user);
        if (enabled && restart) {
            Common.runtimeManager.restartDataPoint(vo);
        } else {
            Common.runtimeManager.enableDataPoint(vo, enabled);
        }
    }
    
    @Override
    public ProcessResult validate(DataPointVO vo, PermissionHolder user) {
        ProcessResult result = commonValidation(vo, user);
        permissionService.validateVoRoles(result, "readPermission", user, false, null, vo.getReadRoles());
        permissionService.validateVoRoles(result, "setPermission", user, false, null, vo.getSetRoles());
        return result;
    }
    
    @Override
    public ProcessResult validate(DataPointVO existing, DataPointVO vo, PermissionHolder user) {
        ProcessResult result = commonValidation(vo, user);
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

        DataSourceVO<?> dsvo = DataSourceDao.getInstance().get(vo.getDataSourceId());
        if(dsvo == null) {
            response.addContextualMessage("dataSourceId", "validate.invalidValue");
            return response;
        }
        //Validate the point locator
        vo.getPointLocator().validate(response, vo, dsvo);

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

        // Check chart renderer type
        if (vo.getChartRenderer() != null) {
            if(!vo.getChartRenderer().getDef().supports(vo.getPointLocator().getDataTypeId()))
                response.addGenericMessage("validate.chart.incompatible");
            vo.getChartRenderer().validate(response);
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
        
        if((vo.getTemplateId() != null) &&(vo.getTemplateId() > 0)){
            DataPointPropertiesTemplateVO template = (DataPointPropertiesTemplateVO) TemplateDao.getInstance().get(vo.getTemplateId());
            if(template == null){
                response.addContextualMessage("template", "pointEdit.template.validate.templateNotFound", vo.getTemplateId());
            }else if(template.getDataTypeId() != vo.getPointLocator().getDataTypeId()){
                response.addContextualMessage("template", "pointEdit.template.validate.templateDataTypeNotCompatible");
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
    
}
