/**
 * Copyright (C) 2014 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.web.servlet;

import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Component;

import com.serotonin.json.JsonException;
import com.serotonin.json.JsonWriter;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.IMangoLifecycle;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.i18n.Translations;
import com.serotonin.m2m2.module.DefaultPagesDefinition;
import com.serotonin.provider.Providers;

/**
 * Class to provide JSON status of Mango
 *
 * @author Terry Packer
 *
 */
@Component
@WebServlet(urlPatterns = {"/status/*"})
public class StatusServlet extends HttpServlet{

    private static final long serialVersionUID = 1L;
    private final Log LOG = LogFactory.getLog(StatusServlet.class);
    private Translations translations;

    public StatusServlet(){
        this.translations = Translations.getTranslations();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        IMangoLifecycle lifecycle = Providers.get(IMangoLifecycle.class);

        response.setContentType("application/json;charset=utf-8");
        response.setStatus(HttpServletResponse.SC_OK);
        response.setHeader("Access-Control-Allow-Origin", "*");

        //Get the Info and pack it up
        Map<String,Object> data = new HashMap<String,Object>();
        StringWriter sw = new StringWriter();
        JsonWriter writer = new JsonWriter(Common.JSON_CONTEXT, sw);

        data.put("startupProgress", lifecycle.getStartupProgress());
        data.put("shutdownProgress", lifecycle.getShutdownProgress());
        data.put("state", getLifecycleStateMessage(lifecycle.getLifecycleState()));

        //Can only get the Startup URI once the database is initalized
        if(lifecycle.getLifecycleState() > IMangoLifecycle.DATABASE_INITIALIZE){
            Common.envProps.getBoolean("", false);
            Boolean isSsl = Common.envProps.getBoolean("ssl.on", false);
            String uri;
            if(isSsl){
                int port = Common.envProps.getInt("ssl.port", 443);
                uri = "https://" + request.getServerName() + ":" + port +  DefaultPagesDefinition.getLoginUri(request,response);

            }else{
                uri = DefaultPagesDefinition.getLoginUri(request,response);
            }
            data.put("startupUri", uri);
        }

        try {
            writer.writeObject(data);
            response.getWriter().write(sw.toString());

        } catch (JsonException e) {
            LOG.error(e);
        }

    }

    /**
     * Method to handle a request from an external handler or process
     *
     * @param request
     * @param response
     * @throws IOException
     * @throws ServletException
     */
    public void handleRequest(HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException {
        this.doGet(request, response);
    }

    /* (non-Javadoc)
     * @see javax.servlet.http.HttpServlet#doOptions(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
    @Override
    protected void doOptions(HttpServletRequest req, HttpServletResponse response)
            throws ServletException, IOException {
        response.setHeader("Access-Control-Allow-Origin", "*");
        response.setHeader("Access-Control-Allow-Headers", "Content-Type,X-Requested-With");
        super.doOptions(req, response);
    }

    /**
     * Convert a numeric state to the translated message
     * @param state
     * @return
     */
    public String getLifecycleStateMessage(int state){
        switch(state){
            case IMangoLifecycle.NOT_STARTED:
                return this.translations.translate("startup.state.notStarted");
            case IMangoLifecycle.WEB_SERVER_INITIALIZE:
                return this.translations.translate("startup.state.webServerInitialize");
            case IMangoLifecycle.PRE_INITIALIZE:
                return this.translations.translate("startup.state.preInitialize");
            case IMangoLifecycle.TIMER_INITIALIZE:
                return this.translations.translate("startup.state.timerInitialize");
            case IMangoLifecycle.JSON_INITIALIZE:
                return this.translations.translate("startup.state.jsonInitialize");
            case IMangoLifecycle.EPOLL_INITIALIZE:
                return this.translations.translate("startup.state.epollInitialize");
            case IMangoLifecycle.LICENSE_CHECK:
                return this.translations.translate("startup.state.licenseCheck");
            case IMangoLifecycle.FREEMARKER_INITIALIZE:
                return this.translations.translate("startup.state.freeMarkerInitialize");
            case IMangoLifecycle.DATABASE_INITIALIZE:
                return this.translations.translate("startup.state.databaseInitialize");
            case IMangoLifecycle.POST_DATABASE_INITIALIZE:
                return this.translations.translate("startup.state.postDatabaseInitialize");
            case IMangoLifecycle.UTILITIES_INITIALIZE:
                return this.translations.translate("startup.state.utilitesInitialize");
            case IMangoLifecycle.EVENT_MANAGER_INITIALIZE:
                return this.translations.translate("startup.state.eventManagerInitialize");
            case IMangoLifecycle.RUNTIME_MANAGER_INITIALIZE:
                if(Common.runtimeManager != null)
                    return Common.runtimeManager.getStateMessage().translate(this.translations);
                else
                    return new TranslatableMessage("startup.state.runtimeManagerInitialize").translate(this.translations);
            case IMangoLifecycle.MAINTENANCE_INITIALIZE:
                return this.translations.translate("startup.state.maintenanceInitialize");
            case IMangoLifecycle.IMAGE_SET_INITIALIZE:
                return this.translations.translate("startup.state.imageSetInitialize");
            case IMangoLifecycle.WEB_SERVER_FINALIZE:
                return this.translations.translate("startup.state.webServerFinalize");
            case IMangoLifecycle.POST_INITIALIZE:
                return this.translations.translate("startup.state.postInitialize");
            case IMangoLifecycle.STARTUP_TASKS_RUNNING:
                return this.translations.translate("startup.state.startupTasksRunning");
            case IMangoLifecycle.RUNNING:
                return this.translations.translate("startup.state.running");
            case IMangoLifecycle.PRE_TERMINATE:
                return this.translations.translate("shutdown.state.preTerminate");
            case IMangoLifecycle.SHUTDOWN_TASKS_RUNNING:
                return this.translations.translate("shutdown.state.shutdownTasksRunning");
            case IMangoLifecycle.WEB_SERVER_TERMINATE:
                return this.translations.translate("shutdown.state.webServerTerminate");
            case IMangoLifecycle.RUNTIME_MANAGER_TERMINATE:
            case IMangoLifecycle.EPOLL_TERMINATE:
            case IMangoLifecycle.TIMER_TERMINATE:
            case IMangoLifecycle.EVENT_MANAGER_TERMINATE:
            case IMangoLifecycle.DATABASE_TERMINATE:
            case IMangoLifecycle.TERMINATED:
            default:
                return this.translations.translate("startup.state.unknown");
        }
    }
}
