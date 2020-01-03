/**
 * Copyright (C) 2014 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.web.handler;

import java.io.IOException;
import java.nio.file.Files;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.util.resource.PathResource;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.web.util.matcher.AndRequestMatcher;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.MediaTypeRequestMatcher;
import org.springframework.security.web.util.matcher.NegatedRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestHeaderRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.accept.ContentNegotiationManager;
import org.springframework.web.accept.ContentNegotiationStrategy;
import org.springframework.web.accept.HeaderContentNegotiationStrategy;
import org.springframework.web.accept.PathExtensionContentNegotiationStrategy;

import com.infiniteautomation.mango.webapp.servlets.StatusServlet;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.IMangoLifecycle;
import com.serotonin.m2m2.module.DefaultPagesDefinition;
import com.serotonin.m2m2.web.OverridingFileResource;
import com.serotonin.provider.Providers;

/**
 * Handle All Requests During the startup phase of Mango
 *
 * @author Terry Packer
 *
 */
public class StartupContextHandler extends ResourceHandler{
    //private final Log LOG = LogFactory.getLog(StartupContextHandler.class);
    private final String STARTUP_PAGE_TEMPLATE = "startupTemplate.htm";

    private final StatusServlet statusServlet;
    private String pageTemplate;
    private RequestMatcher pageRequestMatcher;
    private RequestMatcher resourceRequestMatcher;
    private RequestMatcher statusRequestMatcher;
    private RequestMatcher restRequestMatcher;

    public StartupContextHandler() throws IOException {
        setBaseResource(new OverridingFileResource(new PathResource(Common.OVERRIDES_WEB), new PathResource(Common.WEB)));
        this.statusServlet = new StatusServlet();

        final MediaType TEXT_CSS = MediaType.valueOf("text/css");
        final MediaType APPLICATION_JAVASCRIPT = MediaType.valueOf("application/javascript");
        final MediaType IMAGE_ICO = MediaType.valueOf("image/x-icon");

        Map<String, MediaType> mediaTypes = new HashMap<>();
        mediaTypes.put("css", TEXT_CSS);
        mediaTypes.put("js", APPLICATION_JAVASCRIPT);
        mediaTypes.put("map", MediaType.APPLICATION_JSON);
        mediaTypes.put("json", MediaType.APPLICATION_JSON);

        mediaTypes.put("ico", IMAGE_ICO);
        mediaTypes.put("jpg", MediaType.IMAGE_JPEG);
        mediaTypes.put("jpeg", MediaType.IMAGE_JPEG);
        mediaTypes.put("gif", MediaType.IMAGE_GIF);
        mediaTypes.put("png", MediaType.IMAGE_PNG);

        ContentNegotiationStrategy headerStrategy = new HeaderContentNegotiationStrategy();
        ContentNegotiationStrategy extensionStrategy = new PathExtensionContentNegotiationStrategy(mediaTypes);
        ContentNegotiationStrategy mixedStrategy = new ContentNegotiationManager(headerStrategy, extensionStrategy);

        // Match ONLY GET
        RequestMatcher getMatcher = new AntPathRequestMatcher("/**", HttpMethod.GET.name());

        // Content type of text/html
        MediaTypeRequestMatcher textHtmlMatcher = new MediaTypeRequestMatcher(
                headerStrategy, MediaType.APPLICATION_XHTML_XML, MediaType.TEXT_HTML);
        textHtmlMatcher.setIgnoredMediaTypes(Collections.singleton(MediaType.ALL));

        // Images, CSS etc
        MediaTypeRequestMatcher resourceMediaTypeMatcher = new MediaTypeRequestMatcher(
                mixedStrategy, MediaType.valueOf("image/*"), TEXT_CSS,
                APPLICATION_JAVASCRIPT, MediaType.APPLICATION_JSON);
        resourceMediaTypeMatcher.setIgnoredMediaTypes(Collections.singleton(MediaType.ALL));

        RequestMatcher resourcePathsMatcher = new OrRequestMatcher(
                new AntPathRequestMatcher("/resources/**"),
                new AntPathRequestMatcher("/images/**"));

        // Not XHR Request
        RequestMatcher notXRequestedWith = new NegatedRequestMatcher(
                new RequestHeaderRequestMatcher("X-Requested-With", "XMLHttpRequest"));

        // Setup the page Request Matcher to:
        // 1. Match ONLY GET, 2. Content type of text/html 3. Not XHR Request
        this.pageRequestMatcher = new AndRequestMatcher(getMatcher, notXRequestedWith, textHtmlMatcher);

        this.resourceRequestMatcher = new AndRequestMatcher(getMatcher, resourcePathsMatcher, resourceMediaTypeMatcher);
        this.statusRequestMatcher = new AndRequestMatcher(getMatcher, new AntPathRequestMatcher("/status/**"));

        this.restRequestMatcher = new AntPathRequestMatcher("/rest/**");
    }

    @Override
    public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        if (restRequestMatcher.matches(request)) {
            //Return options response
            response.setStatus(HttpStatus.SERVICE_UNAVAILABLE_503);
            response.setContentLength(0);
            IMangoLifecycle lifecycle = Providers.get(IMangoLifecycle.class);
            Float progress = lifecycle.getStartupProgress();
            int state = lifecycle.getLifecycleState();
            response.setHeader("Mango-Startup-Progress", String.format("%d", progress.intValue()));
            response.setHeader("Mango-Startup-State", this.statusServlet.getLifecycleStateMessage(state));
            baseRequest.setHandled(true);
        } else if (statusRequestMatcher.matches(request)) {
            statusServlet.handleRequest(request,response);
            baseRequest.setHandled(true);
        } else if (resourceRequestMatcher.matches(request)) {
            super.handle(target, baseRequest, request, response);
        } else if (pageRequestMatcher.matches(request)) {
            //Check to see if there are any default pages defined for this
            String uri = DefaultPagesDefinition.getStartupUri(request, response);
            if(uri != null){
                RequestDispatcher dispatcher = request.getRequestDispatcher(uri);
                dispatcher.forward(request, response);
            }else{
                response.setContentType("text/html;charset=utf-8");
                response.setStatus(HttpStatus.SERVICE_UNAVAILABLE_503);

                baseRequest.setHandled(true);
                //Load page template
                byte[] fileData = Files.readAllBytes(Common.WEB.resolve(STARTUP_PAGE_TEMPLATE));
                pageTemplate = new String(fileData, Common.UTF8_CS);

                String processedTemplate = pageTemplate;
                response.getWriter().write(processedTemplate);
            }
        } else {
            response.sendError(HttpStatus.SERVICE_UNAVAILABLE_503);
        }
    }
}
