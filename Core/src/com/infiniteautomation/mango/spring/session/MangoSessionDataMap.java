/**
 * Copyright (C) 2020  Infinite Automation Software. All rights reserved.
 */

package com.infiniteautomation.mango.spring.session;

import org.eclipse.jetty.server.session.SessionContext;
import org.eclipse.jetty.server.session.SessionData;
import org.eclipse.jetty.server.session.SessionDataMap;
import org.eclipse.jetty.util.component.AbstractLifeCycle;
import org.springframework.stereotype.Component;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

/**
 *
 * @author Terry Packer
 */
@Component
public class MangoSessionDataMap extends AbstractLifeCycle implements SessionDataMap {

    private final Cache<String, SessionData> cache;

    public MangoSessionDataMap() {
        this.cache = Caffeine.newBuilder().build();
    }

    @Override
    public void initialize(SessionContext context) throws Exception {

    }

    @Override
    public SessionData load(String id) throws Exception {
        return cache.getIfPresent(id);
    }

    @Override
    public void store(String id, SessionData data) throws Exception {
        if (data == null || id == null) {
            return;
        }
        cache.put(id, data);
    }

    @Override
    public boolean delete(String id) throws Exception {
        cache.invalidate(id);
        //The jetty memcache implementation behaves the same by always returning true
        return true;
    }

}
