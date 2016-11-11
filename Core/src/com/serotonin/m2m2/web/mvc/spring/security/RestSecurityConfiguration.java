/**
 * Copyright (C) 2015 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.web.mvc.spring.security;

import java.util.Arrays;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.module.DefaultPagesDefinition;
import com.serotonin.m2m2.module.ModuleRegistry;
import com.serotonin.m2m2.web.mvc.spring.authentication.MangoUserAuthenticationProvider;
import com.serotonin.m2m2.web.mvc.spring.authentication.MangoUserDetailsService;

/**
 * Spring Security Setup for REST based requests 
 * 
 * @author Terry Packer
 *
 */
@Configuration
@EnableWebSecurity
public class RestSecurityConfiguration extends WebSecurityConfigurerAdapter {
	private static final Log LOG = LogFactory.getLog(RestSecurityConfiguration.class);
	
	@Override
	public void configure(AuthenticationManagerBuilder auth) throws Exception {
		auth.userDetailsService(userDetailsService());
		auth.authenticationProvider(authenticationProvider());
	}

	@Bean
	public UserDetailsService userDetailsService() {
		return new MangoUserDetailsService();
	}

	@Bean
	public AuthenticationProvider authenticationProvider() {
		return new MangoUserAuthenticationProvider();
	}

	public AccessDeniedHandler accessDeniedHandler(){
		return new MangoAccessDeniedHandler();
	}
	
	@Override
	protected void configure(HttpSecurity http) throws Exception {
		
		http.authorizeRequests()
			//We are letting the legacy permissions system handle these pages for now
			.antMatchers(HttpMethod.GET, "/**/*.shtm").permitAll()
			.antMatchers(HttpMethod.POST, "/**/*.shtm").permitAll()
			.antMatchers(HttpMethod.GET, "/*.shtm").permitAll();
		
	        //Now add all defined login pages
	        for(DefaultPagesDefinition def : ModuleRegistry.getDefinitions(DefaultPagesDefinition.class)){
	        	try{
		        	String uri = def.getLoginPageUri(null, null);
		        	if(uri != null){
		        		if(def.getModule() == null)
		        			LOG.info("Core allowing full access to URL: " + uri);
		        		else
		        			LOG.info("Module " + def.getModule().getName() + " allowing full access to URL: " + uri);
		        		http.authorizeRequests().antMatchers(uri).permitAll();
		        	}
	        	}catch(Exception e){
	        		LOG.error("Problem setting login page definition for class: " + def.getClass().getCanonicalName() + " because " + e.getMessage());
	        	}
	        }
			
			//Allow Startup REST Endpoint
	        http.authorizeRequests().antMatchers(HttpMethod.GET, "/status**").permitAll()
			
			//REST api Restrictions
			.antMatchers(HttpMethod.GET, "/rest/v1/login/*").permitAll()
			.antMatchers(HttpMethod.OPTIONS, "/rest/v1/login/*").permitAll() //For CORS reqeusts
			.antMatchers(HttpMethod.GET, "/rest/v1/translations/public/*").permitAll() //For public translations
			.antMatchers(HttpMethod.OPTIONS, "/rest/v1/translations/public/*").permitAll() //For public translations
			.antMatchers(HttpMethod.GET, "/rest/v1/json-data/public/*").permitAll() //For public json-data
			.antMatchers(HttpMethod.OPTIONS, "/rest/v1/json-data/public/*").permitAll() //For public json-data			
			.antMatchers(HttpMethod.OPTIONS, "/rest/v1/**").authenticated()
			.antMatchers(HttpMethod.POST, "/rest/v1/**").authenticated()
			.antMatchers(HttpMethod.PUT, "/rest/v1/**").authenticated()
			.antMatchers(HttpMethod.DELETE, "/rest/v1/**").authenticated()
			.antMatchers(HttpMethod.GET, "/rest/v1/**").authenticated(); //Since we are currently checking credentials in the REST Code we can use this for now
			
		//CSRF Headers https://spring.io/blog/2015/01/12/the-login-page-angular-js-and-spring-security-part-ii
		http.authorizeRequests().anyRequest().authenticated().and().addFilterAfter(new CsrfHeaderFilter(), CsrfFilter.class)
			.csrf().csrfTokenRepository(csrfTokenRepository());
		
		//Use the MVC Cors Configuration
		if(Common.envProps.getBoolean("rest.cors.enabled", false))
			http.cors();
		
		//Later when we add Authority restrictions to various URLs we can use the .antMatchers().hasAuthority() instead of hasRole() to avoid the ROLE_ prefix being appended
		
		//We could replace our Form Login but it will need to be customized for Module defined login/logout actions
//		FormLoginConfigurer<HttpSecurity> formLogin = http.formLogin();
//		formLogin.loginPage("/login.htm");
		
		//For Logout Control See: http://docs.spring.io/spring-security/site/docs/current/reference/html/jc.html#jc-hello-wsca
		http.logout()
				.invalidateHttpSession(true)
				.deleteCookies("XSRF-TOKEN","MANGO" + Common.envProps.getInt("web.port", 8080));
//		.logoutUrl("/my/logout")
//		.logoutSuccessUrl("/my/index")
//		.logoutSuccessHandler(logoutSuccessHandler)
//		.addLogoutHandler(logoutHandler)
		
		//Exception Handling
		http.exceptionHandling().accessDeniedHandler(accessDeniedHandler());
		
		//Customize the headers here
		http.headers().frameOptions().sameOrigin();
		
		//Allow ETags to be used See MangoShallowEtagHeaderFilter
		http.headers().cacheControl().disable();
	}

	@Bean
	public CorsConfigurationSource corsConfigurationSource(){
		if(Common.envProps.getBoolean("rest.cors.enabled", false)){
			CorsConfiguration configuration = new CorsConfiguration();
			configuration.setAllowedOrigins(Arrays.asList(Common.envProps.getStringArray("rest.cors.allowedOrigins", ",", new String[0])));
			configuration.setAllowedMethods(Arrays.asList(Common.envProps.getStringArray("rest.cors.allowedMethods", ",", new String[0])));
			configuration.setAllowedHeaders(Arrays.asList(Common.envProps.getStringArray("rest.cors.allowedHeaders", ",", new String[0])));
			configuration.setExposedHeaders(Arrays.asList(Common.envProps.getStringArray("rest.cors.exposedHeaders", ",", new String[0])));
			configuration.setAllowCredentials(Common.envProps.getBoolean("rest.cors.allowCredentials", false));
			configuration.setMaxAge(Common.envProps.getLong("rest.cors.maxAge", 0));
			UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
			source.registerCorsConfiguration("/**", configuration);
			return source;
		}else
			return null;
	}

	
	private CsrfTokenRepository csrfTokenRepository() {
		  HttpSessionCsrfTokenRepository repository = new HttpSessionCsrfTokenRepository();
		  repository.setHeaderName("X-XSRF-TOKEN");
		  return repository;
		}
	
}
