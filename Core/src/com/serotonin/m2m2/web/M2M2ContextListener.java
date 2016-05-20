/*
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.web;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.SessionCookieConfig;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.DataTypes;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.rt.event.type.AuditEventType;
import com.serotonin.m2m2.rt.event.type.EventType;
import com.serotonin.m2m2.rt.event.type.SystemEventType;
import com.serotonin.m2m2.vo.comment.UserCommentVO;
import com.serotonin.m2m2.vo.permission.Permissions;

public class M2M2ContextListener implements ServletContextListener {
    private final Log log = LogFactory.getLog(M2M2ContextListener.class);

    @Override
    public void contextInitialized(ServletContextEvent evt) {
        log.info("Mango Automation context starting");

        // Get a handle on the context.
        ServletContext ctx = evt.getServletContext();

        // Create all the stuff we need.
        constantsInitialize(ctx);

        log.info("Mango Automation context started");
    }

    @Override
    public void contextDestroyed(ServletContextEvent evt) {
        // no op
    }

    //
    //
    // Constants
    //
    private void constantsInitialize(ServletContext ctx) {
        ctx.setAttribute("constants.Common.NEW_ID", Common.NEW_ID);

        ctx.setAttribute("constants.DataTypes.BINARY", DataTypes.BINARY);
        ctx.setAttribute("constants.DataTypes.MULTISTATE", DataTypes.MULTISTATE);
        ctx.setAttribute("constants.DataTypes.NUMERIC", DataTypes.NUMERIC);
        ctx.setAttribute("constants.DataTypes.ALPHANUMERIC", DataTypes.ALPHANUMERIC);
        ctx.setAttribute("constants.DataTypes.IMAGE", DataTypes.IMAGE);

        ctx.setAttribute("constants.Permissions.DataPointAccessTypes.NONE", Permissions.DataPointAccessTypes.NONE);
        ctx.setAttribute("constants.Permissions.DataPointAccessTypes.READ", Permissions.DataPointAccessTypes.READ);
        ctx.setAttribute("constants.Permissions.DataPointAccessTypes.SET", Permissions.DataPointAccessTypes.SET);
        ctx.setAttribute("constants.Permissions.DataPointAccessTypes.DATA_SOURCE",
                Permissions.DataPointAccessTypes.DATA_SOURCE);
        ctx.setAttribute("constants.Permissions.DataPointAccessTypes.ADMIN", Permissions.DataPointAccessTypes.ADMIN);

        ctx.setAttribute("constants.EventType.EventTypeNames.DATA_POINT", EventType.EventTypeNames.DATA_POINT);
        ctx.setAttribute("constants.EventType.EventTypeNames.DATA_SOURCE", EventType.EventTypeNames.DATA_SOURCE);
        ctx.setAttribute("constants.EventType.EventTypeNames.SYSTEM", EventType.EventTypeNames.SYSTEM);
        ctx.setAttribute("constants.EventType.EventTypeNames.PUBLISHER", EventType.EventTypeNames.PUBLISHER);
        ctx.setAttribute("constants.EventType.EventTypeNames.AUDIT", EventType.EventTypeNames.AUDIT);

        ctx.setAttribute("constants.SystemEventType.TYPE_SYSTEM_STARTUP", SystemEventType.TYPE_SYSTEM_STARTUP);
        ctx.setAttribute("constants.SystemEventType.TYPE_SYSTEM_SHUTDOWN", SystemEventType.TYPE_SYSTEM_SHUTDOWN);
        ctx.setAttribute("constants.SystemEventType.TYPE_MAX_ALARM_LEVEL_CHANGED",
                SystemEventType.TYPE_MAX_ALARM_LEVEL_CHANGED);
        ctx.setAttribute("constants.SystemEventType.TYPE_USER_LOGIN", SystemEventType.TYPE_USER_LOGIN);
        ctx.setAttribute("constants.SystemEventType.TYPE_SET_POINT_HANDLER_FAILURE",
                SystemEventType.TYPE_SET_POINT_HANDLER_FAILURE);
        ctx.setAttribute("constants.SystemEventType.TYPE_EMAIL_SEND_FAILURE", SystemEventType.TYPE_EMAIL_SEND_FAILURE);
        ctx.setAttribute("constants.SystemEventType.TYPE_PROCESS_FAILURE", SystemEventType.TYPE_PROCESS_FAILURE);
        ctx.setAttribute("constants.SystemEventType.TYPE_LICENSE_CHECK", SystemEventType.TYPE_LICENSE_CHECK);
        ctx.setAttribute("constants.SystemEventType.TYPE_UPGRADE_CHECK", SystemEventType.TYPE_UPGRADE_CHECK);        
        
        ctx.setAttribute("constants.AuditEventType.TYPE_DATA_SOURCE", AuditEventType.TYPE_DATA_SOURCE);
        ctx.setAttribute("constants.AuditEventType.TYPE_DATA_POINT", AuditEventType.TYPE_DATA_POINT);
        ctx.setAttribute("constants.AuditEventType.TYPE_EVENT_DETECTOR", AuditEventType.TYPE_EVENT_DETECTOR);
        ctx.setAttribute("constants.AuditEventType.TYPE_EVENT_HANDLER", AuditEventType.TYPE_EVENT_HANDLER);

        ctx.setAttribute("constants.UserComment.TYPE_EVENT", UserCommentVO.TYPE_EVENT);
        ctx.setAttribute("constants.UserComment.TYPE_POINT", UserCommentVO.TYPE_POINT);

        String[] codes = { "common.access.read", "common.access.set", "common.alarmLevel.none",
                "common.alarmLevel.info", "common.alarmLevel.important", "common.alarmLevel.warning", "common.alarmLevel.urgent", "common.alarmLevel.critical",
                "common.alarmLevel.lifeSafety", "common.alarmLevel.doNotLog", "common.alarmLevel.ignore", "common.disabled", "common.administrator", "common.user",
                "common.disabledToggle", "common.enabledToggle", "common.maximize", "common.minimize",
                "common.loading", "js.help.error", "js.help.related", "js.help.lastUpdated", "common.sendTestEmail",
                "js.email.noRecipients", "js.email.addMailingList", "js.email.addUser", "js.email.addAddress",
                "js.email.noRecipForEmail", "js.email.testSent", "events.silence", "events.unsilence", "header.mute",
                "header.unmute", };
        Map<String, TranslatableMessage> messages = new HashMap<>();
        for (String code : codes)
            messages.put(code, new TranslatableMessage(code));
        ctx.setAttribute("clientSideMessages", messages);
        
        SessionCookieConfig sessionCookieConfig = ctx.getSessionCookieConfig();
        sessionCookieConfig.setHttpOnly(true);
        sessionCookieConfig.setName("MANGO" + Common.envProps.getInt("web.port", 8080));
    }
}
