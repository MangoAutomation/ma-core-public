/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */

package com.infiniteautomation.mango.webapp.session;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.jetty.server.session.AbstractSessionDataStore;
import org.eclipse.jetty.server.session.SessionContext;
import org.eclipse.jetty.server.session.SessionData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Scope;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Component;

import com.infiniteautomation.mango.spring.components.RunAs;
import com.infiniteautomation.mango.spring.events.SessionLoadedEvent;
import com.infiniteautomation.mango.spring.service.UsersService;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.dao.MangoSessionDataDao;
import com.serotonin.m2m2.vo.MangoSessionDataVO;
import com.serotonin.m2m2.vo.User;
import com.serotonin.m2m2.web.mvc.spring.security.authentication.MangoPasswordAuthenticationProvider;
import com.serotonin.util.properties.MangoConfigurationWatcher.MangoConfigurationReloadedEvent;

/**
 *
 * @author Terry Packer
 */
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class MangoJdbcSessionDataStore extends AbstractSessionDataStore implements MangoSessionDataStore {

    private final UsersService userService;
    private final MangoSessionDataDao sessionDao;
    private final ApplicationEventPublisher eventPublisher;
    private final RunAs runAs;
    private final Environment env;

    @Autowired
    public MangoJdbcSessionDataStore(UsersService userService, MangoSessionDataDao sessionDao,
                                     ApplicationEventPublisher publisher, RunAs runAs, Environment env) {
        this.userService = userService;
        this.sessionDao = sessionDao;
        this.eventPublisher = publisher;
        this.runAs = runAs;
        this.env = env;

        updatePersistPeriod();
    }

    @Override
    public boolean isPassivating() {
        return true;
    }

    @Override
    public boolean exists(String id) throws Exception {
        return sessionDao.sessionExists(id, _context.getCanonicalContextPath(),  _context.getVhost());
    }

    @Override
    public boolean delete(String id) throws Exception {
        return sessionDao.delete(id, _context.getCanonicalContextPath(),  _context.getVhost());
    }

    @Override
    public void doStore(String id, SessionData data, long lastSaveTime) throws Exception {
        if (data == null || id == null) {
            return;
        }

        if (lastSaveTime <= 0) {
            MangoSessionDataVO vo = new MangoSessionDataVO(data);
            vo.setSessionId(id);
            maybeSetUserId(vo, data);
            sessionDao.insert(vo);
        }else{
            MangoSessionDataVO vo = new MangoSessionDataVO(data);
            vo.setSessionId(id);
            maybeSetUserId(vo, data);
            sessionDao.update(id, _context.getCanonicalContextPath(), _context.getVhost(), vo);
        }
    }

    /**
     */
    private void maybeSetUserId(MangoSessionDataVO vo, SessionData data) {
        Object context = data.getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
        //Persist the user id to retrieve on load
        if (context instanceof SecurityContext) {
            SecurityContext securityContext = (SecurityContext) context;
            Authentication auth = securityContext.getAuthentication();
            if (auth != null) {
                Object principle = auth.getPrincipal();
                // this is correct as we do not want to store the user id for OAuth2 authenticated users
                if (principle instanceof User) {
                    User user = (User) principle;
                    vo.setUserId(user.getId());
                }
            }
        }
    }

    @Override
    public SessionData doLoad(String id) throws Exception {
        MangoSessionDataVO vo = sessionDao.get(id, _context.getCanonicalContextPath(), _context.getVhost());
        if(vo == null) {
            return null;
        }
        SessionData data = newSessionData(id,
                vo.getCreateTime(),
                vo.getAccessTime(),
                vo.getLastAccessTime(),
                vo.getMaxInterval());

        data.setContextPath(_context.getCanonicalContextPath());
        data.setVhost(_context.getVhost());

        data.setCookieSet(vo.getCookieTime());
        data.setLastNode(vo.getLastNode());
        data.setLastSaved(vo.getLastSavedTime());
        data.setExpiry(vo.getExpiryTime());

        if (vo.getUserId() > 0) {
            User user = runAs.runAs(runAs.systemSuperadmin(), () -> userService.getByIdViaCache(vo.getUserId()));
            SecurityContext sessionContext = SecurityContextHolder.createEmptyContext();
            sessionContext.setAuthentication(MangoPasswordAuthenticationProvider.createAuthenticatedToken(user));
            data.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, sessionContext);
            this.eventPublisher.publishEvent(new SessionLoadedEvent(this, id, user));
        }

        return data;
    }

    @Override
    public Set<String> doGetExpired(Set<String> candidates) {
        long now = Common.timer.currentTimeMillis();
        Set<String> expiredSessionKeys = new HashSet<>();

        /*
         * 1. Select sessions managed by this node for our context that have expired
         */
        long upperBound = now;
        expiredSessionKeys.addAll(sessionDao.getExpiredSessionIds(_context.getCanonicalContextPath(), _context.getVhost(), upperBound));

        /*
         *  2. Select sessions for any node or context that have expired
         *  at least 1 graceperiod since the last expiry check. If we haven't done previous expiry checks, then check
         *  those that have expired at least 3 graceperiod ago.
         */
        if (_lastExpiryCheckTime <= 0)
            upperBound = (now - (3 * (1000L * _gracePeriodSec)));
        else
            upperBound = _lastExpiryCheckTime - (1000L * _gracePeriodSec);
        expiredSessionKeys.addAll(sessionDao.getExpiredSessionIds(_context.getCanonicalContextPath(), _context.getVhost(), upperBound));


        Set<String> notExpiredInDB = new HashSet<>();
        for (String k : candidates) {
            //there are some keys that the session store thought had expired, but were not
            //found in our sweep either because it is no longer in the db, or its
            //expiry time was updated
            if (!expiredSessionKeys.contains(k))
                notExpiredInDB.add(k);
        }

        if (!notExpiredInDB.isEmpty()) {
            //we have some sessions to check
            for (String k : notExpiredInDB) {
                if(sessionDao.sessionExists(k, _context.getCanonicalContextPath(), _context.getVhost())) {
                    //session doesn't exist any more, can be expired
                    expiredSessionKeys.add(k);
                }
            }
        }

        return expiredSessionKeys;
    }

    /**
     */
    @Override
    public SessionContext getSessionContext() {
        return _context;
    }

    @Override
    public boolean deleteSessionsForUser(int id) {
        return sessionDao.deleteSessionsForUser(id);
    }

    @Override
    public MangoSessionDataVO get(String sessionId, String contextPath, String virtualHost) {
        return sessionDao.get(sessionId, contextPath, virtualHost);
    }

    @Override
    public void update(String sessionId, String contextPath, String virtualHost,
            MangoSessionDataVO vo) {
        sessionDao.update(sessionId, contextPath, virtualHost, vo);
    }

    @Override
    public boolean delete(String sessionId, String contextPath, String virtualHost) {
        return sessionDao.delete(sessionId, contextPath, virtualHost);
    }

    @Override
    public void add(MangoSessionDataVO vo) {
        sessionDao.insert(vo);
    }

    @EventListener
    public void propertiesReloaded(MangoConfigurationReloadedEvent event) {
        updatePersistPeriod();
    }

    private void updatePersistPeriod() {
        this.setSavePeriodSec(env.getProperty("sessionCookie.persistPeriodSeconds", Integer.class, 30));
    }

}
