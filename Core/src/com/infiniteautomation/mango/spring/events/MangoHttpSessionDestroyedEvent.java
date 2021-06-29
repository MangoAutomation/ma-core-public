/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.infiniteautomation.mango.spring.events;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpSession;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.web.session.HttpSessionDestroyedEvent;

import com.serotonin.m2m2.web.mvc.spring.security.MangoSessionRegistry;

/**
 * Work around for not being able to access security contexts and authentication objects in new thread after session destroyed.
 * @author Jared Wiltshire
 */
public class MangoHttpSessionDestroyedEvent extends HttpSessionDestroyedEvent {
    private static final long serialVersionUID = 1L;
    private final Map<SecurityContext, Authentication> contexts;
    private final boolean userMigratedToNewSession;

    public MangoHttpSessionDestroyedEvent(HttpSession session) {
        super(session);

        this.contexts = new HashMap<>();
        for (SecurityContext context: super.getSecurityContexts()) {
            this.contexts.put(context, context.getAuthentication());
        }

        this.userMigratedToNewSession = session.getAttribute(MangoSessionRegistry.USER_MIGRATED_TO_NEW_SESSION_ATTRIBUTE) == Boolean.TRUE ? true : false;
    }

    @Override
    public List<SecurityContext> getSecurityContexts() {
        return new ArrayList<>(this.contexts.keySet());
    }

    public Collection<Authentication> getAuthentications() {
        return Collections.unmodifiableCollection(this.contexts.values());
    }

    /**
     * {@link MangoSessionRegistry#userUpdated} can register a new session for a user and copy all its attributes over.
     * This event will still be fired but you can check if a user was migrated to a new session.
     *
     * @return true if the user associated with this session was migrated to a new session
     */
    public boolean isUserMigratedToNewSession() {
        return userMigratedToNewSession;
    }
}