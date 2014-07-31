/**
 * Copyright (C) 2014 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.web.mvc.rest.v1;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
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
import org.springframework.test.web.servlet.MvcResult;

import com.serotonin.m2m2.db.dao.DaoRegistry;
import com.serotonin.m2m2.db.dao.DataPointDao;
import com.serotonin.m2m2.db.dao.DataSourceDao;
import com.serotonin.m2m2.db.dao.UserDao;
import com.serotonin.m2m2.test.data.DataSourceData;
import com.serotonin.m2m2.test.data.UserTestData;
import com.serotonin.m2m2.vo.User;
import com.serotonin.m2m2.vo.dataSource.DataSourceVO;
import com.serotonin.m2m2.web.mvc.rest.BaseRestTest;
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
	

	//TODO Eventually move these to BaseRestTest
    @Mock
	protected UserDao userDao;
    @Mock
    protected DataSourceDao dataSourceDao;
    @Mock
    protected DataPointDao dataPointDao;
    
    @InjectMocks
    protected UserRestController mockController;
	
    @Before
    public void setup() {
    	MockitoAnnotations.initMocks(this);
    	this.setupMvc(mockController);
    	
        //Mock our Daos so they
        // return exactly what we want.
    	DaoRegistry.dataPointDao = this.dataPointDao;
    	DaoRegistry.dataSourceDao = this.dataSourceDao;
    	DaoRegistry.userDao = this.userDao;
    }
    
    
    /**
     * Test reading all users
     * @throws Exception
     */
	@Test
	public void testGetAll(){
		
		List<User> users = new ArrayList<User>();
		User standardUser = UserTestData.standardUser();
		users.add(standardUser);
		
		//This will ensure that the getUsers() method returns 
		// the mock list of users
		when(userDao.getUsers()).thenReturn(users);
		
		//Mock up the permissions requests
		for(User user : users){
			for(Integer dsId : user.getDataSourcePermissions()){
				DataSourceVO ds = DataSourceData.mockDataSource();
				when(this.dataSourceDao.get(dsId)).thenReturn(ds);
				when(this.dataSourceDao.getByXid(ds.getXid())).thenReturn(ds);
				
			}
//			for(DataPointAccess access : user.getDataPointPermissions()){
//				when(DataSourceDao.instance.getByXid())
//			}
		}
		
		try{
			MvcResult result = this.mockMvc.perform(
		            get("/v1/users")
		                    .accept(MediaType.APPLICATION_JSON))
		            .andDo(print())
		            .andExpect(status().isOk())
		            .andReturn();
	
			//Check the result
			System.out.println(result.getResponse().getContentAsString());
			
			//Could re-create the User object from the Json
			
			//May not be necessary but allows arrays of size 1
		    //objectMapper.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
	
			UserModel[] models = this.objectMapper.readValue(result.getResponse().getContentAsString(), UserModel[].class);
			//ArrayList<UserModel> models = this.objectMapper.readValue(result.getResponse().getContentAsString(), ArrayList.class);
			//List<UserModel> models = this.objectMapper.readValue(result.getResponse().getContentAsString(), objectMapper.getTypeFactory().constructCollectionType(List.class, UserModel.class));
			//Check the size
			assertEquals(users.size(), models.length);
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
	public void testGet() throws Exception{
		
		User standardUser = UserTestData.standardUser();
		List<User> users = new ArrayList<User>();
		users.add(standardUser);
		
		//This will ensure that the getUsers() method returns 
		// the mock list of users
		when(userDao.getUser(standardUser.getUsername())).thenReturn(standardUser);

		
		this.mockMvc.perform(
	            get("/v1/users/" + standardUser.getUsername())
	                    .accept(MediaType.APPLICATION_JSON))
	            .andDo(print())
	            .andExpect(status().isOk());
	}
	
}
