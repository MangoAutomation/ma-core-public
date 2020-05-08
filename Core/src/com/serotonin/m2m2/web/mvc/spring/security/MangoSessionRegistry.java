/**
 * Copyright (C) 2017 Infinite Automation Software. All rights reserved.
 *
 */
package com.serotonin.m2m2.web.mvc.spring.security;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.stereotype.Component;

import com.infiniteautomation.mango.spring.events.SessionLoadedEvent;
import com.infiniteautomation.mango.spring.session.MangoSessionDataStore;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.vo.User;
import com.serotonin.m2m2.vo.permission.PermissionHolder;
import com.serotonin.m2m2.web.mvc.spring.security.authentication.MangoPasswordAuthenticationProvider;

/**
 *
 * Customize Session Registration Actions
 *
 * TODO Mango 4.0 consider this being merged into the @see MangoJdbcSessionDataStore
 *
 * @author Terry Packer
 */
@Component
public class MangoSessionRegistry extends SessionRegistryImpl {

    private final MangoSessionDataStore store;

    @Autowired
    public MangoSessionRegistry(MangoSessionDataStore store) {
        this.store = store;
    }

    /**
     * Used to indicate that a user was migrated to a new session
     */
    public static final String USER_MIGRATED_TO_NEW_SESSION_ATTRIBUTE = "MANGO_MIGRATED_TO_NEW_SESSION";

    /**
     * Return a count of all active sessions.  This specifically counts
     *  active sessions in Jetty and does not consider any
     *  sessions in the store that have not yet been loaded
     *
     * @return
     */
    public int getActiveSessionCount(){
        int activeCount = 0;
        final List<Object> allPrincipals = this.getAllPrincipals();

        for (final Object principal : allPrincipals) {
            if (principal instanceof User) {
                activeCount += this.getAllSessions(principal, false).size();
            }
        }

        return activeCount;
    }


    /**
     * Expires the http sessions for a user.
     * The sessions are located by the user's id (the session registry stores Users in a Map, User.equals() and User.hashCode() only compare the id).
     * Note that if you wish to keep the current http session active you should register a new session and set the security context. See UserRestController.
     *
     * @param user - cannot be null
     */
    public void exireSessionsForUser(User user) {
        List<SessionInformation> userSessions = this.getAllSessions(user, false);
        for (SessionInformation info : userSessions) {
            info.expireNow();
        }
        this.store.deleteSessionsForUser(user.getId());
    }

    /**
     * This method should be called if a user is updated via HTTP (e.g. via our UserRestController).
     * If the user's ID is the same as the current HTTP user's ID then the Spring Security context and
     * session attributes will be updated.
     *
     * @param request
     * @param user
     */
    public void userUpdated(HttpServletRequest request, User user) {
        PermissionHolder currentUser = Common.getUser();
        if(currentUser instanceof User && (((User)currentUser).getId() == user.getId())) {
            HttpSession session = request.getSession(false);
            if (session != null) {
                SessionInformation info = this.getSessionInformation(session.getId());
                if (info == null) {
                    if (logger.isWarnEnabled()) {
                        logger.warn("Unknown session " + session.getId());
                    }
                } else if (info.isExpired() && !user.isDisabled()) {
                    // Session was set to expire via a call to exireSessionsForUser() from the DAO.
                    // Invalidate the current session and register a new one right now so the user can continue working.

                    // Copy all attributes as per SessionFixationProtectionStrategy
                    Enumeration<String> names = session.getAttributeNames();
                    Map<String, Object> attributes = new HashMap<>();
                    while (names.hasMoreElements()) {
                        String name = names.nextElement();
                        attributes.put(name, session.getAttribute(name));
                    }

                    this.removeSessionInformation(session.getId());

                    session.setAttribute(USER_MIGRATED_TO_NEW_SESSION_ATTRIBUTE, Boolean.TRUE);
                    session.invalidate();

                    HttpSession newSession = request.getSession(true);

                    this.registerNewSession(newSession.getId(), user);

                    for (Entry<String, Object> entry : attributes.entrySet()) {
                        newSession.setAttribute(entry.getKey(), entry.getValue());
                    }

                    session = newSession;
                }
            }

            // Set the spring security context (thread local) to a new Authentication with the updated user and authorities.
            // Updates the SPRING_SECURITY_CONTEXT session attribute as well.
            // Should always be a UsernamePasswordAuthenticationToken a user cannot update themselves via a JWT.
            Authentication currentAuthentication = SecurityContextHolder.getContext().getAuthentication();
            if (currentAuthentication instanceof UsernamePasswordAuthenticationToken) {
                UsernamePasswordAuthenticationToken newAuthentication = MangoPasswordAuthenticationProvider.createAuthenticatedToken(user);
                SecurityContextHolder.getContext().setAuthentication(newAuthentication);
            }
        }
    }

    /**
     * Be aware of any sessions that are loaded from the store this
     *  assumes that sessions in the store that are not actively loaded
     * @param event
     */
    @EventListener
    public void handleSessionLoadedEvent(SessionLoadedEvent event) {
        this.registerNewSession(event.getSessionId(), event.getPrinciple());
    }
}
