/*
 *  Copyright (C) 2023 RadixIot LLC. All rights reserved.
 */

package com.infiniteautomation.mango.spring.service;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.vo.permission.PermissionException;

import au.com.bytecode.opencsv.CSVReader;

@Service("configurationTemplateService")
public class ConfigurationTemplateServiceImpl implements ConfigurationTemplateService {

    private static final Logger LOG = LoggerFactory.getLogger(ConfigurationTemplateServiceImpl.class);

    private final FileStoreService fileStoreService;

    private final PermissionService permissionService;

    public ConfigurationTemplateServiceImpl(FileStoreService fileStoreService, PermissionService permissionService) {
        this.fileStoreService = fileStoreService;
        this.permissionService = permissionService;
    }

    @Override
    public Map<String, Object> generateTemplateModel(String fileStore, String filePath, CSVHiearchy csvHiearchy) throws IOException,
            PermissionException {
        permissionService.ensureAdminRole(Common.getUser());

        Path path = fileStoreService.getPathForRead(fileStore, filePath);
        try (Reader in = new InputStreamReader(Files.newInputStream(path), Charset.defaultCharset())) {
            CSVReader csvReader = new CSVReader(in);
            List<Map<String, Object>> readFileStructure = readCSV(csvReader);

            List<Map<String, Object>> levels = groupByLevels(readFileStructure, csvHiearchy.getLevels());

            //Do final level mapping for root object
            Map<String, Object> templateModel = new LinkedHashMap<>();
            templateModel.put(csvHiearchy.getRoot(), levels);

            return templateModel;
        }
    }

    @Override
    public String generateMangoConfigurationJson(String fileStore, String filePath, String template, CSVHiearchy csvHiearchy) throws IOException,
            PermissionException {
        MustacheFactory mf = new DefaultMustacheFactory();
        Map<String, Object> model = generateTemplateModel(fileStore, filePath, csvHiearchy);

        ClassPathResource cp = new ClassPathResource(template);
        Mustache m = mf.compile(cp.getPath());
        StringWriter writer = new StringWriter();
        m.execute(writer, model).flush();
        StringBuilder result = new StringBuilder(writer.toString());

        //TODO: Fix this trailing comma situation
        int trailingComma = result.lastIndexOf(",");
        result.delete(trailingComma, trailingComma+1);
        return result.toString();
    }

    /**
     * returns a group from the list of map<String, Object> based on the provided keys.
     *
     * @param array - List of data from the data source file.
     * @param level - map of keys used for structuring data for the configuration file.
     * @return grouped data based on the provided keys in a hierarchical structure.
     */
    private List<Map<String, Object>> groupByLevel(List<Map<String, Object>> array, CSVLevel level) {
        Map<Object, Map<String, Object>> map = new LinkedHashMap<>();

        for (Map<String, Object> item : array) {
            Object keyValue = item.get(level.getGroupBy());
            if (!map.containsKey(keyValue)) {
                Map<String, Object> group = new LinkedHashMap<>();
                group.put(level.getGroupBy(), keyValue);
                group.put(level.getInto(), new ArrayList<Map<String, Object>>());
                map.put(keyValue, group);
            }
            ((List)map.get(keyValue).get(level.getInto())).add(item);
        }
        List<Map<String, Object>> groups = new ArrayList<>(map.values());

        // find common properties from the children and copy them to the group
        for (Map<String, Object> group : groups) {
            Map<String, Object> common = findCommonProperties((List<Map<String, Object>>) group.get(level.getInto()));
            group.putAll(common);
        }

        return groups;
    }

    /**
     * Based on the provided data from the source file, generates a hierarchical data structure for the template to process.
     * @param data data to be mapped for the template configuration Json.
     * @param structure Structure of values in which the data is going to be mapped.
     * @return List<Map<String, Object>> List of hierarchical structure map based on provided keys for the template processor.
     */
    private List<Map<String, Object>> groupByLevels(List<Map<String, Object>> data, List<CSVLevel> structure) {
        if (structure.isEmpty()) {
            return data;
        }
        CSVLevel levelZero = structure.get(0);
        List<CSVLevel> levels = structure.subList(1, structure.size());
        List<Map<String, Object>> groups = groupByLevel(data, levelZero);
        for (Map<String, Object> group : groups) {
            group.put(levelZero.getInto(), groupByLevels((List<Map<String, Object>>) group.get(levelZero.getInto()), levels));
        }
        return groups;
    }

    /**
     * find common properties from each hierarchical section or subgroup.
     *
     * @param data list of sub data from the mapped group structure.
     * @return Map<String, Object> generated from common properties sor each section of the group structure.
     */
    private Map<String, Object> findCommonProperties(List<Map<String, Object>> data) {
        if (data.isEmpty()) {
            return new LinkedHashMap<>();
        }
        Map<String, Object> common = new LinkedHashMap<>(data.get(0));
        for (Map<String, Object> item : data) {
            for (Map.Entry<String, Object> entry : new LinkedHashMap<>(common).entrySet()) {
                String k = entry.getKey();
                Object v = entry.getValue();
                if (!item.get(k).equals(v)) {
                    common.remove(k);
                }
            }
        }
        return common;
    }

    private List<Map<String, Object>> readCSV(CSVReader reader) throws IOException {
        List<Map<String, Object>> result = new ArrayList<>();
        String[] header = reader.readNext();
        String[] row;

        while((row = reader.readNext()) != null) {
            //TODO what if there are empty lines at the end of the file
            // probably should TRIM or something?  maybe a setting in the CSVReader?
            Map<String, Object> temp = new LinkedHashMap<>();
            for(int i =0; i < header.length; i ++) {
                temp.put(header[i], row[i]);
            }
            result.add(temp);
        }
        return result;
    }

    /**
     * Container for definition of CSV hierarhcy
     */
    public static final class CSVHiearchy {
        private final String root;
        private final List<CSVLevel> levels;

        public CSVHiearchy(String root, List<CSVLevel> levels) {
            this.root = root;
            this.levels = levels;
        }

        public String getRoot() {
            return root;
        }

        public List<CSVLevel> getLevels() {
            return levels;
        }

        public static CSVHiearchyBuilder newBuilder() {
            return new CSVHiearchyBuilder();
        }


        public static final class CSVHiearchyBuilder {
            private String root;
            private List<ConfigurationTemplateServiceImpl.CSVLevel> levels;

            public CSVHiearchyBuilder setRoot(String root) {
                this.root = root;
                return this;
            }

            public CSVHiearchyBuilder setLevels(List<ConfigurationTemplateServiceImpl.CSVLevel> levels) {
                this.levels = levels;
                return this;
            }

            public ConfigurationTemplateServiceImpl.CSVHiearchy createCSVHiearchy() {
                return new ConfigurationTemplateServiceImpl.CSVHiearchy(root, levels);
            }
        }
    }



    /**
     * Defines one level of the model for the template.
     *
     * Group by 'sites' into 'data halls'
     */
    public static final class CSVLevel {
        private final String groupBy;
        private final String into;

        public CSVLevel(String groupBy, String into) {
            this.groupBy = groupBy;
            this.into = into;
        }

        public String getGroupBy() {
            return groupBy;
        }

        public String getInto() {
            return into;
        }

        public static CSVLevelBuilder newBuilder() {
            return new CSVLevelBuilder();
        }

        public static final class CSVLevelBuilder {
            private String groupBy;
            private String into;

            public CSVLevelBuilder setGroupBy(String groupBy) {
                this.groupBy = groupBy;
                return this;
            }

            public CSVLevelBuilder setInto(String into) {
                this.into = into;
                return this;
            }

            public ConfigurationTemplateServiceImpl.CSVLevel createTemplateLevel() {
                return new ConfigurationTemplateServiceImpl.CSVLevel(groupBy, into);
            }
        }
    }
}
