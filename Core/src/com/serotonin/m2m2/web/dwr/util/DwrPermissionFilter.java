/*
    Copyright (C) 2006-2011 Serotonin Software Technologies Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.web.dwr.util;

import java.lang.reflect.Method;

import org.directwebremoting.AjaxFilter;
import org.directwebremoting.AjaxFilterChain;

import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.vo.User;
import com.serotonin.m2m2.vo.permission.PermissionException;

/**
 * @author Matthew Lohbihler
 */
public class DwrPermissionFilter implements AjaxFilter {
    public Object doFilter(Object obj, Method method, Object[] params, AjaxFilterChain chain) throws Exception {
        DwrPermission permission = method.getAnnotation(DwrPermission.class);

        if (permission == null)
            // No access if not annotated.
            throw new PermissionException("Method " + method.getName() + " not annotated with permissions", null);

        if (!permission.anonymous()) {
            User user = Common.getUser();

            if (user == null)
                // Not logged in.
                throw new PermissionException("Method " + method.getName() + " does not allow anonymous access", null);

            if (!user.isAdmin() && permission.admin())
                throw new PermissionException("Method " + method.getName() + " requires admin access", user);
        }

        return chain.doFilter(obj, method, params);
    }
}
