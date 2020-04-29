/*
 * Copyright (C) 2020 Infinite Automation Software. All rights reserved.
 */
package com.infiniteautomation.mango.spring.components.urlhandlers;

import java.net.URL;
import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author Jared Wiltshire
 */
@Component
public class MangoURLStreamHandlerFactory implements URLStreamHandlerFactory {

    private final Map<String, URLStreamHandler> handlers;

    @Autowired
    public MangoURLStreamHandlerFactory(List<URLStreamHandler> handlers) {
        this.handlers = new HashMap<>(handlers.size());

        // want lowest precedence handlers first, iterate in reverse
        for (int i = handlers.size() - 1; i >= 0; i--) {
            URLStreamHandler handler = handlers.get(i);
            SupportedProtocols annotation = handler.getClass().getAnnotation(SupportedProtocols.class);
            if (annotation != null) {
                for (String protocol : annotation.value()) {
                    this.handlers.put(protocol, handler);
                }
            }
        }

        // can only be done once, ever
        URL.setURLStreamHandlerFactory(this);
    }

    @Override
    public URLStreamHandler createURLStreamHandler(String protocol) {
        return handlers.get(protocol);
    }

}
