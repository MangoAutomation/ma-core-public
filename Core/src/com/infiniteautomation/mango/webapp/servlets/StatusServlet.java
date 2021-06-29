/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.infiniteautomation.mango.webapp.servlets;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Component;

import com.serotonin.json.JsonException;
import com.serotonin.json.JsonWriter;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.IMangoLifecycle;
import com.serotonin.m2m2.LifecycleState;
import com.serotonin.m2m2.i18n.Translations;
import com.serotonin.provider.Providers;

/**
 * Class to provide JSON status of Mango
 *
 * @author Terry Packer
 * @author Jared Wiltshire
 *
 */
@Component
@WebServlet(urlPatterns = {"/status"})
public class StatusServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private final IMangoLifecycle lifecycle = Providers.get(IMangoLifecycle.class);
    private final Translations translations = Translations.getTranslations();

    public StatusServlet() {
        super();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        LifecycleState state = lifecycle.getLifecycleState();
        int startupProgress = state.getStartupProgress();
        int shutdownProgress = state.getShutdownProgress();

        Map<String, Object> data = new HashMap<>();

        data.put("startupProgress", startupProgress);
        data.put("shutdownProgress", shutdownProgress);
        data.put("state", state.getDescription().translate(translations));
        data.put("stateName", state.name());
        data.put("stateValue", state.getValue());

        try {
            JsonWriter writer = new JsonWriter(Common.JSON_CONTEXT, response.getWriter());
            writer.writeObject(data);
        } catch (JsonException e) {
            throw new RuntimeException(e);
        }
    }

}
