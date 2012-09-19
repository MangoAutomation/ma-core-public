/*
    Copyright (C) 2006-2011 Serotonin Software Technologies Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.web.mvc.controller;

import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.PageContext;

import org.apache.commons.lang3.StringUtils;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;
import org.springframework.web.servlet.LocaleResolver;

import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.dao.DataPointDao;
import com.serotonin.m2m2.db.dao.UserDao;
import com.serotonin.m2m2.i18n.ProcessResult;
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
        List<DataPointSummary> userPoints = new LinkedList<DataPointSummary>();

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

    public static ProcessResult tryLogin(HttpServletRequest request, String username, String password) {
        ProcessResult result = new ProcessResult();

        // Make sure there is a username
        if (StringUtils.isBlank(username))
            result.addContextualMessage("username", "login.validation.noUsername");

        // Make sure there is a password
        if (StringUtils.isBlank(password))
            result.addContextualMessage("password", "login.validation.noPassword");

        // If there are no errors yet, try validating the username and password.
        if (!result.getHasMessages()) {
            String passwordHash = Common.encrypt(password);

            UserDao userDao = new UserDao();
            User user = userDao.getUser(username);

            if (user == null || !passwordHash.equals(user.getPassword()))
                result.addGenericMessage("login.validation.invalidLogin");
            else if (user.isDisabled())
                result.addGenericMessage("login.validation.accountDisabled");

            // If there still are no errors, continue with the login.
            if (!result.getHasMessages()) {
                // Check if the user is already logged in.
                if (user.equals(Common.getUser(request))) {
                    // The user is already logged in. Nothing to do.
                }
                else {
                    // Update the last login time.
                    userDao.recordLogin(user.getId());

                    // Add the user object to the session. This indicates to the rest
                    // of the application whether the user is logged in or not.
                    Common.setUser(request, user);
                }

                result.addData("user", user);
            }
        }

        return result;
    }

    public static void doLogout(HttpServletRequest request) {
        // Check if the user is logged in.
        User user = Common.getUser(request);
        if (user != null)
            // The user is in fact logged in. Invalidate the session.
            request.getSession().invalidate();
    }

    public static String getDomain(HttpServletRequest request) {
        StringBuilder sb = new StringBuilder();
        sb.append(request.getServerName());

        int port = request.getServerPort();
        if (!request.isSecure() && port == 80)
            port = -1;
        else if (request.isSecure() && port == 443)
            port = -1;

        if (port != -1)
            sb.append(":").append(port);

        sb.append(request.getContextPath());

        return sb.toString();
    }
}
