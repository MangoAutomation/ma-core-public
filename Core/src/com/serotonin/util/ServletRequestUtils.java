/*
    Copyright (C) 2006-2007 Serotonin Software Technologies Inc.
 	@author Matthew Lohbihler
 */
package com.serotonin.util;

import javax.servlet.ServletRequest;

/**
 * @author Matthew Lohbihler
 */
public class ServletRequestUtils {
    /**
     * Useful for deriving simple values from a request, when creating a command form would be overkill.
     * @param request the servlet request
     * @param parameterName the name of the parameter to retrieve
     * @param defaultValue the value to return if the parameter doesn't exist or is not parseable.
     * @return the integer value of the parameter or the default value.
     */
    public static int getIntParameter(ServletRequest request, String parameterName, int defaultValue) {
        int result = defaultValue;
        
        String paramStr = request.getParameter(parameterName);
        if (paramStr != null) {
            try {
                result = Integer.parseInt(paramStr);
            }
            catch (NumberFormatException e) {
                // Ignore
            }
        }
        
        return result;
    }
    
    /**
     * Useful for deriving simple values from a request, when creating command form would be overkill.
     * @param request the servlet request
     * @param parameterName the name of the parameter to retrieve
     * @param defaultValue the value to return if the parameter doesn't exist or is not parseable.
     * @return the long value of the parameter or the default value.
     */
    public static long getLongParameter(ServletRequest request, String parameterName, long defaultValue) {
        long result = defaultValue;
        
        String paramStr = request.getParameter(parameterName);
        if (paramStr != null) {
            try {
                result = Long.parseLong(paramStr);
            }
            catch (NumberFormatException e) {
                // Ignore
            }
        }
        
        return result;
    }
}
