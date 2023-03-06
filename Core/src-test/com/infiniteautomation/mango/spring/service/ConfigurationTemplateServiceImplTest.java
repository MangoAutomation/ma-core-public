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
    private final String dataSourceFilePath =
            "/com/infiniteautomation/mango/spring/service/configurationTemplate/configTemplateTestData.csv";
    private final String templatePath =
            "/com/infiniteautomation/mango/spring/service/configurationTemplate/roles.mustache";
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
        //TODO: assert all common fields

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
        //TODO: assert all common fields

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
        //TODO: assert all common fields

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

        Map<String, Object> two = calleRojoNumbers.get(1);
        assertTrue(two.containsKey("country"));
        assertTrue(two.get("country").equals("MX"));
        assertTrue(two.containsKey("state"));
        assertTrue(two.get("state").equals("Nuevo Leon"));
        assertTrue(two.containsKey("city"));
        assertTrue(two.get("city").equals("Monterrey"));
        assertTrue(two.containsKey("number"));
        assertTrue(two.get("number").equals("2"));

        Map<String, Object> three = calleRojoNumbers.get(2);
        assertTrue(three.containsKey("country"));
        assertTrue(three.get("country").equals("MX"));
        assertTrue(three.containsKey("state"));
        assertTrue(three.get("state").equals("Nuevo Leon"));
        assertTrue(three.containsKey("city"));
        assertTrue(three.get("city").equals("Monterrey"));
        assertTrue(three.containsKey("number"));
        assertTrue(three.get("number").equals("3"));

        //Get US
        Map<String, Object> unitedStates = countries.get(1);
        assertTrue(unitedStates.containsKey("country"));
        assertTrue(unitedStates.get("country").equals("US"));
        assertTrue(unitedStates.containsKey("states"));
        List<Map<String, Object>> usStates = (List<Map<String, Object>>) unitedStates.get("states");

        //assert states
        Map<String, Object> hawaii = usStates.get(0);
        assertTrue(hawaii.containsKey("country"));
        assertTrue(hawaii.get("country").equals("US"));
        assertTrue(hawaii.containsKey("state"));
        assertTrue(hawaii.get("state").equals("Hawaii"));
        assertTrue(hawaii.containsKey("cities"));
        //TODO: assert all common fields

        List<Map<String, Object>> hawaiiCities = (List<Map<String, Object>>) hawaii.get("cities");
        assertTrue(hawaiiCities.size() == 1);
        Map<String, Object> Lihue = hawaiiCities.get(0);
        assertTrue(Lihue.containsKey("country"));
        assertTrue(Lihue.get("country").equals("US"));
        assertTrue(Lihue.containsKey("state"));
        assertTrue(Lihue.get("state").equals("Hawaii"));
        assertTrue(Lihue.containsKey("city"));
        assertTrue(Lihue.get("city").equals("Lihue"));
        assertTrue(Lihue.containsKey("streets"));
        //TODO: assert all common fields

        List<Map<String, Object>> LihueStreets = (List<Map<String, Object>>) Lihue.get("streets");
        assertTrue(LihueStreets.size() == 1);
        Map<String, Object> riceSt = LihueStreets.get(0);
        assertTrue(riceSt.containsKey("country"));
        assertTrue(riceSt.get("country").equals("US"));
        assertTrue(riceSt.containsKey("state"));
        assertTrue(riceSt.get("state").equals("Hawaii"));
        assertTrue(riceSt.containsKey("city"));
        assertTrue(riceSt.get("city").equals("Lihue"));
        assertTrue(riceSt.containsKey("numbers"));

        //Assert Rise Street numbers
        List<Map<String, Object>> riceStNumbers = (List<Map<String, Object>>) riceSt.get("numbers");
        assertTrue(riceStNumbers.size() == 1);
        one = riceStNumbers.get(0);
        assertTrue(one.containsKey("country"));
        assertTrue(one.get("country").equals("US"));
        assertTrue(one.containsKey("state"));
        assertTrue(one.get("state").equals("Hawaii"));
        assertTrue(one.containsKey("city"));
        assertTrue(one.get("city").equals("Lihue"));
        assertTrue(one.containsKey("number"));
        assertTrue(one.get("number").equals("1"));
    }

    @Test
    public void testGuitarDataModel() throws IOException {
        ConfigurationTemplateServiceImpl service = Common.getBean(ConfigurationTemplateServiceImpl.class);

        String filename = "guitars.csv";
        String csvFile = "/com/infiniteautomation/mango/spring/service/configurationTemplate/" + filename;

        List<ConfigurationTemplateServiceImpl.CSVLevel> levels = new ArrayList<>();
        levels.add(ConfigurationTemplateServiceImpl
                .CSVLevel.newBuilder()
                .setGroupBy("type")
                .setInto("brands")
                .createTemplateLevel());
        levels.add(ConfigurationTemplateServiceImpl
                .CSVLevel.newBuilder()
                .setGroupBy("brand")
                .setInto("models")
                .createTemplateLevel());
        levels.add(ConfigurationTemplateServiceImpl
                .CSVLevel.newBuilder()
                .setGroupBy("model")
                .setInto("pickups")
                .createTemplateLevel());
        levels.add(ConfigurationTemplateServiceImpl
                .CSVLevel.newBuilder()
                .setGroupBy("pickup")
                .setInto("colors")
                .createTemplateLevel());

        ConfigurationTemplateServiceImpl.CSVHiearchy hierarchy = ConfigurationTemplateServiceImpl.CSVHiearchy.newBuilder()
                .setRoot("guitars")
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

        String result = service.generateMangoConfigurationJson(
                "default",
                "guitars.csv",
                "/com/infiniteautomation/mango/spring/service/configurationTemplate/guitars.mustache",
                hierarchy
        );

        //The first level of the model should be a list of 2 brands
        assertNotNull(model.get(hierarchy.getRoot()));
        assertTrue(model.get(hierarchy.getRoot()) instanceof ArrayList);
        //Assert Type is size one and no common properties
        List<Map<String, Object>> guitars = (List<Map<String, Object>>) model.get(hierarchy.getRoot());
        assertTrue(guitars.size() == 2);

        Map<String, Object> electric = guitars.get(0);
        assertTrue(electric.containsKey("type"));
        assertTrue(electric.get("type").equals("Electric"));
        assertTrue(electric.containsKey("brands"));
        List<Map<String, Object>> brands = (List<Map<String, Object>>) electric.get("brands");

        Map<String, Object> fender = brands.get(0);
        assertTrue(fender.containsKey("type"));
        assertTrue(fender.get("type").equals("Electric"));
        assertTrue(fender.containsKey("brand"));
        assertTrue(fender.get("brand").equals("Fender"));
        assertTrue(fender.containsKey("models"));

        List<Map<String, Object>> fenderModels = (List<Map<String, Object>>) fender.get("models");
        assertTrue(fenderModels.size() == 1);
        Map<String, Object> strat = fenderModels.get(0);
        assertTrue(strat.containsKey("type"));
        assertTrue(strat.get("type").equals("Electric"));
        assertTrue(strat.containsKey("brand"));
        assertTrue(strat.get("brand").equals("Fender"));
        assertTrue(strat.containsKey("model"));
        assertTrue(strat.get("model").equals("Strat"));
        assertTrue(strat.containsKey("pickups"));

        List<Map<String, Object>> stratPickups = (List<Map<String, Object>>) strat.get("pickups");
        assertTrue(stratPickups.size() == 1);
        Map<String, Object> singleCoil = stratPickups.get(0);
        assertTrue(singleCoil.containsKey("type"));
        assertTrue(singleCoil.get("type").equals("Electric"));
        assertTrue(singleCoil.containsKey("brand"));
        assertTrue(singleCoil.get("brand").equals("Fender"));
        assertTrue(singleCoil.containsKey("model"));
        assertTrue(singleCoil.get("model").equals("Strat"));
        assertTrue(singleCoil.containsKey("colors"));

        List<Map<String, Object>> singleCoilColors = (List<Map<String, Object>>) singleCoil.get("colors");
        assertTrue(singleCoilColors.size() == 2);
        Map<String, Object> one = singleCoilColors.get(0);
        assertTrue(one.containsKey("type"));
        assertTrue(one.get("type").equals("Electric"));
        assertTrue(one.containsKey("brand"));
        assertTrue(one.get("brand").equals("Fender"));
        assertTrue(one.containsKey("model"));
        assertTrue(one.get("model").equals("Strat"));
        assertTrue(one.containsKey("color"));
        assertTrue(one.get("color").equals("red"));

        Map<String, Object> two = singleCoilColors.get(1);
        assertTrue(two.containsKey("type"));
        assertTrue(two.get("type").equals("Electric"));
        assertTrue(two.containsKey("brand"));
        assertTrue(two.get("brand").equals("Fender"));
        assertTrue(two.containsKey("model"));
        assertTrue(two.get("model").equals("Strat"));
        assertTrue(two.containsKey("color"));
        assertTrue(two.get("color").equals("white"));

        Map<String, Object> acoustics = guitars.get(1);
        assertTrue(acoustics.containsKey("type"));
        assertTrue(acoustics.get("type").equals("Acoustic"));
        assertTrue(acoustics.containsKey("brands"));
        brands = (List<Map<String, Object>>) acoustics.get("brands");

        Map<String, Object> yamaha = brands.get(0);
        assertTrue(yamaha.containsKey("type"));
        assertTrue(yamaha.get("type").equals("Acoustic"));
        assertTrue(yamaha.containsKey("brand"));
        assertTrue(yamaha.get("brand").equals("Yamaha"));
        assertTrue(yamaha.containsKey("models"));

        List<Map<String, Object>> yamahaModels = (List<Map<String, Object>>) yamaha.get("models");
        assertTrue(yamahaModels.size() == 1);
        Map<String, Object> apx = yamahaModels.get(0);
        assertTrue(apx.containsKey("type"));
        assertTrue(apx.get("type").equals("Acoustic"));
        assertTrue(apx.containsKey("brand"));
        assertTrue(apx.get("brand").equals("Yamaha"));
        assertTrue(apx.containsKey("model"));
        assertTrue(apx.get("model").equals("APX600BL"));
        assertTrue(apx.containsKey("pickups"));

        List<Map<String, Object>> acousticPickups = (List<Map<String, Object>>) apx.get("pickups");
        assertTrue(acousticPickups.size() == 1);
        Map<String, Object> acousticCoil = acousticPickups.get(0);
        assertTrue(acousticCoil.containsKey("type"));
        assertTrue(acousticCoil.get("type").equals("Acoustic"));
        assertTrue(acousticCoil.containsKey("brand"));
        assertTrue(acousticCoil.get("brand").equals("Yamaha"));
        assertTrue(acousticCoil.containsKey("model"));
        assertTrue(acousticCoil.get("model").equals("APX600BL"));
        assertTrue(acousticCoil.containsKey("colors"));

        List<Map<String, Object>> acousticColors = (List<Map<String, Object>>) acousticCoil.get("colors");
        assertTrue(acousticColors.size() == 2);
        one = acousticColors.get(0);
        assertTrue(one.containsKey("type"));
        assertTrue(one.get("type").equals("Acoustic"));
        assertTrue(one.containsKey("brand"));
        assertTrue(one.get("brand").equals("Yamaha"));
        assertTrue(one.containsKey("model"));
        assertTrue(one.get("model").equals("APX600BL"));
        assertTrue(one.containsKey("color"));
        assertTrue(one.get("color").equals("black"));

        two = acousticColors.get(1);
        assertTrue(two.containsKey("type"));
        assertTrue(two.get("type").equals("Acoustic"));
        assertTrue(two.containsKey("brand"));
        assertTrue(two.get("brand").equals("Yamaha"));
        assertTrue(two.containsKey("model"));
        assertTrue(two.get("model").equals("APX600BL"));
        assertTrue(two.containsKey("color"));
        assertTrue(two.get("color").equals("natural"));
    }
}
