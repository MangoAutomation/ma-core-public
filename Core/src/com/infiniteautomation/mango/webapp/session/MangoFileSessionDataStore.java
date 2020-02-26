/**
 * Copyright (C) 2020  Infinite Automation Software. All rights reserved.
 */

package com.infiniteautomation.mango.webapp.session;

import org.eclipse.jetty.server.session.FileSessionDataStore;
import org.eclipse.jetty.server.session.SessionData;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;

import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.dao.UserDao;
import com.serotonin.m2m2.vo.User;
import com.serotonin.m2m2.web.mvc.spring.security.authentication.MangoPasswordAuthenticationProvider;

/**
 * Handle saving only certain information from our sessions as some of the
 *  data is not serializable.
 *
 * @author Terry Packer
 */
public class MangoFileSessionDataStore extends FileSessionDataStore {

    private static final String USER_ID = "userId";
    private final UserDao userDao;

    public MangoFileSessionDataStore() {
        this.userDao = UserDao.getInstance();
    }

    @Override
    public void doStore(String id, SessionData data, long lastSaveTime) throws Exception {

        SessionData copy = new SessionData(data.getId(),
                data.getContextPath(),
                data.getVhost(),
                data.getCreated(),
                data.getAccessed(),
                data.getLastAccessed(),
                data.getMaxInactiveMs(),
                data.getAllAttributes());
        copy.copy(data);

        //Strip out security context, we will save info to reload if there is a valid user
        Object context = copy.getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
        copy.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, null);

        //Remove the latest exception, this may not be serializable
        copy.setAttribute(Common.SESSION_USER_EXCEPTION, null);

        //Persist the user id to retrieve on load
        if (context instanceof SecurityContext) {
            SecurityContext securityContext = (SecurityContext) context;
            Authentication auth = securityContext.getAuthentication();
            if (auth != null) {
                Object principle = auth.getPrincipal();
                if (principle instanceof User) {
                    User user = (User) principle;
                    data.setAttribute(USER_ID, user.getId());
                }
            }
        }
        super.doStore(id, copy, lastSaveTime);
    }

    @Override
    public SessionData doLoad(String id) throws Exception {
        SessionData data = super.doLoad(id);
        if(data == null) {
            return data;
        }
        //Put the user into the session
        Integer userId = (Integer)data.getAttribute(USER_ID);
        if(userId != null) {
            User user = userDao.get(userId);
            if(user != null) {
                UsernamePasswordAuthenticationToken auth = MangoPasswordAuthenticationProvider.createAuthenticatedToken(user);
                SecurityContextImpl impl = new SecurityContextImpl(auth);
                data.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, impl);
            }
        }
        return data;
    }
}
