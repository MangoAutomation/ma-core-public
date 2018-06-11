/**
 * Copyright (C) 2017 Infinite Automation Software. All rights reserved.
 */
package com.serotonin.m2m2.web.mvc.spring.security;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AccountStatusUserDetailsChecker;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.BeanIds;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.userdetails.UserDetailsChecker;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.access.ExceptionTranslationFilter;
import org.springframework.security.web.access.intercept.FilterSecurityInterceptor;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.security.web.authentication.switchuser.SwitchUserFilter;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.firewall.DefaultHttpFirewall;
import org.springframework.security.web.firewall.HttpFirewall;
import org.springframework.security.web.header.writers.frameoptions.XFrameOptionsHeaderWriter;
import org.springframework.security.web.savedrequest.NullRequestCache;
import org.springframework.security.web.savedrequest.RequestCache;
import org.springframework.security.web.session.SessionInformationExpiredStrategy;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.NegatedRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.accept.ContentNegotiationStrategy;
import org.springframework.web.accept.HeaderContentNegotiationStrategy;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.module.AuthenticationDefinition;
import com.serotonin.m2m2.module.ModuleRegistry;
import com.serotonin.m2m2.web.mvc.spring.MangoRestSpringConfiguration;
import com.serotonin.m2m2.web.mvc.spring.security.authentication.MangoPasswordAuthenticationProvider;
import com.serotonin.m2m2.web.mvc.spring.security.authentication.MangoTokenAuthenticationProvider;
import com.serotonin.m2m2.web.mvc.spring.security.authentication.MangoUserDetailsService;

/**
 * @author Jared Wiltshire
 */
@Configuration
@EnableWebSecurity
@ComponentScan(basePackages = {"com.serotonin.m2m2.web.mvc.spring.security"})
public class MangoSecurityConfiguration {

    @Autowired
    private ConfigurableListableBeanFactory beanFactory;

    @Autowired
    public void configureAuthenticationManager(AuthenticationManagerBuilder auth,
            MangoUserDetailsService userDetails,
            MangoPasswordAuthenticationProvider passwordAuthenticationProvider,
            MangoTokenAuthenticationProvider tokenAuthProvider,
            @Value("${authentication.token.enabled:true}") boolean tokenAuthEnabled
            ) throws Exception {

        auth.userDetailsService(userDetails);

        for (AuthenticationDefinition def : ModuleRegistry.getDefinitions(AuthenticationDefinition.class)) {
            auth.authenticationProvider(def.authenticationProvider());
        }

        auth.authenticationProvider(passwordAuthenticationProvider);

        if (tokenAuthEnabled) {
            auth.authenticationProvider(tokenAuthProvider);
        }
    }

    @Bean
    public UserDetailsChecker userDetailsChecker() {
        return new AccountStatusUserDetailsChecker();
    }

    @Bean
    public UserDetailsService userDetailsService(MangoUserDetailsService userDetails) {
        return userDetails;
    }

    @Bean
    public AuthenticationEntryPoint authenticationEntryPoint(MangoAuthenticationEntryPoint authenticationEntryPoint) {
        return authenticationEntryPoint;
    }

    @Bean
    public AuthenticationSuccessHandler mangoAuthenticationSuccessHandler() {
        return new MangoAuthenticationSuccessHandler(requestCache(), browserHtmlRequestMatcher());
    }

    @Bean
    public AuthenticationFailureHandler authenticationFailureHandler(MangoAuthenticationFailureHandler authenticationFailureHandler) {
        return authenticationFailureHandler;
    }

    @Bean
    public LogoutHandler logoutHandler(MangoLogoutHandler logoutHandler) {
        return logoutHandler;
    }

    @Bean
    public LogoutSuccessHandler logoutSuccessHandler(MangoLogoutSuccessHandler logoutSuccessHandler) {
        return logoutSuccessHandler;
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
    // TODO Mango 3.5 remove static, fix UI module
    @Bean(name="browserHtmlRequestMatcher")
    public static RequestMatcher browserHtmlRequestMatcher() {
        return BrowserRequestMatcher.INSTANCE;
    }

    @Bean
    public SessionInformationExpiredStrategy sessionInformationExpiredStrategy(@Qualifier("browserHtmlRequestMatcher") RequestMatcher matcher) {
        return new MangoExpiredSessionStrategy(matcher);
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource(
            @Value("${rest.cors.enabled:false}") boolean enabled,
            @Value("${rest.cors.allowedOrigins:}") List<String> allowedOrigins,
            @Value("${rest.cors.allowedMethods:PUT,POST,GET,OPTIONS,DELETE}") List<String> allowedMethods,
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
    public PermissionExceptionFilter permissionExceptionFilter(){
        return new PermissionExceptionFilter();
    }

    @Primary
    @Bean("restObjectMapper")
    public ObjectMapper objectMapper() {
        return MangoRestSpringConfiguration.getObjectMapper();
    }

    @Bean
    public SessionRegistry sessionRegistry() {
        return beanFactory.createBean(MangoSessionRegistry.class);
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
    @Autowired @Qualifier("restAccessDeniedHandler") AccessDeniedHandler restAccessDeniedHandler;
    @Autowired AuthenticationEntryPoint authenticationEntryPoint;
    @Autowired CorsConfigurationSource corsConfigurationSource;
    @Autowired AuthenticationSuccessHandler authenticationSuccessHandler;
    @Autowired AuthenticationFailureHandler authenticationFailureHandler;
    @Autowired LogoutHandler logoutHandler;
    @Autowired LogoutSuccessHandler logoutSuccessHandler;
    @Autowired RequestCache requestCache;
    @Autowired PermissionExceptionFilter permissionExceptionFilter;
    @Autowired SessionRegistry sessionRegistry;
    @Autowired SessionInformationExpiredStrategy sessionInformationExpiredStrategy;
    @Autowired JsonLoginConfigurer jsonLoginConfigurer;
    @Autowired @Qualifier("ipRateLimiter") RateLimiter<String> ipRateLimiter;
    @Autowired @Qualifier("userRateLimiter") RateLimiter<Integer> userRateLimiter;
    @Autowired Environment env;

    @Value("${authentication.token.enabled:true}") boolean tokenAuthEnabled;
    @Value("${authentication.basic.enabled:true}") boolean basicAuthenticationEnabled;
    @Value("${authentication.basic.realm:Mango}") String basicAuthenticationRealm;
    @Value("${rest.cors.enabled:false}") boolean corsEnabled;
    @Value("${authentication.session.maxSessions:10}") int maxSessions;

    @Value("${ssl.on:false}") boolean sslOn;
    @Value("${ssl.hsts.enabled:true}") boolean sslHstsEnabled;
    @Value("${ssl.hsts.maxAge:31536000}") long sslHstsMaxAge;
    @Value("${ssl.hsts.includeSubDomains:false}") boolean sslHstsIncludeSubDomains;

    @Value("${web.security.iFrameAccess:SAMEORIGIN}") String iFrameAccess;

    @Value("${web.security.contentSecurityPolicy.enabled:true}") boolean cspEnabled;
    @Value("${web.security.contentSecurityPolicy.reportOnly:false}") boolean cspReportOnly;
    @Value("${web.security.contentSecurityPolicy.other:}") String cspOther;

    @Value("${web.security.contentSecurityPolicy.legacyUi.enabled:true}") boolean legacyCspEnabled;
    @Value("${web.security.contentSecurityPolicy.legacyUi.reportOnly:false}") boolean legacyCspReportOnly;
    @Value("${web.security.contentSecurityPolicy.legacyUi.other:}") String legacyCspOther;

    final static String[] SRC_TYPES = new String[] {"default", "script", "style", "connect", "img", "font", "media", "object", "frame", "worker", "manifest"};

    // Configure a separate WebSecurityConfigurerAdapter for REST requests which have an Authorization header.
    // We use a stateless session creation policy and disable CSRF for these requests so that the Authentication is not
    // persisted in the session inside the SecurityContext. This security configuration allows the JWT token authentication
    // and also basic authentication.
    @Configuration
    @Order(1)
    public class TokenAuthenticatedRestSecurityConfiguration extends WebSecurityConfigurerAdapter {

        @Bean(name=BeanIds.AUTHENTICATION_MANAGER)
        @Override
        public AuthenticationManager authenticationManagerBean() throws Exception {
            return super.authenticationManagerBean();
        }

        @Override
        public void configure(WebSecurity web) throws Exception {
            web.httpFirewall(httpFirewall);
        }

        @Override
        protected void configure(HttpSecurity http) throws Exception {

            http.requestMatcher(new RequestMatcher() {
                AntPathRequestMatcher pathMatcher = new AntPathRequestMatcher("/rest/**");
                @Override
                public boolean matches(HttpServletRequest request) {
                    String header = request.getHeader("Authorization");
                    return header != null && pathMatcher.matches(request);
                }
            });

            // stops the SessionManagementConfigurer from using a HttpSessionSecurityContextRepository to
            // store the SecurityContext, instead it creates a NullSecurityContextRepository which does
            // result in session creation
            http.sessionManagement()
            .sessionCreationPolicy(SessionCreationPolicy.STATELESS);

            http.authorizeRequests()
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
            .antMatchers(HttpMethod.OPTIONS).permitAll()
            .anyRequest().authenticated();

            // do not need CSRF protection when we are using a JWT token
            http.csrf().disable()
            .rememberMe().disable()
            .logout().disable()
            .formLogin().disable()
            .requestCache().disable();

            if (basicAuthenticationEnabled) {
                http.httpBasic()
                .realmName(basicAuthenticationRealm)
                .authenticationEntryPoint(authenticationEntryPoint);
            } else {
                http.httpBasic().disable();
            }

            http.exceptionHandling()
            .authenticationEntryPoint(authenticationEntryPoint)
            .accessDeniedHandler(restAccessDeniedHandler);

            if (tokenAuthEnabled) {
                http.addFilterBefore(new BearerAuthenticationFilter(authenticationManagerBean(), authenticationEntryPoint), BasicAuthenticationFilter.class);
            }

            if (ipRateLimiter != null || userRateLimiter != null) {
                http.addFilterAfter(new RateLimitingFilter(ipRateLimiter, userRateLimiter), ExceptionTranslationFilter.class);
            }

            //Configure the headers
            configureHeaders(http, true);
            configureHSTS(http, false);

            // Use the MVC Cors Configuration
            if (corsEnabled) {
                http.cors().configurationSource(corsConfigurationSource);
            }
        }
    }

    @Configuration
    @Order(2)
    public class RestSecurityConfiguration extends WebSecurityConfigurerAdapter {

        @Override
        public void configure(WebSecurity web) throws Exception {
            web.httpFirewall(httpFirewall);
        }

        @Override
        protected void configure(HttpSecurity http) throws Exception {
            http.antMatcher("/rest/**");

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

            http.authorizeRequests()
            .antMatchers("/rest/*/login").permitAll()
            .antMatchers("/rest/*/exception/**").permitAll() //For exception info for a user's session...
            .antMatchers(HttpMethod.POST, "/rest/*/login/su").hasRole("ADMIN")
            .antMatchers(HttpMethod.GET, "/rest/*/translations/public/**").permitAll() //For public translations
            .antMatchers(HttpMethod.GET, "/rest/*/json-data/public/**").permitAll() //For public json-data
            .antMatchers(HttpMethod.GET, "/rest/*/modules/angularjs-modules/public/**").permitAll() //For public angularjs modules
            .antMatchers(HttpMethod.GET, "/rest/*/file-stores/public/**").permitAll() //For public file store
            .antMatchers("/rest/*/password-reset/**").permitAll() // password reset must be public
            .antMatchers("/rest/*/auth-tokens/**").permitAll() // should be able to get public key and verify tokens
            .antMatchers(HttpMethod.OPTIONS).permitAll()
            .anyRequest().authenticated();

            http.apply(jsonLoginConfigurer)
            .successHandler(authenticationSuccessHandler)
            .failureHandler(authenticationFailureHandler);

            http.logout()
            .logoutRequestMatcher(new AntPathRequestMatcher("/rest/*/logout", "POST"))
            .addLogoutHandler(logoutHandler)
            .invalidateHttpSession(true)
            // XSRF token is deleted but its own logout handler, session cookie doesn't really need to be deleted as its invalidated
            // but why not for the sake of cleanliness
            .deleteCookies(Common.getCookieName())
            .logoutSuccessHandler(logoutSuccessHandler);

            http.csrf()
            .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse());

            http.rememberMe().disable()
            .formLogin().disable()
            .requestCache().disable();

            http.exceptionHandling()
            .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
            .accessDeniedHandler(restAccessDeniedHandler);

            http.addFilterAfter(switchUserFilter(), FilterSecurityInterceptor.class);
            http.addFilterAfter(permissionExceptionFilter, ExceptionTranslationFilter.class);

            if (ipRateLimiter != null || userRateLimiter != null) {
                http.addFilterAfter(new RateLimitingFilter(ipRateLimiter, userRateLimiter), ExceptionTranslationFilter.class);
            }

            //Configure headers
            configureHeaders(http, true);
            configureHSTS(http, false);

            // Use the MVC Cors Configuration
            if (corsEnabled) {
                http.cors().configurationSource(corsConfigurationSource);
            }
        }

        @Bean
        public SwitchUserFilter switchUserFilter() {
            SwitchUserFilter filter = new MangoSwitchUserFilter();
            filter.setUserDetailsService(userDetailsService());
            filter.setSuccessHandler(authenticationSuccessHandler);
            filter.setUsernameParameter("username");
            return filter;
        }
    }

    @Configuration
    @Order(3)
    public class DefaultSecurityConfiguration extends WebSecurityConfigurerAdapter {

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

            http.formLogin()
            // setting this prevents FormLoginConfigurer from adding the login page generating filter
            // this adds an authentication entry point but it wont be used as we have already specified one below in exceptionHandling()
            .loginPage("/login-xyz.htm")
            .loginProcessingUrl("/login")
            .successHandler(authenticationSuccessHandler)
            .failureHandler(authenticationFailureHandler)
            .permitAll();

            http.logout()
            .logoutUrl("/logout")
            .addLogoutHandler(logoutHandler)
            .invalidateHttpSession(true)
            // XSRF token is deleted but its own logout handler, session cookie doesn't really need to be deleted as its invalidated
            // but why not for the sake of cleanliness
            .deleteCookies(Common.getCookieName())
            .logoutSuccessHandler(logoutSuccessHandler);

            http.rememberMe().disable();

            http.authorizeRequests()
            // dont allow access to any modules folders other than web
            .antMatchers(HttpMethod.GET, "/modules/*/web/**").permitAll()
            .antMatchers("/modules/**").denyAll()
            // Access to *.shtm files must be authenticated
            .antMatchers("/**/*.shtm").authenticated()
            //Access to protected folder
            .antMatchers("/protected/**").authenticated()
            // Default to permit all
            .anyRequest().permitAll();

            http.csrf()
            // DWR handles its own CRSF protection (It is set to look at the same cookie in Lifecyle)
            .ignoringAntMatchers("/dwr/**", "/httpds")
            .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse());

            http.requestCache().requestCache(requestCache);

            http.exceptionHandling()
            .authenticationEntryPoint(authenticationEntryPoint)
            .accessDeniedHandler(accessDeniedHandler);

            http.addFilterAfter(permissionExceptionFilter, ExceptionTranslationFilter.class);

            //Customize the headers here
            configureHeaders(http, false);
            configureHSTS(http, true);
        }
    }

    private void configureHSTS(HttpSecurity http, boolean requiresSecure) throws Exception {
        HeadersConfigurer<HttpSecurity>.HstsConfig hsts = http.headers().httpStrictTransportSecurity();

        // If using SSL then enable the hsts and secure forwarding
        if (sslOn && sslHstsEnabled) {
            // dont enable "requiresSecure" for REST calls
            // this options sets the REQUIRES_SECURE_CHANNEL attribute and causes ChannelProcessingFilter
            // to perform a 302 redirect to https://
            if (requiresSecure) {
                http.requiresChannel().anyRequest().requiresSecure();
            }
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
    private void configureHeaders(HttpSecurity http, boolean isRest) throws Exception {
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

        if (!isRest && (cspEnabled || legacyCspEnabled)) {
            RequestMatcher legacyUiMatcher = new OrRequestMatcher(new AntPathRequestMatcher("/*.htm"), new AntPathRequestMatcher("/**/*.shtm"));
            RequestMatcher otherMatcher = new NegatedRequestMatcher(legacyUiMatcher);

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
                headers.addHeaderWriter(new MangoCSPHeaderWriter(cspReportOnly, policies, otherMatcher));
            }
            if (legacyCspEnabled && !legacyPolicies.isEmpty()) {
                headers.addHeaderWriter(new MangoCSPHeaderWriter(legacyCspReportOnly, legacyPolicies, legacyUiMatcher));
            }
        }
    }
}