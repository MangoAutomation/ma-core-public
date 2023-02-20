/*
 *  Copyright (C) 2023 RadixIot LLC. All rights reserved.
 */

package com.infiniteautomation.mango.spring.service;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.MangoTestBase;

public class ConfigurationTemplateServiceImplTest extends MangoTestBase {

    private final String dataSourceFilePath = "/com/infiniteautomation/mango/spring/service/configTemplateTestData.csv";
    private final String templatePath = "/com/infiniteautomation/mango/spring/service/roles.mustache";
    @Test
    public void testConfigurationTemplateService() throws IOException {
        ConfigurationTemplateServiceImpl service = Common.getBean(ConfigurationTemplateServiceImpl.class);

        List<Map<String, String>> keys = new ArrayList<>(){{
            add(new HashMap<>(){{
                put("groupKey", "site");
                put("childrenKey", "dataHalls");}});
            add(new HashMap<>(){{
                put("groupKey", "dataHall");
                put("childrenKey", "devices");}});
        }};

        //Move template files into file store
        Path rootPath = Common.getBean(FileStoreService.class).getPathForWrite("default", "");
        Files.createDirectories(rootPath);
        InputStream is = getClass().getResourceAsStream(dataSourceFilePath);

        Path dst = rootPath.resolve("configTemplateTestData.csv");
        Files.copy(is, dst, StandardCopyOption.REPLACE_EXISTING);
        List<Map<String, Object>>
                result = service.generateConfig("default", "configTemplateTestData.csv", keys);

        assertNotNull(result);
        assertTrue(result.size() > 0);
    }
    @Test
    public void testConfigurationTemplateServiceTemplate() throws IOException {
        ConfigurationTemplateServiceImpl service = Common.getBean(ConfigurationTemplateServiceImpl.class);

        //Move template files into file store
        Path rootPath = Common.getBean(FileStoreService.class).getPathForWrite("default", "");
        Files.createDirectories(rootPath);
        InputStream is = getClass().getResourceAsStream(dataSourceFilePath);
        Path dst = rootPath.resolve("configTemplateTestData.csv");
        Files.copy(is, dst, StandardCopyOption.REPLACE_EXISTING);

        List<Map<String, String>> keys = new ArrayList<>(){{
            add(new HashMap<>(){{
                put("groupKey", "site");
                put("childrenKey", "dataHalls");}});
            add(new HashMap<>(){{
                put("groupKey", "dataHall");
                put("childrenKey", "devices");}});
        }};

        String result = service.generateMangoConfigurationJson(
                "default",
                "configTemplateTestData.csv",
                templatePath,
                keys
        );

        ObjectMapper mapper = new ObjectMapper();
        JsonFactory factory = mapper.getFactory();
        JsonParser parser = factory.createParser(result);
        JsonNode actualObj = mapper.readTree(parser);

        JsonNode jsonNode1 = actualObj.get(0).get("name");
        JsonNode jsonNode2 = actualObj.get(0).get("xid");
        JsonNode jsonNode3 = actualObj.get(0).get("inherited");

        assertNotNull(result);
        assertTrue(result.length() > 0);
        assertTrue(jsonNode1.asText().contains("Admin"));
        assertTrue(jsonNode2.asText().contains("ROLE_ADMIN"));
        assertTrue(jsonNode3.isArray());
    }
}
