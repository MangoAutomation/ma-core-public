/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */

package com.serotonin.m2m2.web.mvc.spring.security.oauth2;

import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
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
import com.infiniteautomation.mango.util.EnvironmentPropertyMapper;

@Configuration
@ConditionalOnProperty("${authentication.oauth2.enabled}")
public class OAuth2Configuration {

    @Bean
    public ClientRegistrationRepository clientRegistrationRepository(Environment env, RegistrationPropertyMapperFactory mapperFactory) {
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
                    builder.redirectUri("{baseUrl}/oauth2/callback/{registrationId}");

                    EnvironmentPropertyMapper propertyMapper = mapperFactory.forRegistrationId(registrationId);

                    propertyMapper.map("authorizationUri").ifPresent(builder::authorizationUri);
                    propertyMapper.map("tokenUri").ifPresent(builder::tokenUri);
                    propertyMapper.map("jwkSetUri").ifPresent(builder::jwkSetUri);
                    propertyMapper.map("issuerUri").ifPresent(builder::issuerUri);
                    propertyMapper.map("userInfoUri").ifPresent(builder::userInfoUri);
                    propertyMapper.map("userInfoAuthenticationMethod", AuthenticationMethod.class).ifPresent(builder::userInfoAuthenticationMethod);
                    propertyMapper.map("userNameAttributeName").ifPresent(builder::userNameAttributeName);
                    propertyMapper.map("clientAuthenticationMethod", ClientAuthenticationMethod.class).ifPresent(builder::clientAuthenticationMethod);
                    propertyMapper.map("authorizationGrantType", AuthorizationGrantType.class).ifPresent(builder::authorizationGrantType);
                    propertyMapper.map("scope", String[].class).ifPresent(builder::scope);
                    propertyMapper.map("clientName").ifPresent(builder::clientName);
                    propertyMapper.map("clientId").ifPresent(builder::clientId);
                    propertyMapper.map("clientSecret").ifPresent(builder::clientSecret);
                    propertyMapper.map("redirectUri").ifPresent(builder::redirectUri);

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

    @Bean
    public RegistrationPropertyMapperFactory registrationPropertyMapperFactory(@Autowired Environment env) {
        return (registrationId, suffix) -> {
            String providerId = env.getProperty("oauth2.client.registration." + registrationId + ".provider", registrationId);
            String registrationPrefix = "oauth2.client.registration." + registrationId + "." + suffix;
            String providerPrefix = "oauth2.client.provider." + providerId + "." + suffix;
            String defaultPrefix = "oauth2.client.default." + suffix;
            return new EnvironmentPropertyMapper(env, registrationPrefix, providerPrefix, defaultPrefix);
        };
    }

}
