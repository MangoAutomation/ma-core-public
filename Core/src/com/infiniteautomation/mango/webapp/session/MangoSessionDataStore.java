/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */

package com.infiniteautomation.mango.webapp.session;

import org.eclipse.jetty.server.session.SessionContext;
import org.eclipse.jetty.server.session.SessionDataStore;

import com.serotonin.m2m2.vo.MangoSessionDataVO;

/**
 * TODO Mango 4.2 remove all the methods which are used only for testing
 *
 * @author Terry Packer
 */
public interface MangoSessionDataStore extends SessionDataStore {

    /**
     * TODO Mango 4.2 do we even need to do this?
     * Delete all stored sessions for this user
     */
    boolean deleteSessionsForUser(int id);

    /**
     * Get a stored session
     */
    MangoSessionDataVO get(String sessionId, String contextPath, String virtualHost);

    /**
     * Update a stored session
     */
    void update(String sessionId, String contextPath, String virtualHost, MangoSessionDataVO vo);

    /**
     * Remove a stored session
     */
    boolean delete(String sessionId, String contextPath, String virtualHost);

    /**
     * Add a new stored session
     */
    void add(MangoSessionDataVO vo);

    /**
     * return the session context
     */
    SessionContext getSessionContext();
}
