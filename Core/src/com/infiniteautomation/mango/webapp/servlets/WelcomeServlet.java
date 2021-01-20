/*
 * Copyright (C) 2020 Infinite Automation Software. All rights reserved.
 */
package com.infiniteautomation.mango.webapp.servlets;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Component;

import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.module.DefaultPagesDefinition;
import com.serotonin.m2m2.vo.permission.PermissionHolder;

/**
 * @author Jared Wiltshire
 */
@Component
@WebServlet(name = WelcomeServlet.NAME, urlPatterns = {"/index.html"})
public class WelcomeServlet extends HttpServlet {

    private static final long serialVersionUID = -1349303330406758755L;
    public static final String NAME = "welcome-servlet";

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        PermissionHolder user = Common.getUser();
        String uri = DefaultPagesDefinition.getDefaultUri(req, resp, user.getUser());
        resp.sendRedirect(uri);
    }

}
