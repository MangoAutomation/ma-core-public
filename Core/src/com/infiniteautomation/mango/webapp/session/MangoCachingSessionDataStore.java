/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */

package com.infiniteautomation.mango.webapp.session;

import org.eclipse.jetty.server.session.CachingSessionDataStore;
import org.eclipse.jetty.server.session.SessionContext;
import org.eclipse.jetty.server.session.SessionDataMap;

import com.serotonin.m2m2.vo.MangoSessionDataVO;

/**
 *
 * @author Terry Packer
 */
public class MangoCachingSessionDataStore extends CachingSessionDataStore implements MangoSessionDataStore {

    private final MangoSessionDataStore delegate;

    public MangoCachingSessionDataStore(SessionDataMap cache, MangoSessionDataStore delegate) {
        super(cache, delegate);
        this.delegate = delegate;
    }

    @Override
    public boolean deleteSessionsForUser(int id) {
        // TODO Mango 4.2 this does not remove sessions from either L1 cache (DefaultSessionCache) or L2 cache (SessionDataMap)
        return delegate.deleteSessionsForUser(id);
    }

    @Override
    public MangoSessionDataVO get(String sessionId, String contextPath, String virtualHost) {
        return delegate.get(sessionId, contextPath, virtualHost);
    }

    @Override
    public void update(String sessionId, String contextPath, String virtualHost,
            MangoSessionDataVO vo) {
        delegate.update(sessionId, contextPath, virtualHost, vo);
    }

    @Override
    public boolean delete(String sessionId, String contextPath, String virtualHost) {
        return delegate.delete(sessionId, contextPath, virtualHost);
    }

    @Override
    public void add(MangoSessionDataVO vo) {
        delegate.add(vo);
    }

    @Override
    public SessionContext getSessionContext() {
        return delegate.getSessionContext();
    }

}
