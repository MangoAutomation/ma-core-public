/**
 * Copyright (C) 2014 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.vo.template;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.List;

import com.serotonin.json.JsonException;
import com.serotonin.json.ObjectWriter;
import com.serotonin.json.spi.JsonProperty;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.module.TemplateDefinition;
import com.serotonin.m2m2.rt.event.type.AuditEventType;
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
    public void jsonWrite(ObjectWriter writer) throws IOException, JsonException {
        super.jsonWrite(writer);
        writer.writeEntry("templateType", this.getDefinition().getTemplateTypeName());
    }

    @Override
    public void addProperties(List<TranslatableMessage> list) {
        super.addProperties(list);
        AuditEventType.addPropertyMessage(list, "pointEdit.props.permission.read", readPermission);
        AuditEventType.addPropertyMessage(list, "pointEdit.props.permission.set", setPermission);
    }

    @Override
    public void addPropertyChanges(List<TranslatableMessage> list, BaseTemplateVO<?> from) {
        super.addPropertyChanges(list, from);
 
        AuditEventType.maybeAddPropertyChangeMessage(list, "pointEdit.props.permission.read", from.readPermission,
                readPermission);
        AuditEventType.maybeAddPropertyChangeMessage(list, "pointEdit.props.permission.set", from.setPermission,
                setPermission);
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
