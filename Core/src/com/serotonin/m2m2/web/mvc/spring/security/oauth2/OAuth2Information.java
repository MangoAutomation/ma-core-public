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

@Service
// this service is always enabled
public class OAuth2Information {

    private final ClientRegistrationRepository repository;

    @Autowired
    public OAuth2Information(Optional<ClientRegistrationRepository> repository) {
        this.repository = repository.orElse(null);
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
                .map(OAuth2ClientInfo::new)
                .collect(Collectors.toList());
    }

    public static class OAuth2ClientInfo {
        private final String registrationId;
        private final String clientName;

        private OAuth2ClientInfo(ClientRegistration registration) {
            this.registrationId = registration.getRegistrationId();
            this.clientName = registration.getClientName();
        }

        public String getRegistrationId() {
            return registrationId;
        }

        public String getClientName() {
            return clientName;
        }
    }
}
