/*
    Copyright (C) 2013 Deltamation Software All rights reserved.
    @author Terry Packer
 */
package com.serotonin.m2m2.vo;

import java.io.IOException;

import com.serotonin.ShouldNeverHappenException;
import com.serotonin.json.JsonException;
import com.serotonin.json.JsonReader;
import com.serotonin.json.ObjectWriter;
import com.serotonin.json.spi.JsonSerializable;
import com.serotonin.json.type.JsonObject;

/**
 * 
 * Class that needs an Enable/Disable Member
 * 
 * Copyright (C) 2013 Deltamation Software. All Rights Reserved.
 * @author Terry Packer
 *
 */
public abstract class AbstractActionVO<VO extends AbstractActionVO<VO>> extends AbstractVO<VO> implements JsonSerializable {
    private static final long serialVersionUID = -1;
    public static final String ENABLED_KEY = "enabled";
    
    protected boolean enabled = false;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    @Override
    public void jsonRead(JsonReader reader, JsonObject jsonObject) throws JsonException {
    	super.jsonRead(reader, jsonObject);
    	enabled = jsonObject.getBoolean(ENABLED_KEY);
    }
    
    @Override
	public void jsonWrite(ObjectWriter writer) throws IOException,
			JsonException {
		super.jsonWrite(writer);
		writer.writeEntry(ENABLED_KEY, enabled);
    }
    
    /**
     * Copies a vo
     * @return Copy of this vo
     */
    @SuppressWarnings("unchecked")
    public VO copy() {
        // TODO make sure this works
        try {
            return (VO) super.clone();
        }
        catch (CloneNotSupportedException e) {
            throw new ShouldNeverHappenException(e);
        }
    }

}
