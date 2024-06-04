/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.infiniteautomation.mango.spring.components;

import java.security.KeyPair;
import java.time.Clock;
import java.util.Date;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import com.infiniteautomation.mango.jwt.JwtSignerVerifier;
import com.infiniteautomation.mango.spring.events.AuthTokensRevokedEvent;
import com.infiniteautomation.mango.spring.service.PermissionService;
import com.infiniteautomation.mango.spring.service.UsersService;
import com.infiniteautomation.mango.util.exception.NotFoundException;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.dao.SystemSettingsDao;
import com.serotonin.m2m2.db.dao.UserDao;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.vo.User;
import com.serotonin.m2m2.vo.permission.PermissionException;
import com.serotonin.m2m2.vo.permission.PermissionHolder;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtBuilder;

/**
 * @author Jared Wiltshire
 */
@Service
public final class TokenAuthenticationService extends JwtSignerVerifier<User> {
    public static final String PUBLIC_KEY_SYSTEM_SETTING = "jwt.userAuth.publicKey";
    public static final String PRIVATE_KEY_SYSTEM_SETTING = "jwt.userAuth.privateKey";

    public static final String TOKEN_TYPE_VALUE = "auth";
    public static final String USER_ID_CLAIM = "id";
    public static final String USER_TOKEN_VERSION_CLAIM = "v";

    private static final int DEFAULT_EXPIRY = 5 * 60 * 1000; // 5 minutes

    private final PermissionService permissionService;
    private final UsersService usersService;
    private final ApplicationContext context;
    private final RunAs runAs;
    private final UserDao userDao;

    @Autowired
    public TokenAuthenticationService(
            PermissionService permissionService,
            UsersService usersService,
            ApplicationContext context,
            RunAs runAs,
            UserDao userDao,
            Clock clock) {

        super(clock);
        this.permissionService = permissionService;
        this.usersService = usersService;
        this.context = context;
        this.runAs = runAs;
        this.userDao = userDao;
    }

    @Override
    protected String tokenType() {
        return TOKEN_TYPE_VALUE;
    }

    @Override
    protected void saveKeyPair(KeyPair keyPair) {
        SystemSettingsDao.getInstance().setValue(PUBLIC_KEY_SYSTEM_SETTING, keyToString(keyPair.getPublic()));
        SystemSettingsDao.getInstance().setValue(PRIVATE_KEY_SYSTEM_SETTING, keyToString(keyPair.getPrivate()));
    }

    @Override
    protected KeyPair loadKeyPair() {
        String publicKeyStr = SystemSettingsDao.getInstance().getValue(PUBLIC_KEY_SYSTEM_SETTING);
        String privateKeyStr = SystemSettingsDao.getInstance().getValue(PRIVATE_KEY_SYSTEM_SETTING);

        if (publicKeyStr != null && !publicKeyStr.isEmpty() && privateKeyStr != null && !privateKeyStr.isEmpty()) {
            return keysToKeyPair(publicKeyStr, privateKeyStr);
        }
        return null;
    }

    public void resetKeys() {
        PermissionHolder user = Common.getUser();
        if (!permissionService.hasAdminRole(user)) {
            throw new PermissionException(new TranslatableMessage("permission.exception.mustBeAdmin"), user);
        }
        this.generateNewKeyPair();
        this.context.publishEvent(new AuthTokensRevokedEvent(this));
    }

    public String generateToken(User user) {
        return this.generateToken(user, null);
    }

    public String generateToken(User user, Date expiry) {
        PermissionHolder currentUser = Common.getUser();
        usersService.ensureEditPermission(currentUser, user);

        if (expiry == null) {
            expiry = new Date(System.currentTimeMillis() + DEFAULT_EXPIRY);
        }

        JwtBuilder builder = this.newToken(user.getUsername(), expiry)
                .claim(USER_ID_CLAIM, user.getId())
                .claim(USER_TOKEN_VERSION_CLAIM, user.getTokenVersion());

        return this.sign(builder);
    }

    public void revokeTokens(User user) {
        PermissionHolder currentUser = Common.getUser();
        usersService.ensureEditPermission(currentUser, user);
        userDao.revokeTokens(user);
    }

    @Override
    protected User verifyClaims(Jws<Claims> token) {
        Claims claims = token.getBody();

        String username = claims.getSubject();
        if (username == null) {
            throw new NotFoundException();
        }
        User user = this.runAs.runAs(runAs.systemSuperadmin(), () -> this.usersService.get(username));
        Integer userId = user.getId();
        this.verifyClaim(token, USER_ID_CLAIM, userId);

        Integer tokenVersion = user.getTokenVersion();
        this.verifyClaim(token, USER_TOKEN_VERSION_CLAIM, tokenVersion);
        return user;
    }
}
