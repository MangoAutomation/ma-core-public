/**
 * Copyright (C) 2014 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.web.mvc.rest.v1;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.databind.ObjectWriter;
import com.serotonin.m2m2.test.data.DataSourceTestData;
import com.serotonin.m2m2.test.data.UserTestData;
import com.serotonin.m2m2.vo.User;
import com.serotonin.m2m2.vo.dataSource.DataSourceVO;
import com.serotonin.m2m2.web.mvc.rest.BaseRestTest;
import com.serotonin.m2m2.web.mvc.rest.v1.mapping.JsonViews;
import com.serotonin.m2m2.web.mvc.rest.v1.model.UserModel;

/**
 * @See http://spring.io/guides/tutorials/rest/2/ 
 * @See http://spring.io/guides/tutorials/rest/4/
 * 
 * on example tests
 * 
 * 
 * @author Terry Packer
 *
 */
public class UserFunctionalTests extends BaseRestTest{
	
    
    @InjectMocks
    protected UserRestController mockController;
	    
    @Before
    public void setup() {
    	MockitoAnnotations.initMocks(this);
    	this.setupMvc(mockController);
    }
    
    
    /**
     * Test Posting an empty user
     * @throws Exception
     */
	@Test
	public void testValidationFailure(){
		
		User standardUser = UserTestData.standardUser();
		User adminUser = UserTestData.adminUser();
		
		List<User> users = new ArrayList<User>();
		users.add(standardUser);
		
		//This will ensure that the getUsers() method returns 
		// the mock list of users
		when(userDao.getUser(standardUser.getUsername())).thenReturn(null);
		
		standardUser.setEmail("");
		standardUser.setPassword("testing-password");
		
		ObjectWriter writer = this.objectMapper.writerWithView(JsonViews.Test.class);
		
		try{
			String userJson = writer.writeValueAsString(new UserModel(standardUser));
		this.mockMvc.perform(
	            post("/v1/users/")
	            .content(userJson)
	            .contentType(MediaType.APPLICATION_JSON)
	            .sessionAttr("sessionUser", adminUser)  
	            .accept(MediaType.APPLICATION_JSON))
	            .andDo(print())
	            .andExpect(status().isBadRequest());
		}catch(Exception e){
			e.printStackTrace();
			fail(e.getMessage());
		}
	}
    
    
    /**
     * Test Creating a User
     * TODO This test fails!!!! Because we don't render the password in the JSON property yet. :(
     */
	@Test
	public void testAdminCreateUser() {
		
		User standardUser = UserTestData.standardUser();
		User adminUser = UserTestData.adminUser();
		List<User> users = new ArrayList<User>();
		users.add(standardUser);
		
		//This will ensure that the getUsers() method returns 
		// the mock list of users
		when(userDao.getUser(standardUser.getUsername())).thenReturn(null);
		
		ObjectWriter writer = this.objectMapper.writerWithView(JsonViews.Test.class);
		//standardUser.setUsername("");
		
		
		try{
			String userJson = writer.writeValueAsString(new UserModel(standardUser));
			this.mockMvc.perform(
	            post("/v1/users/")
	            .content(userJson)
	            .contentType(MediaType.APPLICATION_JSON)
	            .sessionAttr("sessionUser", adminUser)  
	            .accept(MediaType.APPLICATION_JSON))
	            .andDo(print())
	            .andExpect(status().isCreated());
		}catch(Exception e){
			e.printStackTrace();
			fail(e.getMessage());
		}
	}
    
    
    /**
     * Test a non-admin user creating a User
     * 
     * Should show 401 - Unauthorized
     * @throws Exception
     */
	@Test
	public void testNonAdminCreateUserError() throws Exception{
		
		User standardUser = UserTestData.standardUser();
		List<User> users = new ArrayList<User>();
		users.add(standardUser);
		
		//This will ensure that the getUsers() method returns 
		// the mock list of users
		when(userDao.getUser(standardUser.getUsername())).thenReturn(standardUser);
		
		standardUser.setEmail(null);
		
		String userJson = this.objectMapper.writeValueAsString(new UserModel(standardUser));
		
		try{
		this.mockMvc.perform(
	            post("/v1/users/")
	            .content(userJson)
	            .contentType(MediaType.APPLICATION_JSON)
	            .sessionAttr("sessionUser", standardUser)  
	            .accept(MediaType.APPLICATION_JSON))
	            .andDo(print())
	            .andExpect(status().isUnauthorized());
		}catch(Exception e){
			fail(e.getMessage());
		}
	}
    
    
    
    /**
     * Test reading all users that should fail
     * as user is non-admin
     * @throws Exception
     */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Test
	public void testGetAllNonAdmin(){
		
		List<User> users = new ArrayList<User>();
		User standardUser = UserTestData.standardUser();
		users.add(standardUser);
		
		//This will ensure that the getUsers() method returns 
		// the mock list of users
		when(userDao.getUsers()).thenReturn(users);
		
		//Mock up the permissions requests
		for(User user : users){
			for(Integer dsId : user.getDataSourcePermissions()){
				DataSourceVO ds = DataSourceTestData.mockDataSource();
				when(this.dataSourceDao.get(dsId)).thenReturn(ds);
				when(this.dataSourceDao.getByXid(ds.getXid())).thenReturn(ds);
				
			}
//			for(DataPointAccess access : user.getDataPointPermissions()){
//				when(DataSourceDao.instance.getByXid())
//			}
		}
		
		try{
			this.mockMvc.perform(
		            get("/v1/users")
		            .sessionAttr("sessionUser", standardUser)  
		            .accept(MediaType.APPLICATION_JSON))
		            .andDo(print())
		            .andExpect(status().isUnauthorized())
		            .andReturn();
		}catch(Exception e){
			fail(e.getMessage());
		}
		//Check the data
		
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Test
	public void testGetAllAdmin(){
		
		List<User> users = new ArrayList<User>();
		User adminUser = UserTestData.adminUser();
		users.add(adminUser);
		users.add(UserTestData.newAdminUser());
		users.add(UserTestData.standardUser());
		
		//This will ensure that the getUsers() method returns 
		// the mock list of users
		when(userDao.getUsers()).thenReturn(users);
		
		//Mock up the permissions requests
		for(User user : users){
			for(Integer dsId : user.getDataSourcePermissions()){
				DataSourceVO ds = DataSourceTestData.mockDataSource();
				when(this.dataSourceDao.get(dsId)).thenReturn(ds);
				when(this.dataSourceDao.getByXid(ds.getXid())).thenReturn(ds);
				
			}
		}
		
		try{
			MvcResult result = this.mockMvc.perform(
		            get("/v1/users")
		            .sessionAttr("sessionUser", adminUser)  
		            .accept(MediaType.APPLICATION_JSON))
		            .andDo(print())
		            .andExpect(status().isOk())
		            .andReturn();

			List<UserModel> models = this.objectMapper.readValue(result.getResponse().getContentAsString(), objectMapper.getTypeFactory().constructCollectionType(List.class, UserModel.class));
			//Check the size
			assertEquals(users.size(), models.size());
		}catch(Exception e){
			fail(e.getMessage());
		}
		//Check the data
		
	}
	
	
    /**
     * Test reading all users
     * @throws Exception
     */
	@Test
	public void testGetSelf() throws Exception{
		
		User standardUser = UserTestData.standardUser();
		List<User> users = new ArrayList<User>();
		users.add(standardUser);
		
		//This will ensure that the getUsers() method returns 
		// the mock list of users
		when(userDao.getUser(standardUser.getUsername())).thenReturn(standardUser);

		
		this.mockMvc.perform(
	            get("/v1/users/" + standardUser.getUsername())
	             .sessionAttr("sessionUser", standardUser)  
	             .accept(MediaType.APPLICATION_JSON))
	            .andDo(print())
	            .andExpect(status().isOk());
	}
	
}
