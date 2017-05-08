/*
    Copyright (C) 2006-2007 Serotonin Software Technologies Inc.
 	@author Matthew Lohbihler
 */
package com.serotonin.web.http;

import java.io.IOException;

import org.apache.http.HttpResponse;

/**
 * @author Matthew Lohbihler
 */
public interface HttpResponseHandler4<T, E extends Exception> {
    public T handle(HttpResponse response) throws IOException, E;
}
