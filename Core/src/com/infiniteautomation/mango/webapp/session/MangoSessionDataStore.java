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
     * @param id
     */
    boolean deleteSessionsForUser(int id);

    /**
     * Get a stored session
     * @param sessionId
     * @param contextPath
     * @param virtualHost
     * @return
     */
    MangoSessionDataVO get(String sessionId, String contextPath, String virtualHost);

    /**
     * Update a stored session
     * @param sessionId
     * @param contextPath
     * @param virtualHost
     * @param vo
     */
    void update(String sessionId, String contextPath, String virtualHost, MangoSessionDataVO vo);

    /**
     * Remove a stored session
     * @param sessionId
     * @param contextPath
     * @param virtualHost
     * @return
     */
    boolean delete(String sessionId, String contextPath, String virtualHost);

    /**
     * Add a new stored session
     * @param vo
     */
    void add(MangoSessionDataVO vo);

    /**
     * return the session context
     * @return
     */
    SessionContext getSessionContext();
}
