/**
 * Copyright (C) 2019  Infinite Automation Software. All rights reserved.
 */
package com.infiniteautomation.mango.spring.components.executors;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinPool.ForkJoinWorkerThreadFactory;
import java.util.concurrent.ForkJoinWorkerThread;

import com.serotonin.m2m2.Common;

/**
 * Ensure the Mango Module class loader is available to all ForkJoinWorker Threads
 * 
 * @author Terry Packer
 *
 */
public class MangoForkJoinWorkerThreadFactory implements ForkJoinWorkerThreadFactory {

    
    @Override
    public ForkJoinWorkerThread newThread(ForkJoinPool pool) {
        return new MangoForkJoinWorkerThread(Common.getRuntimeContext().getClassLoader(), pool);
    }
    
    /**
     * Thread that will set its context class loader prior to execution
     * @author Terry Packer
     *
     */
    class MangoForkJoinWorkerThread extends ForkJoinWorkerThread {

        protected MangoForkJoinWorkerThread(ClassLoader classLoader, ForkJoinPool pool) {
            super(pool);
            this.setContextClassLoader(classLoader);
        }
        
    }
    
}
