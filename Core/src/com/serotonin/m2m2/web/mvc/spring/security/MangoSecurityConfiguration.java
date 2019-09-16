/**
 * Copyright (C) 2017 Infinite Automation Software. All rights reserved.
 */
package com.serotonin.m2m2.web.mvc.spring.security;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpMethod;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.authentication.AccountStatusUserDetailsChecker;
import org.springframework.security.authentication.AuthenticationEventPublisher;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.DefaultAuthenticationEventPublisher;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.annotation.web.configurers.ExpressionUrlAuthorizationConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.userdetails.UserDetailsChecker;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.access.ExceptionTranslationFilter;
import org.springframework.security.web.access.intercept.FilterSecurityInterceptor;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.security.web.authentication.switchuser.SwitchUserFilter;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.security.web.firewall.DefaultHttpFirewall;
import org.springframework.security.web.firewall.HttpFirewall;
import org.springframework.security.web.header.writers.frameoptions.XFrameOptionsHeaderWriter;
import org.springframework.security.web.savedrequest.NullRequestCache;
import org.springframework.security.web.savedrequest.RequestCache;
import org.springframework.security.web.session.SessionInformationExpiredStrategy;
import org.springframework.security.web.util.matcher.AndRequestMatcher;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.NegatedRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.accept.ContentNegotiationStrategy;
import org.springframework.web.accept.HeaderContentNegotiationStrategy;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.module.AuthenticationDefinition;
import com.serotonin.m2m2.module.ModuleRegistry;
import com.serotonin.m2m2.web.mvc.spring.security.authentication.MangoPasswordAuthenticationProvider;
import com.serotonin.m2m2.web.mvc.spring.security.authentication.MangoTokenAuthenticationProvider;
import com.serotonin.m2m2.web.mvc.spring.security.authentication.MangoUserDetailsService;
import com.serotonin.m2m2.web.mvc.spring.security.permissions.MangoMethodSecurityExpressionHandler;
import com.serotonin.m2m2.web.mvc.spring.security.permissions.MangoPermissionEvaluator;

/**
 * @author Jared Wiltshire
 */
@EnableWebSecurity
@ComponentScan(basePackages = {"com.serotonin.m2m2.web.mvc.spring.security"})
public class MangoSecurityConfiguration {

    public static final String IS_PROXY_REQUEST_ATTRIBUTE = "MANGO_IS_PROXY_REQUEST";

    @Bean
    public MethodSecurityExpressionHandler methodSecurityExpressionHandler() {
        MangoMethodSecurityExpressionHandler expressionHandler = new MangoMethodSecurityExpressionHandler();
        expressionHandler.setPermissionEvaluator(new MangoPermissionEvaluator());
        return expressionHandler;
    }

    @Autowired
    public void configureAuthenticationManager(AuthenticationManagerBuilder auth,
            MangoUserDetailsService userDetails,
            MangoPasswordAuthenticationProvider passwordAuthenticationProvider,
            MangoTokenAuthenticationProvider tokenAuthProvider,
            @Value("${authentication.token.enabled:true}") boolean tokenAuthEnabled,
            AuthenticationEventPublisher authenticationEventPublisher
            ) throws Exception {

        // causes DaoAuthenticationProvider to be added which we don't want
        //auth.userDetailsService(userDetails);

        for (AuthenticationDefinition def : ModuleRegistry.getDefinitions(AuthenticationDefinition.class)) {
            AuthenticationProvider provider = def.authenticationProvider();
            if (provider != null) {
                auth.authenticationProvider(provider);
            }
        }

        auth.authenticationProvider(passwordAuthenticationProvider);

        if (tokenAuthEnabled) {
            auth.authenticationProvider(tokenAuthProvider);
        }

        auth.authenticationEventPublisher(authenticationEventPublisher);
    }

    @Bean
    public AuthenticationEventPublisher authenticationEventPublisher(ApplicationEventPublisher eventPublisher) {
        return new DefaultAuthenticationEventPublisher(eventPublisher);
    }

    @Bean
    public UserDetailsChecker userDetailsChecker() {
        return new AccountStatusUserDetailsChecker();
    }

    @Bean
    public static ContentNegotiationStrategy contentNegotiationStrategy() {
        return new HeaderContentNegotiationStrategy();
    }

    @Bean
    public RequestCache requestCache() {
        return new NullRequestCache();
    }

    // used to dectect if we should do redirects on login/authentication failure/logout etc
    @Bean(name="browserHtmlRequestMatcher")
    public RequestMatcher browserHtmlRequestMatcher() {
        return BrowserRequestMatcher.INSTANCE;
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource(
            @Value("${rest.cors.enabled:false}") boolean enabled,
            @Value("${rest.cors.allowedOrigins:}") List<String> allowedOrigins,
            @Value("${rest.cors.allowedMethods:PUT,POST,GET,OPTIONS,DELETE,HEAD}") List<String> allowedMethods,
            @Value("${rest.cors.allowedHeaders:content-type,x-requested-with,authorization}") List<String> allowedHeaders,
            @Value("${rest.cors.exposedHeaders:}") List<String> exposedHeaders,
            @Value("${rest.cors.allowCredentials:false}") boolean allowCredentials,
            @Value("${rest.cors.maxAge:3600}") long maxAge) {

        if (!enabled) return null;

        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(allowedOrigins);
        configuration.setAllowedMethods(allowedMethods);
        configuration.setAllowedHeaders(allowedHeaders);
        configuration.setExposedHeaders(exposedHeaders);
        configuration.setAllowCredentials(allowCredentials);
        configuration.setMaxAge(maxAge);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.setAlwaysUseFullPath(true); //Don't chop off the starting /rest stuff
        source.registerCorsConfiguration("/rest/**", configuration);

        return source;
    }

    @Bean
    public HttpFirewall allowUrlEncodedSlashHttpFirewall() {
        DefaultHttpFirewall firewall = new DefaultHttpFirewall();
        firewall.setAllowUrlEncodedSlash(true);
        return firewall;
    }

    @Bean("ipRateLimiter")
    public RateLimiter<String> ipRateLimiter(
            @Value("${rateLimit.rest.anonymous.enabled:true}") boolean enabled,
            @Value("${rateLimit.rest.anonymous.burstQuantity:10}") long burstQuantity,
            @Value("${rateLimit.rest.anonymous.quanitity:2}") long quanitity,
            @Value("${rateLimit.rest.anonymous.period:1}") long period,
            @Value("${rateLimit.rest.anonymous.periodUnit:SECONDS}") TimeUnit periodUnit) {

        return !enabled ? null : new RateLimiter<>(burstQuantity, quanitity, period, periodUnit);
    }

    @Bean("userRateLimiter")
    public RateLimiter<Integer> userRateLimiter(
            @Value("${rateLimit.rest.user.enabled:false}") boolean enabled,
            @Value("${rateLimit.rest.user.burstQuantity:20}") long burstQuantity,
            @Value("${rateLimit.rest.user.quanitity:10}") long quanitity,
            @Value("${rateLimit.rest.user.period:1}") long period,
            @Value("${rateLimit.rest.user.periodUnit:SECONDS}") TimeUnit periodUnit) {

        return !enabled ? null : new RateLimiter<>(burstQuantity, quanitity, period, periodUnit);
    }

    @Autowired HttpFirewall httpFirewall;
    @Autowired AccessDeniedHandler accessDeniedHandler;
    @Autowired AuthenticationEntryPoint authenticationEntryPoint;
    @Autowired Optional<CorsConfigurationSource> corsConfigurationSource;
    @Autowired AuthenticationSuccessHandler authenticationSuccessHandler;
    @Autowired AuthenticationFailureHandler authenticationFailureHandler;
    @Autowired LogoutHandler logoutHandler;
    @Autowired LogoutSuccessHandler logoutSuccessHandler;
    @Autowired RequestCache requestCache;
    @Autowired CsrfTokenRepository csrfTokenRepository;
    @Autowired SwitchUserFilter switchUserFilter;
    @Autowired PermissionExceptionFilter permissionExceptionFilter;
    @Autowired SessionRegistry sessionRegistry;
    @Autowired SessionInformationExpiredStrategy sessionInformationExpiredStrategy;
    @Autowired JsonLoginConfigurer jsonLoginConfigurer;
    @Autowired @Qualifier("ipRateLimiter") Optional<RateLimiter<String>> ipRateLimiter;
    @Autowired @Qualifier("userRateLimiter") Optional<RateLimiter<Integer>> userRateLimiter;
    @Autowired Environment env;

    @Autowired @Qualifier("browserHtmlRequestMatcher") RequestMatcher browserHtmlRequestMatcher;
    @Autowired MangoRequiresCsrfMatcher mangoRequiresCsrfMatcher;
    @Autowired TokenAuthMatcher tokenAuthMatcher;
    @Autowired AuthHeaderMatcher authHeaderMatcher;

    RequestMatcher restRequestMatcher = new AntPathRequestMatcher("/rest/**");
    RequestMatcher notRestRequestMatcher = new NegatedRequestMatcher(restRequestMatcher);
    RequestMatcher proxiedRestRequestMatcher = new AntPathRequestMatcher("/cloud-connect-proxy/rest/**");

    RequestMatcher legacyUiMatcher = new OrRequestMatcher(
            new AntPathRequestMatcher("/*.htm"),
            new AntPathRequestMatcher("/**/*.shtm"),
            new AntPathRequestMatcher("/**/*.jsp"),
            new AntPathRequestMatcher("/swagger/**"));
    RequestMatcher legacyCspMatcher = new AndRequestMatcher(notRestRequestMatcher, legacyUiMatcher);
    RequestMatcher standardCspMatcher = new AndRequestMatcher(notRestRequestMatcher, new NegatedRequestMatcher(legacyUiMatcher));

    @Value("${rateLimit.rest.anonymous.honorXForwardedFor:false}") boolean honorXForwardedFor;

    @Value("${authentication.token.enabled:true}") boolean tokenAuthEnabled;
    @Value("${authentication.basic.enabled:true}") boolean basicAuthenticationEnabled;
    @Value("${authentication.basic.realm:Mango}") String basicAuthenticationRealm;
    @Value("${authentication.session.maxSessions:10}") int maxSessions;

    @Value("${ssl.on:false}") boolean sslOn;
    @Value("${ssl.hsts.enabled:true}") boolean sslHstsEnabled;
    @Value("${ssl.hsts.maxAge:31536000}") long sslHstsMaxAge;
    @Value("${ssl.hsts.includeSubDomains:false}") boolean sslHstsIncludeSubDomains;

    @Value("${web.security.iFrameAccess:SAMEORIGIN}") String iFrameAccess;

    @Value("${web.security.contentSecurityPolicy.enabled:false}") boolean cspEnabled;
    @Value("${web.security.contentSecurityPolicy.reportOnly:false}") boolean cspReportOnly;
    @Value("${web.security.contentSecurityPolicy.other:}") String cspOther;

    @Value("${web.security.contentSecurityPolicy.legacyUi.enabled:false}") boolean legacyCspEnabled;
    @Value("${web.security.contentSecurityPolicy.legacyUi.reportOnly:false}") boolean legacyCspReportOnly;
    @Value("${web.security.contentSecurityPolicy.legacyUi.other:}") String legacyCspOther;

    @Value("${swagger.apidocs.protected:true}") boolean swaggerApiDocsProtected;
    @Value("${springfox.documentation.swagger.v2.path}") String swagger2Endpoint;

    @Value("${web.port:8080}") int webPort;
    @Value("${ssl.port:8443}") int sslPort;

    final static String[] SRC_TYPES = new String[] {"default", "script", "style", "connect", "img", "font", "media", "object", "frame", "worker", "manifest"};

    @Configuration
    @Order(1)
    public class ProxySecurityConfiguration extends WebSecurityConfigurerAdapter {

        @Override
        public void configure(WebSecurity web) throws Exception {
            web.httpFirewall(httpFirewall);
        }

        @Override
        protected void configure(HttpSecurity http) throws Exception {
            http.requestMatcher(request -> request.getAttribute(IS_PROXY_REQUEST_ATTRIBUTE) != null);

            http.sessionManagement()
            // dont actually want an invalid session strategy, just treat them as having no session
            //.invalidSessionStrategy(invalidSessionStrategy)
            .maximumSessions(maxSessions)
            .maxSessionsPreventsLogin(false)
            .sessionRegistry(sessionRegistry)
            .expiredSessionStrategy(sessionInformationExpiredStrategy)
            .and()
            .sessionCreationPolicy(SessionCreationPolicy.NEVER)
            .sessionFixation()
            .newSession();

            http.authorizeRequests().anyRequest().permitAll();

            http.csrf()
            .requireCsrfProtectionMatcher(mangoRequiresCsrfMatcher)
            .csrfTokenRepository(csrfTokenRepository);

            http.rememberMe().disable();
            http.logout().disable();
            http.formLogin().disable();
            http.requestCache().disable();

            // TODO
            //http.exceptionHandling()
            //.authenticationEntryPoint(authenticationEntryPoint)
            //.accessDeniedHandler(accessDeniedHandler);

            http.addFilterAfter(permissionExceptionFilter, ExceptionTranslationFilter.class);

            // can we enable token and basic auth for this proxy?
            http.httpBasic().disable();

            if (ipRateLimiter.isPresent() || userRateLimiter.isPresent()) {
                http.addFilterAfter(new RateLimitingFilter(proxiedRestRequestMatcher, ipRateLimiter.orElse(null), userRateLimiter.orElse(null), honorXForwardedFor), ExceptionTranslationFilter.class);
            }

            if (sslOn && sslHstsEnabled) {
                http.headers().defaultsDisabled();
                configureHSTS(http);
            } else {
                http.headers().disable();
            }

            http.cors().disable();
        }
    }

    // Configure a separate WebSecurityConfigurerAdapter for REST requests which have an Authorization header.
    // We use a stateless session creation policy and disable CSRF for these requests so that the Authentication is not
    // persisted in the session inside the SecurityContext. This security configuration will be used for JWT token authentication
    // and also basic authentication.
    @Configuration
    @Order(2)
    public class StatelessSecurityConfiguration extends WebSecurityConfigurerAdapter {

        @Override
        public void configure(WebSecurity web) throws Exception {
            web.httpFirewall(httpFirewall);
        }

        @Override
        protected void configure(HttpSecurity http) throws Exception {
            http.requestMatcher(authHeaderMatcher);

            // problem with using STATELESS CsrfAuthenticationStrategy changes the XSRF-TOKEN on every request
            // as it believes that we are newly authenticated on every request
            // disabled the whole session management filter
            http.sessionManagement().disable();
            //.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            //.sessionFixation().none();

            // stops authentication being stored in the session
            http.securityContext().disable();
            // .securityContextRepository(new NullSecurityContextRepository())

            ExpressionUrlAuthorizationConfigurer<HttpSecurity>.ExpressionInterceptUrlRegistry authRequests = http.authorizeRequests();
            authRequests.antMatchers(HttpMethod.OPTIONS).permitAll()
            .antMatchers("/rest/*/login/**").denyAll()
            .antMatchers("/rest/*/logout/**").denyAll()
            .antMatchers(HttpMethod.POST, "/rest/*/login/su").denyAll()
            .antMatchers(HttpMethod.POST, "/rest/*/login/exit-su").denyAll()
            .antMatchers(HttpMethod.GET, "/rest/*/translations/public/**").permitAll() //For public translations
            .antMatchers(HttpMethod.GET, "/rest/*/json-data/public/**").permitAll() //For public json-data
            .antMatchers(HttpMethod.GET, "/rest/*/modules/angularjs-modules/public/**").permitAll() //For public angularjs modules
            .antMatchers(HttpMethod.GET, "/rest/*/file-stores/public/**").permitAll() //For public file store
            .antMatchers("/rest/*/password-reset/**").permitAll() // password reset must be public
            .antMatchers("/rest/*/auth-tokens/**").permitAll() // should be able to get public key and verify tokens
            .antMatchers("/rest/*/ui-bootstrap/**").permitAll() // UI bootstrap has a public method
            .antMatchers("/rest/*/users/registration/public/**").permitAll(); //For public user registration

            if (swaggerApiDocsProtected) {
                authRequests.antMatchers(swagger2Endpoint).authenticated(); //protected swagger api-docs
            }else {
                //Add exceptions for the REST swagger endpoints
                authRequests.antMatchers("/rest/*" + swagger2Endpoint).permitAll();
            }

            authRequests.requestMatchers(restRequestMatcher).authenticated()
            .antMatchers(HttpMethod.GET, "/modules/*/web/**").permitAll() // dont allow access to any modules folders other than web
            .antMatchers("/modules/**").denyAll()
            .antMatchers("/**/*.shtm").authenticated() // access to *.shtm files must be authenticated
            .antMatchers("/protected/**").authenticated(); // protected folder requires authentication

            if (swaggerApiDocsProtected) {
                authRequests.antMatchers(swagger2Endpoint).authenticated(); //protected swagger api-docs
            }

            authRequests.anyRequest().permitAll(); // default to permit all

            http.csrf().disable();
            // After testing, we found that only the Spring Form POST submission is rendered un-usable without CSRF being enabled.  It
            //  was decided that these pages will be re-written in the new UI and we will leave the leagcy pages broken for the CloudConnect proxy.
            // Since the JSP pages behind a proxy do not get the CSRF token from
            // the cookie set as a request parameter and therefore the hidden inputs are not filled out on JSP pages.
            // if we enable CSRF but just say no pages require protection it solves the issue of getting the cookie on the page but it causes the
            // submission to hang because the extraction of either URL or Body parameters reads the input stream of the body.  The only other way to do this
            // is to use Javascript to submit the forms with the X-XSRF-TOKEN header.
            //            http.csrf()
            //            .requireCsrfProtectionMatcher(request -> false)
            //            .csrfTokenRepository(csrfTokenRepository);

            http.rememberMe().disable();
            http.logout().disable();
            http.formLogin().disable();
            http.requestCache().disable();

            http.exceptionHandling()
            .authenticationEntryPoint(authenticationEntryPoint)
            .accessDeniedHandler(accessDeniedHandler);

            http.addFilterAfter(permissionExceptionFilter, ExceptionTranslationFilter.class);

            if (basicAuthenticationEnabled) {
                http.httpBasic()
                .realmName(basicAuthenticationRealm)
                .authenticationEntryPoint(authenticationEntryPoint);
            } else {
                http.httpBasic().disable();
            }

            if (tokenAuthEnabled) {
                http.addFilterBefore(new BearerAuthenticationFilter(authenticationManagerBean(), authenticationEntryPoint), BasicAuthenticationFilter.class);
            }

            if (ipRateLimiter.isPresent() || userRateLimiter.isPresent()) {
                http.addFilterAfter(new RateLimitingFilter(restRequestMatcher, ipRateLimiter.orElse(null), userRateLimiter.orElse(null), honorXForwardedFor), ExceptionTranslationFilter.class);
            }

            //Configure the headers
            configureHeaders(http);
            configureHSTS(http);

            // Use the MVC Cors Configuration
            if (corsConfigurationSource.isPresent()) {
                http.cors().configurationSource(corsConfigurationSource.get());
            }
        }
    }

    @Configuration
    @Order(3)
    public class SessionSecurityConfiguration extends WebSecurityConfigurerAdapter {

        @Override
        public void configure(WebSecurity web) throws Exception {
            web.httpFirewall(httpFirewall);
        }

        @Override
        protected void configure(HttpSecurity http) throws Exception {
            http.sessionManagement()
            // dont actually want an invalid session strategy, just treat them as having no session
            //.invalidSessionStrategy(invalidSessionStrategy)
            .maximumSessions(maxSessions)
            .maxSessionsPreventsLogin(false)
            .sessionRegistry(sessionRegistry)
            .expiredSessionStrategy(sessionInformationExpiredStrategy)
            .and()
            .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
            .sessionFixation()
            .newSession();

            ExpressionUrlAuthorizationConfigurer<HttpSecurity>.ExpressionInterceptUrlRegistry authRequests = http.authorizeRequests();
            authRequests.antMatchers(HttpMethod.OPTIONS).permitAll()
            .antMatchers("/rest/*/login").permitAll()
            .antMatchers("/rest/*/exception/**").permitAll() //For exception info for a user's session...
            .antMatchers(HttpMethod.POST, "/rest/*/login/su").hasRole("ADMIN")
            .antMatchers(HttpMethod.GET, "/rest/*/translations/public/**").permitAll() //For public translations
            .antMatchers(HttpMethod.GET, "/rest/*/json-data/public/**").permitAll() //For public json-data
            .antMatchers(HttpMethod.GET, "/rest/*/modules/angularjs-modules/public/**").permitAll() //For public angularjs modules
            .antMatchers(HttpMethod.GET, "/rest/*/file-stores/public/**").permitAll() //For public file store
            .antMatchers("/rest/*/password-reset/**").permitAll() // password reset must be public
            .antMatchers("/rest/*/auth-tokens/**").permitAll() // should be able to get public key and verify tokens
            .antMatchers("/rest/*/ui-bootstrap/**").permitAll() // UI bootstrap has a public method
            .antMatchers("/rest/*/users/registration/public/**").permitAll(); //For public user registration

            if (swaggerApiDocsProtected) {
                authRequests.antMatchers(swagger2Endpoint).authenticated(); //protected swagger api-docs
            }else {
                //Add exceptions for the REST swagger endpoints
                authRequests.antMatchers("/rest/*" + swagger2Endpoint).permitAll();
            }

            authRequests.requestMatchers(restRequestMatcher).authenticated()
            .antMatchers(HttpMethod.GET, "/modules/*/web/**").permitAll() // dont allow access to any modules folders other than web
            .antMatchers("/modules/**").denyAll()
            .antMatchers("/**/*.shtm").authenticated() // access to *.shtm files must be authenticated
            .antMatchers("/protected/**").authenticated(); // protected folder requires authentication

            authRequests.anyRequest().permitAll(); // default to permit all

            http.csrf()
            .requireCsrfProtectionMatcher(mangoRequiresCsrfMatcher)
            .csrfTokenRepository(csrfTokenRepository);

            http.rememberMe().disable();
            http.requestCache().disable();

            http.apply(jsonLoginConfigurer)
            .successHandler(authenticationSuccessHandler)
            .failureHandler(authenticationFailureHandler);

            http.formLogin()
            // setting this prevents FormLoginConfigurer from adding the login page generating filter
            // this adds an authentication entry point but it wont be used as we have already specified one below in exceptionHandling()
            .loginPage("/login-xyz.htm")
            .loginProcessingUrl("/login")
            .successHandler(authenticationSuccessHandler)
            .failureHandler(authenticationFailureHandler)
            .permitAll();

            http.logout()
            .logoutRequestMatcher(new OrRequestMatcher(new AntPathRequestMatcher("/rest/*/logout", "POST"), new AntPathRequestMatcher("/logout", "POST")))
            .addLogoutHandler(logoutHandler)
            .invalidateHttpSession(true)
            // XSRF token is deleted but its own logout handler, session cookie doesn't really need to be deleted as its invalidated
            // but why not for the sake of cleanliness
            .deleteCookies(Common.getCookieName())
            .logoutSuccessHandler(logoutSuccessHandler);

            http.exceptionHandling()
            .authenticationEntryPoint(authenticationEntryPoint)
            .accessDeniedHandler(accessDeniedHandler);

            http.addFilterAfter(switchUserFilter, FilterSecurityInterceptor.class);
            http.addFilterAfter(permissionExceptionFilter, ExceptionTranslationFilter.class);

            if (ipRateLimiter.isPresent() || userRateLimiter.isPresent()) {
                http.addFilterAfter(new RateLimitingFilter(restRequestMatcher, ipRateLimiter.orElse(null), userRateLimiter.orElse(null), honorXForwardedFor), ExceptionTranslationFilter.class);
            }

            //Configure headers
            configureHeaders(http);
            configureHSTS(http);

            // Use the MVC Cors Configuration
            if (corsConfigurationSource.isPresent()) {
                http.cors().configurationSource(corsConfigurationSource.get());
            }
        }
    }

    private void configureHSTS(HttpSecurity http) throws Exception {
        HeadersConfigurer<HttpSecurity>.HstsConfig hsts = http.headers().httpStrictTransportSecurity();

        // If using SSL then enable the hsts and secure forwarding
        if (sslOn && sslHstsEnabled) {
            // only enable "requiresSecure" for browser requests (not for XHR/REST requests)
            // this options sets the REQUIRES_SECURE_CHANNEL attribute and causes ChannelProcessingFilter
            // to perform a 302 redirect to https://
            http.portMapper().http(webPort).mapsTo(sslPort);
            http.requiresChannel().requestMatchers(browserHtmlRequestMatcher).requiresSecure();
            hsts.maxAgeInSeconds(sslHstsMaxAge).includeSubDomains(sslHstsIncludeSubDomains);
        } else {
            hsts.disable();
        }
    }

    /**
     * Ensure the headers are properly configured
     *
     * @param http
     * @throws Exception
     */
    private void configureHeaders(HttpSecurity http) throws Exception {
        HeadersConfigurer<HttpSecurity> headers = http.headers();
        headers.cacheControl().disable();

        if (StringUtils.equals(iFrameAccess, "SAMEORIGIN")) {
            headers.frameOptions().sameOrigin();
        } else if (StringUtils.equals(iFrameAccess, "DENY")) {
            headers.frameOptions().deny();
        } else if (StringUtils.equals(iFrameAccess, "ANY")) {
            headers.frameOptions().disable();
        } else {
            // TODO Ensure these are valid Domains?
            XFrameOptionsHeaderWriter headerWriter = new XFrameOptionsHeaderWriter(request -> iFrameAccess);
            headers.addHeaderWriter(headerWriter).frameOptions().disable();
        }

        if (cspEnabled || legacyCspEnabled) {
            List<String> policies = new ArrayList<>();
            List<String> legacyPolicies = new ArrayList<>();

            for (String srcType : SRC_TYPES) {
                String policy = env.getProperty("web.security.contentSecurityPolicy." + srcType + "Src");
                if (policy !=null && !policy.isEmpty()) {
                    policies.add(srcType + "-src " + policy);
                }

                String legacyPolicy = env.getProperty("web.security.contentSecurityPolicy.legacyUi." + srcType + "Src");
                if (legacyPolicy != null && !legacyPolicy.isEmpty()) {
                    legacyPolicies.add(srcType + "-src " + legacyPolicy);
                }
            }

            if (cspOther != null && !cspOther.isEmpty()) {
                policies.add(cspOther);
            }
            if (legacyCspOther != null && !legacyCspOther.isEmpty()) {
                legacyPolicies.add(legacyCspOther);
            }

            if (cspEnabled && !policies.isEmpty()) {
                headers.addHeaderWriter(new MangoCSPHeaderWriter(cspReportOnly, policies, standardCspMatcher));
            }
            if (legacyCspEnabled && !legacyPolicies.isEmpty()) {
                headers.addHeaderWriter(new MangoCSPHeaderWriter(legacyCspReportOnly, legacyPolicies, legacyCspMatcher));
            }
        }
    }
}