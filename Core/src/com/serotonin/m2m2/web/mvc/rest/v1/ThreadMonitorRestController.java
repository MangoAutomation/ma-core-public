/**
 * Copyright (C) 2014 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.web.mvc.rest.v1;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.serotonin.m2m2.web.mvc.rest.v1.message.RestProcessResult;
import com.serotonin.m2m2.web.mvc.rest.v1.model.ThreadModel;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiParam;

/**
 * @author Terry Packer
 *
 */
@Api(value="Threads", description="Operations on Threads")
@RestController
@RequestMapping("/v1/threads")
public class ThreadMonitorRestController extends MangoRestController<ThreadModel> {

	private final ThreadGroup root; //The root group, always will be there
	
	public ThreadMonitorRestController(){
		
		ThreadGroup tg = Thread.currentThread( ).getThreadGroup( );
	    ThreadGroup ptg;
	    while ( (ptg = tg.getParent( )) != null )
	        tg = ptg;
	    this.root = tg;
	}
	
	@RequestMapping(method = RequestMethod.GET)
	public ResponseEntity<List<ThreadModel>> getThreads(HttpServletRequest request,
			@ApiParam(value = "Limit size of stack trace", allowMultiple = false, defaultValue="10")
			@RequestParam(value="stackDepth", defaultValue="10") int stackDepth
			){
		
		RestProcessResult<List<ThreadModel>> result = new RestProcessResult<List<ThreadModel>>(HttpStatus.OK);
		this.checkUser(request, result);
		
    	if(result.isOk()){
    		
    		List<ThreadModel> models = new ArrayList<ThreadModel>();
			
    		Thread[] allThreads = this.getAllThreads();
			ThreadMXBean manager = ManagementFactory.getThreadMXBean();
			for(Thread t : allThreads){
				ThreadInfo info = manager.getThreadInfo(t.getId(), stackDepth);
				ThreadModel model = new ThreadModel(info, t);
				models.add(model);
			}
			return result.createResponseEntity(models);
    	}
    	return result.createResponseEntity();
	}
		
	private Thread[] getAllThreads( ) {
	    final ThreadMXBean thbean = ManagementFactory.getThreadMXBean( );
	    int nAlloc = thbean.getThreadCount( );
	    int n = 0;
	    Thread[] threads;
	    do {
	        nAlloc *= 2;
	        threads = new Thread[ nAlloc ];
	        n = root.enumerate( threads, true );
	    } while ( n == nAlloc );
	    return java.util.Arrays.copyOf( threads, n );
	}
}
