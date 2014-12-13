/**
 * Copyright (C) 2014 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.web.mvc.converter;

import java.util.ArrayList;
import java.util.List;

/**
 * 
 * Registry to hold Point Locator definitions in a Globally Accessible location
 * 
 * Modules may contain definitions that are loaded into the Registry at Startup.
 * 
 * 
 * @author Terry Packer
 *
 */
public class PointLocatorRegistry {

	public static final PointLocatorRegistry instance = new PointLocatorRegistry();
	
	private List<PointLocatorDefinition> definitions = new ArrayList<PointLocatorDefinition>();

	private PointLocatorRegistry(){
	}
	
	public void addDefinition(PointLocatorDefinition def){
		this.definitions.add(def);
	}
	
	public List<PointLocatorDefinition> getDefinitions(){
		return this.definitions;
	}
	
    public List<String> getExportTypes() {
        List<String> result = new ArrayList<String>(definitions.size());
        for (PointLocatorDefinition def : definitions)
            result.add(def.getExportName());
        return result;
    }

	/**
	 * @param pointLocatorDefinitions
	 */
	public void addDefinitions(
			List<PointLocatorDefinition> pointLocatorDefinitions) {
		this.definitions.addAll(pointLocatorDefinitions);
		
	}
	
	
	
}
