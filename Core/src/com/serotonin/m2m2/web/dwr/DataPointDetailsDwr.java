/*
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.web.dwr;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.directwebremoting.WebContextFactory;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.i18n.ProcessResult;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.rt.dataImage.AnnotatedPointValueTime;
import com.serotonin.m2m2.rt.dataImage.DataPointRT;
import com.serotonin.m2m2.rt.dataImage.PointValueFacade;
import com.serotonin.m2m2.rt.dataImage.PointValueTime;
import com.serotonin.m2m2.rt.dataImage.types.ImageValue;
import com.serotonin.m2m2.view.chart.StatisticsChartRenderer;
import com.serotonin.m2m2.vo.DataPointVO;
import com.serotonin.m2m2.vo.User;
import com.serotonin.m2m2.vo.bean.ImageValueBean;
import com.serotonin.m2m2.web.dwr.beans.DataExportDefinition;
import com.serotonin.m2m2.web.dwr.beans.PointDetailsState;
import com.serotonin.m2m2.web.dwr.beans.RenderedPointValueTime;
import com.serotonin.m2m2.web.dwr.util.DwrPermission;
import com.serotonin.m2m2.web.servlet.ImageValueServlet;
import com.serotonin.m2m2.web.taglib.Functions;

public class DataPointDetailsDwr extends DataPointDwr {
    public static PointDetailsState getPointData() {
        // Get the point from the user's session. It should have been set by the controller.
        HttpServletRequest request = WebContextFactory.get().getHttpServletRequest();
        User user = Common.getUser(request);
        DataPointVO pointVO = user.getEditPoint();

        // Create the watch list state.
        Map<String, Object> model = new HashMap<String, Object>();

        // Get the data point status from the data image.
        DataPointRT pointRT = Common.runtimeManager.getDataPoint(pointVO.getId());

        PointDetailsState state = new PointDetailsState();
        state.setId(Integer.toString(pointVO.getId()));

        PointValueTime pointValue = prepareBasePointState(Integer.toString(pointVO.getId()), state, pointVO, pointRT,
                model);
        setPrettyText(request, state, pointVO, model, pointValue);
        setChange(pointVO, state, pointRT, request, model, user);

        setEvents(pointVO, user, model);
        setMessages(state, request, "dataPointMessages.jsp", model);

        return state;
    }

    @DwrPermission(user = true)
    public ProcessResult getHistoryTableData(int limit) {
        DataPointVO pointVO = Common.getUser().getEditPoint();
        PointValueFacade facade = new PointValueFacade(pointVO.getId());

        List<PointValueTime> rawData = facade.getLatestPointValues(limit);
        List<RenderedPointValueTime> renderedData = new ArrayList<RenderedPointValueTime>(rawData.size());

        for (PointValueTime pvt : rawData) {
            RenderedPointValueTime rpvt = new RenderedPointValueTime();
            rpvt.setValue(Functions.getHtmlText(pointVO, pvt));
            rpvt.setTime(Functions.getTime(pvt));
            if (pvt.isAnnotated()) {
                AnnotatedPointValueTime apvt = (AnnotatedPointValueTime) pvt;
                rpvt.setAnnotation(apvt.getAnnotation(getTranslations()));
            }
            renderedData.add(rpvt);
        }

        ProcessResult response = new ProcessResult();
        response.addData("history", renderedData);
        addAsof(response);
        return response;
    }

    @DwrPermission(user = true)
    public ProcessResult getImageChartData(int fromYear, int fromMonth, int fromDay, int fromHour, int fromMinute,
            int fromSecond, boolean fromNone, int toYear, int toMonth, int toDay, int toHour, int toMinute,
            int toSecond, boolean toNone, int width, int height) {
        DateTimeZone dtz = Common.getUser().getDateTimeZoneInstance();
        DateTime from = createDateTime(fromYear, fromMonth, fromDay, fromHour, fromMinute, fromSecond, fromNone, dtz);
        DateTime to = createDateTime(toYear, toMonth, toDay, toHour, toMinute, toSecond, toNone, dtz);

        StringBuilder htmlData = new StringBuilder();
        htmlData.append("<img src=\"chart/ft_");
        htmlData.append(System.currentTimeMillis());
        htmlData.append('_');
        htmlData.append(fromNone ? -1 : from.getMillis());
        htmlData.append('_');
        htmlData.append(toNone ? -1 : to.getMillis());
        htmlData.append('_');
        htmlData.append(getDataPointVO().getId());
        htmlData.append(".png?w=");
        htmlData.append(width);
        htmlData.append("&h=");
        htmlData.append(height);
        htmlData.append("\" alt=\"" + translate("common.imageChart") + "\"/>");

        ProcessResult response = new ProcessResult();
        response.addData("chart", htmlData.toString());
        addAsof(response);
        return response;
    }

    @DwrPermission(user = true)
    public void getChartData(int fromYear, int fromMonth, int fromDay, int fromHour, int fromMinute, int fromSecond,
            boolean fromNone, int toYear, int toMonth, int toDay, int toHour, int toMinute, int toSecond, boolean toNone) {
        User user = Common.getUser();
        DateTimeZone dtz = user.getDateTimeZoneInstance();
        DateTime from = createDateTime(fromYear, fromMonth, fromDay, fromHour, fromMinute, fromSecond, fromNone, dtz);
        DateTime to = createDateTime(toYear, toMonth, toDay, toHour, toMinute, toSecond, toNone, dtz);
        DataExportDefinition def = new DataExportDefinition(new int[] { getDataPointVO().getId() }, from, to);
        user.setDataExportDefinition(def);
    }
    
    

    @DwrPermission(user = true)
    public ProcessResult getStatsChartData(int periodType, int period, boolean includeSum) {
        HttpServletRequest request = WebContextFactory.get().getHttpServletRequest();
        DataPointVO pointVO = Common.getUser(request).getEditPoint();

        Map<String, Object> model = new HashMap<String, Object>();
        model.put("point", pointVO);
        StatisticsChartRenderer r = new StatisticsChartRenderer(periodType, period, includeSum);
        r.addDataToModel(model, pointVO);

        ProcessResult response = new ProcessResult();
        response.addData("stats", generateContent(request, "statsChart.jsp", model));
        addAsof(response);
        return response;
    }

    private DataPointVO getDataPointVO() {
        return Common.getUser().getEditPoint();
    }

    @DwrPermission(user = true)
    public ProcessResult getFlipbookData(int limit) {
        HttpServletRequest request = WebContextFactory.get().getHttpServletRequest();
        DataPointVO vo = Common.getUser(request).getEditPoint();
        PointValueFacade facade = new PointValueFacade(vo.getId());

        List<PointValueTime> values = facade.getLatestPointValues(limit);
        Collections.reverse(values);
        List<ImageValueBean> result = new ArrayList<ImageValueBean>();
        for (PointValueTime pvt : values) {
            ImageValue imageValue = (ImageValue) pvt.getValue();
            String uri = ImageValueServlet.servletPath + ImageValueServlet.historyPrefix + pvt.getTime() + "_"
                    + vo.getId() + "." + imageValue.getTypeExtension();
            result.add(new ImageValueBean(Functions.getTime(pvt), uri));
        }

        ProcessResult response = new ProcessResult();
        response.addData("images", result);
        addAsof(response);
        return response;
    }

    private void addAsof(ProcessResult response) {
        response.addData("asof",
                new TranslatableMessage("dsDetils.asof", Functions.getFullSecondTime(System.currentTimeMillis())));
    }
}
