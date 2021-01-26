/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */

package com.serotonin.m2m2.web.mvc.spring.security.oauth2;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.stereotype.Service;

import com.infiniteautomation.mango.util.EnvironmentPropertyMapper;

@Service
// this service is always enabled
public class OAuth2Information {

    private final ClientRegistrationRepository repository;
    private final RegistrationPropertyMapperFactory mapperFactory;

    @Autowired
    public OAuth2Information(Optional<ClientRegistrationRepository> repository,
                             Optional<RegistrationPropertyMapperFactory> mapperFactory) {
        this.repository = repository.orElse(null);
        this.mapperFactory = mapperFactory.orElse(null);;
    }

    public List<OAuth2ClientInfo> enabledClients() {
        List<ClientRegistration> registrations = new ArrayList<>();
        if (repository instanceof Iterable) {
            for (Object entry : (Iterable<?>) repository) {
                if (entry instanceof ClientRegistration) {
                    registrations.add((ClientRegistration) entry);
                }
            }
        }

        return registrations.stream()
                .map(registration -> {
                    EnvironmentPropertyMapper propertyMapper = mapperFactory.forRegistrationId(registration.getRegistrationId());
                    String logoUri = propertyMapper.map("logoUri").orElse(null);
                    return new OAuth2ClientInfo(registration, logoUri);
                })
                .collect(Collectors.toList());
    }

    public static class OAuth2ClientInfo {
        private final String registrationId;
        private final String clientName;
        private final String logoUri;

        private OAuth2ClientInfo(ClientRegistration registration, String logoUri) {
            this.registrationId = registration.getRegistrationId();
            this.clientName = registration.getClientName();
            this.logoUri = logoUri;
        }

        public String getRegistrationId() {
            return registrationId;
        }

        public String getClientName() {
            return clientName;
        }

        public String getLogoUri() {
            return logoUri;
        }
    }
}
