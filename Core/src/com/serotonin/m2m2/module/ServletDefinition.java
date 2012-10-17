/*
    Copyright (C) 2006-2011 Serotonin Software Technologies Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.module;

import java.util.Collections;
import java.util.Map;

import javax.servlet.http.HttpServlet;

/**
 * A servlet definition provides the necessary information to register a servlet into the MA instance. Servlets do not
 * use JSP or HTML pages to render their responses, and so can be used for tasks such as image generation, export files,
 * etc.
 * 
 * Servlets can also be used to listen for incoming information, such as with the HTTP receiver data source.
 * 
 * All methods in this definition are analogous to servlet and servlet-mapping elements of the web.xml file.
 * 
 * @author Matthew Lohbihler
 */
abstract public class ServletDefinition extends ModuleElementDefinition {
    /**
     * @return an instance of the servlet.
     */
    abstract public HttpServlet getServlet();

    /**
     * @return the URI pattern used to direct requests to this servlet.
     */
    abstract public String getUriPattern();

    /**
     * @return the init order of the servlet
     */
    public int getInitOrder() {
        return -1;
    }

    /**
     * @return initialization parameters provided to the servlet instance
     */
    public Map<String, String> getInitParameters() {
        return Collections.emptyMap();
    }
}
