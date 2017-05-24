/*
    Copyright (C) 2006-2007 Serotonin Software Technologies Inc.

    This program is free software; you can redistribute it and/or modify
    it under the terms of version 2 of the GNU General Public License as 
    published by the Free Software Foundation.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program; if not, write to the Free Software Foundation, Inc.,
    51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA

    @author Matthew Lohbihler
 */
package com.serotonin.util;

public interface ILifecycle {
	/**
	 * Initialize only if not in safe mode
	 * @param safe
	 * @throws LifecycleException
	 */
    void initialize(boolean safe) throws LifecycleException;

    /**
     * Begin the termination process
     * @throws LifecycleException
     */
    void terminate() throws LifecycleException;
    
    /**
     * Wait for the task to terminate gracefully for some time 
     * and then kill forcefully
     */
    void joinTermination();
    
    //States for state machines
    public static final int NOT_STARTED = 0;
    public static final int PRE_INITIALIZE = 10;
    public static final int INITIALIZE = 20;
    public static final int RUNNING = 30;
    public static final int TERMINATE = 40;
    public static final int POST_TERMINATE = 50;
    public static final int TERMINATED = 60;
}
