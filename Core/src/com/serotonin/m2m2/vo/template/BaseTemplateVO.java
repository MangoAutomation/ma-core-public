/**
 * Copyright (C) 2014 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.vo.template;

import java.io.IOException;
import java.util.List;

import com.serotonin.json.JsonException;
import com.serotonin.json.ObjectWriter;
import com.serotonin.json.spi.JsonProperty;
import com.serotonin.m2m2.i18n.TranslatableMessage;
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
	
    /* Template Permissions */
    @JsonProperty
    private String readPermission;
    @JsonProperty
    private String setPermission;

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

	public String getTemplateType() {
		return getTemplateTypeName();
	}

	/**
	 * Override in subclass for specific type, this will 
	 * be stored in the templateType column in the DB.
	 * 
	 * Must be unique within an MA instance, and is recommended
     * to be unique inasmuch as possible across all modules.
	 * @return
	 */
	protected abstract String getTemplateTypeName();


        
    @Override
    public void jsonWrite(ObjectWriter writer) throws IOException, JsonException {
        super.jsonWrite(writer);
        writer.writeEntry("templateType", this.getTemplateType());
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
    
    
    
}
