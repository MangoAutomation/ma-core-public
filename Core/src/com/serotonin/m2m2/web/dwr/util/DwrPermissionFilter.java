/*
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.web.dwr.util;

import java.lang.reflect.Method;

import org.directwebremoting.AjaxFilter;
import org.directwebremoting.AjaxFilterChain;

import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.dao.SystemSettingsDao;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.vo.User;
import com.serotonin.m2m2.vo.permission.PermissionException;
import com.serotonin.m2m2.vo.permission.Permissions;

/**
 * @author Matthew Lohbihler
 */
public class DwrPermissionFilter implements AjaxFilter {
    public Object doFilter(Object obj, Method method, Object[] params, AjaxFilterChain chain) throws Exception {
        DwrPermission permission = method.getAnnotation(DwrPermission.class);

        if (permission == null)
            // No access if not annotated.
            throw new PermissionException(new TranslatableMessage("common.default", "Method " + method.getName() + " not annotated with permissions"), null);

        if (!permission.anonymous()) {
            User user = Common.getUser();

            if (user == null)
                // Not logged in.
                throw new PermissionException(new TranslatableMessage("common.default", "Method " + method.getName() + " does not allow anonymous access"), null);

            if(!permission.custom().isEmpty()){
            	if(!Permissions.hasPermission(user, SystemSettingsDao.instance.getValue(permission.custom())))
            		throw new PermissionException(new TranslatableMessage("common.default", "Method " + method.getName() + " requires " + permission.custom() + " access"), user);
            }
            
            if (!Permissions.hasAdminPermission(user) && permission.admin())
                throw new PermissionException(new TranslatableMessage("common.default", "Method " + method.getName() + " requires admin access"), user);
        }

        return chain.doFilter(obj, method, params);
    }
}
