/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */

package com.infiniteautomation.mango.spring.components.pageresolver;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import com.infiniteautomation.mango.spring.service.PermissionService;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.dao.SystemSettingsDao;
import com.serotonin.m2m2.module.DefaultPagesDefinition;
import com.serotonin.m2m2.module.ModuleRegistry;
import com.serotonin.m2m2.vo.User;

@Component
public class DefaultPageResolver implements PageResolver {

    private final PermissionService permissionService;
    private final SystemSettingsDao systemSettingsDao;

    public DefaultPageResolver(PermissionService permissionService, SystemSettingsDao systemSettingsDao) {
        this.permissionService = permissionService;
        this.systemSettingsDao = systemSettingsDao;
    }

    @Override
    public LoginUriInfo getDefaultUriInfo(HttpServletRequest request, HttpServletResponse response, User user) {
        String uri = null;
        boolean required = false;

        if (user == null)
            uri = getLoginUri(request, response);
        else {
            // If this is the first login to the instance by an admin...
            if (permissionService.hasAdminRole(user) && systemSettingsDao.getBooleanValue(SystemSettingsDao.NEW_INSTANCE)) {
                // Remove the flag
                systemSettingsDao.removeValue(SystemSettingsDao.NEW_INSTANCE);

                // If there is a page to which to forward, do so. This could be null.
                uri = getFirstLoginUri(request, response);
                required = true;

            }else if(permissionService.hasAdminRole(user) && (systemSettingsDao.getIntValue(SystemSettingsDao.LICENSE_AGREEMENT_VERSION) != Common.getLicenseAgreementVersion())) {
                //When a new license version has been released but it is NOT the first login.
                uri = getAdminLicenseUpgradeLoginUri(request, response);
                required = true;
            }

            if (uri == null) {
                if (user.isFirstLogin())
                    uri = getFirstUserLoginUri(request, response, user);
                if (StringUtils.isBlank(uri))
                    uri = getLoggedInUriPreHome(request, response, user);
                if (StringUtils.isBlank(uri))
                    uri = user.getHomeUrl();
                if (StringUtils.isBlank(uri))
                    uri = getLoggedInUri(request, response, user);
            }
        }

        return new LoginUriInfo(uri, required);
    }

    @Override
    public String getUnauthorizedUri(HttpServletRequest request, HttpServletResponse response, User user) {
        String uri = null;
        for (DefaultPagesDefinition def : ModuleRegistry.getDefinitions(DefaultPagesDefinition.class)) {
            uri = def.getUnauthorizedPageUri(request, response, user);
            if (!StringUtils.isBlank(uri))
                break;
        }
        return uri;
    }

    @Override
    public String getLoginUri(HttpServletRequest request, HttpServletResponse response) {
        String uri = null;
        for (DefaultPagesDefinition def : ModuleRegistry.getDefinitions(DefaultPagesDefinition.class)) {
            uri = def.getLoginPageUri(request, response);
            if (!StringUtils.isBlank(uri))
                break;
        }
        return uri;
    }

    @Override
    public String getPasswordResetUri() {
        String uri = null;
        for (DefaultPagesDefinition def : ModuleRegistry.getDefinitions(DefaultPagesDefinition.class)) {
            uri = def.getPasswordResetPageUri();
            if (!StringUtils.isBlank(uri))
                break;
        }
        return uri;
    }

    @Override
    public String getEmailVerificationUri() {
        String uri = null;
        for (DefaultPagesDefinition def : ModuleRegistry.getDefinitions(DefaultPagesDefinition.class)) {
            uri = def.getEmailVerificationPageUri();
            if (!StringUtils.isBlank(uri))
                break;
        }
        return uri;
    }

    private String getFirstLoginUri(HttpServletRequest request, HttpServletResponse response) {
        String uri = null;
        for (DefaultPagesDefinition def : ModuleRegistry.getDefinitions(DefaultPagesDefinition.class)) {
            uri = def.getFirstLoginPageUri(request, response);
            if (!StringUtils.isBlank(uri))
                break;
        }
        return uri;
    }

    private String getAdminLicenseUpgradeLoginUri(HttpServletRequest request, HttpServletResponse response) {
        String uri = null;
        for (DefaultPagesDefinition def : ModuleRegistry.getDefinitions(DefaultPagesDefinition.class)) {
            uri = def.getAdminLicenseUpgradeLoginPageUri(request, response);
            if (!StringUtils.isBlank(uri))
                break;
        }
        return uri;
    }

    private String getFirstUserLoginUri(HttpServletRequest request, HttpServletResponse response, User user) {
        String uri = null;
        for (DefaultPagesDefinition def : ModuleRegistry.getDefinitions(DefaultPagesDefinition.class)) {
            uri = def.getFirstUserLoginPageUri(request, response, user);
            if (!StringUtils.isBlank(uri))
                break;
        }
        return uri;
    }

    private String getLoggedInUriPreHome(HttpServletRequest request, HttpServletResponse response, User user) {
        String uri = null;
        for (DefaultPagesDefinition def : ModuleRegistry.getDefinitions(DefaultPagesDefinition.class)) {
            uri = def.getLoggedInPageUriPreHome(request, response, user);
            if (!StringUtils.isBlank(uri))
                break;
        }
        return uri;
    }

    private String getLoggedInUri(HttpServletRequest request, HttpServletResponse response, User user) {
        String uri = null;
        for (DefaultPagesDefinition def : ModuleRegistry.getDefinitions(DefaultPagesDefinition.class)) {
            uri = def.getLoggedInPageUri(request, response, user);
            if (!StringUtils.isBlank(uri))
                break;
        }
        return uri;
    }

    @Override
    public String getNotFoundUri(HttpServletRequest request, HttpServletResponse response) {
        String uri = null;
        for (DefaultPagesDefinition def : ModuleRegistry.getDefinitions(DefaultPagesDefinition.class)) {
            uri = def.getNotFoundPageUri(request, response);
            if (!StringUtils.isBlank(uri))
                break;
        }
        return uri;
    }

    @Override
    public String getErrorUri(HttpServletRequest request, HttpServletResponse response) {
        String uri = null;
        for (DefaultPagesDefinition def : ModuleRegistry.getDefinitions(DefaultPagesDefinition.class)) {
            uri = def.getErrorPageUri(request, response);
            if (!StringUtils.isBlank(uri))
                break;
        }
        return uri;
    }

    @Override
    public String getStartupUri(HttpServletRequest request, HttpServletResponse response){
        String uri = null;
        for (DefaultPagesDefinition def : ModuleRegistry.getDefinitions(DefaultPagesDefinition.class)) {
            uri = def.getStartupPageUri(request, response);
            if (!StringUtils.isBlank(uri))
                break;
        }
        return uri;
    }

}
