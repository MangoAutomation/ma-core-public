/*
    Copyright (C) 2006-2007 Serotonin Software Technologies Inc.
 	@author Matthew Lohbihler
 */
package com.serotonin.web.dwr;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.directwebremoting.AjaxFilter;
import org.directwebremoting.AjaxFilterChain;

/**
 * @author Matthew Lohbihler
 */
public class ExceptionDetectionFilter implements AjaxFilter {
    private static final Log LOG = LogFactory.getLog(ExceptionDetectionFilter.class);

    private String logLevel = "error";
    private Class<? extends Throwable>[] ignoreList;

    public void setLogLevel(String logLevel) {
        this.logLevel = logLevel;
    }

    @SuppressWarnings("unchecked")
    public void setIgnoreList(String ignoreList) {
        String[] parts = ignoreList.split(",");

        this.ignoreList = (Class<? extends Throwable>[]) new Class<?>[parts.length];

        try {
            for (int i = 0; i < parts.length; i++)
                this.ignoreList[i] = (Class<? extends Throwable>) Class.forName(parts[i].trim());
        }
        catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public Object doFilter(Object obj, Method method, Object[] params, AjaxFilterChain chain) throws Exception {
        try {
            return chain.doFilter(obj, method, params);
        }
        catch (Exception e) {
            Throwable e2 = e;
            if (e2 instanceof InvocationTargetException)
                e2 = ((InvocationTargetException) e).getTargetException();

            if (ignoreList == null || !ArrayUtils.contains(ignoreList, e2.getClass())) {
                if ("trace".equalsIgnoreCase(logLevel))
                    LOG.trace("DWR invocation exception", e2);
                else if ("debug".equalsIgnoreCase(logLevel))
                    LOG.debug("DWR invocation exception", e2);
                else if ("info".equalsIgnoreCase(logLevel))
                    LOG.info("DWR invocation exception", e2);
                else if ("warn".equalsIgnoreCase(logLevel))
                    LOG.warn("DWR invocation exception", e2);
                else if ("error".equalsIgnoreCase(logLevel))
                    LOG.error("DWR invocation exception", e2);
                else if ("fatal".equalsIgnoreCase(logLevel))
                    LOG.fatal("DWR invocation exception", e2);
                else {
                    LOG.error("Unknown log level: " + logLevel);
                    LOG.error("DWR invocation exception", e2);
                }
            }

            throw e;
        }
    }
}
