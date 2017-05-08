package com.serotonin.io.serial;

public interface SerialPortProxyEventCompleteListener {

	/**
	 * Event fired when event has completed
	 * @param time
	 */
	public void eventComplete(long time, SerialPortProxyEventTask task);
	
}
