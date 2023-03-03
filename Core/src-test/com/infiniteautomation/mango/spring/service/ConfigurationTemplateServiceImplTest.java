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

    public ConfigurationTemplateServiceImplTest() {
        this.retryRule = null;
    }
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


    @Test
    public void testMexicoDataModel() throws IOException {
        ConfigurationTemplateServiceImpl service = Common.getBean(ConfigurationTemplateServiceImpl.class);

        String filename = "mexico.csv";
        String csvFile = "/com/infiniteautomation/mango/spring/service/configurationTemplate/" + filename;

        List<ConfigurationTemplateServiceImpl.CSVLevel> levels = new ArrayList<>();
        levels.add(ConfigurationTemplateServiceImpl
                .CSVLevel.newBuilder()
                .setGroupBy("country")
                .setInto("states")
                .createTemplateLevel());
        levels.add(ConfigurationTemplateServiceImpl
                .CSVLevel.newBuilder()
                .setGroupBy("state")
                .setInto("cities")
                .createTemplateLevel());
        levels.add(ConfigurationTemplateServiceImpl
                .CSVLevel.newBuilder()
                .setGroupBy("city")
                .setInto("streets")
                .createTemplateLevel());
        levels.add(ConfigurationTemplateServiceImpl
                .CSVLevel.newBuilder()
                .setGroupBy("street")
                .setInto("numbers")
                .createTemplateLevel());

        ConfigurationTemplateServiceImpl.CSVHiearchy hierarchy = ConfigurationTemplateServiceImpl.CSVHiearchy.newBuilder()
                .setRoot("countries")
                .setLevels(levels)
                .createCSVHiearchy();


        //Move template files into file store
        Path rootPath = Common.getBean(FileStoreService.class).getPathForWrite("default", "");
        Files.createDirectories(rootPath);
        InputStream is = getClass().getResourceAsStream(csvFile);

        Path dst = rootPath.resolve(filename);
        Files.copy(is, dst, StandardCopyOption.REPLACE_EXISTING);
        Map<String, Object>
                model = service.generateTemplateModel("default", filename, hierarchy);

        assertNotNull(model);

        //The first level of the model should be a list of 2 sites
        assertNotNull(model.get(hierarchy.getRoot()));
        assertTrue(model.get(hierarchy.getRoot()) instanceof ArrayList);
        //Assert Country is size one and no common properties
        List<Map<String, Object>> countries = (List<Map<String, Object>>) model.get(hierarchy.getRoot());
        assertTrue(countries.size() == 2);

        //Get Mexico
        Map<String, Object> mexico = countries.get(0);
        assertTrue(mexico.containsKey("country"));
        assertTrue(mexico.get("country").equals("MX"));
        assertTrue(mexico.containsKey("states"));
        List<Map<String, Object>> mexicanStates = (List<Map<String, Object>>) mexico.get("states");
        //assert states
        Map<String, Object> nuevoLeon = mexicanStates.get(0);
        assertTrue(nuevoLeon.containsKey("country"));
        assertTrue(nuevoLeon.get("country").equals("MX"));
        assertTrue(nuevoLeon.containsKey("state"));
        assertTrue(nuevoLeon.get("state").equals("Nuevo Leon"));
        assertTrue(nuevoLeon.containsKey("cities"));
        //TODO should not have city or street or number keys???

        //TODO assert cities
        List<Map<String, Object>> nuevoLeonCities = (List<Map<String, Object>>) nuevoLeon.get("cities");
        assertTrue(nuevoLeonCities.size() == 1);
        Map<String, Object> monterrey = nuevoLeonCities.get(0);
        assertTrue(monterrey.containsKey("country"));
        assertTrue(monterrey.get("country").equals("MX"));
        assertTrue(monterrey.containsKey("state"));
        assertTrue(monterrey.get("state").equals("Nuevo Leon"));
        assertTrue(monterrey.containsKey("city"));
        assertTrue(monterrey.get("city").equals("Monterrey"));
        assertTrue(monterrey.containsKey("streets"));
        //TODO should not have street or number keys???

        //TODO assert streets for each city
        List<Map<String, Object>> monterreyStreets = (List<Map<String, Object>>) monterrey.get("streets");
        assertTrue(monterreyStreets.size() == 1);
        Map<String, Object> calleRojo = monterreyStreets.get(0);
        assertTrue(calleRojo.containsKey("country"));
        assertTrue(calleRojo.get("country").equals("MX"));
        assertTrue(calleRojo.containsKey("state"));
        assertTrue(calleRojo.get("state").equals("Nuevo Leon"));
        assertTrue(calleRojo.containsKey("city"));
        assertTrue(calleRojo.get("city").equals("Monterrey"));
        assertTrue(calleRojo.containsKey("numbers"));
        //TODO should not have number keys???

        //Assert Calle Rojo numbers
        List<Map<String, Object>> calleRojoNumbers = (List<Map<String, Object>>) calleRojo.get("numbers");
        assertTrue(calleRojoNumbers.size() == 3);
        Map<String, Object> one = calleRojoNumbers.get(0);
        assertTrue(one.containsKey("country"));
        assertTrue(one.get("country").equals("MX"));
        assertTrue(one.containsKey("state"));
        assertTrue(one.get("state").equals("Nuevo Leon"));
        assertTrue(one.containsKey("city"));
        assertTrue(one.get("city").equals("Monterrey"));
        assertTrue(one.containsKey("number"));
        assertTrue(one.get("number").equals("1"));

        //TODO assert other Calle Rojo numbers

        //Get US
        Map<String, Object> unitedStates = countries.get(1);
        assertTrue(unitedStates.containsKey("country"));
        assertTrue(unitedStates.get("country").equals("US"));
        assertTrue(unitedStates.containsKey("states"));
        //TODO assert states



    }
}
