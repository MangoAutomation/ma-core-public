/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.json.junit.vo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Date;

import org.junit.Test;

import com.infiniteautomation.mango.emport.ImportContext;
import com.serotonin.json.JsonException;
import com.serotonin.json.JsonReader;
import com.serotonin.json.JsonWriter;
import com.serotonin.json.type.JsonObject;
import com.serotonin.json.type.JsonTypeReader;
import com.serotonin.json.type.JsonTypeWriter;
import com.serotonin.json.type.JsonValue;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.MangoTestBase;
import com.serotonin.m2m2.i18n.ProcessResult;
import com.serotonin.m2m2.vo.User;

/**
 * @author Terry Packer
 *
 */
public class UserTest extends MangoTestBase {
    
    @Test
    public void testUser() {
        User user = new User();
        Date created = new Date();
        user.setCreated(created);
        
        String userJson;
        try (StringWriter stringWriter = new StringWriter()) {
            JsonWriter writer = new JsonWriter(Common.JSON_CONTEXT, stringWriter);
            JsonTypeWriter typeWriter = new JsonTypeWriter(Common.JSON_CONTEXT);
            JsonValue value = typeWriter.writeObject(user);
            
            writer.setPrettyIndent(0);
            writer.setPrettyOutput(true);
            writer.writeObject(value);
            userJson = stringWriter.toString();
            //System.out.println(userJson);
            JsonTypeReader typeReader = new JsonTypeReader(userJson);
            JsonValue read = typeReader.read();
            JsonObject root = read.toJsonObject();
            JsonReader reader = new JsonReader(Common.JSON_CONTEXT, root);
            ImportContext context = new ImportContext(reader, new ProcessResult(), Common.getTranslations());
            User readUser = new User();
            context.getReader().readInto(readUser, root);
            
            //Assert the dates are the same
            assertEquals(created, readUser.getCreated());
        } catch (IOException | JsonException e) {
            fail(e.getMessage());
        }
        
    }

}
