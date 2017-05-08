package com.serotonin.io.serial;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


public class SerialPortProxyEventTask extends Thread {
	private final Log LOG = LogFactory.getLog(SerialPortProxyEventTask.class);
	
	private SerialPortProxyEventListener listener;
	private SerialPortProxyEvent event;
	private long creationTime;
	private SerialPortProxyEventCompleteListener completeListener;
	
	
	public SerialPortProxyEventTask(SerialPortProxyEventListener listener, SerialPortProxyEvent event, SerialPortProxyEventCompleteListener completeListener){
		this.creationTime = System.currentTimeMillis();
		
		this.listener = listener;
		this.event = event;
		this.completeListener = completeListener;
	}

	@Override
	public void run() {
		
		try{
			if(LOG.isDebugEnabled())
				LOG.debug("Running event created at: " + this.event.getCreationTime());
			
			this.event.setTimeExecuted(System.currentTimeMillis());
			listener.serialEvent(this.event);
		}catch(Exception e){
			LOG.error(e);
		}finally{
			//I'm done here
			this.completeListener.eventComplete(System.currentTimeMillis(), this);
		}
	}
	
	/**
	 * Get the time the task was created
	 * @return
	 */
	public long getEventCreationTime(){
		return this.creationTime;
	}

}
