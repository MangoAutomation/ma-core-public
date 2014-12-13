package com.serotonin.m2m2.rt.serial;

import java.io.IOException;
import java.io.OutputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.serotonin.io.serial.SerialPortOutputStream;

public class EthernetComBridgeOutputStream extends SerialPortOutputStream{

	private final static Log LOG = LogFactory.getLog(EthernetComBridgeOutputStream.class);
	
	private OutputStream stream;
	
	public EthernetComBridgeOutputStream(OutputStream os){
		this.stream = os;
	}
	
	@Override
	public void write(int arg0) throws IOException {
		this.stream.write(arg0);
	}

	@Override
	public void flush() {
		try {
			this.stream.flush();
		} catch (IOException e) {
			LOG.error(e.getMessage(), e);
		}
	}

}
