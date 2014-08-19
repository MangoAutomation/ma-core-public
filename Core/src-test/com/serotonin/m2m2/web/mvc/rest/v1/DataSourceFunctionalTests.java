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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

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
import com.serotonin.m2m2.web.mvc.rest.v1.model.AbstractDataSourceModel;
import com.serotonin.m2m2.web.mvc.rest.v1.model.MockDataSourceModel;
import com.serotonin.m2m2.web.mvc.rest.v1.model.UserModel;

/**
 * @author Terry Packer
 *
 */
public class DataSourceFunctionalTests extends BaseRestTest{

	@InjectMocks
	protected DataSourceRestController mockController;
	
    @Before
    public void setup() {
    	MockitoAnnotations.initMocks(this);
    	this.setupMvc(mockController);
    }
	
    @Test
    public void testGetDataSource(){
    	
    	User adminUser = UserTestData.adminUser();
    	DataSourceVO ds = DataSourceTestData.mockDataSource();
    	when(dataSourceDao.getByXid(ds.getXid())).thenReturn(ds);
    	
    	
		try{
			MvcResult result = this.mockMvc.perform(
		            get("/v1/dataSources/" + ds.getXid())
		            .sessionAttr("sessionUser", adminUser)  
		            .accept(MediaType.APPLICATION_JSON))
		            .andDo(print())
		            .andExpect(status().isOk())
		            .andReturn();

			AbstractDataSourceModel model = this.objectMapper.readValue(
					result.getResponse().getContentAsString(),
					MockDataSourceModel.class);
			assertEquals(model.getName(), ds.getModel().getName());
			//Check the size
		}catch(Exception e){
			fail(e.getMessage());
		}

    	
    }

    /**
     * Test creating a mock data source
     */
	public void testAdminCreate() {
		
    	DataSourceVO ds = DataSourceTestData.mockDataSource();
    	when(dataSourceDao.getByXid(ds.getXid())).thenReturn(ds);
    	
		User adminUser = UserTestData.adminUser();
		
		
		ObjectWriter writer = this.objectMapper.writerWithView(JsonViews.Test.class);
		
		
		try{
			String userJson = writer.writeValueAsString(new MockDataSourceModel(ds));
			this.mockMvc.perform(
	            post("/v1/dataSources/")
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
     * Test udpating a mock data source
     */
	@Test
	public void testAdminUpdate() {
		
    	DataSourceVO ds = DataSourceTestData.mockDataSource();
    	when(dataSourceDao.getByXid(ds.getXid())).thenReturn(ds);
    	
		User adminUser = UserTestData.adminUser();
		
		
		ObjectWriter writer = this.objectMapper.writerWithView(JsonViews.Test.class);
		
		
		try{
			String userJson = writer.writeValueAsString(new MockDataSourceModel(ds));
			this.mockMvc.perform(
	            put("/v1/dataSources/" + ds.getXid())
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
}
