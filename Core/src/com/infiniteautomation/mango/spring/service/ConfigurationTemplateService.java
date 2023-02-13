/*
 *  Copyright (C) 2023 RadixIot LLC. All rights reserved.
 */

package com.infiniteautomation.mango.spring.service;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.vo.permission.PermissionException;

import au.com.bytecode.opencsv.CSVReader;

@Service
public class ConfigurationTemplateService {

    private static final Logger LOG = LoggerFactory.getLogger(ConfigurationTemplateService.class);

    private final FileStoreService fileStoreService;

    private final PermissionService permissionService;

    public ConfigurationTemplateService(FileStoreService fileStoreService, PermissionService permissionService) {
        this.fileStoreService = fileStoreService;
        this.permissionService = permissionService;
    }

    /**
     *
     * @param fileStore
     * @param filePath
     * @return
     * @throws IOException
     * @throws PermissionException
     */
    public List<Map<String, Object>> generateConfig(String fileStore, String filePath) throws IOException,
            PermissionException {
        permissionService.ensureAdminRole(Common.getUser());

        Path path = fileStoreService.getPathForRead(fileStore, filePath);
        try (Reader in = new InputStreamReader(Files.newInputStream(path), Charset.defaultCharset())) {
            CSVReader csvReader = new CSVReader(in);
            List<Map<String, Object>> readFileStructure = readCSV(csvReader);
            List<Map<String, String>> keys = new ArrayList<>(){{
                add(new HashMap<>(){{
                    put("groupKey", "site");
                    put("childrenKey", "dataHalls");}});
                add(new HashMap<>(){{
                    put("groupKey", "dataHall");
                    put("childrenKey", "devices");}});
            }};

            List<Map<String, Object>> result = groupByKeys(readFileStructure, keys);
            return result;
        }
    }

    public static List<Map<String, Object>> groupByKey(List<Map<String, Object>> array, Map<String, String> key) {
        Map<Object, Map<String, Object>> map = new HashMap<>();
        for (Map<String, Object> item : array) {
            Object keyValue = item.get(key.get("groupKey"));
            if (!map.containsKey(keyValue)) {
                Map<String, Object> group = new HashMap<>();
                group.put(key.get("groupKey"), keyValue);
                group.put(key.get("childrenKey"), new ArrayList<Map<String, Object>>());
                map.put(keyValue, group);
            }
            ((List)map.get(keyValue).get(key.get("childrenKey"))).add(item);
        }
        List<Map<String, Object>> groups = new ArrayList<>(map.values());

        // find common properties from the children and copy them to the group
        for (Map<String, Object> group : groups) {
            Map<String, Object> common = findCommonProperties((List<Map<String, Object>>) group.get(key.get("childrenKey")));
            group.putAll(common);
        }

        return groups;
    }

    public static List<Map<String, Object>> groupByKeys(List<Map<String, Object>> data, List<Map<String, String>> keys) {
        if (keys.isEmpty()) {
            return data;
        }
        Map<String, String> firstKey = keys.get(0);
        List<Map<String, String>> nextKeys = keys.subList(1, keys.size());
        List<Map<String, Object>> groups = groupByKey(data, firstKey);
        for (Map<String, Object> group : groups) {
            group.put(firstKey.get("childrenKey"), groupByKeys((List<Map<String, Object>>) group.get(firstKey.get("childrenKey")), nextKeys));
        }
        return groups;
    }

    public static Map<String, Object> findCommonProperties(List<Map<String, Object>> data) {
        if (data.isEmpty()) {
            return new HashMap<>();
        }
        Map<String, Object> common = new HashMap<>(data.get(0));
        for (Map<String, Object> item : data) {
            for (Map.Entry<String, Object> entry : new HashMap<>(common).entrySet()) {
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
            Map<String, Object> temp = new HashMap<>();
            for(int i =0; i < header.length; i ++) {
                temp.put(header[i], row[i]);
            }
            result.add(temp);
        }
        return result;
    }
}
