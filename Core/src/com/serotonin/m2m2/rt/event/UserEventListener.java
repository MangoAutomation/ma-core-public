/**
 * Copyright (C) 2015 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.rt.event;

/**
 * @author Terry Packer
 *
 */
public interface UserEventListener {

	/**
	 * Return the User ID to be used to filter events for
	 * 
	 * @return
	 */
	int getUserId();

	/**
	 * New Event was raised
	 * 
	 * @param evt
	 */
	void raised(EventInstance evt);

	/**
	 * @param evt
	 */
	void returnToNormal(EventInstance evt);

	/**
	 * @param evt
	 */
	void deactivated(EventInstance evt);

}
