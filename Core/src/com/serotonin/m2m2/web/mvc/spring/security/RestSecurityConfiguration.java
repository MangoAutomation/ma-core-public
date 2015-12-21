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
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;

import com.serotonin.m2m2.Common;
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

	public AccessDeniedHandler accessDeniedHandler(){
		return new MangoAccessDeniedHandler();
	}
	
	@Override
	protected void configure(HttpSecurity http) throws Exception {
		
		http.authorizeRequests()
			//We are letting the legacy permissions system handle these pages for now
			.antMatchers(HttpMethod.GET, "/**/*.shtm").permitAll()
			.antMatchers(HttpMethod.POST, "/**/*.shtm").permitAll()
			.antMatchers(HttpMethod.GET, "/*.shtm").permitAll()
			
			//Allow all access for legacy login
			.antMatchers(HttpMethod.GET, "/login*").permitAll() 
			.antMatchers(HttpMethod.POST, "/login*").permitAll()
			
			
			//Allow Startup REST Endpoint
			.antMatchers(HttpMethod.GET, "/status**").permitAll()
			
			//REST api Restrictions
			.antMatchers(HttpMethod.GET, "/rest/v1/login/*").permitAll()
			.antMatchers(HttpMethod.OPTIONS, "/rest/v1/login/*").permitAll() //For CORS reqeusts
			.antMatchers(HttpMethod.OPTIONS, "/rest/v1/**").authenticated()
			.antMatchers(HttpMethod.POST, "/rest/v1/**").authenticated()
			.antMatchers(HttpMethod.PUT, "/rest/v1/**").authenticated()
			.antMatchers(HttpMethod.DELETE, "/rest/v1/**").authenticated()
			.antMatchers(HttpMethod.GET, "/rest/v1/**").authenticated(); //Since we are currently checking credentials in the REST Code we can use this for now

		//CSRF Headers https://spring.io/blog/2015/01/12/the-login-page-angular-js-and-spring-security-part-ii
		http.authorizeRequests().anyRequest().authenticated().and().addFilterAfter(new CsrfHeaderFilter(), CsrfFilter.class)
			.csrf().csrfTokenRepository(csrfTokenRepository());
		
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
	}
	
	private CsrfTokenRepository csrfTokenRepository() {
		  HttpSessionCsrfTokenRepository repository = new HttpSessionCsrfTokenRepository();
		  repository.setHeaderName("X-XSRF-TOKEN");
		  return repository;
		}
	
}
