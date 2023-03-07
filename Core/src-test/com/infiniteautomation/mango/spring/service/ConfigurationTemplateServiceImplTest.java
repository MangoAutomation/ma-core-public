/*
 *  Copyright (C) 2023 RadixIot LLC. All rights reserved.
 */

package com.infiniteautomation.mango.spring.service;

import static org.junit.Assert.assertEquals;
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
import com.infiniteautomation.mango.spring.service.ConfigurationTemplateServiceImpl.CSVHierarchy;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.MangoTestBase;

public class ConfigurationTemplateServiceImplTest extends MangoTestBase {

    public ConfigurationTemplateServiceImplTest() {
        this.retryRule = null;
    }

    @Test
    public void testConfigurationTemplateService() throws IOException {
        ConfigurationTemplateServiceImpl service = Common.getBean(ConfigurationTemplateServiceImpl.class);


        List<ConfigurationTemplateServiceImpl.CSVLevel> levels = new ArrayList<>();
        levels.add(ConfigurationTemplateServiceImpl
                .CSVLevel.newBuilder()
                .setGroupBy("site")
                .setInto("dataHalls")
                .createCSVLevel());
        levels.add(ConfigurationTemplateServiceImpl
                .CSVLevel.newBuilder()
                .setGroupBy("dataHall")
                .setInto("devices")
                .createCSVLevel());

        CSVHierarchy hierarchy = CSVHierarchy.newBuilder()
                .setRoot("sites")
                .setLevels(levels)
                .createCSVHierarchy();


        //Move files into file store
        setupResource("configTemplateTestData.csv");

        Map<String, Object>
                model = service.generateTemplateModel("default", "configTemplateTestData.csv", hierarchy);

        assertNotNull(model);

        //The first level of the model should be a list of 2 sites
        assertNotNull(model.get(hierarchy.getRoot()));
        List<Map<String, Object>> sites = (List<Map<String, Object>>) model.get(hierarchy.getRoot());
        assertTrue(model.get(hierarchy.getRoot()) instanceof ArrayList);
        assertEquals(2, sites.size());
        assertEquals("SiteA", sites.get(0).get("site"));
        assertEquals("SiteB", sites.get(1).get("site"));

        //Site A has 2 data halls (1,2)
        List<Map<String, Object>> siteADataHalls = (List<Map<String, Object>>) sites.get(0).get("dataHalls");
        assertEquals(2, siteADataHalls.size());
        assertEquals("1", siteADataHalls.get(0).get("dataHall"));
        assertEquals("2", siteADataHalls.get(1).get("dataHall"));
        //TODO assert devices

        //Site B has 1 data hall (3)
        List<Map<String, Object>> siteBDataHalls = (List<Map<String, Object>>) sites.get(1).get("dataHalls");
        assertEquals(1, siteBDataHalls.size());
        assertEquals("3", siteBDataHalls.get(0).get("dataHall"));
        //TODO assert devices

    }
    @Test
    public void testConfigurationTemplateServiceTemplate() throws IOException {
        ConfigurationTemplateServiceImpl service = Common.getBean(ConfigurationTemplateServiceImpl.class);

        //Move files into file store
        String dataFile = "configTemplateTestData.csv";
        setupResource(dataFile);
        String templateFile = "roles.mustache";
        setupResource(templateFile);

        List<ConfigurationTemplateServiceImpl.CSVLevel> levels = new ArrayList<>();
        levels.add(ConfigurationTemplateServiceImpl
                .CSVLevel.newBuilder()
                .setGroupBy("site")
                .setInto("dataHalls")
                .createCSVLevel());
        levels.add(ConfigurationTemplateServiceImpl
                .CSVLevel.newBuilder()
                .setGroupBy("dataHall")
                .setInto("devices")
                .createCSVLevel());

        CSVHierarchy hierarchy = CSVHierarchy.newBuilder()
                .setRoot("sites")
                .setLevels(levels)
                .createCSVHierarchy();

        String result = service.generateMangoConfigurationJson(
                "default",
                dataFile,
                templateFile,
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
    public void testConfigurationTemplateServiceTemplateGuitars() throws IOException {
        ConfigurationTemplateServiceImpl service = Common.getBean(ConfigurationTemplateServiceImpl.class);

        //Move files into file store
        String dataFile = "guitars.csv";
        setupResource(dataFile);
        String templateFile = "guitars.mustache";
        setupResource(templateFile);

        List<ConfigurationTemplateServiceImpl.CSVLevel> levels = new ArrayList<>();
        levels.add(ConfigurationTemplateServiceImpl
                .CSVLevel.newBuilder()
                .setGroupBy("type")
                .setInto("brands")
                .createCSVLevel());
        levels.add(ConfigurationTemplateServiceImpl
                .CSVLevel.newBuilder()
                .setGroupBy("brand")
                .setInto("models")
                .createCSVLevel());
        levels.add(ConfigurationTemplateServiceImpl
                .CSVLevel.newBuilder()
                .setGroupBy("model")
                .setInto("pickups")
                .createCSVLevel());
        levels.add(ConfigurationTemplateServiceImpl
                .CSVLevel.newBuilder()
                .setGroupBy("pickup")
                .setInto("colors")
                .createCSVLevel());

        CSVHierarchy hierarchy = CSVHierarchy.newBuilder()
                .setRoot("guitars")
                .setLevels(levels)
                .createCSVHierarchy();

        String result = service.generateMangoConfigurationJson(
                "default",
                dataFile,
                templateFile,
                hierarchy
        );

        ObjectMapper mapper = new ObjectMapper();
        JsonFactory factory = mapper.getFactory();
        JsonParser parser = factory.createParser(result);
        JsonNode actualObj = mapper.readTree(parser);
        JsonNode jsonNode1 = actualObj.get(0).get("type");
        JsonNode jsonNode2 = actualObj.get(1).get("brand");
        JsonNode jsonNode3 = actualObj.get(2).get("color");
        JsonNode jsonNode4 = actualObj.get(4).get("brand");

        assertNotNull(result);
        assertTrue(result.length() > 0);
        assertTrue(jsonNode1.asText().contains("Electric_guitar"));
        assertTrue(jsonNode2.asText().contains("Fender"));
        assertTrue(jsonNode3.asText().contains("red"));
        assertTrue(jsonNode4.asText().contains("Ibanez"));
    }

    @Test
    public void testMexicoDataModel() throws IOException {
        ConfigurationTemplateServiceImpl service = Common.getBean(ConfigurationTemplateServiceImpl.class);

        List<ConfigurationTemplateServiceImpl.CSVLevel> levels = new ArrayList<>();
        levels.add(ConfigurationTemplateServiceImpl
                .CSVLevel.newBuilder()
                .setGroupBy("country")
                .setInto("states")
                .createCSVLevel());
        levels.add(ConfigurationTemplateServiceImpl
                .CSVLevel.newBuilder()
                .setGroupBy("state")
                .setInto("cities")
                .createCSVLevel());
        levels.add(ConfigurationTemplateServiceImpl
                .CSVLevel.newBuilder()
                .setGroupBy("city")
                .setInto("streets")
                .createCSVLevel());
        levels.add(ConfigurationTemplateServiceImpl
                .CSVLevel.newBuilder()
                .setGroupBy("street")
                .setInto("numbers")
                .createCSVLevel());

        CSVHierarchy hierarchy = CSVHierarchy.newBuilder()
                .setRoot("countries")
                .setLevels(levels)
                .createCSVHierarchy();

        //Move files into file store
        String dataFile = "mexico.csv";
        setupResource(dataFile);

        Map<String, Object>
                model = service.generateTemplateModel("default", dataFile, hierarchy);

        assertNotNull(model);

        //The first level of the model should be a list of 2 sites
        assertNotNull(model.get(hierarchy.getRoot()));
        assertTrue(model.get(hierarchy.getRoot()) instanceof ArrayList);
        //Assert Country is size one and no common properties
        List<Map<String, Object>> countries = (List<Map<String, Object>>) model.get(hierarchy.getRoot());
        assertEquals(2, countries.size());

        //Get Mexico
        Map<String, Object> mexico = countries.get(0);
        assertTrue(mexico.containsKey("country"));
        assertEquals("MX", mexico.get("country"));
        assertTrue(mexico.containsKey("states"));
        List<Map<String, Object>> mexicanStates = (List<Map<String, Object>>) mexico.get("states");

        //assert states
        Map<String, Object> nuevoLeon = mexicanStates.get(0);
        assertTrue(nuevoLeon.containsKey("country"));
        assertEquals("MX", nuevoLeon.get("country"));
        assertTrue(nuevoLeon.containsKey("state"));
        assertEquals("Nuevo Leon", nuevoLeon.get("state"));
        assertTrue(nuevoLeon.containsKey("cities"));
        //TODO: assert all common fields

        List<Map<String, Object>> nuevoLeonCities = (List<Map<String, Object>>) nuevoLeon.get("cities");
        assertEquals(1, nuevoLeonCities.size());
        Map<String, Object> monterrey = nuevoLeonCities.get(0);
        assertTrue(monterrey.containsKey("country"));
        assertEquals("MX", monterrey.get("country"));
        assertTrue(monterrey.containsKey("state"));
        assertEquals("Nuevo Leon", monterrey.get("state"));
        assertTrue(monterrey.containsKey("city"));
        assertEquals("Monterrey", monterrey.get("city"));
        assertTrue(monterrey.containsKey("streets"));
        //TODO: assert all common fields

        List<Map<String, Object>> monterreyStreets = (List<Map<String, Object>>) monterrey.get("streets");
        assertEquals(1, monterreyStreets.size());
        Map<String, Object> calleRojo = monterreyStreets.get(0);
        assertTrue(calleRojo.containsKey("country"));
        assertEquals("MX", calleRojo.get("country"));
        assertTrue(calleRojo.containsKey("state"));
        assertEquals("Nuevo Leon", calleRojo.get("state"));
        assertTrue(calleRojo.containsKey("city"));
        assertEquals("Monterrey", calleRojo.get("city"));
        assertTrue(calleRojo.containsKey("numbers"));
        //TODO: assert all common fields

        //Assert Calle Rojo numbers
        List<Map<String, Object>> calleRojoNumbers = (List<Map<String, Object>>) calleRojo.get("numbers");
        assertEquals(3, calleRojoNumbers.size());
        Map<String, Object> one = calleRojoNumbers.get(0);
        assertTrue(one.containsKey("country"));
        assertEquals("MX", one.get("country"));
        assertTrue(one.containsKey("state"));
        assertEquals("Nuevo Leon", one.get("state"));
        assertTrue(one.containsKey("city"));
        assertEquals("Monterrey", one.get("city"));
        assertTrue(one.containsKey("number"));
        assertEquals("1", one.get("number"));

        Map<String, Object> two = calleRojoNumbers.get(1);
        assertTrue(two.containsKey("country"));
        assertEquals("MX", two.get("country"));
        assertTrue(two.containsKey("state"));
        assertEquals("Nuevo Leon", two.get("state"));
        assertTrue(two.containsKey("city"));
        assertEquals("Monterrey", two.get("city"));
        assertTrue(two.containsKey("number"));
        assertEquals("2", two.get("number"));

        Map<String, Object> three = calleRojoNumbers.get(2);
        assertTrue(three.containsKey("country"));
        assertEquals("MX", three.get("country"));
        assertTrue(three.containsKey("state"));
        assertEquals("Nuevo Leon", three.get("state"));
        assertTrue(three.containsKey("city"));
        assertEquals("Monterrey", three.get("city"));
        assertTrue(three.containsKey("number"));
        assertEquals("3", three.get("number"));

        //Get US
        Map<String, Object> unitedStates = countries.get(1);
        assertTrue(unitedStates.containsKey("country"));
        assertEquals("US", unitedStates.get("country"));
        assertTrue(unitedStates.containsKey("states"));
        List<Map<String, Object>> usStates = (List<Map<String, Object>>) unitedStates.get("states");

        //assert states
        Map<String, Object> hawaii = usStates.get(0);
        assertTrue(hawaii.containsKey("country"));
        assertEquals("US", hawaii.get("country"));
        assertTrue(hawaii.containsKey("state"));
        assertEquals("Hawaii", hawaii.get("state"));
        assertTrue(hawaii.containsKey("cities"));
        //TODO: assert all common fields

        List<Map<String, Object>> hawaiiCities = (List<Map<String, Object>>) hawaii.get("cities");
        assertEquals(1, hawaiiCities.size());
        Map<String, Object> Lihue = hawaiiCities.get(0);
        assertTrue(Lihue.containsKey("country"));
        assertEquals("US", Lihue.get("country"));
        assertTrue(Lihue.containsKey("state"));
        assertEquals("Hawaii", Lihue.get("state"));
        assertTrue(Lihue.containsKey("city"));
        assertEquals("Lihue", Lihue.get("city"));
        assertTrue(Lihue.containsKey("streets"));
        //TODO: assert all common fields

        List<Map<String, Object>> LihueStreets = (List<Map<String, Object>>) Lihue.get("streets");
        assertEquals(1, LihueStreets.size());
        Map<String, Object> riceSt = LihueStreets.get(0);
        assertTrue(riceSt.containsKey("country"));
        assertEquals("US", riceSt.get("country"));
        assertTrue(riceSt.containsKey("state"));
        assertEquals("Hawaii", riceSt.get("state"));
        assertTrue(riceSt.containsKey("city"));
        assertEquals("Lihue", riceSt.get("city"));
        assertTrue(riceSt.containsKey("numbers"));

        //Assert Rise Street numbers
        List<Map<String, Object>> riceStNumbers = (List<Map<String, Object>>) riceSt.get("numbers");
        assertEquals(1, riceStNumbers.size());
        one = riceStNumbers.get(0);
        assertTrue(one.containsKey("country"));
        assertEquals("US", one.get("country"));
        assertTrue(one.containsKey("state"));
        assertEquals("Hawaii", one.get("state"));
        assertTrue(one.containsKey("city"));
        assertEquals("Lihue", one.get("city"));
        assertTrue(one.containsKey("number"));
        assertEquals("1", one.get("number"));
    }

    @Test
    public void testGuitarDataModel() throws IOException {
        ConfigurationTemplateServiceImpl service = Common.getBean(ConfigurationTemplateServiceImpl.class);

        List<ConfigurationTemplateServiceImpl.CSVLevel> levels = new ArrayList<>();
        levels.add(ConfigurationTemplateServiceImpl
                .CSVLevel.newBuilder()
                .setGroupBy("type")
                .setInto("brands")
                .createCSVLevel());
        levels.add(ConfigurationTemplateServiceImpl
                .CSVLevel.newBuilder()
                .setGroupBy("brand")
                .setInto("models")
                .createCSVLevel());
        levels.add(ConfigurationTemplateServiceImpl
                .CSVLevel.newBuilder()
                .setGroupBy("model")
                .setInto("pickups")
                .createCSVLevel());
        levels.add(ConfigurationTemplateServiceImpl
                .CSVLevel.newBuilder()
                .setGroupBy("pickup")
                .setInto("colors")
                .createCSVLevel());

        CSVHierarchy hierarchy = CSVHierarchy.newBuilder()
                .setRoot("guitars")
                .setLevels(levels)
                .createCSVHierarchy();


        //Move template files into file store
        //Move files into file store
        String dataFile = "guitars.csv";
        setupResource(dataFile);
        String templateFile = "guitars.mustache";
        setupResource(templateFile);

        Map<String, Object>
                model = service.generateTemplateModel("default", dataFile, hierarchy);

        assertNotNull(model);

        String result = service.generateMangoConfigurationJson(
                "default",
                dataFile,
                templateFile,
                hierarchy
        );

        //The first level of the model should be a list of 2 brands
        assertNotNull(model.get(hierarchy.getRoot()));
        assertTrue(model.get(hierarchy.getRoot()) instanceof ArrayList);
        //Assert Type is size one and no common properties
        List<Map<String, Object>> guitars = (List<Map<String, Object>>) model.get(hierarchy.getRoot());
        assertEquals(2, guitars.size());

        Map<String, Object> electric = guitars.get(0);
        assertTrue(electric.containsKey("type"));
        assertEquals("Electric", electric.get("type"));
        assertTrue(electric.containsKey("brands"));
        List<Map<String, Object>> brands = (List<Map<String, Object>>) electric.get("brands");

        Map<String, Object> fender = brands.get(0);
        assertTrue(fender.containsKey("type"));
        assertEquals("Electric", fender.get("type"));
        assertTrue(fender.containsKey("brand"));
        assertEquals("Fender", fender.get("brand"));
        assertTrue(fender.containsKey("models"));

        List<Map<String, Object>> fenderModels = (List<Map<String, Object>>) fender.get("models");
        assertEquals(1, fenderModels.size());
        Map<String, Object> strat = fenderModels.get(0);
        assertTrue(strat.containsKey("type"));
        assertEquals("Electric", strat.get("type"));
        assertTrue(strat.containsKey("brand"));
        assertEquals("Fender", strat.get("brand"));
        assertTrue(strat.containsKey("model"));
        assertEquals("Strat", strat.get("model"));
        assertTrue(strat.containsKey("pickups"));

        List<Map<String, Object>> stratPickups = (List<Map<String, Object>>) strat.get("pickups");
        assertEquals(1, stratPickups.size());
        Map<String, Object> singleCoil = stratPickups.get(0);
        assertTrue(singleCoil.containsKey("type"));
        assertEquals("Electric", singleCoil.get("type"));
        assertTrue(singleCoil.containsKey("brand"));
        assertEquals("Fender", singleCoil.get("brand"));
        assertTrue(singleCoil.containsKey("model"));
        assertEquals("Strat", singleCoil.get("model"));
        assertTrue(singleCoil.containsKey("colors"));

        List<Map<String, Object>> singleCoilColors = (List<Map<String, Object>>) singleCoil.get("colors");
        assertEquals(2, singleCoilColors.size());
        Map<String, Object> one = singleCoilColors.get(0);
        assertTrue(one.containsKey("type"));
        assertEquals("Electric", one.get("type"));
        assertTrue(one.containsKey("brand"));
        assertEquals("Fender", one.get("brand"));
        assertTrue(one.containsKey("model"));
        assertEquals("Strat", one.get("model"));
        assertTrue(one.containsKey("color"));
        assertEquals("red", one.get("color"));

        Map<String, Object> two = singleCoilColors.get(1);
        assertTrue(two.containsKey("type"));
        assertEquals("Electric", two.get("type"));
        assertTrue(two.containsKey("brand"));
        assertEquals("Fender", two.get("brand"));
        assertTrue(two.containsKey("model"));
        assertEquals("Strat", two.get("model"));
        assertTrue(two.containsKey("color"));
        assertEquals("white", two.get("color"));

        Map<String, Object> acoustics = guitars.get(1);
        assertTrue(acoustics.containsKey("type"));
        assertEquals("Acoustic", acoustics.get("type"));
        assertTrue(acoustics.containsKey("brands"));
        brands = (List<Map<String, Object>>) acoustics.get("brands");

        Map<String, Object> yamaha = brands.get(0);
        assertTrue(yamaha.containsKey("type"));
        assertEquals("Acoustic", yamaha.get("type"));
        assertTrue(yamaha.containsKey("brand"));
        assertEquals("Yamaha", yamaha.get("brand"));
        assertTrue(yamaha.containsKey("models"));

        List<Map<String, Object>> yamahaModels = (List<Map<String, Object>>) yamaha.get("models");
        assertEquals(1, yamahaModels.size());
        Map<String, Object> apx = yamahaModels.get(0);
        assertTrue(apx.containsKey("type"));
        assertEquals("Acoustic", apx.get("type"));
        assertTrue(apx.containsKey("brand"));
        assertEquals("Yamaha", apx.get("brand"));
        assertTrue(apx.containsKey("model"));
        assertEquals("APX600BL", apx.get("model"));
        assertTrue(apx.containsKey("pickups"));

        List<Map<String, Object>> acousticPickups = (List<Map<String, Object>>) apx.get("pickups");
        assertEquals(1, acousticPickups.size());
        Map<String, Object> acousticCoil = acousticPickups.get(0);
        assertTrue(acousticCoil.containsKey("type"));
        assertEquals("Acoustic", acousticCoil.get("type"));
        assertTrue(acousticCoil.containsKey("brand"));
        assertEquals("Yamaha", acousticCoil.get("brand"));
        assertTrue(acousticCoil.containsKey("model"));
        assertEquals("APX600BL", acousticCoil.get("model"));
        assertTrue(acousticCoil.containsKey("colors"));

        List<Map<String, Object>> acousticColors = (List<Map<String, Object>>) acousticCoil.get("colors");
        assertEquals(2, acousticColors.size());
        one = acousticColors.get(0);
        assertTrue(one.containsKey("type"));
        assertEquals("Acoustic", one.get("type"));
        assertTrue(one.containsKey("brand"));
        assertEquals("Yamaha", one.get("brand"));
        assertTrue(one.containsKey("model"));
        assertEquals("APX600BL", one.get("model"));
        assertTrue(one.containsKey("color"));
        assertEquals("black", one.get("color"));

        two = acousticColors.get(1);
        assertTrue(two.containsKey("type"));
        assertEquals("Acoustic", two.get("type"));
        assertTrue(two.containsKey("brand"));
        assertEquals("Yamaha", two.get("brand"));
        assertTrue(two.containsKey("model"));
        assertEquals("APX600BL", two.get("model"));
        assertTrue(two.containsKey("color"));
        assertEquals("natural", two.get("color"));
    }

    /**
     * Copy resource to default filestore
     */
    private void setupResource(String csvName) throws IOException {
        String packageName = "/com/infiniteautomation/mango/spring/service/configurationTemplate/";


        //Move file into file store
        Path rootPath = Common.getBean(FileStoreService.class).getPathForWrite("default", "");
        Files.createDirectories(rootPath);
        InputStream is = getClass().getResourceAsStream(packageName + csvName);
        Path dst = rootPath.resolve(csvName);
        Files.copy(is, dst, StandardCopyOption.REPLACE_EXISTING);
    }
}
