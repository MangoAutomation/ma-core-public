package com.serotonin.m2m2.web.mvc.spring.security.authentication;

import static com.serotonin.m2m2.Common.getBean;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertThrows;

import java.time.Duration;
import java.util.Date;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.server.resource.BearerTokenAuthenticationToken;
import org.springframework.security.web.authentication.WebAuthenticationDetails;

import com.infiniteautomation.mango.spring.components.TokenAuthenticationService;
import com.infiniteautomation.mango.spring.service.UsersService;
import com.serotonin.m2m2.MangoTestBase;
import com.serotonin.m2m2.MockMangoLifecycle;
import com.serotonin.m2m2.vo.User;
import com.serotonin.timer.SimulationTimer;

import io.jsonwebtoken.ExpiredJwtException;

/**
 * @author Jared Wiltshire
 */

public class MangoTokenAuthenticationProviderTest extends MangoTestBase {

  AuthenticationManager authenticationManager;
  TokenAuthenticationService tokenAuthService;
  UsersService usersService;
  SimulationTimer clock;

  @Override
  protected MockMangoLifecycle getLifecycle() {
    return super.getLifecycle().setInitializeWebServer(true);
  }

  @Before
  public void setUp() throws Exception {
    // AuthenticationManager is not registered as a bean, get it from AuthenticationConfiguration.
    // To get an auth manager for a particular config (e.g. SessionSecurityConfiguration) you could use the
    // WebSecurityConfigurerAdapter.authenticationManagerBean() method (which includes the anonymous auth provider).
    var authenticationConfiguration = lifecycle.getRootWebAppContext().getBean(AuthenticationConfiguration.class);
    this.authenticationManager = authenticationConfiguration.getAuthenticationManager();
    this.tokenAuthService = getBean(TokenAuthenticationService.class);
    this.usersService = getBean(UsersService.class);
    this.clock = getBean(SimulationTimer.class);
  }

  @Test
  public void successfulAuthentication() {
    Date expiry = Date.from(clock.instant().plus(Duration.ofDays(1)));
    User user = usersService.get("admin");
    String token = tokenAuthService.generateToken(user, expiry);

    BearerTokenAuthenticationToken authRequest = authRequest(token);
    Authentication authentication = authenticationManager.authenticate(authRequest);

    assertThat(authentication, notNullValue());
    assertThat(authentication, instanceOf(JwtAuthentication.class));
    assertThat(authentication.getCredentials(), equalTo(token));
    assertThat(authentication.isAuthenticated(), equalTo(true));
    assertThat(authentication.getPrincipal(), equalTo(user));
    assertThat(
        authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority).collect(Collectors.toList()),
        containsInAnyOrder("ROLE_ANONYMOUS", "ROLE_USER", "ROLE_ADMIN")
    );
  }

  @Test
  public void expiredToken() {
    Date expiry = Date.from(clock.instant().plus(Duration.ofDays(1)));
    User user = usersService.get("admin");
    String token = tokenAuthService.generateToken(user, expiry);

    clock.fastForward(Duration.ofDays(2));

    BearerTokenAuthenticationToken authRequest = authRequest(token);
    var ex = assertThrows(CredentialsExpiredException.class, () -> authenticationManager.authenticate(authRequest));
    assertThat(ex.getCause(), instanceOf(ExpiredJwtException.class));
  }

  private BearerTokenAuthenticationToken authRequest(String token) {
    var authRequest = new BearerTokenAuthenticationToken(token);
    authRequest.setDetails(new WebAuthenticationDetails(new MockHttpServletRequest()));
    return authRequest;
  }
}
