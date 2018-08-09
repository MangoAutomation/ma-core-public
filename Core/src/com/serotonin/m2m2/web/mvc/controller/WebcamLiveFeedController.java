/*
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.web.mvc.controller;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.ParameterizableViewController;

/**
 * @author Matthew Lohbihler
 */
public class WebcamLiveFeedController extends ParameterizableViewController {
    @Override
    protected ModelAndView handleRequestInternal(HttpServletRequest request, HttpServletResponse response) {
        // TODO

        //        int pointId = Integer.parseInt(request.getParameter("pointId"));
        //        DataPointDao dataPointDao = DataPointDao.getInstance();
        //        DataPointVO dp = dataPointDao.getDataPoint(pointId);

        //        if (!(dp.getPointLocator() instanceof HttpImagePointLocatorVO))
        //            throw new Exception("Point is not an HTTP Image point");
        //
        //        // User user = Common.getUser(request);
        //        // Permissions.ensureDataPointReadPermission(user, dp);
        //
        Map<String, Object> model = new HashMap<String, Object>();
        //        model.put("code", ((HttpImagePointLocatorVO) dp.getPointLocator()).getWebcamLiveFeedCode());

        return new ModelAndView(getViewName(), model);
    }
}
