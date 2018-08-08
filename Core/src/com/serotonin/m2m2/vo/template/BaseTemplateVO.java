/**
 * Copyright (C) 2014 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.vo.template;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import com.infiniteautomation.mango.spring.dao.TemplateDao;
import com.serotonin.json.JsonException;
import com.serotonin.json.ObjectWriter;
import com.serotonin.json.spi.JsonProperty;
import com.serotonin.m2m2.module.TemplateDefinition;
import com.serotonin.m2m2.util.ExportCodes;
import com.serotonin.m2m2.vo.AbstractVO;

/**
 * @author Terry Packer
 *
 */
public abstract class BaseTemplateVO<T extends BaseTemplateVO<?>> extends AbstractVO<BaseTemplateVO<?>>{

	public static final String XID_PREFIX = "TPL_";
	public interface TemplateTypes {
		int DATA_POINT_PROPERTIES = 1;
	}
    
	public static final ExportCodes TEMPLATE_TYPE_CODES = new ExportCodes();
    static {
        TEMPLATE_TYPE_CODES.addElement(TemplateTypes.DATA_POINT_PROPERTIES, "DATA_POINT_PROPERTIES", "pointEdit.template.types.dataPointProperties");
    }
    
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private TemplateDefinition definition;
	
    /* Template Permissions */
    @JsonProperty
    private String readPermission;
    @JsonProperty
    private String setPermission;
    
	public TemplateDefinition getDefinition() {
		return definition;
	}

	public void setDefinition(TemplateDefinition definition) {
		this.definition = definition;
	}

	public String getReadPermission() {
		return readPermission;
	}

	public void setReadPermission(String readPermission) {
		this.readPermission = readPermission;
	}

	public String getSetPermission() {
		return setPermission;
	}

	public void setSetPermission(String setPermission) {
		this.setPermission = setPermission;
	}

	@Override
	protected TemplateDao getDao() {
		return TemplateDao.instance;
	}
        
    @Override
    public void jsonWrite(ObjectWriter writer) throws IOException, JsonException {
        super.jsonWrite(writer);
        writer.writeEntry("templateType", this.getDefinition().getTemplateTypeName());
    }

    //
    //
    // Serialization
    //
    private static final int version = 1;

    private void writeObject(ObjectOutputStream out) throws IOException {
    	out.writeInt(version);
    }
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
    	in.readInt();
//
//        // Switch on the version of the class so that version changes can be
//        // elegantly handled.
//        if (ver == 1) {
//        	
//        }
    }
    
}
