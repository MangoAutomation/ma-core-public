/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.infiniteautomation.mango.emport;

import org.apache.commons.lang3.StringUtils;

import com.infiniteautomation.mango.io.serial.virtual.VirtualSerialPortConfig;
import com.infiniteautomation.mango.io.serial.virtual.VirtualSerialPortConfigDao;
import com.infiniteautomation.mango.io.serial.virtual.VirtualSerialPortConfigResolver;
import com.serotonin.json.JsonException;
import com.serotonin.json.type.JsonObject;
import com.serotonin.m2m2.i18n.ProcessResult;
import com.serotonin.m2m2.i18n.TranslatableJsonException;

/**
 * Template Importer
 * 
 * @author Terry Packer
 *
 */
public class VirtualSerialPortImporter extends Importer {
    public VirtualSerialPortImporter(JsonObject json) {
        super(json);
    }

    @Override
    protected void importImpl() {
        String xid = json.getString("xid");

        boolean isNew = false;
        if (StringUtils.isBlank(xid)){
            xid = VirtualSerialPortConfigDao.getInstance().generateUniqueXid();
            isNew = true;
        }
            
        VirtualSerialPortConfig vo = VirtualSerialPortConfigDao.getInstance().getByXid(xid);
        if (vo == null) {
        	isNew = true;
            String typeStr = json.getString("type");
            if (StringUtils.isBlank(typeStr))
                addFailureMessage("emport.virtualserialport.missingType", xid, VirtualSerialPortConfig.PORT_TYPE_CODES);
            else {
            	try{
            		Class<?> virtualPortClass = VirtualSerialPortConfigResolver.findClass(typeStr);
            		vo = (VirtualSerialPortConfig) virtualPortClass.newInstance();
            		vo.setXid(xid);
            	}catch(TranslatableJsonException ex){
            		addFailureMessage("emport.virtualserialport.prefix", xid, ex.getMsg());
            	} catch (InstantiationException e) {
            		addFailureMessage("emport.virtualserialport.prefix", xid, e.getMessage());
				} catch (IllegalAccessException e) {
					addFailureMessage("emport.virtualserialport.prefix", xid, e.getMessage());
				}
            }
        }

        if (vo != null) {
            try {
                // The VO was found or successfully created. Finish reading it in.
                ctx.getReader().readInto(vo, json);

                // Now validate it. Use a new response object so we can distinguish errors in this vo from
                // other errors.
                ProcessResult voResponse = new ProcessResult();
                vo.validate(voResponse);
                if (voResponse.getHasMessages())
                    setValidationMessages(voResponse, "emport.virtualserialport.prefix", xid);
                else {
                    // Sweet. Save it.
                    VirtualSerialPortConfigDao.getInstance().save(vo);
                    addSuccessMessage(isNew, "emport.virtualserialport.prefix", xid);
                }
            }
            catch (TranslatableJsonException e) {
                addFailureMessage("emport.virtualserialport.prefix", xid, e.getMsg());
            }
            catch (JsonException e) {
                addFailureMessage("emport.virtualserialport.prefix", xid, getJsonExceptionMessage(e));
            }
        }
    }

}
