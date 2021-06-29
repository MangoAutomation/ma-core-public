/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.infiniteautomation.mango.store;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.apache.http.HttpException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.AuthSchemes;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.junit.Assert;
import org.junit.Before;

import com.github.zafarkhaja.semver.Version;
import com.serotonin.json.JsonContext;
import com.serotonin.json.JsonException;
import com.serotonin.json.JsonWriter;
import com.serotonin.json.type.JsonArray;
import com.serotonin.json.type.JsonTypeReader;
import com.serotonin.json.type.JsonValue;
import com.serotonin.m2m2.UpgradeVersionState;
import com.serotonin.m2m2.module.ModuleRegistry;
import com.serotonin.web.http.HttpUtils4;

/**
 * Test response from store to various requests based on versions,
 *   this test assumes that the latest core version in the store is a PRODUCTION version
 *   and that there is at least 2 PRODUCTION state modules (mangoApi and any other)
 *
 * Upgrade: new modules and/or core for existing core
 * Update: a new core and any module for it
 *
 *
 *
 *
 * versionState - the state of the latest core in the store for the release channel of the request, if there is one.
 *
 * upgrades - Newest core and modules for anything installed
 *
 * upgradesError - Any error message
 *
 * newInstalls - New modules for the core in the upgrades list
 *
 * newInstalls-oldCore - Any module for the current core that isn't installed
 *
 * update-versionState - Version state of new core if there is one in updates list
 *
 * updates - New core and modules if Major version update exists
 *
 * missingModules -
 *
 * @author Terry Packer
 */
public class StoreLatestProductionTest {

    private static String latestProductionCore = "3.3.0";
    private static String latestProductionCorePrefix = "3.3.";
    private static String latestDevelopmentCore = "3.2.3-SNAPSHOT";

    private static JsonContext JSON_CONTEXT = new JsonContext();

    private static String[] modules = {"mangoApi"};
    private static Map<String, String> twoEightUpgradableModules = new HashMap<>();
    private static Map<String, String> twoEightLatestModules = new HashMap<>();

    //    private String[] modules = {"advancedScheduler", "BACnet", "excelReports",
    //            "mangoApi", "http", "mangoUI", "mangoNoSqlDatabase", "virtualDS", "watchlists"};

    static {
        twoEightLatestModules.put("mangoApi", "1.2.1");
        twoEightLatestModules.put("meta", "2.3.0");

        twoEightUpgradableModules.put("mangoApi", "1.2.0");
    }


    private String baseUrl = "https://teststore.mangoautomation.net";
    private Map<String, Object> json;
    Map<String, String> jsonModules;

    @Before
    public void before() {
        json = new HashMap<>();
        json.put("guid", "");
        json.put("description", "Test Instance");
        json.put("distributor", "IAS");

        //Setup Modules
        jsonModules = new HashMap<>();
        json.put("modules", jsonModules);
    }

    //@Test
    public void testTwoEightEightReleaseVersion() throws JsonException, IOException, HttpException {
        //Setup json
        json.put("upgradeVersionState", UpgradeVersionState.PRODUCTION);
        json.put("currentVersionState", UpgradeVersionState.PRODUCTION);

        jsonModules.put(ModuleRegistry.CORE_MODULE_NAME, "2.8.8");
        jsonModules.put("mangoApi", "1.2.0");


        String url = baseUrl + "/servlet/versionCheck";
        HttpPost post = new HttpPost(url);

        StringWriter stringWriter = new StringWriter();
        new JsonWriter(JSON_CONTEXT, stringWriter).writeObject(json);
        String requestData = stringWriter.toString();
        post.setEntity(new StringEntity(requestData));

        String responseData = HttpUtils4.getTextContent(getHttpClient(30000), post, 1);
        JsonTypeReader jsonReader = new JsonTypeReader(responseData);
        JsonValue response = jsonReader.read();
        //printResponse(response);

        Assert.assertNotNull(response.getJsonValue("versionState"));
        Assert.assertEquals("PRODUCTION", response.getJsonValue("versionState").toString());

        //Assert newInstalls-oldCore should be for 2.8
        Assert.assertNotNull(response.getJsonValue("newInstalls-oldCore"));
        JsonArray newInstallsOldCore = response.getJsonValue("newInstalls-oldCore").toJsonArray();
        Assert.assertEquals(true, newInstallsOldCore.size() > 0);
        for(JsonValue nioc : newInstallsOldCore) {
            if(nioc.getJsonValue("name").toString().equals("meta"))
                Assert.assertEquals(true, nioc.getJsonValue("fullVersion").toString().equals("2.3.0"));
        }

        //Assert update-versionState
        Assert.assertNotNull(response.getJsonValue("update-versionState"));
        Assert.assertEquals("PRODUCTION", response.getJsonValue("update-versionState").toString());

        //Assert updates for current core
        Assert.assertNotNull(response.getJsonValue("updates"));
        JsonArray updates = response.getJsonValue("updates").toJsonArray();
        Assert.assertEquals(true, updates.size() > 0);
        for(JsonValue upgrade : updates) {
            if(upgrade.getJsonValue("name").toString().equals("mangoApi"))
                Assert.assertEquals(true, upgrade.getJsonValue("fullVersion").toString().equals("1.2.1"));
        }

        //Assert upgrades
        Assert.assertNotNull(response.getJsonValue("upgrades"));
        JsonArray upgrades = response.getJsonValue("upgrades").toJsonArray();
        Assert.assertEquals(true, upgrades.size() > 0);
        for(JsonValue upgrade : upgrades) {
            if(upgrade.getJsonValue("name").toString().equals(ModuleRegistry.CORE_MODULE_NAME)) {
                Assert.assertEquals(true, upgrade.getJsonValue("fullVersion").toString().startsWith(latestProductionCore));
            }else {
                Assert.assertEquals(true, upgrade.getJsonValue("fullVersion").toString().startsWith(latestProductionCorePrefix));
            }
        }

        //Assert newInstalls should be for latest production core
        Assert.assertNotNull(response.getJsonValue("newInstalls"));
        JsonArray newInstalls = response.getJsonValue("newInstalls").toJsonArray();
        Assert.assertEquals(true, newInstalls.size() > 0);
        for(JsonValue install : newInstalls) {
            Assert.assertEquals(true, install.getJsonValue("fullVersion").toString().startsWith(latestProductionCorePrefix));
        }

    }

    //@Test
    public void testThreeThreeDevelopmentVersion() throws JsonException, IOException, HttpException {
        //Setup json
        json.put("upgradeVersionState", UpgradeVersionState.DEVELOPMENT);
        json.put("currentVersionState", UpgradeVersionState.DEVELOPMENT);

        jsonModules.put(ModuleRegistry.CORE_MODULE_NAME, "3.3.0-SNAPSHOT");
        jsonModules.put("mangoApi", "3.3.0-SNAPSHOT");

        String url = baseUrl + "/servlet/versionCheck";
        HttpPost post = new HttpPost(url);

        StringWriter stringWriter = new StringWriter();
        new JsonWriter(JSON_CONTEXT, stringWriter).writeObject(json);
        String requestData = stringWriter.toString();
        post.setEntity(new StringEntity(requestData));

        String responseData = HttpUtils4.getTextContent(getHttpClient(30000), post, 1);
        JsonTypeReader jsonReader = new JsonTypeReader(responseData);
        JsonValue response = jsonReader.read();
        //printResponse(response);

        Assert.assertNotNull(response.getJsonValue("versionState"));
        Assert.assertEquals("PRODUCTION", response.getJsonValue("versionState").toString());

        //Assert newInstalls-oldCore should be empty
        Assert.assertNotNull(response.getJsonValue("newInstalls-oldCore"));
        JsonArray newInstallsOldCore = response.getJsonValue("newInstalls-oldCore").toJsonArray();
        Assert.assertEquals(true, newInstallsOldCore.size() == 0);

        //Assert update-versionState
        Assert.assertNull(response.getJsonValue("update-versionState"));

        //Assert updates for this core iff major upgrade available
        Assert.assertNotNull(response.getJsonValue("updates"));
        JsonArray updates = response.getJsonValue("updates").toJsonArray();
        Assert.assertEquals(true, updates.size() == 0);


        //Assert upgrades
        Assert.assertNotNull(response.getJsonValue("upgrades"));
        JsonArray upgrades = response.getJsonValue("upgrades").toJsonArray();
        Assert.assertEquals(true, upgrades.size() > 0);
        for(JsonValue upgrade : upgrades) {
            Assert.assertEquals(true, upgrade.getJsonValue("fullVersion").toString().startsWith(latestProductionCorePrefix));
        }

        //Assert newInstalls should be for latest production core
        Assert.assertNotNull(response.getJsonValue("newInstalls"));
        JsonArray newInstalls = response.getJsonValue("newInstalls").toJsonArray();
        Assert.assertEquals(true, newInstalls.size() > 0);
        for(JsonValue install : newInstalls) {
            Assert.assertEquals(true, install.getJsonValue("fullVersion").toString().startsWith(latestProductionCorePrefix));
        }
    }

    public void testThreeThreeReleaseVersion() throws JsonException, IOException, HttpException {
        //Setup json
        json.put("upgradeVersionState", UpgradeVersionState.PRODUCTION);
        json.put("currentVersionState", UpgradeVersionState.PRODUCTION);

        jsonModules.put(ModuleRegistry.CORE_MODULE_NAME, "3.3.0");
        jsonModules.put("mangoApi", "3.3.0");

        String url = baseUrl + "/servlet/versionCheck";
        HttpPost post = new HttpPost(url);

        StringWriter stringWriter = new StringWriter();
        new JsonWriter(JSON_CONTEXT, stringWriter).writeObject(json);
        String requestData = stringWriter.toString();
        post.setEntity(new StringEntity(requestData));

        String responseData = HttpUtils4.getTextContent(getHttpClient(30000), post, 1);
        JsonTypeReader jsonReader = new JsonTypeReader(responseData);
        JsonValue response = jsonReader.read();
        //printResponse(response);

        Assert.assertNotNull(response.getJsonValue("versionState"));
        Assert.assertEquals("PRODUCTION", response.getJsonValue("versionState").toString());

        //Assert newInstalls-oldCore should be empty
        Assert.assertNotNull(response.getJsonValue("newInstalls-oldCore"));
        JsonArray newInstallsOldCore = response.getJsonValue("newInstalls-oldCore").toJsonArray();
        Assert.assertEquals(true, newInstallsOldCore.size() == 0);

        //Assert update-versionState
        Assert.assertNull(response.getJsonValue("update-versionState"));

        //Assert updates for this core iff major upgrade available
        Assert.assertNotNull(response.getJsonValue("updates"));
        JsonArray updates = response.getJsonValue("updates").toJsonArray();
        Assert.assertEquals(true, updates.size() == 0);


        //Assert upgrades
        Assert.assertNotNull(response.getJsonValue("upgrades"));
        JsonArray upgrades = response.getJsonValue("upgrades").toJsonArray();
        Assert.assertEquals(true, upgrades.size() > 0);
        for(JsonValue upgrade : upgrades) {
            Assert.assertEquals(true, upgrade.getJsonValue("fullVersion").toString().startsWith(latestProductionCorePrefix));
        }

        //Assert newInstalls should be for latest production core
        Assert.assertNotNull(response.getJsonValue("newInstalls"));
        JsonArray newInstalls = response.getJsonValue("newInstalls").toJsonArray();
        Assert.assertEquals(true, newInstalls.size() > 0);
        for(JsonValue install : newInstalls) {
            Assert.assertEquals(true, install.getJsonValue("fullVersion").toString().startsWith(latestProductionCorePrefix));
        }
    }


    //@Test
    public void testThreeTwoDevelopmentVersion() throws JsonException, IOException, HttpException {
        //Setup json
        json.put("upgradeVersionState", UpgradeVersionState.DEVELOPMENT);
        json.put("currentVersionState", UpgradeVersionState.DEVELOPMENT);

        jsonModules.put(ModuleRegistry.CORE_MODULE_NAME, latestDevelopmentCore);
        jsonModules.put("mangoApi", "3.2.3-SNAPSHOT");

        String url = baseUrl + "/servlet/versionCheck";
        HttpPost post = new HttpPost(url);

        StringWriter stringWriter = new StringWriter();
        new JsonWriter(JSON_CONTEXT, stringWriter).writeObject(json);
        String requestData = stringWriter.toString();
        post.setEntity(new StringEntity(requestData));

        String responseData = HttpUtils4.getTextContent(getHttpClient(30000), post, 1);
        JsonTypeReader jsonReader = new JsonTypeReader(responseData);
        JsonValue response = jsonReader.read();
        printResponse(response);

        Assert.assertNotNull(response.getJsonValue("versionState"));
        Assert.assertEquals("PRODUCTION", response.getJsonValue("versionState").toString());

        //Assert newInstalls-oldCore should be empty
        Assert.assertNotNull(response.getJsonValue("newInstalls-oldCore"));
        JsonArray newInstallsOldCore = response.getJsonValue("newInstalls-oldCore").toJsonArray();
        Assert.assertEquals(true, newInstallsOldCore.size() == 0);

        //Assert update-versionState
        Assert.assertNull(response.getJsonValue("update-versionState"));

        //Assert updates for this core iff major upgrade available
        Assert.assertNotNull(response.getJsonValue("updates"));
        JsonArray updates = response.getJsonValue("updates").toJsonArray();
        Assert.assertEquals(true, updates.size() == 0);


        //Assert upgrades
        Assert.assertNotNull(response.getJsonValue("upgrades"));
        JsonArray upgrades = response.getJsonValue("upgrades").toJsonArray();
        Assert.assertEquals(true, upgrades.size() > 0);
        for(JsonValue upgrade : upgrades) {
            Assert.assertEquals(true, upgrade.getJsonValue("fullVersion").toString().startsWith(latestProductionCorePrefix));
        }

        //Assert newInstalls should be for latest production core
        Assert.assertNotNull(response.getJsonValue("newInstalls"));
        JsonArray newInstalls = response.getJsonValue("newInstalls").toJsonArray();
        Assert.assertEquals(true, newInstalls.size() > 0);
        for(JsonValue install : newInstalls) {
            Assert.assertEquals(true, install.getJsonValue("fullVersion").toString().startsWith(latestProductionCorePrefix));
        }
    }

    //@Test
    public void testThreeTwoPreviousDevelopmentVersion() throws JsonException, IOException, HttpException {
        //Setup json
        json.put("upgradeVersionState", UpgradeVersionState.DEVELOPMENT);
        json.put("currentVersionState", UpgradeVersionState.DEVELOPMENT);

        jsonModules.put(ModuleRegistry.CORE_MODULE_NAME, "3.2.2-SNAPSHOT");
        jsonModules.put("mangoApi", "3.2.3-SNAPSHOT");

        String url = baseUrl + "/servlet/versionCheck";
        HttpPost post = new HttpPost(url);

        StringWriter stringWriter = new StringWriter();
        new JsonWriter(JSON_CONTEXT, stringWriter).writeObject(json);
        String requestData = stringWriter.toString();
        post.setEntity(new StringEntity(requestData));

        String responseData = HttpUtils4.getTextContent(getHttpClient(30000), post, 1);
        JsonTypeReader jsonReader = new JsonTypeReader(responseData);
        JsonValue response = jsonReader.read();
        printResponse(response);

        Assert.assertNotNull(response.getJsonValue("versionState"));
        Assert.assertEquals("PRODUCTION", response.getJsonValue("versionState").toString());

        //Assert newInstalls-oldCore should be empty
        Assert.assertNotNull(response.getJsonValue("newInstalls-oldCore"));
        JsonArray newInstallsOldCore = response.getJsonValue("newInstalls-oldCore").toJsonArray();
        Assert.assertEquals(true, newInstallsOldCore.size() == 0);

        //Assert update-versionState
        Assert.assertNull(response.getJsonValue("update-versionState"));

        //Assert updates for this core iff major upgrade available
        Assert.assertNotNull(response.getJsonValue("updates"));
        JsonArray updates = response.getJsonValue("updates").toJsonArray();
        Assert.assertEquals(true, updates.size() == 0);


        //Assert upgrades
        Assert.assertNotNull(response.getJsonValue("upgrades"));
        JsonArray upgrades = response.getJsonValue("upgrades").toJsonArray();
        Assert.assertEquals(true, upgrades.size() > 0);
        for(JsonValue upgrade : upgrades) {
            Assert.assertEquals(true, upgrade.getJsonValue("fullVersion").toString().startsWith(latestProductionCorePrefix));
        }

        //Assert newInstalls should be for latest production core
        Assert.assertNotNull(response.getJsonValue("newInstalls"));
        JsonArray newInstalls = response.getJsonValue("newInstalls").toJsonArray();
        Assert.assertEquals(true, newInstalls.size() > 0);
        for(JsonValue install : newInstalls) {
            Assert.assertEquals(true, install.getJsonValue("fullVersion").toString().startsWith(latestProductionCorePrefix));
        }
    }

    //@Test
    public void testThreeTwoReleaseVersion() throws JsonException, IOException, HttpException {
        //Setup json
        json.put("upgradeVersionState", UpgradeVersionState.PRODUCTION);
        json.put("currentVersionState", UpgradeVersionState.PRODUCTION);

        jsonModules.put(ModuleRegistry.CORE_MODULE_NAME, "3.2.0");
        jsonModules.put("mangoApi", "3.2.0");

        String url = baseUrl + "/servlet/versionCheck";
        HttpPost post = new HttpPost(url);

        StringWriter stringWriter = new StringWriter();
        new JsonWriter(JSON_CONTEXT, stringWriter).writeObject(json);
        String requestData = stringWriter.toString();
        post.setEntity(new StringEntity(requestData));

        String responseData = HttpUtils4.getTextContent(getHttpClient(30000), post, 1);
        JsonTypeReader jsonReader = new JsonTypeReader(responseData);
        JsonValue response = jsonReader.read();
        printResponse(response);

        Assert.assertNotNull(response.getJsonValue("versionState"));
        Assert.assertEquals("PRODUCTION", response.getJsonValue("versionState").toString());

        //Assert newInstalls-oldCore should be empty
        Assert.assertNotNull(response.getJsonValue("newInstalls-oldCore"));
        JsonArray newInstallsOldCore = response.getJsonValue("newInstalls-oldCore").toJsonArray();
        Assert.assertEquals(true, newInstallsOldCore.size() == 0);

        //Assert update-versionState
        Assert.assertNull(response.getJsonValue("update-versionState"));

        //Assert updates for this core iff major upgrade available
        Assert.assertNotNull(response.getJsonValue("updates"));
        JsonArray updates = response.getJsonValue("updates").toJsonArray();
        Assert.assertEquals(true, updates.size() == 0);


        //Assert upgrades
        Assert.assertNotNull(response.getJsonValue("upgrades"));
        JsonArray upgrades = response.getJsonValue("upgrades").toJsonArray();
        Assert.assertEquals(true, upgrades.size() == 2);
        for(JsonValue upgrade : upgrades) {
            Assert.assertEquals(true, upgrade.getJsonValue("fullVersion").toString().startsWith(latestProductionCorePrefix));
        }

        //Assert newInstalls should be for latest production core
        Assert.assertNotNull(response.getJsonValue("newInstalls"));
        JsonArray newInstalls = response.getJsonValue("newInstalls").toJsonArray();
        Assert.assertEquals(true, newInstalls.size() > 0);
        for(JsonValue install : newInstalls) {
            Assert.assertEquals(true, install.getJsonValue("fullVersion").toString().startsWith(latestProductionCorePrefix));
        }
    }


    /**
     * Setup the core and modules for request
     * @param coreVersion
     * @param moduleVersionState
     */
    protected void setupModules(Version coreVersion, String moduleVersion) {

        switch(coreVersion.getPreReleaseVersion()){
            case "SNAPSHOT":
                json.put("currentVersionState", UpgradeVersionState.DEVELOPMENT);
                break;
            default:
                json.put("currentVersionState", UpgradeVersionState.PRODUCTION);
        }

        jsonModules.put(ModuleRegistry.CORE_MODULE_NAME, coreVersion.toString());
        for(String module : modules) {
            jsonModules.put(module, moduleVersion);
        }
    }


    protected HttpClient getHttpClient(int timeout) {
        // Create global request configuration
        RequestConfig defaultRequestConfig = RequestConfig.custom()
                .setExpectContinueEnabled(true)
                .setTargetPreferredAuthSchemes(Arrays.asList(AuthSchemes.NTLM, AuthSchemes.DIGEST))
                .setProxyPreferredAuthSchemes(Arrays.asList(AuthSchemes.BASIC)).setSocketTimeout(timeout)
                .setConnectTimeout(timeout).build();
        // Create an HttpClient with the given custom dependencies and configuration.
        CloseableHttpClient httpclient = HttpClients.custom().setDefaultRequestConfig(defaultRequestConfig).build();
        return httpclient;
    }

    /**
     * Helper to print the json neatly
     * @param response
     * @throws IOException
     * @throws JsonException
     */
    protected void printResponse(JsonValue response) throws IOException, JsonException{
        StringWriter stringWriter = new StringWriter();
        JsonWriter jsonWriter = new JsonWriter(JSON_CONTEXT, stringWriter);
        jsonWriter.setPrettyOutput(true);
        jsonWriter.setPrettyIndent(4);
        jsonWriter.writeObject(response);
        System.out.print(stringWriter.toString());
    }
}
