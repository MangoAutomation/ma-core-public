/*
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.vo.permission;

import java.io.IOException;

import org.apache.commons.lang3.StringUtils;

import com.infiniteautomation.mango.spring.dao.DataPointDao;
import com.serotonin.json.JsonException;
import com.serotonin.json.JsonReader;
import com.serotonin.json.ObjectWriter;
import com.serotonin.json.spi.JsonSerializable;
import com.serotonin.json.type.JsonObject;
import com.serotonin.m2m2.i18n.TranslatableJsonException;
import com.serotonin.m2m2.util.ExportCodes;

/**
 * @author Matthew Lohbihler
 * @deprecated use tag-based permissions instead
 */
@Deprecated
public class DataPointAccess implements JsonSerializable {
    public static final int READ = 1;
    public static final int SET = 2;

    private static final ExportCodes ACCESS_CODES = new ExportCodes();
    static {
        ACCESS_CODES.addElement(READ, "READ", "common.access.read");
        ACCESS_CODES.addElement(SET, "SET", "common.access.set");
    }

    private int dataPointId;
    private int permission;

    public int getDataPointId() {
        return dataPointId;
    }

    public void setDataPointId(int dataPointId) {
        this.dataPointId = dataPointId;
    }

    public int getPermission() {
        return permission;
    }

    public void setPermission(int permission) {
        this.permission = permission;
    }

    @Override
    public void jsonWrite(ObjectWriter writer) throws IOException, JsonException {
        writer.writeEntry("dataPointXid", DataPointDao.instance.getXidById(dataPointId));
        writer.writeEntry("permission", ACCESS_CODES.getCode(permission));
    }

    @Override
    public void jsonRead(JsonReader reader, JsonObject jsonObject) throws JsonException {
        String text = jsonObject.getString("dataPointXid");
        if (StringUtils.isBlank(text))
            throw new TranslatableJsonException("emport.error.permission.missing", "dataPointXid");

        Integer dpid = DataPointDao.instance.getIdByXid(text);
        if (dpid == null)
            throw new TranslatableJsonException("emport.error.missingPoint", text);
        dataPointId = dpid;

        text = jsonObject.getString("permission");
        if (StringUtils.isBlank(text))
            throw new TranslatableJsonException("emport.error.missing", "permission", ACCESS_CODES.getCodeList());
        permission = ACCESS_CODES.getId(text);
        if (permission == -1)
            throw new TranslatableJsonException("emport.error.invalid", "permission", text, ACCESS_CODES.getCodeList());
    }
}
