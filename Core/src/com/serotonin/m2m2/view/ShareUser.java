/*
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.view;

import java.io.IOException;

import org.apache.commons.lang3.StringUtils;

import com.serotonin.json.JsonException;
import com.serotonin.json.JsonReader;
import com.serotonin.json.ObjectWriter;
import com.serotonin.json.spi.JsonSerializable;
import com.serotonin.json.type.JsonObject;
import com.serotonin.m2m2.db.dao.UserDao;
import com.serotonin.m2m2.i18n.TranslatableJsonException;
import com.serotonin.m2m2.util.ExportCodes;
import com.serotonin.m2m2.vo.User;

/**
 * @author Matthew Lohbihler
 */
public class ShareUser implements JsonSerializable {
    public static final int ACCESS_NONE = 0;
    public static final int ACCESS_READ = 1;
    public static final int ACCESS_SET = 2;
    public static final int ACCESS_OWNER = 3;

    public static final ExportCodes ACCESS_CODES = new ExportCodes();
    static {
        ACCESS_CODES.addElement(ACCESS_NONE, "NONE", "common.access.none");
        ACCESS_CODES.addElement(ACCESS_READ, "READ", "common.access.read");
        ACCESS_CODES.addElement(ACCESS_SET, "SET", "common.access.set");
    }

    private int userId;
    private int accessType;

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public int getAccessType() {
        return accessType;
    }

    public void setAccessType(int accessType) {
        this.accessType = accessType;
    }

    @Override
    public void jsonWrite(ObjectWriter writer) throws IOException, JsonException {
        writer.writeEntry("user", UserDao.getInstance().getUser(userId).getUsername());
        writer.writeEntry("accessType", ACCESS_CODES.getCode(accessType));
    }

    @Override
    public void jsonRead(JsonReader reader, JsonObject jsonObject) throws JsonException {
        String text = jsonObject.getString("user");
        if (StringUtils.isBlank(text))
            throw new TranslatableJsonException("emport.error.viewShare.missing", "user");
        User user = UserDao.getInstance().getUser(text);
        if (user == null)
            throw new TranslatableJsonException("emport.error.missingUser", text);
        userId = user.getId();

        text = jsonObject.getString("accessType");
        if (StringUtils.isBlank(text))
            throw new TranslatableJsonException("emport.error.missing", "accessType",
                    ACCESS_CODES.getCodeList(ACCESS_OWNER));
        accessType = ACCESS_CODES.getId(text, ACCESS_OWNER);
        if (accessType == -1)
            throw new TranslatableJsonException("emport.error.invalid", "permission", text,
                    ACCESS_CODES.getCodeList(ACCESS_OWNER));
    }
}
