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


        List<ConfigurationTemplateServiceImpl.CSVLevel> levels = new ArrayList<>();
        levels.add(ConfigurationTemplateServiceImpl
                .CSVLevel.newBuilder()
                .setGroupBy("site")
                .setInto("dataHalls")
                .createTemplateLevel());
        levels.add(ConfigurationTemplateServiceImpl
                .CSVLevel.newBuilder()
                .setGroupBy("dataHall")
                .setInto("devices")
                .createTemplateLevel());

        ConfigurationTemplateServiceImpl.CSVHiearchy hierarchy = ConfigurationTemplateServiceImpl.CSVHiearchy.newBuilder()
                .setRoot("sites")
                .setLevels(levels)
                .createCSVHiearchy();


        //Move template files into file store
        Path rootPath = Common.getBean(FileStoreService.class).getPathForWrite("default", "");
        Files.createDirectories(rootPath);
        InputStream is = getClass().getResourceAsStream(dataSourceFilePath);

        Path dst = rootPath.resolve("configTemplateTestData.csv");
        Files.copy(is, dst, StandardCopyOption.REPLACE_EXISTING);
        Map<String, Object>
                model = service.generateTemplateModel("default", "configTemplateTestData.csv", hierarchy);

        assertNotNull(model);

        //The first level of the model should be a list of 2 sites
        assertNotNull(model.get(hierarchy.getRoot()));
        assertTrue(model.get(hierarchy.getRoot()) instanceof ArrayList);
        List<Map<String, Object>> sites = (List<Map<String, Object>>) model.get(hierarchy.getRoot());
        assertTrue(sites.size() == 2);
        assertTrue(sites.get(0).get("site").equals("SiteA"));
        assertTrue(sites.get(1).get("site").equals("SiteB"));

        //Site A has 2 data halls (1,2)
        List<Map<String, Object>> siteADataHalls = (List<Map<String, Object>>) sites.get(0).get("dataHalls");
        assertTrue(siteADataHalls.size() == 2);
        assertTrue(siteADataHalls.get(0).get("dataHall").equals("1"));
        assertTrue(siteADataHalls.get(1).get("dataHall").equals("2"));
        //TODO assert devices

        //Site B has 1 data hall (3)
        List<Map<String, Object>> siteBDataHalls = (List<Map<String, Object>>) sites.get(1).get("dataHalls");
        assertTrue(siteBDataHalls.size() == 1);
        assertTrue(siteBDataHalls.get(0).get("dataHall").equals("3"));
        //TODO assert devices

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

        List<ConfigurationTemplateServiceImpl.CSVLevel> levels = new ArrayList<>();
        levels.add(ConfigurationTemplateServiceImpl
                .CSVLevel.newBuilder()
                .setGroupBy("site")
                .setInto("dataHalls")
                .createTemplateLevel());
        levels.add(ConfigurationTemplateServiceImpl
                .CSVLevel.newBuilder()
                .setGroupBy("dataHall")
                .setInto("devices")
                .createTemplateLevel());

        ConfigurationTemplateServiceImpl.CSVHiearchy hierarchy = ConfigurationTemplateServiceImpl.CSVHiearchy.newBuilder()
                .setRoot("sites")
                .setLevels(levels)
                .createCSVHiearchy();

        String result = service.generateMangoConfigurationJson(
                "default",
                "configTemplateTestData.csv",
                templatePath,
                hierarchy
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
