/*
 *  Copyright (C) 2023 RadixIot LLC. All rights reserved.
 */

package com.infiniteautomation.mango.spring.service;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.thymeleaf.IEngineConfiguration;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.AbstractConfigurableTemplateResolver;
import org.thymeleaf.templateresource.FileTemplateResource;
import org.thymeleaf.templateresource.ITemplateResource;

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
    public Map<String, Object> generateTemplateModel(String fileStore, String filePath, CSVHierarchy csvHierarchy) throws IOException,
            PermissionException {
        permissionService.ensureAdminRole(Common.getUser());

        Path path = fileStoreService.getPathForRead(fileStore, filePath);
        try (Reader in = new InputStreamReader(Files.newInputStream(path), StandardCharsets.UTF_8)) {
            CSVReader csvReader = new CSVReader(in);
            List<Map<String, Object>> readFileStructure = readCSV(csvReader);

            List<Map<String, Object>> levels = groupByLevels(readFileStructure, csvHierarchy.getLevels());

            //Do final level mapping for root object
            Map<String, Object> templateModel = new LinkedHashMap<>();
            templateModel.put(csvHierarchy.getRoot(), levels);

            return templateModel;
        }
    }

    @Override
    public String generateMangoConfigurationJson(String fileStore, String filePath, String template, CSVHierarchy csvHierarchy) throws IOException,
            PermissionException {

        permissionService.ensureAdminRole(Common.getUser());

        Map<String, Object> model = generateTemplateModel(fileStore, filePath, csvHierarchy);

        FileStoreTemplateResolver resolver = new FileStoreTemplateResolver(fileStoreService, fileStore);
        resolver.setTemplateMode(TemplateMode.TEXT);
        TemplateEngine templateEngine = new TemplateEngine();
        templateEngine.setTemplateResolver(resolver);

        StringWriter writer = new StringWriter();
        templateEngine.process(template, new Context(Common.getLocale(), model), writer);
        StringBuilder result = new StringBuilder(writer.toString());

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
    public static final class CSVHierarchy {
        private final String root;
        private final List<CSVLevel> levels;

        public CSVHierarchy(String root, List<CSVLevel> levels) {
            this.root = root;
            this.levels = levels;
        }

        public String getRoot() {
            return root;
        }

        public List<CSVLevel> getLevels() {
            return levels;
        }

        public static CSVHierarchyBuilder newBuilder() {
            return new CSVHierarchyBuilder();
        }


        public static final class CSVHierarchyBuilder {
            private String root;
            private List<ConfigurationTemplateServiceImpl.CSVLevel> levels;

            public CSVHierarchyBuilder setRoot(String root) {
                this.root = root;
                return this;
            }

            public CSVHierarchyBuilder setLevels(List<ConfigurationTemplateServiceImpl.CSVLevel> levels) {
                this.levels = levels;
                return this;
            }

            public CSVHierarchy createCSVHierarchy() {
                return new CSVHierarchy(root, levels);
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

            public ConfigurationTemplateServiceImpl.CSVLevel createCSVLevel() {
                return new ConfigurationTemplateServiceImpl.CSVLevel(groupBy, into);
            }
        }
    }

    private static class FileStoreTemplateResolver extends AbstractConfigurableTemplateResolver {

        private final FileStoreService service;
        private final String fileStore;

        public FileStoreTemplateResolver(FileStoreService service, String fileStore) {
            this.service = service;
            this.fileStore = fileStore;
        }

        @Override
        protected ITemplateResource computeTemplateResource(final IEngineConfiguration configuration, final String ownerTemplate, final String template, final String resourceName, final String characterEncoding, final Map<String, Object> templateResolutionAttributes) {
            Path path = service.getPathForRead(fileStore, resourceName);
            return new FileTemplateResource(path.toFile(), characterEncoding);
        }

    }
}
