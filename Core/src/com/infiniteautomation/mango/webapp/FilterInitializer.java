/*
 * Copyright (C) 2019 Infinite Automation Software. All rights reserved.
 */
package com.infiniteautomation.mango.webapp;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;

/**
 * Essentially the same thing as a WebApplicationInitializer but is ordered along with Filter instances and called from RegisterFiltersAndServlets
 * @author Jared Wiltshire
 */
public interface FilterInitializer {
    void onStartup(ServletContext servletContext) throws ServletException;
}
