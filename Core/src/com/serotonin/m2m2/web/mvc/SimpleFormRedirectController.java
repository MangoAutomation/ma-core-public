/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.web.mvc;

import javax.servlet.http.HttpServletRequest;

import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;
import org.springframework.web.util.WebUtils;

/**
 * Unusable as of 2.5.0 due to Spring 4 upgrade
 *
 */
@Deprecated
public class SimpleFormRedirectController {
    private String successUrl;

    public void setSuccessUrl(String successUrl) {
        this.successUrl = successUrl;
    }

    public ModelAndView getSuccessRedirectView() {
        return getSuccessRedirectView(null);
    }

    public ModelAndView getSuccessRedirectView(String queryString) {
        String url = successUrl;
        if (queryString != null && queryString.trim().length() > 0) {
            if (queryString.charAt(0) != '?')
                url += '?' + queryString;
            else
                url += queryString;
        }
        RedirectView redirectView = new RedirectView(url, true);
        return new ModelAndView(redirectView);
    }

    public boolean hasSubmitParameter(HttpServletRequest request, String name) {
        return WebUtils.hasSubmitParameter(request, name);
    }
}
