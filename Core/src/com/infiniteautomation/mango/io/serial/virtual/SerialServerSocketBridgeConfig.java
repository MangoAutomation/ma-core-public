/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.infiniteautomation.mango.io.serial.virtual;

import com.infiniteautomation.mango.io.serial.SerialPortIdentifier;
import com.infiniteautomation.mango.io.serial.SerialPortProxy;
import com.serotonin.json.spi.JsonProperty;
import com.serotonin.m2m2.i18n.ProcessResult;
import com.serotonin.util.IpAddressUtils;

public class SerialServerSocketBridgeConfig extends VirtualSerialPortConfig {

	public SerialServerSocketBridgeConfig(String xid, String name, int port, int bufferSize, int timeout) {
		super(xid, name, SerialPortTypes.SERIAL_SERVER_SOCKET_BRIDGE);
		this.port = port;
		this.bufferSize = bufferSize;
		this.timeout = timeout;
	}
	
	public SerialServerSocketBridgeConfig() {}
	
	@JsonProperty
	private int port;
	@JsonProperty
	private int bufferSize;
	@JsonProperty
	private int timeout;
	@JsonProperty
	private String[] ipWhiteList;
	
	public int getPort() {
		return port;
	}
	
	public void setPort(int port) {
		this.port = port;
	}
	
	public int getBufferSize() {
		return bufferSize;
	}
	
	public void setBufferSize(int bufferSize) {
		this.bufferSize = bufferSize;
	}
	
	public int getTimeout() {
		return timeout;
	}
	
	public void setTimeout(int timeout) {
		this.timeout = timeout;
	}
	
	public String[] getIpWhiteList() {
	    return ipWhiteList;
	}
	
	public void setIpWhiteList(String[] ipWhiteList) {
	    this.ipWhiteList = ipWhiteList;
	}
	
	@Override
	public void validate(ProcessResult response) {
		super.validate(response);
		if (port <= 0)
            response.addContextualMessage("port", "validate.greaterThanZero");
		if (bufferSize <= 0)
            response.addContextualMessage("bufferSize", "validate.greaterThanZero");
		if (timeout < 0)
            response.addContextualMessage("timeout", "validate.cannotBeNegative");
		
		if(ipWhiteList != null)
		    for(String ipMask : ipWhiteList) {
		        String msg = IpAddressUtils.checkIpMask(ipMask);
	            if (msg != null)
	                response.addContextualMessage("ipWhiteList", "common.default", msg);
		    }
	}
	
	@Override
	public SerialPortProxy createProxy(SerialPortIdentifier id) {
		return new SerialServerSocketBridge(id, port, bufferSize, timeout, ipWhiteList);
	}
}
