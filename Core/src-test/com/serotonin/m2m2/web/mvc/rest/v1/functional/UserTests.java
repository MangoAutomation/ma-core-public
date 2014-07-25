/**
 * Copyright (C) 2014 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.web.mvc.rest.v1.functional;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;

import com.serotonin.m2m2.db.dao.UserDao;
import com.serotonin.m2m2.test.data.UserTestData;
import com.serotonin.m2m2.vo.User;
import com.serotonin.m2m2.web.mvc.rest.v1.BaseRestTest;
import com.serotonin.m2m2.web.mvc.rest.v1.UserRestController;

/**
 * @See http://spring.io/guides/tutorials/rest/2/ 
 * 
 * on example tests
 * 
 * 
 * @author Terry Packer
 *
 */

public class UserTests extends BaseRestTest{
	

    @Mock
	protected UserDao dao;
    
    @InjectMocks
    protected UserRestController mockController;
	
    @Before
    public void setup() {
    	MockitoAnnotations.initMocks(this);
    	this.setupMvc(mockController);
    	
        //Mock our Dao so that the controller 
        // returns exactly what we want.
        this.mockController.setUserDao(this.dao);
    }
    
    
    /**
     * Test reading all users
     * @throws Exception
     */
	@Test
	public void testReadAll() throws Exception{
		
		List<User> users = new ArrayList<User>();
		users.add(UserTestData.standardUser());
		
		//This will ensure that the getUsers() method returns 
		// the mock list of users
		when(dao.getUsers()).thenReturn(users);
		
		
		this.mockMvc.perform(
	            get("/v1/users")
	                    .accept(MediaType.APPLICATION_JSON))
	            .andDo(print())
	            .andExpect(status().isOk());
		
		
	}
	
	
}
