/*
 * Copyright (C) 2021 RadixIot LLC. All rights reserved.
 */

package com.infiniteautomation.mango.configs;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.junit.Before;
import org.junit.Test;

import com.infiniteautomation.mango.spring.service.DataPointService;
import com.infiniteautomation.mango.spring.service.DataSourceService;
import com.infiniteautomation.mango.spring.service.EmportService;
import com.infiniteautomation.mango.util.ConfigurationExportData;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.MangoTestBase;
import com.serotonin.m2m2.vo.dataPoint.MockPointLocatorVO;
import com.serotonin.m2m2.vo.dataSource.mock.MockDataSourceVO;

import static org.junit.Assert.fail;

public class GenerateMockDataSourceConfig extends MangoTestBase {

    //Services
    protected DataPointService dataPointService;
    protected DataSourceService dataSourceService;

    @Before
    @Override
    public void before() {
        super.before();
        this.dataPointService = Common.getBean(DataPointService.class);
        this.dataSourceService = Common.getBean(DataSourceService.class);
    }

    /**
     * This test must be run manually because it does not start with 'test'
     */
    @Test
    public void generateConfig() {

        MockDataSourceVO vo = createMockDataSource(false);

        int dataPointCount = 400;
        for (int i = 0; i < dataPointCount; i++) {
            createMockDataPoint(vo, new MockPointLocatorVO(), false);
        }

        String filename = "mock-ds-configuration.json";
        File outputDir = new File("configs");
        outputDir.mkdirs();

        System.out.println("Writing dev configuration to file " + filename);
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(new File(outputDir, filename)))) {
            Common.getBean(EmportService.class)
                    .export(ConfigurationExportData.createExportDataMap(getExportElements()),
                            writer, 4);
            writer.flush();
        } catch (IOException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }

    }

    protected String[] getExportElements() {
        return new String[]{ConfigurationExportData.DATA_SOURCES,
                ConfigurationExportData.DATA_POINTS};
    }
}
