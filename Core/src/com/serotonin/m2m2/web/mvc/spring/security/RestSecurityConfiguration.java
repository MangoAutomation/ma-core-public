/**
 * Copyright (C) 2015 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.web.mvc.spring.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.userdetails.UserDetailsService;

import com.serotonin.m2m2.web.mvc.spring.authentication.MangoUserAuthenticationProvider;
import com.serotonin.m2m2.web.mvc.spring.authentication.MangoUserDetailsService;

/**
 * @author Terry Packer
 *
 */
@Configuration
@EnableWebSecurity
public class RestSecurityConfiguration extends WebSecurityConfigurerAdapter {
	//private static final Log LOG = LogFactory.getLog(RestSecurityConfiguration.class);
	
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

	@Override
	protected void configure(HttpSecurity http) throws Exception {
		
		http.authorizeRequests()
			.antMatchers(HttpMethod.GET, "/login*").anonymous() //Ensure we add the login authorization to our Context if we login via legacy
			.antMatchers(HttpMethod.POST, "/login*").anonymous()
			//.antMatchers(HttpMethod.GET, "/*").anonymous()
			.antMatchers(HttpMethod.GET, "/rest/v1/login/*").anonymous()
			.antMatchers(HttpMethod.POST, "/rest/v1/**").authenticated()
			.antMatchers(HttpMethod.PUT, "/rest/v1/**").authenticated()
			.antMatchers(HttpMethod.DELETE, "/rest/v1/**").authenticated()
			.antMatchers(HttpMethod.GET, "/rest/v1/**").authenticated(); //Since we are currently checking credentials in the REST Code we can use this for now
			
			//Later when we add Authority restrictions to various URLs we can use the .antMatchers().hasAuthority() instead of hasRole() to avoid the ROLE_ prefix being appended
		//Basic HTTP 
		//http.httpBasic();
		
		//FormLoginConfigurer<HttpSecurity> formLogin = http.formLogin();
		//formLogin.loginPage("/login.htm");
		
		//For Logout Control See: http://docs.spring.io/spring-security/site/docs/current/reference/html/jc.html#jc-hello-wsca
//		http.logout()
//		.logoutUrl("/my/logout")
//		.logoutSuccessUrl("/my/index")
//		.logoutSuccessHandler(logoutSuccessHandler)
//		.invalidateHttpSession(true)
//		.addLogoutHandler(logoutHandler)
//		.deleteCookies(cookieNamesToClear)
		
	}
}
