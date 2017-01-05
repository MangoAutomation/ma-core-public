/*
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.web.mvc.controller;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.springframework.web.servlet.View;

import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.dao.DataPointDao;
import com.serotonin.m2m2.db.dao.EventDao;
import com.serotonin.m2m2.db.dao.EventDetectorDao;
import com.serotonin.m2m2.db.dao.UserDao;
import com.serotonin.m2m2.module.definitions.event.detectors.PointEventDetectorDefinition;
import com.serotonin.m2m2.view.chart.ImageChartRenderer;
import com.serotonin.m2m2.view.chart.ImageFlipbookRenderer;
import com.serotonin.m2m2.view.chart.TableChartRenderer;
import com.serotonin.m2m2.vo.DataPointSummary;
import com.serotonin.m2m2.vo.DataPointVO;
import com.serotonin.m2m2.vo.User;
import com.serotonin.m2m2.vo.permission.Permissions;
import com.serotonin.m2m2.web.mvc.UrlHandler;
import com.serotonin.util.CollectionUtils;

public class DataPointDetailsController implements UrlHandler {
    @Override
    public View handleRequest(HttpServletRequest request, HttpServletResponse response, Map<String, Object> model)
            throws Exception {
        User user = Common.getUser(request);

        int id = -1;
        if (user.getEditPoint() != null)
            id = user.getEditPoint().getId();

        DataPointDao dataPointDao = DataPointDao.instance;
        String idStr = request.getParameter("dpid");
        DataPointVO point = null;

        if (StringUtils.isBlank(idStr)) {
            // Check for pedid (point event detector id)
            String pedStr = request.getParameter("pedid");
            if (StringUtils.isBlank(pedStr)) {
                // Check if an XID was provided.
                String xid = request.getParameter("dpxid");
                if (!StringUtils.isBlank(xid)) {
                    model.put("currentXid", xid);
                    point = dataPointDao.getDataPoint(xid);
                    id = point == null ? -2 : point.getId();
                }
            }
            else {
                int pedid = Integer.parseInt(pedStr);
                id = EventDetectorDao.instance.getSourceId(pedid, PointEventDetectorDefinition.SOURCE_TYPE_NAME);
            }
        }
        else
            id = Integer.parseInt(idStr);

        // Find accessible points for the goto list
        List<DataPointSummary> userPoints = ControllerUtils.addPointListDataToModel(user, id, model);

        // Get the point.
        if (point == null && id != -1)
            point = dataPointDao.getDataPoint(id);

        if (point == null && id != -2 /* -2 means an explicit XID was provided but not found */&& !userPoints.isEmpty()) {
            //Load at least 1 point, there may be many points but some might not actually load if thier data source DNE anymore
            for (DataPointSummary userPoint : userPoints) {
                point = dataPointDao.getDataPoint(userPoint.getId());
                if (point != null)
                    break;
            }
        }

        if (point != null) {
            // Check permissions
            Permissions.ensureDataPointReadPermission(user, point);

            // Put the point in the model.
            model.put("point", point);

            // Get the users that have access to this point.
            List<User> allUsers = UserDao.instance.getUsers();
            List<Map<String, Object>> users = new LinkedList<>();
            Map<String, Object> userData;
            int accessType;
            for (User mangoUser : allUsers) {
                accessType = Permissions.getDataPointAccessType(mangoUser, point);
                if (accessType != Permissions.DataPointAccessTypes.NONE) {
                    userData = new HashMap<>();
                    userData.put("user", mangoUser);
                    userData.put("accessType", accessType);
                    users.add(userData);
                }
            }
            model.put("users", users);

            // Determine whether the link to edit the point should be displayed
            model.put("pointEditor", Permissions.hasDataSourcePermission(user, point.getDataSourceId()));

            // Put the events in the model.
            model.put("events", new EventDao().getEventsForDataPoint(id, user.getId()));

            // Put the default history table count into the model. Default to 10.
            int historyLimit = 10;
            if (point.getChartRenderer() instanceof TableChartRenderer)
                historyLimit = ((TableChartRenderer) point.getChartRenderer()).getLimit();
            else if (point.getChartRenderer() instanceof ImageFlipbookRenderer)
                historyLimit = ((ImageFlipbookRenderer) point.getChartRenderer()).getLimit();
            model.put("historyLimit", historyLimit);

            // Determine our image chart rendering capabilities.
            if (ImageChartRenderer.getDefinition().supports(point.getPointLocator().getDataTypeId())) {
                // This point can render an image chart. Carry on...
                int periodType = Common.TimePeriods.DAYS;
                int periodCount = 1;
                if (point.getChartRenderer() instanceof ImageChartRenderer) {
                    ImageChartRenderer r = (ImageChartRenderer) point.getChartRenderer();
                    periodType = r.getTimePeriod();
                    periodCount = r.getNumberOfPeriods();
                }
                model.put("periodType", periodType);
                model.put("periodCount", periodCount);
            }

            // Determine out flipbook rendering capabilities
            if (ImageFlipbookRenderer.getDefinition().supports(point.getPointLocator().getDataTypeId()))
                model.put("flipbookLimit", 10);

            // Set the point in the session for the dwr.
            user.setEditPoint(point);

            model.put("currentXid", point.getXid());
            model.put("hierPath",
                    CollectionUtils.implode(dataPointDao.getPointHierarchy(true).getPath(id), " &raquo; "));
        }

        return null;
    }

    //    @Override
    //    protected ModelAndView handleRequestInternal(HttpServletRequest request, HttpServletResponse response)
    //            throws Exception {
    //        Map<String, Object> model = new HashMap<String, Object>();
    //        User user = Common.getUser(request);
    //
    //        int id = -1;
    //        if (user.getEditPoint() != null)
    //            id = user.getEditPoint().getId();
    //
    //        DataPointDao dataPointDao = DataPointDao.instance;
    //        String idStr = request.getParameter("dpid");
    //        DataPointVO point = null;
    //
    //        if (StringUtils.isBlank(idStr)) {
    //            // Check for pedid (point event detector id)
    //            String pedStr = request.getParameter("pedid");
    //            if (StringUtils.isBlank(pedStr)) {
    //                // Check if an XID was provided.
    //                String xid = request.getParameter("dpxid");
    //                if (!StringUtils.isBlank(xid)) {
    //                    model.put("currentXid", xid);
    //                    point = dataPointDao.getDataPoint(xid);
    //                    id = point == null ? -2 : point.getId();
    //                }
    //            }
    //            else {
    //                int pedid = Integer.parseInt(pedStr);
    //                id = dataPointDao.getDataPointIdFromDetectorId(pedid);
    //            }
    //        }
    //        else
    //            id = Integer.parseInt(idStr);
    //
    //        // Find accessible points for the goto list
    //        List<DataPointSummary> userPoints = ControllerUtils.addPointListDataToModel(user, id, model);
    //
    //        // Get the point.
    //        if (point == null && id != -1)
    //            point = dataPointDao.getDataPoint(id);
    //
    //        if (point == null && id != -2 /* -2 means an explicit XID was provided but not found */&& !userPoints.isEmpty())
    //            point = dataPointDao.getDataPoint(userPoints.get(0).getId());
    //
    //        if (point != null) {
    //            // Check permissions
    //            Permissions.ensureDataPointReadPermission(user, point);
    //
    //            // Put the point in the model.
    //            model.put("point", point);
    //
    //            // Get the users that have access to this point.
    //            List<User> allUsers = UserDao.instance.getUsers();
    //            List<Map<String, Object>> users = new LinkedList<Map<String, Object>>();
    //            Map<String, Object> userData;
    //            int accessType;
    //            for (User mangoUser : allUsers) {
    //                accessType = Permissions.getDataPointAccessType(mangoUser, point);
    //                if (accessType != Permissions.DataPointAccessTypes.NONE) {
    //                    userData = new HashMap<String, Object>();
    //                    userData.put("user", mangoUser);
    //                    userData.put("accessType", accessType);
    //                    users.add(userData);
    //                }
    //            }
    //            model.put("users", users);
    //
    //            // Determine whether the link to edit the point should be displayed
    //            model.put("pointEditor", Permissions.hasDataSourcePermission(user, point.getDataSourceId()));
    //
    //            // Put the events in the model.
    //            model.put("events", new EventDao().getEventsForDataPoint(id, user.getId()));
    //
    //            // Put the default history table count into the model. Default to 10.
    //            int historyLimit = 10;
    //            if (point.getChartRenderer() instanceof TableChartRenderer)
    //                historyLimit = ((TableChartRenderer) point.getChartRenderer()).getLimit();
    //            else if (point.getChartRenderer() instanceof ImageFlipbookRenderer)
    //                historyLimit = ((ImageFlipbookRenderer) point.getChartRenderer()).getLimit();
    //            model.put("historyLimit", historyLimit);
    //
    //            // Determine our image chart rendering capabilities.
    //            if (ImageChartRenderer.getDefinition().supports(point.getPointLocator().getDataTypeId())) {
    //                // This point can render an image chart. Carry on...
    //                int periodType = Common.TimePeriods.DAYS;
    //                int periodCount = 1;
    //                if (point.getChartRenderer() instanceof ImageChartRenderer) {
    //                    ImageChartRenderer r = (ImageChartRenderer) point.getChartRenderer();
    //                    periodType = r.getTimePeriod();
    //                    periodCount = r.getNumberOfPeriods();
    //                }
    //                model.put("periodType", periodType);
    //                model.put("periodCount", periodCount);
    //            }
    //
    //            // Determine out flipbook rendering capabilities
    //            if (ImageFlipbookRenderer.getDefinition().supports(point.getPointLocator().getDataTypeId()))
    //                model.put("flipbookLimit", 10);
    //
    //            // Set the point in the session for the dwr.
    //            user.setEditPoint(point);
    //
    //            model.put("currentXid", point.getXid());
    //            model.put("hierPath",
    //                    CollectionUtils.implode(dataPointDao.getPointHierarchy(true).getPath(id), " &raquo; "));
    //        }
    //
    //        return new ModelAndView(getViewName(), model);
    //    }
}
