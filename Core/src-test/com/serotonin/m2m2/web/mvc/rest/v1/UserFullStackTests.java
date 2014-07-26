/**
 * Copyright (C) 2014 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.web.mvc.rest.v1;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;

import com.serotonin.m2m2.web.mvc.rest.BaseFullStackRestTest;
import com.serotonin.m2m2.web.mvc.rest.v1.model.UserModel;

/**
 * User full stack tests use the full mango instance to test the REST endpoints with no mocking.
 * 
 * @see http://spring.io/guides/tutorials/rest/4/ for example
 * 
 * @author Terry Packer
 *
 */
public class UserFullStackTests extends BaseFullStackRestTest{

	
	@Test
	public void getAll(){
		//Setup For Rest Call
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
		RestTemplate template = new RestTemplate();
		
		
		List<UserModel> data = template.getForObject("http://localhost:8080/v1/users/", List.class);
		
		
		
		
	}
	
}
