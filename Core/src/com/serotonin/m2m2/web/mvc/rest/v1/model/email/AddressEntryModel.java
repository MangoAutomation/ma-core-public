/**
 * Copyright (C) 2015 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.web.mvc.rest.v1.model.email;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.serotonin.m2m2.vo.mailingList.AddressEntry;

/**
 * @author Terry Packer
 *
 */
public class AddressEntryModel extends EmailRecipientModel<AddressEntry>{

	public AddressEntryModel() {
		super(new AddressEntry());
	}	
	/**
	 * @param data
	 */
	public AddressEntryModel(AddressEntry data) {
		super(data);
	}
	
	@JsonGetter("address")
	public String getAddress(){
		return this.data.getAddress();
	}
	@JsonSetter("address")
	public void setAddress(String address){
		this.data.setAddress(address);
	}
}
