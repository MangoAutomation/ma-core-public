/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.infiniteautomation.mango.webapp.filters;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.servlet.DispatcherType;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.annotation.WebFilter;
import javax.servlet.annotation.WebInitParam;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.eclipse.jetty.servlets.DoSFilter;
import org.eclipse.jetty.util.component.AbstractLifeCycle;
import org.eclipse.jetty.util.thread.Scheduler;
import org.eclipse.jetty.util.thread.Scheduler.Task;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Component;

import com.infiniteautomation.mango.spring.ConditionalOnProperty;
import com.infiniteautomation.mango.spring.components.executors.MangoExecutors;
import com.serotonin.m2m2.vo.permission.PermissionHolder;

/**
 * https://www.eclipse.org/jetty/documentation/current/dos-filter.html
 *
 * @author Jared Wiltshire
 */
@Component(MangoDosFilter.NAME)
@ConditionalOnProperty("${web.dos.enabled:false}")
@WebFilter(
        filterName = MangoDosFilter.NAME,
        asyncSupported = true,
        urlPatterns = {"/*"},
        initParams = {
                @WebInitParam(name = "managedAttr", value = "false"),
                @WebInitParam(name = "maxRequestsPerSec", value = "${web.dos.maxRequestsPerSec:75}"),
                @WebInitParam(name = "delayMs", value = "${web.dos.delayMs:100}"),
                @WebInitParam(name = "throttledRequests", value = "${web.dos.throttledRequests:5}"),
                @WebInitParam(name = "maxWaitMs", value = "${web.dos.maxWaitMs:50}"),
                @WebInitParam(name = "throttleMs", value = "${web.dos.throttleMs:30000}"),
                @WebInitParam(name = "maxRequestMs", value = "${web.dos.maxRequestMs:30000}"),
                @WebInitParam(name = "maxIdleTrackerMs", value = "${web.dos.maxIdleTrackerMs:30000}"),
                @WebInitParam(name = "insertHeaders", value = "${web.dos.insertHeaders:true}"),
                @WebInitParam(name = "trackSessions", value = "${web.dos.trackSessions:true}"),
                @WebInitParam(name = "remotePort", value = "${web.dos.remotePort:false}"),
                @WebInitParam(name = "ipWhitelist", value = "${web.dos.ipWhitelist:}")
        },
        dispatcherTypes = {DispatcherType.REQUEST, DispatcherType.ASYNC})
@Order(FilterOrder.DOS)
public class MangoDosFilter extends DoSFilter {
    public static final String NAME = "mangoDosFilter";

    private final MangoExecutors executors;

    @Autowired
    public MangoDosFilter(MangoExecutors executors) {
        this.executors = executors;
    }

    /**
     * To enable giving priority to logged in users
     */
    @Override
    protected String extractUserId(ServletRequest request) {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpSession session = httpRequest.getSession(false);
        if (session != null) {
            Object context = session.getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
            if (context instanceof SecurityContext) {
                SecurityContext securityContext = (SecurityContext) context;
                Authentication auth = securityContext.getAuthentication();
                if (auth != null && auth.getPrincipal() instanceof PermissionHolder) {
                    PermissionHolder user = (PermissionHolder) auth.getPrincipal();
                    if (user.getUser() != null) {
                        return Integer.toString(user.getUser().getId());
                    }
                }
            }
        }

        return null;
    }

    @Override
    protected Scheduler startScheduler() throws ServletException {
        try {
            Scheduler result = new MangoDosScheduledExecutorWrapper(executors);
            result.start();
            return result;
        }catch (Exception x) {
            throw new ServletException(x);
        }
    }

    /**
     * Wrapper to use our thr
     * @author Terry Packer
     *
     */
    private static class MangoDosScheduledExecutorWrapper extends AbstractLifeCycle implements Scheduler {

        private final MangoExecutors executors;

        public MangoDosScheduledExecutorWrapper(MangoExecutors executors) {
            this.executors = executors;
        }

        @Override
        public Task schedule(Runnable task, long delay, TimeUnit units) {
            ScheduledFuture<?> result = executors.getScheduledExecutor().schedule(task, delay, units);
            return new ScheduledFutureTask(result);
        }
    }

    private static class ScheduledFutureTask implements Task {
        private final ScheduledFuture<?> scheduledFuture;

        ScheduledFutureTask(ScheduledFuture<?> scheduledFuture) {
            this.scheduledFuture = scheduledFuture;
        }

        @Override
        public boolean cancel() {
            return scheduledFuture.cancel(false);
        }
    }
}