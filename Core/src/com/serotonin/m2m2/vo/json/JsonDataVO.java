/**
 * Copyright (C) 2016 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.vo.json;

import java.io.IOException;
import java.io.Serializable;
import java.util.List;

import com.serotonin.json.JsonException;
import com.serotonin.json.JsonReader;
import com.serotonin.json.ObjectWriter;
import com.serotonin.json.spi.JsonProperty;
import com.serotonin.json.spi.JsonSerializable;
import com.serotonin.json.type.JsonObject;
import com.serotonin.m2m2.db.dao.JsonDataDao;
import com.serotonin.m2m2.i18n.ProcessResult;
import com.serotonin.m2m2.i18n.TranslatableJsonException;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.rt.event.type.AuditEventType;
import com.serotonin.m2m2.vo.AbstractVO;

/**
 * @author Terry Packer
 *
 */
public class JsonDataVO extends AbstractVO<JsonDataVO> implements Serializable, JsonSerializable {
	
	public static final String XID_PREFIX = "JSON_";
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private Object jsonData;

	@JsonProperty
	private String readPermission;
	@JsonProperty
	private String editPermission;
	
	public Object getJsonData() {
		return jsonData;
	}
	public void setJsonData(Object data) {
		this.jsonData = data;
	}

	public String getReadPermission() {
		return readPermission;
	}
	public void setReadPermission(String readPermission) {
		this.readPermission = readPermission;
	}

	public String getEditPermission() {
		return editPermission;
	}
	public void setEditPermission(String editPermission) {
		this.editPermission = editPermission;
	}

	@Override
    public void validate(ProcessResult response){
		super.validate(response);
		
		try{
			JsonDataDao.instance.writeValueAsString(this.jsonData);
		}catch(Exception e){
			response.addMessage("jsonData", new TranslatableMessage("common.default", e.getMessage()));
		}
	}
	
	
    @Override
    public final void addProperties(List<TranslatableMessage> list) {
    	super.addProperties(list);
        AuditEventType.addPropertyMessage(list, "jsonData.data", jsonData);
        AuditEventType.addPropertyMessage(list, "jsonData.readPermission", readPermission);
        AuditEventType.addPropertyMessage(list, "jsonData.editPermission", editPermission);
    }
    @Override
    public void addPropertyChanges(List<TranslatableMessage> list, JsonDataVO from) {
    	super.addPropertyChanges(list,from);
        AuditEventType.maybeAddPropertyChangeMessage(list, "jsonData.data", from.jsonData, jsonData);
        AuditEventType.maybeAddPropertyChangeMessage(list, "jsonData.readPermission", from.readPermission, readPermission);
        AuditEventType.maybeAddPropertyChangeMessage(list, "jsonData.editPermission", from.editPermission, editPermission);
    }
    
    @Override
    public void jsonWrite(ObjectWriter writer) throws IOException, JsonException {
        super.jsonWrite(writer);
        writer.writeEntry("jsonData", JsonDataDao.instance.writeValueAsString(jsonData));
    }
    @Override
    public void jsonRead(JsonReader reader, JsonObject jsonObject) throws JsonException {
    	super.jsonRead(reader, jsonObject);
    	String json = jsonObject.getString("jsonData");
    	try{
    		jsonData = JsonDataDao.instance.readValueFromString(json);
    	}catch(Exception e){
            throw new TranslatableJsonException("emport.error.parseError", "jsonData");
    	}
    }
	
	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.util.ChangeComparable#getTypeKey()
	 */
	@Override
	public String getTypeKey() {
		return "event.audit.jsonData";
	}

}
