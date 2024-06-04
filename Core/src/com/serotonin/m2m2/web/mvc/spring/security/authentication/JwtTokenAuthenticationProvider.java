/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.web.mvc.spring.security.authentication;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.qual.DefaultQualifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.env.Environment;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoders;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.BearerTokenAuthenticationToken;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationProvider;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.stereotype.Component;

import com.infiniteautomation.mango.spring.ConditionalOnProperty;
import com.infiniteautomation.mango.spring.components.RunAs;
import com.infiniteautomation.mango.spring.service.RoleService;
import com.infiniteautomation.mango.util.EnvironmentPropertyMapper;
import com.infiniteautomation.mango.util.exception.NotFoundException;
import com.nimbusds.jwt.JWTParser;
import com.serotonin.m2m2.vo.permission.PermissionHolder;
import com.serotonin.m2m2.vo.role.Role;

/**
 * <p>A generic bearer token authentication provider for JWT format OAuth 2 / OIDC bearer tokens.
 * Validates tokens against multiple issuers/providers which are configured by the user via properties.</p>
 *
 * <p>See also {@link MangoTokenAuthenticationProvider} which is used for Mango JWT bearer tokens.</p>
 *
 * <p>Not to be confused with the built-in Spring provider {@link JwtAuthenticationProvider}.</p>
 *
 * @author Jared Wiltshire
 */
@Component
@Order(2)
@ConditionalOnProperty("${authentication.token.enabled}")
@DefaultQualifier(NonNull.class)
public class JwtTokenAuthenticationProvider implements AuthenticationProvider {

  private final Logger log = LoggerFactory.getLogger(JwtTokenAuthenticationProvider.class);
  private final JwtGrantedAuthoritiesConverter jwtGrantedAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();
  private final Map<String, ProviderConfiguration> providerConfigurations = new ConcurrentHashMap<>();
  private final Environment env;
  private final ConversionService conversionService;
  private final RoleService roleService;
  private final RunAs runAs;

  @Autowired
  public JwtTokenAuthenticationProvider(Environment env,
                                        ConversionService conversionService,
                                        RoleService roleService,
                                        RunAs runAs) {

    this.env = env;
    this.conversionService = conversionService;
    this.roleService = roleService;
    this.runAs = runAs;
    reloadProviderConfigurations();
  }

  private void reloadProviderConfigurations() {
    providerConfigurations.clear();

    List<String> providers = toList(env.getRequiredProperty("oauth2.resourceserver.providers"));
    for (String provider : providers) {
      loadProvider(provider);
    }
  }

  private List<String> toList(String value) {
    TypeDescriptor stringType = TypeDescriptor.valueOf(String.class);
    TypeDescriptor listType = TypeDescriptor.collection(List.class, stringType);
    //noinspection unchecked
    return (List<String>) Objects.requireNonNull(conversionService.convert(value, stringType, listType));
  }

  private Set<String> toSet(String value) {
    return new HashSet<>(toList(value));
  }

  private JwtClaimValidator<List<String>> audienceValidator(Set<String> audiences) {
    return new JwtClaimValidator<>(JwtClaimNames.AUD,
        aud -> audiences.isEmpty() || aud.stream().anyMatch(audiences::contains));
  }

  private void loadProvider(String provider) {
    try {
      var mapper = new EnvironmentPropertyMapper(env, String.format("oauth2.resourceserver.%s.jwt.", provider));
      String issuerUri = mapper.map("issuer-uri").orElseThrow();
      NimbusJwtDecoder jwtDecoder = mapper.map("jwk-set-uri")
          .map(jwkSetUri -> NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build())
          .orElseGet(() -> (NimbusJwtDecoder) JwtDecoders.fromIssuerLocation(issuerUri));

      var audiences = mapper.map("audiences", this::toSet)
          .orElse(Set.of());
      var issuerValidator = JwtValidators.createDefaultWithIssuer(issuerUri);
      var audienceValidator = audienceValidator(audiences);
      jwtDecoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(issuerValidator, audienceValidator));

      List<String> roles = mapper.map("roles", this::toList).orElse(List.of());
      Function<String, Collection<String>> scopeMapper =
          scope -> mapper.map(String.format("map-scope.%s", scope), this::toList).orElse(List.of());
      boolean insertRoles = mapper.map("insert-roles", boolean.class).orElse(false);
      var providerConfiguration = new ProviderConfiguration(issuerUri, jwtDecoder, roles, scopeMapper, insertRoles);
      if (providerConfigurations.putIfAbsent(issuerUri, providerConfiguration) != null) {
        log.warn("Provider '{}' skipped, duplicate issuer URI: {}", provider, issuerUri);
      }
    } catch (Exception e) {
      log.error("Failed to load provider '{}'", provider, e);
    }
  }

  @Override
  public @Nullable Authentication authenticate(Authentication authentication) throws AuthenticationException {
    if (!(authentication instanceof BearerTokenAuthenticationToken)) return null;
    BearerTokenAuthenticationToken bearerToken = (BearerTokenAuthenticationToken) authentication;

    String issuer;
    try {
      issuer = JWTParser.parse(bearerToken.getToken()).getJWTClaimsSet().getIssuer();
    } catch (ParseException e) {
      return null;
    }

    if (issuer == null) {
      return null;
    }

    ProviderConfiguration providerConfiguration = providerConfigurations.get(issuer);
    if (providerConfiguration == null) {
      return null;
    }

    Jwt jwt;
    try {
      jwt = providerConfiguration.getJwtDecoder().decode(bearerToken.getToken());
    } catch (BadJwtException e) {
      log.debug("Failed to authenticate since the JWT was invalid");
      throw new InvalidBearerTokenException(e.getMessage(), e);
    } catch (JwtException e) {
      throw new AuthenticationServiceException(e.getMessage(), e);
    }

    return convertJwt(jwt, providerConfiguration, bearerToken.getDetails());
  }

  private PreAuthenticatedAuthenticationToken convertJwt(Jwt jwt, ProviderConfiguration providerConfiguration, Object details) {
    String issuer = jwt.getClaim(JwtClaimNames.ISS);
    String permissionHolderName;
    if (jwt.containsClaim(JwtClaimNames.SUB)) {
      permissionHolderName = String.format("%s/%s", issuer, jwt.getClaim(JwtClaimNames.SUB));
    } else {
      permissionHolderName = String.format("%s/%s", issuer, jwt.getClaim(JwtClaimNames.AUD));
    }

    var roles = getRoles(providerConfiguration, jwt);
    var scopeAuthorities = jwtGrantedAuthoritiesConverter.convert(jwt);
    var roleAuthorities = MangoUserDetailsService.getGrantedAuthorities(roles);
    var authorities = combine(scopeAuthorities, roleAuthorities);

    PermissionHolder principal = new JwtPermissionHolder(permissionHolderName, roles);
    PreAuthenticatedAuthenticationToken preAuthenticated = new PreAuthenticatedAuthenticationToken(principal, jwt, authorities);
    preAuthenticated.setDetails(details);
    return preAuthenticated;
  }

  private Set<Role> getRoles(ProviderConfiguration providerConfiguration, Jwt jwt) {
    var scopeRoles = getRolesForScopes(providerConfiguration, jwt);
    return runAs.runAs(PermissionHolder.SYSTEM_SUPERADMIN, () ->
        Stream.concat(providerConfiguration.getRoles().stream(), scopeRoles)
            .map(providerConfiguration.insertRoles ? this::getOrInsertRole : this::getRole)
            .filter(Objects::nonNull)
            .collect(Collectors.toCollection(HashSet::new)));
  }

  /**
   * See {@link JwtGrantedAuthoritiesConverter}.
   */
  private Stream<String> getRolesForScopes(ProviderConfiguration providerConfiguration, Jwt jwt) {
    String scope = "";
    if (jwt.containsClaim("scope")) {
      scope = jwt.getClaim("scope");
    } else if (jwt.containsClaim("scp")) {
      scope = jwt.getClaim("scp");
    }

    List<String> scopes = scope.isEmpty() ?
        List.of() :
        Arrays.asList(scope.split(" "));

    return scopes.stream().map(providerConfiguration.getScopeMapper()).flatMap(Collection::stream);
  }

  private @Nullable Role getRole(String roleXid) {
    try {
      return roleService.get(roleXid).getRole();
    } catch (NotFoundException e) {
      return null;
    }
  }

  private @Nullable Role getOrInsertRole(String roleXid) {
    return roleService.getOrInsert(roleXid).getRole();
  }

  private Collection<GrantedAuthority> combine(Collection<GrantedAuthority> a, Collection<GrantedAuthority> b) {
    if (!(a instanceof ArrayList)) {
      a = new ArrayList<>(a);
    }
    a.addAll(b);
    return a;
  }

  @Override
  public boolean supports(Class<?> authentication) {
    return BearerTokenAuthenticationToken.class.isAssignableFrom(authentication);
  }

  private static class ProviderConfiguration {
    private final String issuer;
    private final JwtDecoder jwtDecoder;
    private final Collection<String> roles;
    private final Function<String, Collection<String>> scopeMapper;
    private final boolean insertRoles;

    public ProviderConfiguration(
        String issuer, JwtDecoder jwtDecoder, Collection<String> roles,
        Function<String, Collection<String>> scopeMapper, boolean insertRoles) {
      this.issuer = issuer;
      this.jwtDecoder = jwtDecoder;
      this.roles = roles;
      this.scopeMapper = scopeMapper;
      this.insertRoles = insertRoles;
    }

    public String getIssuer() {
      return issuer;
    }

    public JwtDecoder getJwtDecoder() {
      return jwtDecoder;
    }

    public Collection<String> getRoles() {
      return roles;
    }

    public Function<String, Collection<String>> getScopeMapper() {
      return scopeMapper;
    }

    public boolean isInsertRoles() {
      return insertRoles;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      ProviderConfiguration that = (ProviderConfiguration) o;

      if (insertRoles != that.insertRoles) return false;
      if (!Objects.equals(issuer, that.issuer)) return false;
      if (!Objects.equals(jwtDecoder, that.jwtDecoder)) return false;
      if (!Objects.equals(roles, that.roles)) return false;
      return Objects.equals(scopeMapper, that.scopeMapper);
    }

    @Override
    public int hashCode() {
      int result = issuer != null ? issuer.hashCode() : 0;
      result = 31 * result + (jwtDecoder != null ? jwtDecoder.hashCode() : 0);
      result = 31 * result + (roles != null ? roles.hashCode() : 0);
      result = 31 * result + (scopeMapper != null ? scopeMapper.hashCode() : 0);
      result = 31 * result + (insertRoles ? 1 : 0);
      return result;
    }
  }
}
