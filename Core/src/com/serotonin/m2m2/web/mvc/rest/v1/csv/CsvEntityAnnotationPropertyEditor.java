/**
 * Copyright (C) 2015 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.web.mvc.rest.v1.csv;

import java.util.List;

import com.serotonin.m2m2.module.ModelDefinition;
import com.serotonin.m2m2.module.ModuleRegistry;

/**
 * Property Editor that will Insert a Type Into a Type Column
 * 
 * @author Terry Packer
 *
 */
public class CsvEntityAnnotationPropertyEditor extends CSVPropertyEditor{

	private String typeName;
	
	public CsvEntityAnnotationPropertyEditor(String typeName){
		this.typeName = typeName;
	}
	
	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.web.mvc.rest.v1.csv.CSVPropertyEditor#setValue(java.lang.Object)
	 */
	@Override
	public void setValue(Object value) {
		// TODO Auto-generated method stub
		
	}

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.web.mvc.rest.v1.csv.CSVPropertyEditor#getValue()
	 */
	@Override
	public Object getValue() {
		List<ModelDefinition> definitions = ModuleRegistry.getModelDefinitions();
		for(ModelDefinition definition : definitions){
			if(definition.getModelTypeName().equalsIgnoreCase(typeName))
				return definition.createModel();
		}
		//TODO LOG This
		return null;
	}

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.web.mvc.rest.v1.csv.CSVPropertyEditor#getAsText()
	 */
	@Override
	public String getAsText() {
		return typeName;
	}

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.web.mvc.rest.v1.csv.CSVPropertyEditor#setAsText(java.lang.String)
	 */
	@Override
	public void setAsText(String text) throws IllegalArgumentException {
		this.typeName = text;
		
	}

}
