/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.infiniteautomation.mango.io.serial.virtual;

import java.util.ArrayList;
import java.util.List;

import com.infiniteautomation.mango.io.serial.SerialPortManager;
import com.infiniteautomation.mango.util.LazyInitSupplier;
import com.serotonin.json.util.TypeDefinition;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.dao.SystemSettingsDao;

/**
 * 
 * Helper to access the Virtual Serial Port Configs from the System settings
 * 
 * @author Terry Packer
 *
 */
public class VirtualSerialPortConfigDao {

    private static final LazyInitSupplier<VirtualSerialPortConfigDao> instance = new LazyInitSupplier<>(() -> {
        return new VirtualSerialPortConfigDao();
    });

    private VirtualSerialPortConfigDao() {}
    
    public static VirtualSerialPortConfigDao getInstance() {
        return instance.get();
    }
    
	/**
	 * Insert new configs or update existing ones 
	 * @param config
	 * @return current list of configs
	 */
	public List<VirtualSerialPortConfig> save(VirtualSerialPortConfig config){

		List<VirtualSerialPortConfig> all = getAll();
		if(all.contains(config)){
			//Do an update
			int i = all.indexOf(config);
			all.remove(config);
			all.add(i,config);
		}else{
			//Just insert 
			all.add(config);
		}
		
		updateSystemSettings(all);
		return all;
	}
	
	/**
	 * Remove a config from the system
	 * @param config
	 * @return
	 */
	public List<VirtualSerialPortConfig> remove(VirtualSerialPortConfig config){
		List<VirtualSerialPortConfig> all = getAll();
		all.remove(config);
		updateSystemSettings(all);
		return all;
	}
	
	/**
	 * Retrieve all configs
	 * @return
	 */
	public List<VirtualSerialPortConfig> getAll(){
        @SuppressWarnings("unchecked")
		List<VirtualSerialPortConfig> list = (List<VirtualSerialPortConfig>) SystemSettingsDao.instance.getJsonObject(SerialPortManager.VIRTUAL_SERIAL_PORT_KEY,
                new TypeDefinition(List.class, VirtualSerialPortConfig.class));
        if(list == null)
        	list = new ArrayList<VirtualSerialPortConfig>();
        return list;
	}
	
    //
    // XID convenience methods
    //
    public String generateUniqueXid() {
        String xid = Common.generateXid("VSP_");
        List<VirtualSerialPortConfig> configs = getAll();
        
        while (doesXidExist(xid, configs))
            xid = Common.generateXid("VSP_");
        return xid;
    }

    protected boolean doesXidExist(String xid, List<VirtualSerialPortConfig> configs){
    	for(VirtualSerialPortConfig config : configs){
    		if(config.getXid().equals(xid))
    			return true;
    	}
    	return false;
    }
    
    public boolean isPortNameUsed(String xid, String portName) {
    	List<VirtualSerialPortConfig> configs = getAll();
    	for(VirtualSerialPortConfig config : configs) {
    		if(config.getPortName().equals(portName) && !config.getXid().equals(xid))
    			return true;
    	}
    	return false;
    }
	
    /**
     * Save all of them, overwrite existing
     * @param configs
     */
    private void updateSystemSettings(List<VirtualSerialPortConfig> configs){
    	SystemSettingsDao.instance.setJsonObjectValue(SerialPortManager.VIRTUAL_SERIAL_PORT_KEY, configs);
    	//Reload the serial ports
    	try {
			Common.serialPortManager.refreshFreeCommPorts();
		} catch (Exception e) {
			//Don't really care?
		}
    }

	/**
	 * @param xid
	 * @return
	 */
	public VirtualSerialPortConfig getByXid(String xid) {
		List<VirtualSerialPortConfig> configs = getAll();
		for(VirtualSerialPortConfig config : configs){
			if(config.getXid().equals(xid))
				return config;
		}
		return null;
	}
	
}
