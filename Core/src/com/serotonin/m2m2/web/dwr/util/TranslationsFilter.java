/*
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.web.dwr.util;

import java.lang.reflect.Method;

import org.directwebremoting.AjaxFilter;
import org.directwebremoting.AjaxFilterChain;
import org.directwebremoting.WebContext;
import org.directwebremoting.WebContextFactory;
import org.springframework.context.MessageSource;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;
import org.springframework.web.servlet.support.JstlUtils;

/**
 * @author Matthew Lohbihler
 */
public class TranslationsFilter implements AjaxFilter {
    private String messageSourceKey = "messageSource";

    public void setMessageSourceKey(String messageSourceKey) {
        this.messageSourceKey = messageSourceKey;
    }

    public Object doFilter(Object obj, Method method, Object[] params, AjaxFilterChain chain) throws Exception {
        WebContext webContext = WebContextFactory.get();

        WebApplicationContext wac = WebApplicationContextUtils.getRequiredWebApplicationContext(webContext
                .getServletContext());
        MessageSource messageSource = (MessageSource) wac.getBean(messageSourceKey);

        JstlUtils.exposeLocalizationContext(webContext.getHttpServletRequest(), messageSource);

        return chain.doFilter(obj, method, params);
    }
}
