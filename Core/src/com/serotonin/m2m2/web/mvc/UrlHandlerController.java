/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.web.mvc;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.mvc.Controller;

public class UrlHandlerController implements Controller {
    private final UrlHandler urlController;
    private final String modulePath;
    private final String viewName;

    public UrlHandlerController(UrlHandler urlController, String modulePath, String viewName) {
        this.urlController = urlController;
        this.modulePath = modulePath;
        this.viewName = viewName;
    }

    @Override
    public ModelAndView handleRequest(HttpServletRequest request, HttpServletResponse response) throws Exception {
        Map<String, Object> model = new HashMap<String, Object>();
        model.put("modulePath", modulePath);

        View view = null;
        if (urlController != null)
            view = urlController.handleRequest(request, response, model);

        if (view == null) {
            if (viewName == null)
                throw new ServletException("The URL controller " + urlController.getClass()
                        + " returned a null view, but the definition provided a null JSP path. One or the other "
                        + "must be provided.");
            return new ModelAndView(viewName, model);
        }
        return new ModelAndView(view, model);
    }
}
