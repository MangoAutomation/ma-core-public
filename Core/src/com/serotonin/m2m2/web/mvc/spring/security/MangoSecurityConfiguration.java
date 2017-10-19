/**
 * Copyright (C) 2017 Infinite Automation Software. All rights reserved.
 */
package com.serotonin.m2m2.web.mvc.spring.security;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.BeanIds;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
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
import org.springframework.security.web.header.writers.frameoptions.AllowFromStrategy;
import org.springframework.security.web.header.writers.frameoptions.XFrameOptionsHeaderWriter;
import org.springframework.security.web.savedrequest.NullRequestCache;
import org.springframework.security.web.savedrequest.RequestCache;
import org.springframework.security.web.util.matcher.AndRequestMatcher;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.MediaTypeRequestMatcher;
import org.springframework.security.web.util.matcher.NegatedRequestMatcher;
import org.springframework.security.web.util.matcher.RequestHeaderRequestMatcher;
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
import com.serotonin.m2m2.vo.User;
import com.serotonin.m2m2.web.mvc.spring.MangoRestSpringConfiguration;
import com.serotonin.m2m2.web.mvc.spring.security.authentication.MangoPasswordAuthenticationProvider;
import com.serotonin.m2m2.web.mvc.spring.security.authentication.MangoUserDetailsService;

/**
 * @author Jared Wiltshire
 */
@Configuration
@EnableWebSecurity
@ComponentScan(basePackages = {"com.serotonin.m2m2.web.mvc.spring.security"})
public class MangoSecurityConfiguration {

	//Share between all Configurations
	final static SessionRegistry sessionRegistry = new MangoSessionRegistry();
	final static RequestMatcher browserHtmlRequestMatcher = createBrowserHtmlRequestMatcher();
	
    @Autowired
    public void configureAuthenticationManager(AuthenticationManagerBuilder auth,
            MangoUserDetailsService userDetails,
            MangoPasswordAuthenticationProvider passwordAuthenticationProvider
            //MangoJsonWebTokenAuthenticationProvider tokenAuthProvider
            ) throws Exception {
        
        auth.userDetailsService(userDetails);

        for (AuthenticationDefinition def : ModuleRegistry.getDefinitions(AuthenticationDefinition.class)) {
            auth.authenticationProvider(def.authenticationProvider());
        }
        
        auth.authenticationProvider(passwordAuthenticationProvider);
            //.authenticationProvider(tokenAuthProvider);
    }

    @Bean
    public UserDetailsService userDetailsService(MangoUserDetailsService userDetails) {
        return userDetails;
    }

    @Bean
    public AccessDeniedHandler accessDeniedHandler(MangoAccessDeniedHandler handler) {
        return handler;
    }
    
    @Bean(name = "restAccessDeniedHandler")
    public AccessDeniedHandler restAccessDeniedHandler(MangoRestAccessDeniedHandler handler) {
        return new MangoRestAccessDeniedHandler();
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
    @Bean(name="browserHtmlRequestMatcher")
    public static RequestMatcher browserHtmlRequestMatcher() {
        return browserHtmlRequestMatcher;
    }
    
    /**
     * Internal method to create a static matcher
     * @return
     */
    private static RequestMatcher createBrowserHtmlRequestMatcher(){
    	ContentNegotiationStrategy contentNegotiationStrategy = contentNegotiationStrategy();
        
        MediaTypeRequestMatcher mediaMatcher = new MediaTypeRequestMatcher(
                contentNegotiationStrategy, MediaType.APPLICATION_XHTML_XML, MediaType.TEXT_HTML);
        mediaMatcher.setIgnoredMediaTypes(Collections.singleton(MediaType.ALL));

        RequestMatcher notXRequestedWith = new NegatedRequestMatcher(
                new RequestHeaderRequestMatcher("X-Requested-With", "XMLHttpRequest"));

        return new AndRequestMatcher(Arrays.asList(notXRequestedWith, mediaMatcher));
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        if(Common.envProps.getBoolean("rest.cors.enabled", false)) {
            CorsConfiguration configuration = new CorsConfiguration();
            configuration.setAllowedOrigins(Arrays.asList(Common.envProps.getStringArray("rest.cors.allowedOrigins", ",", new String[0])));
            configuration.setAllowedMethods(Arrays.asList(Common.envProps.getStringArray("rest.cors.allowedMethods", ",", new String[0])));
            configuration.setAllowedHeaders(Arrays.asList(Common.envProps.getStringArray("rest.cors.allowedHeaders", ",", new String[0])));
            configuration.setExposedHeaders(Arrays.asList(Common.envProps.getStringArray("rest.cors.exposedHeaders", ",", new String[0])));
            configuration.setAllowCredentials(Common.envProps.getBoolean("rest.cors.allowCredentials", false));
            configuration.setMaxAge(Common.envProps.getLong("rest.cors.maxAge", 0));
            UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
            source.setAlwaysUseFullPath(true); //Don't chop off the starting /rest stuff
            source.registerCorsConfiguration("/rest/**", configuration);
            return source;
        } else {
            return null;
        }
    }
    
    @Bean
    public PermissionExceptionFilter permissionExceptionFilter(){
    	return new PermissionExceptionFilter();
    }
    
    @Bean
    public ObjectMapper objectMapper() {
        return MangoRestSpringConfiguration.getObjectMapper();
    }
    
    @Bean
    public static SessionRegistry sessionRegistry(){
    	return sessionRegistry;
    }
    
    /**
     * Return a count of all active sessions.
     * 
     * @return
     */
    public static int getActiveSessionCount(){
    	int activeCount = 0;
    	final List<Object> allPrincipals = sessionRegistry.getAllPrincipals();

        for (final Object principal : allPrincipals) {
            if (principal instanceof User) {
                activeCount += sessionRegistry.getAllSessions(principal, false).size();
            }
        }
        
        return activeCount;
    }
    
	/**
	 * Temporary Solution to Caching User in Session, called by User Dao when a User is saved or updated.
	 * 
	 * Note that the HttpSession info is updated in the Users REST Controller 
	 * 
	 * @param old - Cannot be null
	 * @param new - can be null if user was deleted
	 */
	public static void replaceUserInSessions(User old, User user) {

		boolean isEditingCurrentUser = false;
		
		User currentUser = Common.getHttpUser();
		if(currentUser != null)
			isEditingCurrentUser = currentUser.getUsername().equals(old.getUsername());
		
		
    	final List<Object> allPrincipals = sessionRegistry.getAllPrincipals();
    	
        for (final Object principal : allPrincipals) {
        	//Confirm we are only editing the user that was modified
            if ((principal instanceof User)&&(StringUtils.equals(((User)principal).getUsername(), old.getUsername()))){
            	List<SessionInformation> sessionInfo = sessionRegistry.getAllSessions(principal, false);
            	if(user == null){
            		//Expire sessions, the user was deleted
            		for(SessionInformation info : sessionInfo){
            			info.expireNow();
            		}
            	}else{
            		//Replace the user's sessions or expire them if they are not I
            		for(SessionInformation info : sessionInfo){
            			if(isEditingCurrentUser)
            				sessionRegistry.registerNewSession(info.getSessionId(), user);
            			else
            				info.expireNow();
            		}
            	}
            }
        }
		
	}
    
    // Configure a separate WebSecurityConfigurerAdapter for REST requests which have a Authorization header with Bearer token
    // We use a stateless session creation policy and disable CSRF for these requests so that the Token Authorization is not
    // persisted in the session inside the SecurityContext
//    @Configuration
//    @Order(1)
    public static class TokenAuthenticatedRestSecurityConfiguration extends WebSecurityConfigurerAdapter {
        AccessDeniedHandler accessDeniedHandler;
        AuthenticationEntryPoint authenticationEntryPoint = new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED);
        CorsConfigurationSource corsConfigurationSource;
        
        @Autowired
        public void init(MangoRestAccessDeniedHandler accessDeniedHandler,
                CorsConfigurationSource corsConfigurationSource) {
            this.accessDeniedHandler = accessDeniedHandler;
            this.corsConfigurationSource = corsConfigurationSource;
        }
        
        @Bean(name=BeanIds.AUTHENTICATION_MANAGER)
        @Override
        public AuthenticationManager authenticationManagerBean() throws Exception {
            return super.authenticationManagerBean();
        }

        @Override
        protected void configure(HttpSecurity http) throws Exception {
            http.requestMatcher(new RequestMatcher() {
                    AntPathRequestMatcher pathMatcher = new AntPathRequestMatcher("/rest/**");
                    public boolean matches(HttpServletRequest request) {
                        String header = request.getHeader("Authorization");
                        return header != null && header.startsWith("Bearer ") && pathMatcher.matches(request);
                    }
                })
                .sessionManagement()
                    // stops the SessionManagementConfigurer from using a HttpSessionSecurityContextRepository to
                    // store the SecurityContext, instead it creates a NullSecurityContextRepository which does 
                    // result in session creation
                    .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                    .and()
                .authorizeRequests()
                    .antMatchers("/rest/*/login/**").denyAll()
                    .antMatchers("/rest/*/logout/**").denyAll()
                    .antMatchers(HttpMethod.POST, "/rest/*/login/su").denyAll()
                    .antMatchers(HttpMethod.POST, "/rest/*/login/exit-su").denyAll()
                    .antMatchers("/rest/*/jwt/**").denyAll()
                    .antMatchers(HttpMethod.GET, "/rest/*/translations/public/**").permitAll() //For public translations
                    .antMatchers(HttpMethod.GET, "/rest/*/json-data/public/**").permitAll() //For public json-data
                    .antMatchers(HttpMethod.GET, "/rest/*/modules/angularjs-modules/public/**").permitAll() //For public angularjs modules
                    .antMatchers(HttpMethod.GET, "/rest/*/file-stores/public/**").permitAll() //For public file store
                    .antMatchers(HttpMethod.OPTIONS).permitAll()
                    .anyRequest().authenticated()
                    .and()
                // do not need CSRF protection when we are using a JWT token
                .csrf().disable()
                .rememberMe().disable()
                .logout().disable()
                .formLogin().disable()
                .requestCache().disable()
                .exceptionHandling()
                    .authenticationEntryPoint(authenticationEntryPoint)
                    .accessDeniedHandler(accessDeniedHandler)
                    .and()
                .addFilterBefore(new BearerAuthenticationFilter(authenticationManagerBean(), authenticationEntryPoint), BasicAuthenticationFilter.class);
            
            //Configure the headers
            configureHeaders(http);
            configureHSTS(http, false);
            
            // Use the MVC Cors Configuration
            if (Common.envProps.getBoolean("rest.cors.enabled", false))
                http.cors().configurationSource(corsConfigurationSource);
        }
    }

    @Configuration
    @Order(2)
    public static class RestSecurityConfiguration extends WebSecurityConfigurerAdapter {
        AuthenticationSuccessHandler authenticationSuccessHandler;
        AuthenticationFailureHandler authenticationFailureHandler;
        AccessDeniedHandler accessDeniedHandler;
        CorsConfigurationSource corsConfigurationSource;
        JsonLoginConfigurer jsonLoginConfigurer;
        LogoutHandler logoutHandler;
        LogoutSuccessHandler logoutSuccessHandler;
        PermissionExceptionFilter permissionExceptionFilter;
        
        @Autowired
        public void init(
                AuthenticationSuccessHandler authenticationSuccessHandler,
                AuthenticationFailureHandler authenticationFailureHandler,
                MangoRestAccessDeniedHandler accessDeniedHandler,
                CorsConfigurationSource corsConfigurationSource,
                JsonLoginConfigurer jsonLoginConfigurer,
                LogoutHandler logoutHandler,
                LogoutSuccessHandler logoutSuccessHandler,
                PermissionExceptionFilter permissionExceptionFilter) {
            this.authenticationSuccessHandler = authenticationSuccessHandler;
            this.authenticationFailureHandler = authenticationFailureHandler;
            this.accessDeniedHandler = accessDeniedHandler;
            this.corsConfigurationSource = corsConfigurationSource;
            this.jsonLoginConfigurer = jsonLoginConfigurer;
            this.logoutHandler = logoutHandler;
            this.logoutSuccessHandler = logoutSuccessHandler;
            this.permissionExceptionFilter = permissionExceptionFilter;
        }
        
        @Override
        protected void configure(HttpSecurity http) throws Exception {
            http.antMatcher("/rest/**")
                .sessionManagement()
	            	.maximumSessions(10)
	            	.maxSessionsPreventsLogin(false)
	            	.sessionRegistry(sessionRegistry)
	            	.and()
                    .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                    .and()
                .authorizeRequests()
                    .antMatchers("/rest/*/login").permitAll()
                    .antMatchers("/rest/*/exception/**").permitAll() //For exception info for a user's session...
                    .antMatchers(HttpMethod.POST, "/rest/*/login/su").hasRole("ADMIN")
                    .antMatchers(HttpMethod.GET, "/rest/*/translations/public/**").permitAll() //For public translations
                    .antMatchers(HttpMethod.GET, "/rest/*/json-data/public/**").permitAll() //For public json-data
                    .antMatchers(HttpMethod.GET, "/rest/*/modules/angularjs-modules/public/**").permitAll() //For public angularjs modules
                    .antMatchers(HttpMethod.GET, "/rest/*/file-stores/public/**").permitAll() //For public file store
                    .antMatchers(HttpMethod.OPTIONS).permitAll()
                    .anyRequest().authenticated()
                    .and()
                .apply(jsonLoginConfigurer)
                    .successHandler(authenticationSuccessHandler)
                    .failureHandler(authenticationFailureHandler)
                    .and()
                .logout()
                    .logoutRequestMatcher(new AntPathRequestMatcher("/rest/*/logout", "POST"))
                    .addLogoutHandler(logoutHandler)
                    .invalidateHttpSession(true)
                    // XSRF token is deleted but its own logout handler, session cookie doesn't really need to be deleted as its invalidated
                    // but why not for the sake of cleanliness
                    .deleteCookies(Common.getCookieName())
                    .logoutSuccessHandler(logoutSuccessHandler)
                    .and()
                .csrf()
                    .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                    .and()
                .rememberMe().disable()
                .formLogin().disable()
                .requestCache().disable()
                .exceptionHandling()
                    .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
                    .accessDeniedHandler(accessDeniedHandler)
                    .and()
                .addFilterAfter(switchUserFilter(), FilterSecurityInterceptor.class)
            	.addFilterAfter(permissionExceptionFilter, ExceptionTranslationFilter.class);
            
            //Configure headers
            configureHeaders(http);
            configureHSTS(http, false);
            
            // Use the MVC Cors Configuration
            if (Common.envProps.getBoolean("rest.cors.enabled", false))
                http.cors().configurationSource(corsConfigurationSource);
        }
        
    	@Bean
    	public SwitchUserFilter switchUserFilter() {
    		SwitchUserFilter filter = new SwitchUserFilter() {
    		    RequestMatcher suMatcher = new AntPathRequestMatcher("/rest/*/login/su", HttpMethod.POST.name());
                RequestMatcher exitSuMatcher = new AntPathRequestMatcher("/rest/*/login/exit-su", HttpMethod.POST.name());
    		    
    		    protected boolean requiresSwitchUser(HttpServletRequest request) {
                    return suMatcher.matches(request);
    		    }
    		    
    		    protected boolean requiresExitUser(HttpServletRequest request) {
    		        return exitSuMatcher.matches(request);
    		    }
    		};
    		filter.setUserDetailsService(userDetailsService());
    		filter.setSuccessHandler(authenticationSuccessHandler);
    		filter.setUsernameParameter("username");
    		return filter;
    	}
    }

    @Configuration
    @Order(3)
    public static class DefaultSecurityConfiguration extends WebSecurityConfigurerAdapter {
        AccessDeniedHandler accessDeniedHandler;
        AuthenticationEntryPoint authenticationEntryPoint;
        AuthenticationSuccessHandler authenticationSuccessHandler;
        AuthenticationFailureHandler authenticationFailureHandler;
        LogoutHandler logoutHandler;
        LogoutSuccessHandler logoutSuccessHandler;
        RequestCache requestCache;
        PermissionExceptionFilter permissionExceptionFilter;
        
        @Autowired
        public void init(AccessDeniedHandler accessDeniedHandler,
                AuthenticationEntryPoint authenticationEntryPoint,
                AuthenticationSuccessHandler authenticationSuccessHandler,
                AuthenticationFailureHandler authenticationFailureHandler,
                LogoutHandler logoutHandler,
                LogoutSuccessHandler logoutSuccessHandler,
                RequestCache requestCache, 
                PermissionExceptionFilter permissionExceptionFilter) {
            this.accessDeniedHandler = accessDeniedHandler;
            this.authenticationEntryPoint = authenticationEntryPoint;
            this.authenticationSuccessHandler = authenticationSuccessHandler;
            this.authenticationFailureHandler = authenticationFailureHandler;
            this.logoutHandler = logoutHandler;
            this.logoutSuccessHandler = logoutSuccessHandler;
            this.requestCache = requestCache;
            this.permissionExceptionFilter = permissionExceptionFilter;
        }

        @Override
        protected void configure(HttpSecurity http) throws Exception {
            http
            .sessionManagement()
            	.maximumSessions(10)
            	.maxSessionsPreventsLogin(false)
            	.sessionRegistry(sessionRegistry)
            	.and()
                .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                .and()
            .formLogin()
                // setting this prevents FormLoginConfigurer from adding the login page generating filter
                // this adds an authentication entry point but it wont be used as we have already specified one below in exceptionHandling()
                .loginPage("/login-xyz.htm")
                .loginProcessingUrl("/login")
                .successHandler(authenticationSuccessHandler)
                .failureHandler(authenticationFailureHandler)
                .permitAll()
                .and()
            .logout()
                .logoutUrl("/logout")
                .addLogoutHandler(logoutHandler)
                .invalidateHttpSession(true)
                // XSRF token is deleted but its own logout handler, session cookie doesn't really need to be deleted as its invalidated
                // but why not for the sake of cleanliness
                .deleteCookies(Common.getCookieName())
                .logoutSuccessHandler(logoutSuccessHandler)
                .and()
            .rememberMe()
                .disable()
            .authorizeRequests()
                // dont allow access to any modules folders other than web
                .antMatchers(HttpMethod.GET, "/modules/*/web/**").permitAll()
                .antMatchers("/modules/**").denyAll()
                // Access to *.shtm files must be authenticated
                .antMatchers("/**/*.shtm").authenticated()
                //Access to protected folder
                .antMatchers("/protected/**").authenticated()
                // Default to permit all
                .anyRequest().permitAll()
                .and()
            .csrf()
                // DWR handles its own CRSF protection (It is set to look at the same cookie in Lifecyle)
                .ignoringAntMatchers("/dwr/**", "/httpds")
                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                .and()
            .requestCache()
                .requestCache(requestCache)
                .and()
            .exceptionHandling()
                .authenticationEntryPoint(authenticationEntryPoint)
                .accessDeniedHandler(accessDeniedHandler)
                .and()
            .addFilterAfter(permissionExceptionFilter, ExceptionTranslationFilter.class);
            
            //Customize the headers here
            configureHeaders(http);
            configureHSTS(http, true);
        }
    }

    static void configureHSTS(HttpSecurity http, boolean requiresSecure) throws Exception {
        // If using SSL then enable the hsts and secure forwarding
        if (Common.envProps.getBoolean("ssl.on", false) && Common.envProps.getBoolean("ssl.hsts.enabled", true)) {
            // dont enable "requiresSecure" for REST calls
            // this options sets the REQUIRES_SECURE_CHANNEL attribute and causes ChannelProcessingFilter
            // to perform a 302 redirect to https://
            if (requiresSecure) {
                http.requiresChannel()
                    .anyRequest()
                        .requiresSecure();
            }
            http.headers()
                .httpStrictTransportSecurity()
                .maxAgeInSeconds(Common.envProps.getLong("ssl.hsts.maxAge", 31536000))
                .includeSubDomains(Common.envProps.getBoolean("ssl.hsts.includeSubDomains", false));
        } else {
            http.headers()
                .httpStrictTransportSecurity()
                .disable();
        }
    }
    
    /**
     * Ensure the headers are properly configured
     * @param http
     * @throws Exception
     */
    static void configureHeaders(HttpSecurity http) throws Exception{
    	 String iFrameControl = Common.envProps.getString("web.security.iFrameAccess", "SAMEORIGIN");
         if(StringUtils.equals(iFrameControl, "SAMEORIGIN")){
         	http.headers()
             .frameOptions().sameOrigin()
             .cacheControl().disable();
         }else if(StringUtils.equals(iFrameControl, "DENY")){
        	 http.headers()
             .frameOptions().deny()
             .cacheControl().disable();
         }else if(StringUtils.equals(iFrameControl, "ANY")){
        	 http.headers()
        	 .frameOptions().disable()
             .cacheControl().disable();
         }else{
        	 //TODO Ensure these are valid Domains?
        	 XFrameOptionsHeaderWriter headerWriter = new XFrameOptionsHeaderWriter(new MangoAllowFromStrategy(iFrameControl));
        	 http.headers().addHeaderWriter(headerWriter)
        	 .frameOptions().disable()
             .cacheControl().disable();
        	 
         }
    }
    
    /**
     * Get the 
     * 
     * @author Terry Packer
     */
    static class MangoAllowFromStrategy implements AllowFromStrategy{

    	String allowedDomain;
    	
    	public MangoAllowFromStrategy(String allowed){
    		this.allowedDomain = allowed;
    	}
		/* (non-Javadoc)
		 * @see org.springframework.security.web.header.writers.frameoptions.AllowFromStrategy#getAllowFromValue(javax.servlet.http.HttpServletRequest)
		 */
		@Override
		public String getAllowFromValue(HttpServletRequest request) {
			return allowedDomain;
		}
    	
    }
}