/*
 *  Copyright (C) 2023 RadixIot LLC. All rights reserved.
 */

package com.infiniteautomation.mango.spring.service;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import com.serotonin.m2m2.vo.permission.PermissionException;

public interface ConfigurationTemplateService {

    /**
     * generate configuration file from source data file.
     * Returns a list containing a map representation of the data input in order to convert it to template.
     *
     * @param fileStore - path to the fileStore.
     * @param filePath - path to the input data file.
     * @param keys - group of keys required for mapping the source data.
     * @return List<Map<String, Object>> - List of converted and mapped data from the source data file.
     * @throws IOException
     * @throws PermissionException
     */
    List<Map<String, Object>> generateConfig(String fileStore, String filePath, List<Map<String, String>> keys) throws
            IOException, PermissionException;

    /**
     * This method generates a Json String based on a template file path and data sample filepath.
     *
     * @param fileStore - path to the fileStore.
     * @param filePath - path to the input data file.
     * @param template - name of the template to be used for the mapped data.
     * @param keys - group of keys required for mapping the source data.
     * @return String - Json config structure generated from the source data file.
     * @throws IOException
     * @throws PermissionException
     */
    String generateMangoConfigurationJson(String fileStore, String filePath, String template, List<Map<String, String>> keys) throws IOException,
            PermissionException;
}
