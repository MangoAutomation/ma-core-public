/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.infiniteautomation.mango.webapp.servlets;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.infiniteautomation.mango.spring.components.pageresolver.PageResolver;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.vo.permission.PermissionHolder;

/**
 * @author Jared Wiltshire
 */
@Component
@WebServlet(name = WelcomeServlet.NAME, urlPatterns = {"/index.html"})
public class WelcomeServlet extends HttpServlet {

    private static final long serialVersionUID = -1349303330406758755L;
    public static final String NAME = "welcome-servlet";

    private final PageResolver pageResolver;

    @Autowired
    public WelcomeServlet(PageResolver pageResolver) {
        this.pageResolver = pageResolver;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        PermissionHolder user = Common.getUser();
        String uri = pageResolver.getDefaultUriInfo(req, resp, user.getUser()).getUri();
        resp.sendRedirect(uri);
    }

}
