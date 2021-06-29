/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
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
	 * Event was raised due to some alarm condition
	 * 
	 * @param evt
	 */
	void raised(EventInstance evt);

	/**
	 * Event was returned to normal because its cause is no longer active
	 * 
	 * @param evt
	 */
	void returnToNormal(EventInstance evt);

	/**
	 * When a data point, data source or publisher is deleted or terminated all its events are deactivated
	 * @param evt
	 */
	void deactivated(EventInstance evt);

	/**
	 * Event was acknowledged by a user
	 * @param event
	 */
	void acknowledged(EventInstance evt);

}
