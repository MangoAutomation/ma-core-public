/**
 * Copyright (C) 2017 Infinite Automation Software. All rights reserved.
 *
 */
package com.serotonin.m2m2.web.handler;

import java.io.IOException;

import javax.servlet.RequestDispatcher;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.ErrorHandler;
import org.springframework.security.web.WebAttributes;
import org.springframework.web.util.NestedServletException;

import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.module.DefaultPagesDefinition;
import com.serotonin.m2m2.vo.User;
import com.serotonin.m2m2.vo.permission.PermissionException;
import com.serotonin.m2m2.vo.permission.PermissionHolder;
import com.serotonin.m2m2.web.mvc.spring.security.BrowserRequestMatcher;

/**
 * Handle and process some of the basic responses that modules may want to
 * override
 *
 * @author Terry Packer
 */
public class MangoErrorHandler extends ErrorHandler {

    private final String ACCESS_DENIED = "/unauthorized.htm";

    @Override
    protected void generateAcceptableResponse(Request baseRequest, HttpServletRequest request,
            HttpServletResponse response, int code, String message, String mimeType) throws IOException {

        //If this response is already comitted we won't bother to handle this
        if(response.isCommitted()) {
            return;
        }
        
        //The cases we need to handle
        // 1) - Not found redirects to not found URI
        // 2) - Exception redirects to error URI
        // 3) - Unauthorized redirects to unauthorized URI
        // 4) - Other ?

        switch (code) {
            case 404:
                if(BrowserRequestMatcher.INSTANCE.matches(request)){
                    //Forward to Not Found URI
                    String uri = DefaultPagesDefinition.getNotFoundUri(request, response);
                    response.sendRedirect(uri);
                }else{
                    //Resource/Rest Request
                    baseRequest.setHandled(true);
                }

                break;
            default:
                //Catch All unhandled Responses with errors
                Throwable th = (Throwable)request.getAttribute(RequestDispatcher.ERROR_EXCEPTION);
                //Does this require handling
                if(th != null){

                    if(th instanceof NestedServletException)
                        th = th.getCause();

                    String uri;

                    //We are handling this here
                    baseRequest.setHandled(true);

                    //We need to do something
                    if(BrowserRequestMatcher.INSTANCE.matches(request)){
                        //TODO There is no longer a way to get a PermissionException here due to the PermissionExceptionFilter.
                        // However, there is no User in the Security Context at this point which means we cannot successfully do a forward...
                        // need to understand how this is happening.
                        //
                        //
                        //Are we a PermissionException
                        if(th instanceof PermissionException){
                            PermissionHolder user = Common.getUser();
                            if(user instanceof User) {
                                uri = DefaultPagesDefinition.getUnauthorizedUri(request, response, (User)user);
                            }else {
                                uri = ACCESS_DENIED;
                            }
                            // Put exception into request scope (perhaps of use to a view)
                            request.setAttribute(WebAttributes.ACCESS_DENIED_403, th);
                            response.sendRedirect(uri);
                        }else{
                            uri = DefaultPagesDefinition.getErrorUri(baseRequest, response);
                            response.sendRedirect(uri);
                        }
                    }else{
                        //Resource/Rest Request
                        baseRequest.setHandled(true);
                    }
                }
                break;
        }
    }
}
