/**
 * Copyright (C) 2015 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.web.mvc.rest.v1.model.email;

import java.util.Set;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.serotonin.m2m2.vo.mailingList.MailingList;

/**
 * @author Terry Packer
 *
 */
public class MailingListModel extends EmailRecipientModel<MailingList>{

	public MailingListModel() {
		super(new MailingList());
	}
	
	/**
	 * @param data
	 */
	public MailingListModel(MailingList data) {
		super(data);
	}

	@JsonGetter("id")
	public int getId(){
		return this.data.getId();
	}
	@JsonSetter("id")
	public void setId(int id){
		this.data.setId(id);
	}
	
	@JsonGetter("xid")
	public String getXid(){
		return this.data.getXid();
	}
	@JsonSetter("xid")
	public void setXid(String xid){
		this.data.setXid(xid);
	}

	@JsonGetter("name")
	public String getName(){
		return this.data.getName();
	}
	@JsonSetter("name")
	public void setName(String name){
		this.data.setName(name);
	}
	
    /**
     * Integers that are present in the inactive intervals set are times at which the mailing list schedule is not to be
     * sent to. Intervals are split into 15 minutes, starting at [00:00 to 00:15) on Monday. Thus, there are 4 * 24 * 7
     * = 672 individual periods.
     */
	@JsonGetter("inactiveIntervals")
	public Set<Integer> getInactiveIntervals(){
		return this.data.getInactiveIntervals();
	}
	@JsonSetter("inactiveIntervals")
	public void setInactiveIntervals(Set<Integer> intervals){
		this.data.setInactiveIntervals(intervals);
	}	
	
}
