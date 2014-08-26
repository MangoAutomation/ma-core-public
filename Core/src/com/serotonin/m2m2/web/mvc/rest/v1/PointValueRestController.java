/**
 * Copyright (C) 2014 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.web.mvc.rest.v1;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.serotonin.db.MappedRowCallback;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.dao.DataPointDao;
import com.serotonin.m2m2.db.dao.PointValueDao;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.rt.dataImage.AnnotatedPointValueTime;
import com.serotonin.m2m2.rt.dataImage.PointValueTime;
import com.serotonin.m2m2.rt.dataImage.SetPointSource;
import com.serotonin.m2m2.vo.DataPointVO;
import com.serotonin.m2m2.vo.User;
import com.serotonin.m2m2.vo.permission.PermissionException;
import com.serotonin.m2m2.vo.permission.Permissions;
import com.serotonin.m2m2.web.mvc.rest.v1.exception.RestValidationFailedException;
import com.serotonin.m2m2.web.mvc.rest.v1.message.RestProcessResult;
import com.serotonin.m2m2.web.mvc.rest.v1.model.pointValue.PointValueRollupCalculator;
import com.serotonin.m2m2.web.mvc.rest.v1.model.pointValue.PointValueTimeModel;
import com.serotonin.m2m2.web.mvc.rest.v1.model.pointValue.RollupEnum;
import com.serotonin.m2m2.web.mvc.rest.v1.model.time.TimePeriod;
import com.serotonin.m2m2.web.mvc.rest.v1.model.time.TimePeriodType;
import com.serotonin.m2m2.web.mvc.spring.MangoRestSpringConfiguration;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import com.wordnik.swagger.annotations.ApiResponse;
import com.wordnik.swagger.annotations.ApiResponses;

/**
 * @author Terry Packer
 * 
 */
@Api(value="Point Values", description="Operations on Point Values")
@RestController
@RequestMapping("/v1/pointValues")
public class PointValueRestController extends MangoRestController<PointValueTimeModel>{

	private static Log LOG = LogFactory.getLog(PointValueRestController.class);
	private PointValueDao dao = Common.databaseProxy.newPointValueDao();

	
	/**
	 * Get the latest point values for a point
	 * @param xid
	 * @param limit
	 * @return
	 */
	@ApiOperation(
			value = "Get Latest Point Values",
			notes = "Default 100, time descending order"
			)
    @RequestMapping(method = RequestMethod.GET, value="/{xid}/latest")
    public ResponseEntity<List<PointValueTimeModel>> getLatestPointValues(
    		HttpServletRequest request, 
    		
    		@ApiParam(value = "Point xid", required = true, allowMultiple = false)
    		@PathVariable String xid,
    		
    		@ApiParam(value = "Limit results", allowMultiple = false, defaultValue="100")
    		@RequestParam(value="limit", defaultValue="100") int limit){
        
    	RestProcessResult<List<PointValueTimeModel>> result = new RestProcessResult<List<PointValueTimeModel>>(HttpStatus.OK);
    	User user = this.checkUser(request, result);
    	if(result.isOk()){
    	
	    	DataPointVO vo = DataPointDao.instance.getByXid(xid);
	    	if(vo == null){
	    		result.addRestMessage(getDoesNotExistMessage());
	    		return result.createResponseEntity();
	    	}

	    	try{
	    		if(Permissions.hasDataPointReadPermission(user, vo)){
	    			List<PointValueTime> pvts = dao.getLatestPointValues(vo.getId(), limit);
	    			List<PointValueTimeModel> models = new ArrayList<PointValueTimeModel>(pvts.size());
	    			for(PointValueTime pvt : pvts){
	    				models.add(new PointValueTimeModel(pvt));
	    			}
	    			return result.createResponseEntity(models);
	    		}else{
	    	 		result.addRestMessage(getUnauthorizedMessage());
	    	 		return result.createResponseEntity();
		    	}
	    	}catch(PermissionException e){
	    		LOG.error(e.getMessage(), e);
	    		result.addRestMessage(getUnauthorizedMessage());
	    		return result.createResponseEntity();
	    	}
    	}else{
    		return result.createResponseEntity();
    	}
    }
    

	@ApiOperation(
			value = "Query Time Range",
			notes = "From time inclusive, To time exclusive"
			)
	@ApiResponses({
		@ApiResponse(code = 200, message = "Query Successful", response=PointValueTimeModel.class),
		@ApiResponse(code = 401, message = "Unauthorized Access", response=ResponseEntity.class)
		})
    @RequestMapping(method = RequestMethod.GET, value="/{xid}")
    public ResponseEntity<List<PointValueTimeModel>> getPointValues(
    		HttpServletRequest request, 
    		
    		@ApiParam(value = "Point xid", required = true, allowMultiple = false)
    		@PathVariable String xid,
    		
    		@ApiParam(value = "From time", required = false, allowMultiple = false)
    		@RequestParam(value="from", required=false, defaultValue="2014-08-10T00:00:00.000-10:00") //Not working yet: defaultValue="2014-08-01 00:00:00.000 -1000" )
    		//Not working yet@DateTimeFormat(pattern = "${rest.customDateInputFormat}") Date from,
    		@DateTimeFormat(iso=ISO.DATE_TIME) Date from,
    		
    		@ApiParam(value = "To time", required = false, allowMultiple = false)
			@RequestParam(value="to", required=false, defaultValue="2014-08-11T23:59:59.999-10:00")//Not working yet defaultValue="2014-08-11 23:59:59.999 -1000")
    		//Not working yet@DateTimeFormat(pattern = "${rest.customDateInputFormat}") Date to,
    		@DateTimeFormat(iso=ISO.DATE_TIME) Date to,
    		
    		@ApiParam(value = "Rollup type", required = false, allowMultiple = false)
			@RequestParam(value="rollup", required=false)
    		RollupEnum rollup,

    		@ApiParam(value = "Time Period Type", required = false, allowMultiple = false)
			@RequestParam(value="timePeriodType", required=false)
    		TimePeriodType timePeriodType,
    		
    		@ApiParam(value = "Time Periods", required = false, allowMultiple = false)
			@RequestParam(value="timePeriods", required=false)
    		Integer timePeriods    		
    		){
        
    	RestProcessResult<List<PointValueTimeModel>> result = new RestProcessResult<List<PointValueTimeModel>>(HttpStatus.OK);
    	User user = this.checkUser(request, result);
    	if(result.isOk()){
    	
	    	DataPointVO vo = DataPointDao.instance.getByXid(xid);
	    	if(vo == null){
	    		result.addRestMessage(getDoesNotExistMessage());
	    		return result.createResponseEntity();
	    	}

	    	try{
	    		if(Permissions.hasDataPointReadPermission(user, vo)){
	    			List<PointValueTimeModel> models;

	    			//Are we using rollup
	    			if(rollup != null){
	    				TimePeriod timePeriod = null;
	    				if((timePeriodType != null)&&(timePeriods != null)){
	    					timePeriod = new TimePeriod(timePeriods, timePeriodType);
	    				}
	    				PointValueRollupCalculator calc = new PointValueRollupCalculator(vo, rollup, timePeriod, from.getTime(), to.getTime());
	    				List<PointValueTime> pvts = calc.calculate();
	    				models = new ArrayList<PointValueTimeModel>(pvts.size());
	    				for(PointValueTime pvt : pvts){
	    					models.add(new PointValueTimeModel(pvt));
	    				}
	    			}else{
		    			List<PointValueTime> pvts = dao.getPointValuesBetween(vo.getId(), from.getTime(), to.getTime());
		    			models = new ArrayList<PointValueTimeModel>(pvts.size());
		    			for(PointValueTime pvt : pvts){
		    				models.add(new PointValueTimeModel(pvt));
		    			}
	    			}
	    			return result.createResponseEntity(models);
	    		}else{
	    	 		result.addRestMessage(getUnauthorizedMessage());
		    		return result.createResponseEntity();
		    		}
	    	}catch(PermissionException e){
	    		LOG.error(e.getMessage(), e);
	    		result.addRestMessage(getUnauthorizedMessage());
	    		return result.createResponseEntity();
	    	}
    	}else{
    		return result.createResponseEntity();
    	}
    }
    
    /**
     * Update a point value in the system
     * @param pvt
     * @param xid
     * @param builder
     * @return
     * @throws RestValidationFailedException 
     */
	@ApiOperation(
			value = "Updatae an existing data point's value",
			notes = "Data point must exist and be enabled"
			)
	@RequestMapping(method = RequestMethod.PUT, value = "/{xid}")
    public ResponseEntity<PointValueTimeModel> putPointValue(
    		HttpServletRequest request, 
    		@RequestBody PointValueTimeModel model, 
    		@PathVariable String xid, 
    		UriComponentsBuilder builder) throws RestValidationFailedException {
		
		RestProcessResult<PointValueTimeModel> result = new RestProcessResult<PointValueTimeModel>(HttpStatus.OK);
		final PointValueTime pvt = model.getData(); 
		User user = this.checkUser(request, result);
		if(result.isOk()){
		
	        DataPointVO existingDp = DataPointDao.instance.getByXid(xid);
	        if (existingDp == null) {
	        	result.addRestMessage(getDoesNotExistMessage());
	        	return result.createResponseEntity();
	    	}
	        
	    	try{
	    		if(Permissions.hasDataPointReadPermission(user, existingDp)){
	    			
	    			//Validate this
	    			model.validate(result);
	    			
	    			//TODO Do we want to use a provided time or let the RTM Decide the time?
	    	        final int dataSourceId = existingDp.getDataSourceId();
	    	        SetPointSource source = null;
	    	        if(pvt instanceof AnnotatedPointValueTime){
	    	        	source = new SetPointSource(){
	
	    					@Override
	    					public String getSetPointSourceType() {
	    						return "REST";
	    					}
	
	    					@Override
	    					public int getSetPointSourceId() {
	    						return dataSourceId;
	    					}
	
	    					@Override
	    					public TranslatableMessage getSetPointSourceMessage() {
	    						return ((AnnotatedPointValueTime)pvt).getSourceMessage();
	    					}
	
	    					@Override
	    					public void raiseRecursionFailureEvent() {
	    						//TODO Flesh this out
	    						LOG.error("Recursive failure while setting point via REST");
	    					}
	    	        		
	    	        	};
	    	        }
	    	        try{
	    	        	Common.runtimeManager.setDataPointValue(existingDp.getId(), pvt, source);

	    	        	URI location = builder.path("/rest/v1/pointValue/{xid}/{time}").buildAndExpand(xid, pvt.getTime()).toUri();
	    		    	result.addRestMessage(getResourceCreatedMessage(location));
	    		        return result.createResponseEntity(new PointValueTimeModel(pvt));
	
	    	        }catch(Exception e){
	    	        	LOG.error(e.getMessage(), e);
	    	        	result.addRestMessage(getInternalServerErrorMessage(e.getMessage()));
	    	        	return result.createResponseEntity();
	    	        	
	    	        }
	    			
	    			
	    		}else{
		    		result.addRestMessage(getUnauthorizedMessage());
		    		return result.createResponseEntity();
	    		}
	    	}catch(PermissionException e){
	    		LOG.error(e.getMessage(), e);
	    		result.addRestMessage(getUnauthorizedMessage());
	    		return result.createResponseEntity();
	    	}
		}else{
			return result.createResponseEntity();
		}
    }
    
	/**
	 * Get large amounts of point values by streaming them 
	 * back in the response.
	 * @param xid
	 * @param limit
	 * @return
	 */
	@ApiOperation(
			value = "Stream large amounts of point values",
			notes = "Useful when dumping a database",
			response=PointValueTimeModel.class,
			responseContainer="Array"
			
			)
	@ApiResponses({
		@ApiResponse(code = 200, message = "Ok", response=PointValueTimeModel.class),
		@ApiResponse(code = 401, message = "Unauthorized Access", response=ResponseEntity.class)
		})
    @RequestMapping(method = RequestMethod.GET, value="/{xid}/stream")
    public void streamPointValues(
    		HttpServletRequest request,
    		HttpServletResponse response,
    		
    		@ApiParam(value = "Point xid", required = true, allowMultiple = false)
    		@PathVariable String xid,
    		
    		@ApiParam(value = "From time", required = false, allowMultiple = false)
    		@RequestParam(value="from", required=false, defaultValue="2014-08-10T00:00:00.000-10:00") //Not working yet: defaultValue="2014-08-01 00:00:00.000 -1000" )
    		//Not working yet@DateTimeFormat(pattern = "${rest.customDateInputFormat}") Date from,
    		@DateTimeFormat(iso=ISO.DATE_TIME) Date from,
    		
    		@ApiParam(value = "To time", required = false, allowMultiple = false)
			@RequestParam(value="to", required=false, defaultValue="2014-08-11T23:59:59.999-10:00")//Not working yet defaultValue="2014-08-11 23:59:59.999 -1000")
    		//Not working yet@DateTimeFormat(pattern = "${rest.customDateInputFormat}") Date to,
    		@DateTimeFormat(iso=ISO.DATE_TIME) Date to){
		
    	RestProcessResult<List<PointValueTimeModel>> result = new RestProcessResult<List<PointValueTimeModel>>(HttpStatus.OK);
    	User user = this.checkUser(request, result);
    	if(result.isOk()){
    	
	    	DataPointVO vo = DataPointDao.instance.getByXid(xid);
	    	if(vo == null){
	    		result.addRestMessage(getDoesNotExistMessage());
	    		//return result.createResponseEntity();
	    	}

	    	try{
	    		if(Permissions.hasDataPointReadPermission(user, vo)){
	    			
	    			try {
	    				final ServletServerHttpResponse outputMessage = new ServletServerHttpResponse(response);
	    				final ObjectMapper objectMapper = MangoRestSpringConfiguration.objectMapper;
	    				
	    				JsonEncoding encoding = getJsonEncoding(outputMessage.getHeaders().getContentType());
	    				// The following has been deprecated as late as Jackson 2.2 (April 2013);
	    				// preserved for the time being, for Jackson 2.0/2.1 compatibility.
	    				@SuppressWarnings("deprecation")
	    				final JsonGenerator jsonGenerator =
	    						objectMapper.getJsonFactory().createJsonGenerator(outputMessage.getBody(), encoding);

	    				// A workaround for JsonGenerators not applying serialization features
	    				// https://github.com/FasterXML/jackson-databind/issues/12
	    				if (objectMapper.isEnabled(SerializationFeature.INDENT_OUTPUT)) {
	    					jsonGenerator.useDefaultPrettyPrinter();
	    				}
	    				
	    				jsonGenerator.writeStartArray();
						dao.getPointValuesBetween(vo.getId(), from.getTime(), to.getTime(), new MappedRowCallback<PointValueTime>(){

							@Override
							public void row(PointValueTime pvt, int index) {
			    				try {
			    					objectMapper.writeValue(jsonGenerator, new PointValueTimeModel(pvt));
			    				}
			    				catch (JsonProcessingException ex) {
			    					throw new HttpMessageNotWritableException("Could not write JSON: " + ex.getMessage(), ex);
			    				} catch (IOException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
							}
							
						});
						
						jsonGenerator.writeEndArray();
						
						outputMessage.close();
					} catch (IOException e1) {
						LOG.error(e1);
					}
	    			//return result.createResponseEntity(models);
	    		}else{
	    	 		result.addRestMessage(getUnauthorizedMessage());
	    	 		//return result.createResponseEntity();
		    	}
	    	}catch(PermissionException e){
	    		LOG.error(e.getMessage(), e);
	    		result.addRestMessage(getUnauthorizedMessage());
	    		//return result.createResponseEntity();
	    	}
    	}else{
    		//return result.createResponseEntity();
    	}
    }
	
	/**
	 * Determine the JSON encoding to use for the given content type.
	 * @param contentType the media type as requested by the caller
	 * @return the JSON encoding to use (never {@code null})
	 */
	protected JsonEncoding getJsonEncoding(MediaType contentType) {
		if (contentType != null && contentType.getCharSet() != null) {
			Charset charset = contentType.getCharSet();
			for (JsonEncoding encoding : JsonEncoding.values()) {
				if (charset.name().equals(encoding.getJavaName())) {
					return encoding;
				}
			}
		}
		return JsonEncoding.UTF8;
	}

	
	
}
