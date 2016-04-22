/**
 * Copyright (C) 2015 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.web.mvc.rest.v1.model.comment;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.serotonin.m2m2.vo.UserComment;
import com.serotonin.m2m2.vo.comment.UserCommentVO;
import com.serotonin.m2m2.web.mvc.rest.v1.model.AbstractVoModel;

/**
 * This class should really JSON ignore xid and name properties
 * 
 * @author Terry Packer
 *
 */
public class UserCommentModel extends AbstractVoModel<UserCommentVO>{

	
	/**
	 * @param data
	 */
	public UserCommentModel(UserCommentVO comment) {
		super(comment);
	}

	public UserCommentModel(){
		super(new UserCommentVO());
	}
	
	public UserCommentModel(UserComment comment){
		super(new UserCommentVO());
		this.data.setUserId(comment.getUserId());
		this.data.setUsername(comment.getUsername());
		this.data.setTs(comment.getTs());
		this.data.setComment(comment.getComment());		
	}
	
	@JsonGetter
	public int getUserId(){
		return this.data.getUserId();
	}
	@JsonSetter
	public void setUserId(int userId){
		this.data.setUserId(userId);
	}
	
	@JsonGetter
	public String getUsername(){
		return this.data.getUsername();
	}
	@JsonSetter
	public void setUsername(String username){
		this.data.setUsername(username);
	}

	@JsonGetter
	public String getComment(){
		return this.data.getComment();
	}
	@JsonSetter
	public void setComment(String comment){
		this.data.setComment(comment);
	}
	
	@JsonGetter
	public long getTimestamp(){
		return this.data.getTs();
	}
	@JsonSetter
	public void setTimestamp(long timestamp){
		this.data.setTs(timestamp);
	}
	
	@JsonGetter
	public String getCommentType(){
		return UserCommentVO.COMMENT_TYPE_CODES.getCode(this.data.getCommentType());
	}
	@JsonSetter
	public void setCommentType(String type){
		this.data.setCommentType(UserCommentVO.COMMENT_TYPE_CODES.getId(type));
	}

	@JsonGetter
	public int getReferenceId(){
		return this.data.getReferenceId();
	}
	@JsonSetter
	public void setReferenceId(int id){
		this.data.setReferenceId(id);
	}
	
	/**
	 * @return
	 */
	@JsonIgnore
	public UserComment getDataAsComment() {
		UserComment comment = new UserComment();
		comment.setUserId(this.data.getUserId());
		comment.setUsername(this.data.getUsername());
		comment.setTs(this.data.getTs());
		comment.setComment(this.data.getComment());
		return comment;
	}
	

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.web.mvc.rest.v1.model.AbstractVoModel#getModelType()
	 */
	@Override
	public String getModelType() {
		return null;
	}
}
