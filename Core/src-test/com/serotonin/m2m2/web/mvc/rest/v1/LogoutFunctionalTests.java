/**
 * Copyright (C) 2014 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.web.mvc.rest.v1;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.dao.DaoRegistry;
import com.serotonin.m2m2.db.dao.UserDao;
import com.serotonin.m2m2.rt.EventManager;
import com.serotonin.m2m2.rt.event.type.SystemEventType;
import com.serotonin.m2m2.test.data.UserTestData;
import com.serotonin.m2m2.vo.User;
import com.serotonin.m2m2.web.mvc.rest.BaseRestTest;
import com.serotonin.m2m2.web.mvc.rest.v1.model.UserModel;

/**
 * @author Terry Packer
 *
 */
public class LogoutFunctionalTests extends BaseRestTest{

	@Mock
	protected EventManager eventManager;
	
	@Mock
	protected UserDao userDao;

	@InjectMocks
	protected LogoutRestController mockController;
	
	@Before
    public void setup() {
    	MockitoAnnotations.initMocks(this);
    	this.setupMvc(mockController);
    	
        //Mock our Daos so they
        // return exactly what we want.
    	DaoRegistry.userDao = this.userDao;
    	Common.eventManager = this.eventManager;
    }
	@Test
	public void testLogout(){
		User standardUser = UserTestData.standardUser();

		//Mock the Dao Get User Call
		when(userDao.getUser(standardUser.getUsername())).thenReturn(standardUser);
		
		//Mock the return to normal for the Logged In User that DNE anyway
		Mockito.doNothing().when(Common.eventManager).returnToNormal(new SystemEventType(SystemEventType.TYPE_USER_LOGIN, standardUser.getId()), System.currentTimeMillis());
		
		try{
			MvcResult result = this.mockMvc.perform(
					post("/v1/logout/{username}",standardUser.getUsername())
					.sessionAttr("sessionUser", standardUser)
					.accept(MediaType.APPLICATION_JSON))
					.andDo(print())
					.andExpect(status().isOk())
					.andReturn();
			
			UserModel loggedOutUserModel = this.objectMapper.readValue(result.getResponse().getContentAsString(), UserModel.class);
			User loggedOutUser = loggedOutUserModel.getData();

			//Check to see that the User is correct
			assertEquals(standardUser.getUsername(), loggedOutUser.getUsername());
						
			//Ensure the User is no longer in the Session
			User sessionUser = (User) result.getRequest().getSession().getAttribute("sessionUser"); //Because Common.SESSION_USER is not public
			assertEquals(null, sessionUser);
		
		
		}catch(Exception e){
			e.printStackTrace();
			fail(e.getMessage());
		}
		
	}
	
	
	@Test
	public void testLogoutFail(){
		User standardUser = UserTestData.standardUser();

		//Mock the Dao Get User Call
		when(userDao.getUser(standardUser.getUsername())).thenReturn(standardUser);
		
		try{
			MvcResult result = this.mockMvc.perform(
					post("/v1/logout/{username}",standardUser.getUsername())
					.accept(MediaType.APPLICATION_JSON))
					.andDo(print())
					.andExpect(status().isUnauthorized())
					.andReturn();
		
		}catch(Exception e){
			fail(e.getMessage());
		}
		
	}
	
}
