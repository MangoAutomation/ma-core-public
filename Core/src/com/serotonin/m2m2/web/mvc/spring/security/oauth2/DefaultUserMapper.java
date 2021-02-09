/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */

package com.serotonin.m2m2.web.mvc.spring.security.oauth2;

import static com.serotonin.m2m2.db.dao.UserDao.LOCKED_PASSWORD;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.oidc.StandardClaimAccessor;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;

import com.infiniteautomation.mango.spring.ConditionalOnProperty;
import com.infiniteautomation.mango.spring.service.RoleService;
import com.infiniteautomation.mango.spring.service.UsersService;
import com.infiniteautomation.mango.util.EnvironmentPropertyMapper;
import com.serotonin.m2m2.vo.LinkedAccount;
import com.serotonin.m2m2.vo.OAuth2LinkedAccount;
import com.serotonin.m2m2.vo.User;
import com.serotonin.m2m2.vo.permission.PermissionHolder;
import com.serotonin.m2m2.vo.role.Role;
import com.serotonin.m2m2.vo.role.RoleVO;

@Component
@ConditionalOnProperty("${authentication.oauth2.enabled}")
public class DefaultUserMapper implements UserMapper {

    private final Logger log = LoggerFactory.getLogger(this.getClass());
    private final RoleService roleService;
    private final UsersService usersService;
    private final RegistrationPropertyMapperFactory mapperFactory;

    @Autowired
    public DefaultUserMapper(RoleService roleService, UsersService usersService, RegistrationPropertyMapperFactory mapperFactory) {
        this.roleService = roleService;
        this.usersService = usersService;
        this.mapperFactory = mapperFactory;
    }

    @Override
    public User mapUser(OAuth2UserRequest userRequest, OAuth2User oAuth2User) {
        if (log.isDebugEnabled()) {
            log.debug("Syncing OAuth2 user {} to Mango user", oAuth2User);
        }

        ClientRegistration clientRegistration = userRequest.getClientRegistration();
        StandardClaimAccessor accessor = toAccessor(oAuth2User);

        String registrationId = clientRegistration.getRegistrationId();
        EnvironmentPropertyMapper userMapping = mapperFactory.forRegistrationId(registrationId, "userMapping.");

        Optional<String> issuerOptional = userMapping.map("issuer.fixed");
        if (!issuerOptional.isPresent()) {
            issuerOptional = userMapping.map("issuer", accessor::getClaimAsString);
        }

        String issuer = issuerOptional.orElseThrow(() -> new IllegalStateException("Issuer is required"));
        String subject = userMapping.map("subject", accessor::getClaimAsString)
                .orElseThrow(() -> new IllegalStateException("Subject is required"));

        LinkedAccount linkedAccount = new OAuth2LinkedAccount(issuer, subject);

        User user = usersService.getUserForLinkedAccount(linkedAccount).orElseGet(() -> {
            // only synchronize the username when creating the user
            String usernamePrefix = userMapping.map("username.prefix").orElse("");
            String usernameSuffix = userMapping.map("username.suffix").orElse("");
            String username = userMapping.map("username", accessor::getClaimAsString)
                    .map(un -> usernamePrefix + un + usernameSuffix)
                    .orElse(null); // user will get a random XID for a username if claim is missing

            User newUser = new User();
            newUser.setUsername(username);
            newUser.setPassword(LOCKED_PASSWORD);
            // in case role sync is not turned on
            newUser.setRoles(Collections.singleton(PermissionHolder.USER_ROLE));
            return newUser;
        });

        String emailPrefix = userMapping.map("email.prefix").orElse("");
        String emailSuffix = userMapping.map("email.suffix").orElse("");
        String email = userMapping.map("email", accessor::getClaimAsString)
                .map(e -> emailPrefix + e + emailSuffix)
                .orElse(null); // validation will fail if email is not set
        user.setEmail(email);

        userMapping.map("name", accessor::getClaimAsString).ifPresent(user::setName);
        userMapping.map("phone", accessor::getClaimAsString).ifPresent(user::setPhone);
        userMapping.map("locale", accessor::getClaimAsString).ifPresent(user::setLocale);
        userMapping.map("timezone", accessor::getClaimAsString).ifPresent(user::setTimezone);

        if (userMapping.map("oauth2.client.default.userMapping.roles.sync", Boolean.class).orElse(true)) {
            String rolePrefix = userMapping.map("roles.prefix").orElse("");
            String roleSuffix = userMapping.map("roles.suffix").orElse("");

            Set<String> ignoreRoles = Arrays.stream(userMapping.map("roles.ignore", String[].class)
                    .orElse(new String[0])).collect(Collectors.toSet());

            Stream<String> oauthRoles = userMapping.map("roles", accessor::getClaimAsStringList)
                    .orElseGet(ArrayList::new).stream()
                    .filter(r -> !ignoreRoles.contains(r))
                    .map(r -> userMapping.map("roles.map." + r).orElse(rolePrefix + r + roleSuffix));

            Stream<String> addRoles = Arrays.stream(userMapping.map("roles.add", String[].class)
                    .orElse(new String[0]));

            Set<Role> roles = Stream.concat(oauthRoles, addRoles)
                    .map(roleService::getOrInsert)
                    .map(RoleVO::getRole)
                    .collect(Collectors.toCollection(HashSet::new));

            // ensure user role is present
            roles.add(PermissionHolder.USER_ROLE);
            user.setRoles(roles);
        }

        if (user.isNew()) {
            usersService.insertUserForLinkedAccount(user, linkedAccount);
        } else {
            usersService.update(user.getId(), user);
        }

        return user;
    }

    /**
     * {@link OAuth2User} does not implement {@link StandardClaimAccessor} but {@link OidcUser} does.
     */
    private StandardClaimAccessor toAccessor(OAuth2User delegate) {
        if (delegate instanceof StandardClaimAccessor) {
            return (StandardClaimAccessor) delegate;
        }
        return delegate::getAttributes;
    }
}
