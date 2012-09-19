/*
    Copyright (C) 2006-2011 Serotonin Software Technologies Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.web.mvc.controller;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.springframework.validation.BindException;
import org.springframework.validation.Errors;
import org.springframework.web.bind.ServletRequestDataBinder;
import org.springframework.web.servlet.mvc.SimpleFormController;
import org.springframework.web.util.WebUtils;

import com.serotonin.ShouldNeverHappenException;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.DataTypes;
import com.serotonin.m2m2.db.dao.DataPointDao;
import com.serotonin.m2m2.view.chart.BaseChartRenderer;
import com.serotonin.m2m2.view.text.BaseTextRenderer;
import com.serotonin.m2m2.vo.DataPointVO;
import com.serotonin.m2m2.vo.User;
import com.serotonin.m2m2.vo.dataSource.DataPointSaveHandler;
import com.serotonin.m2m2.vo.event.PointEventDetectorVO;
import com.serotonin.m2m2.vo.permission.Permissions;
import com.serotonin.propertyEditor.DecimalFormatEditor;
import com.serotonin.propertyEditor.IntegerFormatEditor;
import com.serotonin.util.ValidationUtils;

@SuppressWarnings("deprecation")
public class DataPointEditController extends SimpleFormController {
    public static final String SUBMIT_SAVE = "save";
    public static final String SUBMIT_DISABLE = "disable";
    public static final String SUBMIT_ENABLE = "enable";
    public static final String SUBMIT_RESTART = "restart";

    @Override
    protected Object formBackingObject(HttpServletRequest request) {
        DataPointVO dataPoint;
        User user = Common.getUser(request);

        if (isFormSubmission(request)) {
            dataPoint = user.getEditPoint();
            dataPoint.setDiscardExtremeValues(false); // Checkbox
            dataPoint.setPurgeOverride(false); // Checkbox
        }
        else {
            int id;
            DataPointDao dataPointDao = new DataPointDao();

            // Get the id.
            String idStr = request.getParameter("dpid");
            if (idStr == null) {
                // Check for pedid (point event detector id)
                String pedStr = request.getParameter("pedid");
                if (pedStr == null)
                    throw new ShouldNeverHappenException("dpid or pedid must be provided for this page");

                int pedid = Integer.parseInt(pedStr);
                id = dataPointDao.getDataPointIdFromDetectorId(pedid);
            }
            else
                id = Integer.parseInt(idStr);

            dataPoint = dataPointDao.getDataPoint(id);

            // Save the point in the user object so that DWR calls have access to it.
            user.setEditPoint(dataPoint);
        }

        Permissions.ensureDataSourcePermission(user, dataPoint.getDataSourceId());
        return dataPoint;
    }

    @SuppressWarnings("rawtypes")
    @Override
    protected Map referenceData(HttpServletRequest request, Object command, Errors errors) {
        Map<String, Object> result = new HashMap<String, Object>();
        DataPointVO point = (DataPointVO) command;

        result.put("dataSource", Common.runtimeManager.getDataSource(point.getDataSourceId()));

        result.put("textRenderers", BaseTextRenderer.getImplementation(point.getPointLocator().getDataTypeId()));
        result.put("chartRenderers", BaseChartRenderer.getImplementations(point.getPointLocator().getDataTypeId()));
        result.put("eventDetectors", PointEventDetectorVO.getImplementations(point.getPointLocator().getDataTypeId()));

        ControllerUtils.addPointListDataToModel(Common.getUser(request), point.getId(), result);

        return result;
    }

    @Override
    protected void initBinder(HttpServletRequest request, ServletRequestDataBinder binder) {
        binder.registerCustomEditor(Double.TYPE, "tolerance", new DecimalFormatEditor(new DecimalFormat("#.##"), false));
        binder.registerCustomEditor(Integer.TYPE, "purgePeriod", new IntegerFormatEditor(new DecimalFormat("#"), false));
        binder.registerCustomEditor(Double.TYPE, "discardLowLimit", new DecimalFormatEditor(new DecimalFormat("#.##"),
                false));
        binder.registerCustomEditor(Double.TYPE, "discardHighLimit", new DecimalFormatEditor(new DecimalFormat("#.##"),
                false));
    }

    @Override
    protected void onBindAndValidate(HttpServletRequest request, Object command, BindException errors) throws Exception {
        DataPointVO point = (DataPointVO) command;

        if (StringUtils.isBlank(point.getName()))
            ValidationUtils.rejectValue(errors, "name", "validate.required");

        // Logging properties validation
        if (point.getLoggingType() != DataPointVO.LoggingTypes.ON_CHANGE
                && point.getLoggingType() != DataPointVO.LoggingTypes.ALL
                && point.getLoggingType() != DataPointVO.LoggingTypes.NONE
                && point.getLoggingType() != DataPointVO.LoggingTypes.INTERVAL
                && point.getLoggingType() != DataPointVO.LoggingTypes.ON_TS_CHANGE)
            ValidationUtils.rejectValue(errors, "loggingType", "validate.required");

        if (point.getLoggingType() == DataPointVO.LoggingTypes.INTERVAL) {
            if (point.getIntervalLoggingPeriod() <= 0)
                ValidationUtils.rejectValue(errors, "intervalLoggingPeriod", "validate.greaterThanZero");
        }

        if (point.getLoggingType() == DataPointVO.LoggingTypes.ON_CHANGE
                && point.getPointLocator().getDataTypeId() == DataTypes.NUMERIC) {
            if (point.getTolerance() < 0)
                ValidationUtils.rejectValue(errors, "tolerance", "validate.cannotBeNegative");
        }

        if (point.isDiscardExtremeValues() && point.getDiscardHighLimit() <= point.getDiscardLowLimit())
            ValidationUtils.rejectValue(errors, "discardHighLimit", "validate.greaterThanDiscardLow");

        if (point.isPurgeOverride()) {
            if (point.getLoggingType() != DataPointVO.LoggingTypes.NONE) {
                if (point.getPurgeType() != DataPointVO.PurgeTypes.DAYS
                        && point.getPurgeType() != DataPointVO.PurgeTypes.WEEKS
                        && point.getPurgeType() != DataPointVO.PurgeTypes.MONTHS
                        && point.getPurgeType() != DataPointVO.PurgeTypes.YEARS)
                    ValidationUtils.rejectValue(errors, "purgeType", "validate.required");

                if (point.getPurgePeriod() <= 0)
                    ValidationUtils.rejectValue(errors, "purgePeriod", "validate.greaterThanZero");
            }
        }

        if (point.getDefaultCacheSize() < 0)
            ValidationUtils.rejectValue(errors, "defaultCacheSize", "validate.cannotBeNegative");

        // Make sure that xids are unique
        List<String> xids = new ArrayList<String>();
        for (PointEventDetectorVO ped : point.getEventDetectors()) {
            if (StringUtils.isBlank(ped.getXid())) {
                ValidationUtils.reject(errors, "validate.ped.xidMissing");
                break;
            }

            if (xids.contains(ped.getXid())) {
                ValidationUtils.reject(errors, "validate.ped.xidUsed", ped.getXid());
                break;
            }
            xids.add(ped.getXid());
        }

        if (!errors.hasErrors()) {
            if (WebUtils.hasSubmitParameter(request, SUBMIT_DISABLE)) {
                point.setEnabled(false);
                ValidationUtils.reject(errors, "confirmation.pointDisabled");
            }
            else if (WebUtils.hasSubmitParameter(request, SUBMIT_ENABLE)) {
                point.setEnabled(true);
                ValidationUtils.reject(errors, "confirmation.pointEnabled");
            }
            else if (WebUtils.hasSubmitParameter(request, SUBMIT_RESTART)) {
                point.setEnabled(false);
                Common.runtimeManager.saveDataPoint(point);
                point.setEnabled(true);
                ValidationUtils.reject(errors, "confirmation.pointRestarted");
            }
            else if (WebUtils.hasSubmitParameter(request, SUBMIT_SAVE)) {
                DataPointSaveHandler saveHandler = point.getPointLocator().getDataPointSaveHandler();
                if (saveHandler != null)
                    saveHandler.handleSave(point);

                ValidationUtils.reject(errors, "confirmation.pointSaved");
            }
            else
                throw new ShouldNeverHappenException("Submission task name type not provided");

            Common.runtimeManager.saveDataPoint(point);
        }
    }
}
