/*
 *  Copyright (C) 2023 RadixIot LLC. All rights reserved.
 */

package com.infiniteautomation.mango.spring.service;

import static org.junit.Assert.*;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.MangoTestBase;

public class ConfigurationTemplateServiceTest extends MangoTestBase {

    @Test
    public void testConfigurationTemplateService() throws IOException {
        ConfigurationTemplateService service = Common.getBean(ConfigurationTemplateService.class);

        //Move template files into file store
        Path rootPath = Common.getBean(FileStoreService.class).getPathForWrite("default", "");
        Files.createDirectories(rootPath);
        InputStream is = getClass().getResourceAsStream("/configTemplateTestData.csv");

        Path dst = rootPath.resolve("configTemplateTestData.csv");
        Files.copy(is, dst, StandardCopyOption.REPLACE_EXISTING);
        List<Map<String, Object>> result = service.generateConfig("default", "configTemplateTestData.csv");

        assertNotNull(result);
        assertTrue(result.size() == 2);

    }
}
