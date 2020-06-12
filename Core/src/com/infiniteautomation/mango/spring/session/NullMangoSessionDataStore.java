/**
 * Copyright (C) 2020  Infinite Automation Software. All rights reserved.
 */

package com.infiniteautomation.mango.spring.session;

import org.eclipse.jetty.server.session.NullSessionDataStore;
import org.eclipse.jetty.server.session.SessionContext;

import com.serotonin.m2m2.vo.MangoSessionDataVO;

/**
 *
 * @author Terry Packer
 */
public class NullMangoSessionDataStore extends NullSessionDataStore implements MangoSessionDataStore {

    public NullMangoSessionDataStore() {

    }

    @Override
    public void initialize(SessionContext context) throws Exception {
        if (isStarted())
            throw new IllegalStateException("Context set after SessionDataStore started");
        _context = context;
    }

    @Override
    public SessionContext getSessionContext() {
        return _context;
    }

    @Override
    public boolean deleteSessionsForUser(int id) {
        return false;
    }

    @Override
    public MangoSessionDataVO get(String sessionId, String contextPath, String virtualHost) {
        return null;
    }

    @Override
    public void update(String sessionId, String contextPath, String virtualHost,
            MangoSessionDataVO vo) {
    }

    @Override
    public boolean delete(String sessionId, String contextPath, String virtualHost) {
        return false;
    }

    @Override
    public void add(MangoSessionDataVO vo) {

    }

}
