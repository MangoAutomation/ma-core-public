/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */

package com.serotonin.m2m2.web.mvc.spring.security;

import java.util.Arrays;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.security.config.oauth2.client.CommonOAuth2Provider;
import org.springframework.security.oauth2.client.InMemoryOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.AuthenticatedPrincipalOAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.core.AuthenticationMethod;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;

import com.infiniteautomation.mango.spring.ConditionalOnProperty;

@Configuration
@ConditionalOnProperty("${authentication.oauth2.enabled}")
public class OAuth2Configuration {

    @Bean
    public ClientRegistrationRepository clientRegistrationRepository(Environment env) {
        return new InMemoryClientRegistrationRepository(Arrays.stream(env.getRequiredProperty("oauth2.client.registrationIds", String[].class))
                .map(registrationId -> {
                    ClientRegistration.Builder builder = null;

                    // lookup a provider, defaults to the same name as the registration
                    String providerId = env.getProperty("oauth2.client.registration." + registrationId + ".provider", registrationId);
                    try {
                        builder = CommonOAuth2Provider.valueOf(providerId.toUpperCase(Locale.ROOT)).getBuilder(registrationId);
                    } catch (IllegalArgumentException e) {
                        // dont care
                    }

                    if (builder == null) {
                        builder = ClientRegistration.withRegistrationId(registrationId);
                    }

                    // set URL to redirect back to Mango
                    builder.redirectUri("{baseUrl}/{action}/oauth2/code/{registrationId}");

                    // load provider defaults first
                    PropertyMapper providerMapper = new PropertyMapper(env, "oauth2.client.provider." + providerId + ".");
                    providerMapper.map("authorizationUri", builder::authorizationUri);
                    providerMapper.map("tokenUri", builder::tokenUri);
                    providerMapper.map("jwkSetUri", builder::jwkSetUri);
                    providerMapper.map("issuerUri", builder::issuerUri);
                    providerMapper.map("userInfoUri", builder::userInfoUri);
                    providerMapper.map("userInfoAuthenticationMethod", AuthenticationMethod.class, builder::userInfoAuthenticationMethod);
                    providerMapper.map("userNameAttributeName", builder::userNameAttributeName);
                    providerMapper.map("clientAuthenticationMethod", ClientAuthenticationMethod.class, builder::clientAuthenticationMethod);
                    providerMapper.map("authorizationGrantType", AuthorizationGrantType.class, builder::authorizationGrantType);
                    providerMapper.map("scope", String[].class, builder::scope);
                    providerMapper.map("clientName", builder::clientName);

                    // load registration properties
                    PropertyMapper registrationMapper = new PropertyMapper(env, "oauth2.client.registration." + registrationId + ".");
                    registrationMapper.map("clientId", builder::clientId);
                    registrationMapper.map("clientSecret", builder::clientSecret);
                    registrationMapper.map("clientAuthenticationMethod", ClientAuthenticationMethod.class, builder::clientAuthenticationMethod);
                    registrationMapper.map("authorizationGrantType", AuthorizationGrantType.class, builder::authorizationGrantType);
                    registrationMapper.map("redirectUri", builder::redirectUri);
                    registrationMapper.map("scope", String[].class, builder::scope);
                    registrationMapper.map("clientName", builder::clientName);

                    return builder.build();
                })
                .collect(Collectors.toList()));
    }

    @Bean
    public OAuth2AuthorizedClientService authorizedClientService(ClientRegistrationRepository clientRegistrationRepository) {
        return new InMemoryOAuth2AuthorizedClientService(clientRegistrationRepository);
    }

    @Bean
    public OAuth2AuthorizedClientRepository authorizedClientRepository(OAuth2AuthorizedClientService authorizedClientService) {
        return new AuthenticatedPrincipalOAuth2AuthorizedClientRepository(authorizedClientService);
    }

    static class PropertyMapper {
        final Environment env;
        final String prefix;

        PropertyMapper(Environment env, String prefix) {
            this.env = env;
            this.prefix = prefix;
        }

        <T> void map(String property, Class<T> type, Consumer<T> setter) {
            T value = env.getProperty(prefix + property, type);
            if (value != null) {
                setter.accept(value);
            }
        }

        void map(String property, Consumer<String> setter) {
            map(property, String.class, setter);
        }
    }
}
