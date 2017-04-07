/**
 * Copyright (C) 2016 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.timer;



/**
 * 
 * Class that ensures only OrderedTimerTaskWorkers are sent into the OrderedThreadPoolExecutor
 * @author Terry Packer
 *
 */
class OrderedTimerThread extends TimerThread{
 
	private final OrderedThreadPoolExecutor executorService;

    OrderedTimerThread(TaskQueue queue, OrderedThreadPoolExecutor executorService, TimeSource timeSource) {
        super(queue, executorService, timeSource);
        this.executorService = executorService;
    }

    @Override
    void executeTask(TaskWrapper task) {
    	executorService.execute(task);
    }
    
    @Override
    void execute(TaskWrapper task){
    	executorService.execute(task);
    }
}
