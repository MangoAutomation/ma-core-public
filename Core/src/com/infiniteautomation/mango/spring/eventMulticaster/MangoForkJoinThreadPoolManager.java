/**
 * Copyright (C) 2019  Infinite Automation Software. All rights reserved.
 */
package com.infiniteautomation.mango.spring.eventMulticaster;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinPool.ForkJoinWorkerThreadFactory;
import java.util.concurrent.ForkJoinWorkerThread;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

/**
 * Manage our own ForkJoinThreadPool to set our module class loader on its threads
 * 
 * @author Terry Packer
 *
 */
@Component
public class MangoForkJoinThreadPoolManager {
    // max #workers - 1
    static final int MAX_CAP = 0x7fff;
    private final ForkJoinPool pool;
    
    @Autowired
    public MangoForkJoinThreadPoolManager(ApplicationContext context) {
        //We need our threads to have access to the class loader that contains our module loaded classes,
        //  so we set the class loader for any threads that are created.  The parameters for this constructor
        //  were lifted from ForkJoinPool.makeCommonPool().  Note that we are not catering for a Security Manager
        //  by using this code as the ForkJoinPool.makeCommonPool() does.
        int parallelism = -1;
        if (parallelism < 0 && // default 1 less than #cores
            (parallelism = Runtime.getRuntime().availableProcessors() - 1) <= 0)
            parallelism = 1;
        if (parallelism > MAX_CAP)
            parallelism = MAX_CAP;
        this.pool = new ForkJoinPool(parallelism,
                new ForkJoinWorkerThreadFactory() {
                    @Override
                    public ForkJoinWorkerThread newThread(ForkJoinPool pool) {
                        return new MangoForkJoinWorkerThread(context.getClassLoader(), pool);
                    }
                }, null, false);
    }
    
    public ForkJoinPool getPool() {
        return pool;
    }

    
    class MangoForkJoinWorkerThread extends ForkJoinWorkerThread {

        protected MangoForkJoinWorkerThread(ClassLoader classLoader, ForkJoinPool pool) {
            super(pool);
            this.setContextClassLoader(classLoader);
        }
        
    }
}
