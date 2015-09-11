/*
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.web.mvc.controller;

import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.PageContext;

import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;
import org.springframework.web.servlet.LocaleResolver;

import com.serotonin.m2m2.db.dao.DataPointDao;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.i18n.Translations;
import com.serotonin.m2m2.vo.DataPointExtendedNameComparator;
import com.serotonin.m2m2.vo.DataPointSummary;
import com.serotonin.m2m2.vo.User;
import com.serotonin.m2m2.vo.permission.Permissions;

/**
 * @author Matthew Lohbihler
 */
public class ControllerUtils {
    public static String translate(HttpServletRequest request, String key, Object... args) {
        if (args == null || args.length == 0)
            return getTranslations(request).translate(key);
        return TranslatableMessage.translate(getTranslations(request), key, args);
    }

    public static Translations getTranslations(HttpServletRequest request) {
        return Translations.getTranslations(getLocale(request));
    }

    public static Translations getTranslations(PageContext pageContext) {
        return Translations.getTranslations(getLocale((HttpServletRequest) pageContext.getRequest()));
    }

    public static Locale getLocale(HttpServletRequest request) {
        WebApplicationContext webApplicationContext = WebApplicationContextUtils
                .getRequiredWebApplicationContext(request.getSession().getServletContext());
        LocaleResolver localeResolver = (LocaleResolver) webApplicationContext.getBean("localeResolver");
        return localeResolver.resolveLocale(request);
    }

    public static List<DataPointSummary> addPointListDataToModel(User user, int pointId, Map<String, Object> model) {
        List<DataPointSummary> allPoints = new DataPointDao()
                .getDataPointSummaries(DataPointExtendedNameComparator.instance);
        List<DataPointSummary> userPoints = new LinkedList<>();

        int pointIndex = -1;
        for (DataPointSummary dp : allPoints) {
            if (Permissions.hasDataPointReadPermission(user, dp)) {
                userPoints.add(dp);
                if (dp.getId() == pointId)
                    pointIndex = userPoints.size() - 1;
            }
        }
        model.put("userPoints", userPoints);

        // Determine next and previous ids
        if (pointIndex > 0)
            model.put("prevId", userPoints.get(pointIndex - 1).getId());
        if (pointIndex < userPoints.size() - 1)
            model.put("nextId", userPoints.get(pointIndex + 1).getId());

        return userPoints;
    }
}
