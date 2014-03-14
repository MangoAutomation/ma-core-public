/*
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.web.mvc;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.servlet.View;

/**
 * TODO this should be renamed UriHandler
 * 
 * @author Matthew
 */
public interface UrlHandler {
    View handleRequest(HttpServletRequest request, HttpServletResponse response, Map<String, Object> model)
            throws Exception;
}
