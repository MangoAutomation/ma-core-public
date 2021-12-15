/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.infiniteautomation.mango.io.serial.virtual;

import java.lang.reflect.Type;

import com.serotonin.ShouldNeverHappenException;
import com.serotonin.json.JsonException;
import com.serotonin.json.spi.TypeResolver;
import com.serotonin.json.type.JsonObject;
import com.serotonin.json.type.JsonValue;
import com.serotonin.m2m2.i18n.TranslatableJsonException;

/**
 * @author Terry Packer
 *
 */
public class VirtualSerialPortConfigResolver implements TypeResolver {
	
    @Override
    public Type resolve(JsonValue jsonValue) throws JsonException {
        if (jsonValue == null)
            return null;

        JsonObject json = jsonValue.toJsonObject();

        String text = json.getString("type");
        if (text == null)
            throw new TranslatableJsonException("emport.error.virtual.comm.missing", "type",
            		VirtualSerialPortConfig.PORT_TYPE_CODES);
        
        return findClass(text);
    }
    
    /**
     *
     */
    public static Class<?> findClass(String typeStr) throws TranslatableJsonException{
        int type = VirtualSerialPortConfig.PORT_TYPE_CODES.getId(typeStr);
        if (!VirtualSerialPortConfig.PORT_TYPE_CODES.isValidId(type))
            throw new TranslatableJsonException("emport.error.virtual.comm.invalid", "type", typeStr,
            		VirtualSerialPortConfig.PORT_TYPE_CODES.getCodeList());

        if (type == VirtualSerialPortConfig.SerialPortTypes.JSSC)
            throw new ShouldNeverHappenException("JSSC Ports are not virtual");
        
        if (type == VirtualSerialPortConfig.SerialPortTypes.SERIAL_SOCKET_BRIDGE)
            return SerialSocketBridgeConfig.class;   
        
        if (type == VirtualSerialPortConfig.SerialPortTypes.SERIAL_SERVER_SOCKET_BRIDGE)
        	return SerialServerSocketBridgeConfig.class;
        
        return SerialSocketBridgeConfig.class;
    }
}