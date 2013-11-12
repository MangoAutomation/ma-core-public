/*
    Copyright (C) 2006-2011 Serotonin Software Technologies Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.module;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.serotonin.m2m2.web.dwr.util.DwrClassConversion;

/**
 * A DWR conversion definition determines how Java objects will be converted to Javascript objects and vice versa. Most
 * conversions is done by a "bean" converter (using get/is and set methods), and this is typically sufficient for nearly
 * all purposes.
 * 
 * By default classes are not allowed to be converted, so any classes that should be allowed need to be explicitly
 * declared.
 * 
 * @author Matthew Lohbihler
 */
abstract public class DwrConversionDefinition extends ModuleElementDefinition {
    private final List<DwrClassConversion> conversions = new ArrayList<DwrClassConversion>();

    @Override
    public void preInitialize() {
        super.preInitialize();
        addConversions();
    }

    abstract public void addConversions();

    /**
     * Declares that objects of the given class are allowed to be converted using a bean converter. All available
     * properties of the class are to be converted.
     * 
     * @param clazz
     *            the class to allow
     */
    public void addConversion(Class<?> clazz) {
        conversions.add(new DwrClassConversion(clazz));
    }

    /**
     * Declares that objects of the given class are allowed to be converted using the given converter type.
     * 
     * @param clazz
     *            the class to allow
     * @param converterType
     *            the converter type with which to convert
     */
    public void addConversion(Class<?> clazz, String converterType) {
        DwrClassConversion dcc = new DwrClassConversion(clazz);
        dcc.setConverterType(converterType);
        conversions.add(dcc);
    }
    
    /**
     * Declares that objects of the given class are allowed to be converted using the given converter type.
     * 
     * @param clazz
     *            the class to allow
     * @param converterType
     *            the converter type with which to convert
     * @param parameters
     * 			  a name,value map to load into the converter's parameters
     */
    public void addConversion(Class<?> clazz, String converterType, Map<String,String> parameters) {
        DwrClassConversion dcc = new DwrClassConversion(clazz,parameters);
        dcc.setConverterType(converterType);
        conversions.add(dcc);
    }
    
    
    /**
     * Declares that objects of the given class are allowed to be converted using a bean converter. Only the given
     * properties of the class are to be converted.
     * 
     * @param clazz
     *            the class to allow
     * @param inclusions
     *            a comma-delimited list of properties to convert
     */
    public void addConversionWithInclusions(Class<?> clazz, String inclusions) {
        DwrClassConversion conversion = new DwrClassConversion(clazz);

        if (!StringUtils.isBlank(inclusions)) {
            String[] parts = inclusions.split(",");
            for (String s : parts) {
                s = s.trim();
                if (!StringUtils.isBlank(s))
                    conversion.addInclude(s);
            }
        }

        conversions.add(conversion);
    }

    /**
     * Declares that objects of the given class are allowed to be converted using a bean converter. The given properties
     * of the class are to be excluded from conversion.
     * 
     * @param clazz
     *            the class to allow
     * @param exclusions
     *            a comma-delimited list of properties to exclude
     */
    public void addConversionWithExclusions(Class<?> clazz, String exclusions) {
        DwrClassConversion conversion = new DwrClassConversion(clazz);

        if (!StringUtils.isBlank(exclusions)) {
            String[] parts = exclusions.split(",");
            for (String s : parts) {
                s = s.trim();
                if (!StringUtils.isBlank(s))
                    conversion.addExclude(s);
            }
        }

        conversions.add(conversion);
    }

    /**
     * @return a list of all conversions provided by this definition
     */
    public List<DwrClassConversion> getConversions() {
    	
    	//Ugly Hack for now to ensure all module conversions are javascript enabled
    	if(conversions != null)
	    	for(DwrClassConversion conversion : conversions){
	    		String js = conversion.getClazz().getCanonicalName().substring(conversion.getClazz().getCanonicalName().lastIndexOf(".")+1);
	    		conversion.addParameter("javascript", js);
	    	}
    	
        return conversions;
    }
}
