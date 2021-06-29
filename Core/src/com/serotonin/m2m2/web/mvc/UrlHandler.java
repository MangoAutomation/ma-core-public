/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
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
