/**
 * Copyright (C) 2016 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.timer;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
* This Executor warrants task ordering for tasks with same id.
* 
* Default ID for TimerTasks is hashCode()
* 
* Generally periodic tasks will be scheduled and run as the same object, meaning that only one of them 
* can run at any given time and the first must finish before the next can run.  Single run tasks will always be 
* different instances if they are created when they are executed, so multiple instances can run at the same time.
* 
* The optional defaultQueueSize parameter will allow multiple tasks with the same ID to be queued up and run in order.  The default queue size of 0 
* indicates that no tasks can be queued and that tasks submitted while another is running will be rejected with RejectedTaskReason.TASK_QUEUE_FULL
* 
* The flushFullQueue parameter can be used to force a queue to flush its pending tasks and replace them with the incoming task.  This is useful 
* if pending tasks are deemed out-dated and become irrelevant where by running the most recently scheduled task is preferred to executing all old stale tasks.
* 
* Note that every queue will be removed once it is empty.  
* 
*/
public class OrderedThreadPoolExecutor extends ThreadPoolExecutor implements RejectedExecutionHandler{

	//Task to queue map
	private final Map<String, LimitedTaskQueue> keyedTasks = new HashMap<String, LimitedTaskQueue>();

	private boolean flushFullQueue;
	private RejectedExecutionHandler handler;
	
    /**
	 * @param corePoolSize
	 * @param maximumPoolSize
	 * @param keepAliveTime
	 * @param unit
	 * @param workQueue
	 * @param handler
	 */
	public OrderedThreadPoolExecutor(int corePoolSize, int maximumPoolSize,
			long keepAliveTime, TimeUnit unit,
			BlockingQueue<Runnable> workQueue, RejectedExecutionHandler handler) {
		super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue);
		this.flushFullQueue = false;
		super.setRejectedExecutionHandler(this);
		this.handler = handler;
	}

	/**
	 * @param corePoolSize
	 * @param maximumPoolSize
	 * @param keepAliveTime
	 * @param unit
	 * @param workQueue
	 * @param threadFactory
	 * @param handler
	 */
	public OrderedThreadPoolExecutor(int corePoolSize, int maximumPoolSize,
			long keepAliveTime, TimeUnit unit,
			BlockingQueue<Runnable> workQueue, ThreadFactory threadFactory,
			RejectedExecutionHandler handler) {
		super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue,
				threadFactory);
		super.setRejectedExecutionHandler(this);
		this.handler = handler;
		this.flushFullQueue = false;
	}

	/**
	 * @param corePoolSize
	 * @param maximumPoolSize
	 * @param keepAliveTime
	 * @param unit
	 * @param workQueue
	 * @param threadFactory
	 */
	public OrderedThreadPoolExecutor(int corePoolSize, int maximumPoolSize,
			long keepAliveTime, TimeUnit unit,
			BlockingQueue<Runnable> workQueue, ThreadFactory threadFactory) {
		super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue,
				threadFactory);
		this.flushFullQueue = false;
	}

	/**
	 * 
	 * Overloaded constructor to allow tuning the task queues
	 * 
	 * @param corePoolSize
	 * @param maximumPoolSize
	 * @param keepAliveTime
	 * @param unit
	 * @param workQueue
	 * @param threadFactory
	 * @param defaultQueueSize
	 * @param flushFullQueue
	 */
	public OrderedThreadPoolExecutor(int corePoolSize, int maximumPoolSize,
			long keepAliveTime, TimeUnit unit,
			BlockingQueue<Runnable> workQueue, 
			ThreadFactory threadFactory, 
			RejectedExecutionHandler handler,
			boolean flushFullQueue) {
		super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue,
				threadFactory);
		this.flushFullQueue = flushFullQueue;
		super.setRejectedExecutionHandler(this);
		this.handler = handler;
	}
	
	/**
	 * @param corePoolSize
	 * @param maximumPoolSize
	 * @param keepAliveTime
	 * @param unit
	 * @param workQueue
	 */
	public OrderedThreadPoolExecutor(int corePoolSize, int maximumPoolSize,
			long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue, boolean flushFullQueue) {
		super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue);
		this.flushFullQueue = false;
	}

	/* (non-Javadoc)
	 * @see java.util.concurrent.ThreadPoolExecutor#setRejectedExecutionHandler(java.util.concurrent.RejectedExecutionHandler)
	 */
	@Override
	public void setRejectedExecutionHandler(RejectedExecutionHandler handler) {
		this.handler = handler;
	}
	
	/**
	 * Execute a task that should may be ordered.  Tasks with null ID are run immediately and
	 * potentially in parallel to other tasks of the same type.
	 * @param worker
	 * @param key
	 * @param executionTime for the worker
	 */
	public void execute(TaskWrapper worker) {

		//No ordering ie, the ID is null
		if(worker.task.id == null){
			execute((Runnable)worker);
			return;
		}
		
        boolean first;
        OrderedTaskCollection wrappedTask;
        LimitedTaskQueue dependencyQueue = null;
        
        synchronized (keyedTasks){
        	dependencyQueue = keyedTasks.get(worker.task.id);
            first = (dependencyQueue == null);
            if (dependencyQueue == null){
            	OrderedTaskInfo info = new OrderedTaskInfo(worker.task);
            	if(flushFullQueue)
            		dependencyQueue = new TimePriorityLimitedTaskQueue(info);
            	else
            		dependencyQueue = new LimitedTaskQueue(info);
                keyedTasks.put(worker.task.id, dependencyQueue);
            }

            wrappedTask = wrap(worker, dependencyQueue);
            //Either add or reject 
            if(!first)
            	if(!dependencyQueue.add(wrappedTask, this))
            		return; //Was rejected so nothing to do
        }

        // execute and reject methods can block, call them outside synchronize block
        if (first){
            execute(wrappedTask);
	        // process rejected tasks if we are the first task as 
        	// we have processed/rejected all dependent tasks 
            OrderedTaskCollection t = dependencyQueue.getRejectedTasks().poll();
	    	while(t != null){
	    		t.rejected(this);
	    		t = dependencyQueue.getRejectedTasks().poll();
	    	}
        }
    }

	/** 
	 * We need to ensure we remove the keyed tasks if we get rejected
	 * 
	 * @see java.util.concurrent.RejectedExecutionHandler#rejectedExecution(java.lang.Runnable, java.util.concurrent.ThreadPoolExecutor)
	 */
	@Override
	public void rejectedExecution(Runnable r, ThreadPoolExecutor e) {
		
		if(r instanceof OrderedTaskCollection){
			OrderedTaskCollection t  = (OrderedTaskCollection)r;
			synchronized (keyedTasks){
				//Don't bother trying to run the queue we've got a problem
				if (t.dependencyQueue.isEmpty()){
                    keyedTasks.remove(t.wrapper.task.id);
                }else{
                	//Could be trouble, but let it fail if it must
                    //execute(t.dependencyQueue.poll());
                }
			}
			this.handler.rejectedExecution(t.wrapper, e);
			
		}else{
			if(this.handler != null){
				//Pass it along
				this.handler.rejectedExecution(r, e);
			}else{
				//Default Behavior
				throw new RejectedExecutionException("Task " + r.toString() +
	                    " rejected from " +
	                    e.toString());
			}
		}
	}
	
	/**
	 * Update the default queue size for a given taskId
	 * @param newSize
	 * @param taskId
	 */
	public void updateDefaultQueueSize(int newSize, String taskId){
		LimitedTaskQueue dependencyQueue = keyedTasks.get(taskId);
        if (dependencyQueue != null){
        	dependencyQueue.setLimit(newSize);
        }
	}
	
	/**
	 * Test to see if a queue for this task id exists
	 * @param taskId
	 * @return
	 */
	public boolean queueExists(Object taskId){
		synchronized (keyedTasks){
			return keyedTasks.containsKey(taskId);
		}
	}
	
    private OrderedTaskCollection wrap(TaskWrapper task, LimitedTaskQueue dependencyQueue) {
        return new OrderedTaskCollection(task, dependencyQueue);
    }

    /**
     * Class that ensures tasks of the same type are run in order
     * 
     * When all tasks are run and removed from dependencyQueue 
     * the entire queue is removed from the keyed tasks map
     *
     */
    class OrderedTaskCollection implements Runnable{

        private final LimitedTaskQueue dependencyQueue;
        private final TaskWrapper wrapper;
        private int rejectedReason;

        public OrderedTaskCollection(TaskWrapper task, LimitedTaskQueue dependencyQueue) {
            this.wrapper = task;
            this.dependencyQueue = dependencyQueue;
        }

        @Override
        public void run() {
        	long start = System.currentTimeMillis();
            try{
            	this.wrapper.run();
            } finally {
                Runnable nextTask = null;
                synchronized (keyedTasks){
                    if (this.dependencyQueue.isEmpty()){
                    	keyedTasks.remove(wrapper.task.id);
                    }else{
                        nextTask = this.dependencyQueue.poll();
                    }
                }
                // Update our task info
                this.dependencyQueue.info.addExecutionTime(System.currentTimeMillis() - start);
                this.dependencyQueue.info.updateCurrentQueueSize(this.dependencyQueue.size());
                if (nextTask!=null){
                    execute(nextTask);
                }
            }
        }
        
        public void setRejectedReason(int reason){
        	this.rejectedReason = reason;
        }
        
        public void rejected(Executor e){
        	this.dependencyQueue.info.rejections++;
        	this.wrapper.task.rejected(new RejectedTaskReason(this.rejectedReason, wrapper.executionTime, wrapper.task, e));
        }
        
        @Override
        public String toString(){
        	return this.wrapper.task.toString();
        }
    }
    
    /**
     * Class to hold a limited size queue and reject incoming tasks 
     * if the limit is reached false is returned from the add method.
     * 
     * This class ensures that all submitted tasks that are not rejected 
     * will run.  Which is different to TimePriorityLimitedTaskQueue where incoming tasks to a full queue
     * cause the queue to be cleared and the incoming task to take precedence.
     * 
     * @author Terry Packer
     *
     */
    class LimitedTaskQueue extends ArrayDeque<OrderedTaskCollection>{

		private static final long serialVersionUID = 1L;
		protected int limit;
		protected ArrayDeque<OrderedTaskCollection> rejectedTasks;
		protected final OrderedTaskInfo info;
		
		public LimitedTaskQueue(OrderedTaskInfo info){
			super();
			this.limit = info.queueSizeLimit;
			this.info = info;
			this.rejectedTasks = new ArrayDeque<OrderedTaskCollection>(limit);
		}
		
		/*
		 * (non-Javadoc)
		 * @see java.util.ArrayDeque#add(java.lang.Object)
		 */
		public boolean add(OrderedTaskCollection c, Executor ex) {
			//Add task to end of queue if there is room
			if(this.size() < limit){
				boolean result = super.add(c);
				info.updateCurrentQueueSize(this.size());
				return result;
			}else{
				if(limit > 0){
					c.setRejectedReason(RejectedTaskReason.TASK_QUEUE_FULL);
					c.rejected(ex);
				}else{
					c.setRejectedReason(RejectedTaskReason.CURRENTLY_RUNNING);
					c.rejected(ex);
				}
				this.rejectedTasks.add(c);
				return false;
			}
		}
		
		/**
		 * Useful to get the rejected tasks outside of the sync block where they are added
		 * @return
		 */
		public Queue<OrderedTaskCollection> getRejectedTasks(){
			return this.rejectedTasks;
		}
		
		public void setLimit(int limit){
			this.limit = limit;
		}
    }
    
    /**
     * Class to hold a limited size queue and replace entire queue with incoming task if the queue is full.
     * 
     * This class gives priority to a new task when the queue is full and is ideally used with a size 1 queue 
     * so that the most recent task will execute next.
     * 
     * @author Terry Packer
     *
     */
    class TimePriorityLimitedTaskQueue extends LimitedTaskQueue{

		private static final long serialVersionUID = 1L;
		
		public TimePriorityLimitedTaskQueue(OrderedTaskInfo info){
			super(info);
		}
		
		/* (non-Javadoc)
		 * @see java.util.ArrayDeque#add(java.lang.Object)
		 */
		@Override
		public boolean add(OrderedTaskCollection c, Executor ex) {
			//Add task to end of queue if there is room
			if(this.size() < limit){
				boolean result = super.add(c);
				info.updateCurrentQueueSize(this.size());
				return result;
			}else{
				while(this.size() >= limit){
					OrderedTaskCollection t = this.poll();
					if(limit > 0){
						t.setRejectedReason(RejectedTaskReason.TASK_QUEUE_FULL);
						t.rejected(ex);
					}else{
						t.setRejectedReason(RejectedTaskReason.CURRENTLY_RUNNING);
						t.rejected(ex);
					}
					this.rejectedTasks.add(t);
				}
				/* Now add the task */
				boolean result = super.add(c);
				info.updateCurrentQueueSize(this.size());
				return result;
			}
		}
		
		public void setLimit(int limit){
			this.limit = limit;
		}
    }

	/**
	 * Get information on all tasks running in the ordered queue
	 * 
	 * @return
	 */
	public List<OrderedTaskInfo> getOrderedQueueInfo() {
		//TODO Speed this up
		List<OrderedTaskInfo> stats = new ArrayList<OrderedTaskInfo>(keyedTasks.size());
		synchronized(keyedTasks){
			Iterator<String> keys = keyedTasks.keySet().iterator();
			while(keys.hasNext()){
				String key = keys.next();
				LimitedTaskQueue queue = keyedTasks.get(key);
				stats.add(queue.info);
			}
		}
		return stats;
	}
    
}