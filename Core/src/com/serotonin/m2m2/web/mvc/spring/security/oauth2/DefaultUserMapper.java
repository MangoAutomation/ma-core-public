/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */

package com.serotonin.m2m2.web.mvc.spring.security.oauth2;

import static com.serotonin.m2m2.db.dao.UserDao.LOCKED_PASSWORD;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
import com.infiniteautomation.mango.util.exception.NotFoundException;
import com.serotonin.m2m2.vo.User;
import com.serotonin.m2m2.vo.permission.PermissionHolder;
import com.serotonin.m2m2.vo.role.Role;
import com.serotonin.m2m2.vo.role.RoleVO;

@Component
@ConditionalOnProperty("${authentication.oauth2.enabled}")
public class DefaultUserMapper implements UserMapper {

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
        ClientRegistration clientRegistration = userRequest.getClientRegistration();
        StandardClaimAccessor accessor = toAccessor(oAuth2User);

        String registrationId = clientRegistration.getRegistrationId();
        EnvironmentPropertyMapper userMapping = mapperFactory.forRegistrationId(registrationId, "userMapping.");

        String usernamePrefix = userMapping.map("username.prefix").orElse("");
        String usernameSuffix = userMapping.map("username.suffix").orElse("");
        String username = usernamePrefix +
                userMapping.map("username", accessor::getClaimAsString).orElseThrow(NullPointerException::new) +
                usernameSuffix;

        User user;
        try {
            user = usersService.get(username);
        } catch (NotFoundException e) {
            user = new User();
            user.setUsername(username);
            user.setPassword(LOCKED_PASSWORD);
        }

        String emailPrefix = userMapping.map("email.prefix").orElse("");
        String emailSuffix = userMapping.map("email.suffix").orElse("");
        String email = userMapping.map("email", accessor::getClaimAsString).map(e -> emailPrefix + e + emailSuffix).orElse(null);
        user.setEmail(email);

        userMapping.map("name", accessor::getClaimAsString).ifPresent(user::setName);
        userMapping.map("phone", accessor::getClaimAsString).ifPresent(user::setPhone);
        userMapping.map("locale", accessor::getClaimAsString).ifPresent(user::setLocale);
        userMapping.map("timezone", accessor::getClaimAsString).ifPresent(user::setTimezone);

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

        if (user.isNew()) {
            usersService.insert(user);
        } else {
            usersService.update(username, user);
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
