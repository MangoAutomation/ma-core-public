/**
 * Copyright (C) 2015 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.web.mvc.rest.v1;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import net.jazdw.rql.parser.ASTNode;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import com.infiniteautomation.mango.db.query.QueryAttribute;
import com.infiniteautomation.mango.db.query.TableModel;
import com.infiniteautomation.mango.db.query.appender.SQLColumnQueryAppender;
import com.serotonin.m2m2.db.dao.AbstractBasicDao;
import com.serotonin.m2m2.web.mvc.rest.v1.message.RestProcessResult;
import com.serotonin.m2m2.web.mvc.rest.v1.model.PageQueryStream;
import com.serotonin.m2m2.web.mvc.rest.v1.model.QueryStream;
import com.serotonin.m2m2.web.mvc.rest.v1.model.VoStreamCallback;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiResponse;
import com.wordnik.swagger.annotations.ApiResponses;

/**
 * @author Terry Packer
 *
 */
public abstract class MangoVoRestController<VO, MODEL, DAO extends AbstractBasicDao<VO>> extends MangoRestController{

	protected DAO dao;
	protected VoStreamCallback<VO, MODEL, DAO> callback;
	
	//Map of keys -> model members to value -> Vo member/sql column
	protected Map<String,String> modelMap;
	//Map of Vo member/sql column to value converter
	protected Map<String, SQLColumnQueryAppender> appenders;

	/**
	 * Construct a Controller using the default callback
	 * @param dao
	 */
	public MangoVoRestController(DAO dao){
		this.dao = dao;
		this.callback = new VoStreamCallback<VO, MODEL, DAO>(this);
		this.modelMap = new HashMap<String,String>();
		this.appenders = new HashMap<String, SQLColumnQueryAppender>();
	}

	/**
	 * Construct a Controller
	 * @param dao
	 * @param callback
	 */
	public MangoVoRestController(DAO dao, VoStreamCallback<VO, MODEL, DAO> callback){
		this.dao = dao;
		this.callback = callback;
		this.modelMap = new HashMap<String,String>();
		this.appenders = new HashMap<String, SQLColumnQueryAppender>();
	}
	
	/**
	 * Get the Query Stream for Streaming an array of data
	 * @param query
	 * @return
	 */
	protected QueryStream<VO, MODEL, DAO> getStream(ASTNode root){
		return this.getStream(root, this.callback);
	}
	
	/**
	 * Get the Query Stream for Streaming an array of data
	 * @param query
	 * @return
	 */
	protected QueryStream<VO, MODEL, DAO> getStream(ASTNode root, VoStreamCallback<VO, MODEL, DAO> callback){

		QueryStream<VO, MODEL, DAO> stream = new QueryStream<VO, MODEL, DAO>(dao, this, root, callback);
		//Ensure its ready
		stream.setupQuery();
		return stream;
	}
	
	/**
	 * Get a Stream that is more like a result set with a count
	 * @param query
	 * @return
	 */
	protected PageQueryStream<VO, MODEL, DAO> getPageStream(ASTNode root){
		return getPageStream(root, this.callback);
	}

	/**
	 * Get a Stream that is more like a result set with a count
	 * @param query
	 * @return
	 */
	protected PageQueryStream<VO, MODEL, DAO> getPageStream(ASTNode node, VoStreamCallback<VO, MODEL, DAO> callback){
		PageQueryStream<VO, MODEL, DAO> stream = new PageQueryStream<VO, MODEL, DAO>(dao, this, node, callback);
		//Ensure its ready
		stream.setupQuery();
		return stream;
	}
	
	
	@ApiOperation(
			value = "Get Explaination For Query",
			notes = "What is Query-able on this model"
			)
	@ApiResponses(value = { 
	@ApiResponse(code = 200, message = "Ok"),
	@ApiResponse(code = 403, message = "User does not have access")
	})
	@RequestMapping(method = RequestMethod.GET, produces={"application/json"}, value = "/explain-query")
    public ResponseEntity<TableModel> getTableModel(HttpServletRequest request) {
        
        RestProcessResult<TableModel> result = new RestProcessResult<TableModel>(HttpStatus.OK);
        
        this.checkUser(request, result);
        if(result.isOk()){
 	        result.addRestMessage(getSuccessMessage());
	        return result.createResponseEntity(this.getQueryAttributeModel());
        }
        
        return result.createResponseEntity();
    }
	
	/**
	 * Get the Table Model
	 * @return
	 */
	protected TableModel getQueryAttributeModel(){
		TableModel model = this.dao.getTableModel();
	
		
		//Add in our mappings
		Iterator<String> it = this.modelMap.keySet().iterator();
		while(it.hasNext()){
			String modelMember = it.next();
			String mappedTo = this.modelMap.get(modelMember);
			for(QueryAttribute attribute : model.getAttributes()){
				if(attribute.getColumnName().equals(mappedTo)){
					attribute.addAlias(modelMember);
				}
			}
		}
		return model;
	}
	
	
	/**
	 * Map any Model members to VO Properties
	 * @param list
	 */
	public Map<String,String> getModelMap(){
		return this.modelMap;
	}
	
	public Map<String, SQLColumnQueryAppender> getAppenders(){
		return this.appenders;
	}
	
	/**
	 * Create a Model
	 * @param vo
	 * @return
	 */
	public abstract MODEL createModel(VO vo);
	
}
