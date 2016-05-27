/**
 * Copyright (C) 2015 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.web.mvc.websocket;

import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.PingMessage;
import org.springframework.web.socket.adapter.jetty.JettyWebSocketSession;

import com.serotonin.m2m2.util.timeout.TimeoutClient;
import com.serotonin.m2m2.util.timeout.TimeoutTask;

/**
 * Class to test and disconnect WebSocket sessions if the Client goes away without notifying us
 * 
 * Saves Ping/Pong state in Session attributes
 * 
 * Not convinced to use the real time timer for this, perhaps it should be a WorkItem?
 * 
 * @author Terry Packer
 *
 */
public 	class MangoPingPongTracker extends TimeoutClient{
	
	private static final Log LOG = LogFactory.getLog(MangoPingPongTracker.class);
			
	private JettyWebSocketSession  session; 
	private int timeout;
	
	public MangoPingPongTracker(JettyWebSocketSession  session, int timeout){
		this.session = session;
		this.timeout = timeout;
	}
	
	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.util.timeout.TimeoutClient#scheduleTimeout(long)
	 */
	@Override
	public void scheduleTimeout(long fireTime) {
		
		//Shut'er down if we are already dead
		if((this.session == null)||(!this.session.isOpen()))
			return;
		
		Boolean receivedPong = (Boolean)this.session.getAttributes().get(MangoWebSocketPublisher.RECEIVED_PONG);
		if(receivedPong){
			this.session.getAttributes().put(MangoWebSocketPublisher.RECEIVED_PONG, new Boolean(false));
			try {
				synchronized(session){
					session.getNativeSession().getRemote().sendPing(new PingMessage().getPayload());
				}
			} catch (IOException e) {
				LOG.error(e.getMessage(), e);
			}finally{
				new TimeoutTask(this.timeout, this);
			}
		}else{
			try {
				session.close(new CloseStatus(CloseStatus.SESSION_NOT_RELIABLE.getCode(), "Didn't receive Pong from Endpoint within " + timeout + " ms."));
			} catch (IOException e) {
				LOG.error(e.getMessage(), e);
			}
		}
		
	}

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.util.timeout.TimeoutClient#getName()
	 */
	@Override
	public String getThreadName() {
		return "Mango ping pong tracker";
	}

	private static final String taskId = "MangoPingPongTracker";
	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.util.timeout.TimeoutClient#getTaskId()
	 */
	@Override
	public String getTaskId() {
		return taskId;
	}
}