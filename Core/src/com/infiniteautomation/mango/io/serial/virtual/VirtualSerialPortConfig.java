/**
 * Copyright (C) 2015 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.infiniteautomation.mango.io.serial.virtual;

import java.io.IOException;

import org.apache.commons.lang3.StringUtils;

import com.infiniteautomation.mango.io.serial.SerialPortIdentifier;
import com.infiniteautomation.mango.io.serial.SerialPortProxy;
import com.serotonin.json.JsonException;
import com.serotonin.json.JsonReader;
import com.serotonin.json.ObjectWriter;
import com.serotonin.json.spi.JsonProperty;
import com.serotonin.json.spi.JsonSerializable;
import com.serotonin.json.type.JsonObject;
import com.serotonin.m2m2.i18n.ProcessResult;
import com.serotonin.m2m2.i18n.TranslatableJsonException;
import com.serotonin.m2m2.util.ExportCodes;

/**
 * @author Terry Packer
 *
 */
public abstract class VirtualSerialPortConfig implements JsonSerializable{

	public interface SerialPortTypes {
		int JSSC = 1;
		int SERIAL_SOCKET_BRIDGE = 2;
	}
	
    public static final ExportCodes PORT_TYPE_CODES = new ExportCodes();
    static {
    	PORT_TYPE_CODES.addElement(SerialPortTypes.JSSC, "JSSC", "serial.portType.jssc");
    	PORT_TYPE_CODES.addElement(SerialPortTypes.SERIAL_SOCKET_BRIDGE, "SERIAL_SOCKET_BRIDGE", "serial.portType.serialSocketBridge");
    }
    
    
    public VirtualSerialPortConfig(String name, int type){
    	this.portName = name;
    	this.type = type;
    }
	
    public VirtualSerialPortConfig(){ }
    
	@JsonProperty
	private String portName;
	
	private int type;
	
	public void validate(ProcessResult response){
		 if (StringUtils.isBlank(portName))
	            response.addContextualMessage("portName", "validate.required");
	}
	
    public String getPortName() {
		return portName;
	}

	public void setPortName(String portName) {
		this.portName = portName;
	}

	public int getType() {
		return type;
	}

	public void setType(int type) {
		this.type = type;
	}

	public abstract SerialPortProxy createProxy(SerialPortIdentifier id);
	
	@Override
    public void jsonRead(JsonReader reader, JsonObject jsonObject) throws JsonException {
    	String text = jsonObject.getString("type");
        if (text != null) {
            type = PORT_TYPE_CODES.getId(text);
            if (type == -1)
                throw new TranslatableJsonException("emport.error.invalid", "type", text,
                        PORT_TYPE_CODES.getCodeList());
        }else{
        	 throw new TranslatableJsonException("emport.error.missing", "type", text,
                     PORT_TYPE_CODES.getCodeList());
        }
    }
    
    @Override
	public void jsonWrite(ObjectWriter writer) throws IOException,
			JsonException {
        writer.writeEntry("type", PORT_TYPE_CODES.getCode(type));
    }

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((portName == null) ? 0 : portName.hashCode());
		result = prime * result + type;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		VirtualSerialPortConfig other = (VirtualSerialPortConfig) obj;
		if (portName == null) {
			if (other.portName != null)
				return false;
		} else if (!portName.equals(other.portName))
			return false;
		if (type != other.type)
			return false;
		return true;
	}
	
    
    
}
