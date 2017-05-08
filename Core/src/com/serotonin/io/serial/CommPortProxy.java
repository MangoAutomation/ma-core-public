/*
    Copyright (C) 2006-2013 Serotonin Software Technologies Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.io.serial;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Proxy class to abstract Comm Ports
 * 
 * @author Matthew Lohbihler
 *
 */
public class CommPortProxy {
    
	private final Log LOG = LogFactory.getLog(CommPortProxy.class);
	
	private final String name;
    private String portType;
    private final boolean currentlyOwned;
    private final String currentOwner;
    private String hardwareId;
    private String product;

    /**
     * 
     * @param cpid
     */
    public CommPortProxy(CommPortIdentifier cpid) {
        name = cpid.getName();
        switch (cpid.getPortType()) {
        case CommPortIdentifier.PORT_SERIAL:
            portType = "Serial";
            break;
        case CommPortIdentifier.PORT_PARALLEL:
            portType = "Parallel";
            break;
        default:
            portType = "Unknown (" + cpid.getPortType() + ")";
        }
        currentlyOwned = cpid.isCurrentlyOwned();
        currentOwner = cpid.getCurrentOwner();
        
        if(LOG.isDebugEnabled()){
        	String output = "Creating comm port with id: " + cpid.getName();
        	if(currentlyOwned)
        		output += " Owned by " + cpid.getCurrentOwner();
        	LOG.debug(output);
        }
        	
        
    }

    public CommPortProxy(String name, boolean serial) {
        this.name = name;
        portType = serial ? "Serial" : "Parallel";
        currentlyOwned = false;
        currentOwner = null;
        
        if(LOG.isDebugEnabled())
        	LOG.debug("Creating comm port with id: " + name);
    }

    public boolean isCurrentlyOwned() {
        return currentlyOwned;
    }

    public String getCurrentOwner() {
        return currentOwner;
    }

    public String getName() {
        return name;
    }

    public String getPortType() {
        return portType;
    }

    public String getHardwareId() {
        return hardwareId;
    }

    public void setHardwareId(String hardwareId) {
        this.hardwareId = hardwareId;
    }

    public String getProduct() {
        return product;
    }

    public void setProduct(String product) {
        this.product = product;
    }

    public String getId() {
        if (StringUtils.isEmpty(hardwareId))
            return name;
        return hardwareId;
    }

    public String getDescription() {
        if (!StringUtils.isEmpty(hardwareId) && !StringUtils.isEmpty(product))
            return hardwareId + " (" + product.trim() + ")";
        if (!StringUtils.isEmpty(hardwareId))
            return hardwareId + " (" + name + ")";
        return name;
    }
}
