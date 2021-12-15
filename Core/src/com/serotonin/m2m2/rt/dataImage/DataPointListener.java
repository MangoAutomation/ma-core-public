/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.rt.dataImage;

import java.util.Map;

import com.serotonin.m2m2.vo.DataPointVO;

public interface DataPointListener {
	
	/**
	 * For use in tracking who is listening to whom
	 */
	String getListenerName();
	
    /**
     * This method is called for all existing listeners when the point is initialized. Note that it is not called for
     * listeners that have begun listening to an already-initialized point.
     */
    void pointInitialized();

    /**
     * This method is called every time the point gets a value from somewhere (except for its initialization). This
     * includes when the point changes or is set.
     *
     */
    void pointUpdated(PointValueTime newValue);

    /**
     * This method is called every time the point gets a value that is different from its previous value (except for its
     * initialization). This includes when the point is set, if the set changes the point's value.
     *
     */
    void pointChanged(PointValueTime oldValue, PointValueTime newValue);

    /**
     * This method is called every time the point gets set, whether the value changes or not.
     *
     */
    void pointSet(PointValueTime oldValue, PointValueTime newValue);

    /**
     * This method is called when a backdated value is received.
     *
     */
    void pointBackdated(PointValueTime value);

    /**
     * This method is called when the point has been terminated, allowing listeners to react as necessary.
     */
    void pointTerminated(DataPointVO vo);
    
    /**
     * This method is called when the value is sent to the database for saving.  There is no guarantee that 
     * it has made it to the database if an error occurs at a lower level, but all efforts are made to save it.
     */
    void pointLogged(PointValueTime value);
    
    /**
     * Called when any attribute on a data source has changed, all attributes after the change are supplied to this callback
     */
    default void attributeChanged(Map<String, Object> attributes) { }
}
