/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */

package com.infiniteautomation.mango.spring.components.pageresolver;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.serotonin.m2m2.vo.User;

/**
 * Retrieves information about what URL to redirect to when you need to authenticate, first login page, home page etc.
 */
public interface PageResolver {

    /**
     * Default page, e.g. user home page or some sort of welcome page
     */
    LoginUriInfo getDefaultUriInfo(HttpServletRequest request, HttpServletResponse response, User user);

    /**
     * Page you are redirected to when you are logged in, but not authorized to access a resource.
     */
    String getUnauthorizedUri(HttpServletRequest request, HttpServletResponse response, User user);

    /**
     * Page used to login / authenticate. aka authentication entry point.
     */
    String getLoginUri(HttpServletRequest request, HttpServletResponse response);

    /**
     * Page to use when login / authentication fails. Usually the same as the login page.
     */
    String getLoginErrorUri(HttpServletRequest request, HttpServletResponse response);

    /**
     * Page you are redirected to when you successfully logout. Usually the same as the login page.
     */
    String getLogoutSuccessUri(HttpServletRequest request, HttpServletResponse response);

    /**
     * Page used to reset a user's password.
     */
    String getPasswordResetUri();

    /**
     * Page used to verify a user's email address.
     */
    String getEmailVerificationUri();

    /**
     * Page you are redirected to when you try to access a resource that does not exist.
     */
    String getNotFoundUri(HttpServletRequest request, HttpServletResponse response);

    /**
     * Page you are redirected to when an error occurs accessing a page.
     */
    String getErrorUri(HttpServletRequest request, HttpServletResponse response);

    /**
     * Startup page while Mango is starting up.
     */
    String getStartupUri(HttpServletRequest request, HttpServletResponse response);

}
