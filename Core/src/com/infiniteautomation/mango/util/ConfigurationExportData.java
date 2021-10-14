/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.infiniteautomation.mango.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.ArrayUtils;

import com.infiniteautomation.mango.db.tables.DataPoints;
import com.infiniteautomation.mango.db.tables.DataSources;
import com.infiniteautomation.mango.db.tables.EventDetectors;
import com.infiniteautomation.mango.db.tables.EventHandlers;
import com.infiniteautomation.mango.db.tables.JsonData;
import com.infiniteautomation.mango.db.tables.MailingLists;
import com.infiniteautomation.mango.db.tables.PublishedPoints;
import com.infiniteautomation.mango.db.tables.Publishers;
import com.infiniteautomation.mango.db.tables.SystemSettings;
import com.infiniteautomation.mango.db.tables.Users;
import com.infiniteautomation.mango.io.serial.virtual.VirtualSerialPortConfigDao;
import com.infiniteautomation.mango.permission.MangoPermission;
import com.serotonin.db.pair.StringStringPair;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.dao.DataPointDao;
import com.serotonin.m2m2.db.dao.DataSourceDao;
import com.serotonin.m2m2.db.dao.EventHandlerDao;
import com.serotonin.m2m2.db.dao.JsonDataDao;
import com.serotonin.m2m2.db.dao.MailingListDao;
import com.serotonin.m2m2.db.dao.PublishedPointDao;
import com.serotonin.m2m2.db.dao.PublisherDao;
import com.serotonin.m2m2.db.dao.RoleDao;
import com.serotonin.m2m2.db.dao.SystemSettingsDao;
import com.serotonin.m2m2.db.dao.UserDao;
import com.serotonin.m2m2.module.EmportDefinition;
import com.serotonin.m2m2.module.ModuleRegistry;
import com.serotonin.m2m2.module.PermissionDefinition;

/**
 * Common repo for export data
 *
 * @author Terry Packer
 */
public class ConfigurationExportData {

    //When adding to this list make sure you update getAllExportNames();
    public static final String DATA_SOURCES = DataSources.DATA_SOURCES.getName();
    public static final String DATA_POINTS = DataPoints.DATA_POINTS.getName();
    public static final String EVENT_HANDLERS = EventHandlers.EVENT_HANDLERS.getName();
    public static final String EVENT_DETECTORS = EventDetectors.EVENT_DETECTORS.getName();
    public static final String JSON_DATA = JsonData.JSON_DATA.getName();
    public static final String MAILING_LISTS = MailingLists.MAILING_LISTS.getName();
    public static final String PUBLISHERS = Publishers.PUBLISHERS.getName();
    public static final String PUBLISHED_POINTS = PublishedPoints.PUBLISHED_POINTS.getName();
    public static final String SYSTEM_SETTINGS = SystemSettings.SYSTEM_SETTINGS.getName();
    public static final String USERS = Users.USERS.getName();
    public static final String VIRTUAL_SERIAL_PORTS = "virtualSerialPorts";
    public static final String ROLES = "roles";
    public static final String PERMISSIONS = "permissions";
    /**
     * Get a list of all available export elements
     * @return
     */
    private static String[] getAllExportNames() {
        List<String> names = new ArrayList<>();
        names.add(DATA_SOURCES);
        names.add(DATA_POINTS);
        names.add(EVENT_HANDLERS);
        //TODO reinstate event detectors once there is a non-data-point event detector
        //names.add(EVENT_DETECTORS);
        names.add(JSON_DATA);
        names.add(MAILING_LISTS);
        names.add(PUBLISHERS);
        names.add(PUBLISHED_POINTS);
        names.add(SYSTEM_SETTINGS);
        names.add(USERS);
        names.add(VIRTUAL_SERIAL_PORTS);
        names.add(ROLES);
        names.add(PERMISSIONS);

        for (EmportDefinition def : ModuleRegistry.getDefinitions(EmportDefinition.class))
            if(def.getInView())
                names.add(def.getElementId());

        return names.toArray(new String[names.size()]);
    }

    /**
     * Get a list of pairs of i18n property and export names for all export items
     * @return
     */
    public static List<StringStringPair> getAllExportDescriptions(){
        List<StringStringPair> elements = new ArrayList<StringStringPair>();
        elements.add(new StringStringPair("header.dataSources", DATA_SOURCES));
        elements.add(new StringStringPair("header.dataPoints", DATA_POINTS));
        elements.add(new StringStringPair("header.eventHandlers", EVENT_HANDLERS));
        //TODO reinstate event detectors once there is a non-data-point event detector
        //elements.add(new StringStringPair("header.eventDetectors", EVENT_DETECTORS));
        elements.add(new StringStringPair("header.jsonData", JSON_DATA));
        elements.add(new StringStringPair("header.mailingLists", MAILING_LISTS));
        elements.add(new StringStringPair("header.publishers", PUBLISHERS));
        elements.add(new StringStringPair("header.publishedPoints", PUBLISHED_POINTS));
        elements.add(new StringStringPair("header.systemSettings", SYSTEM_SETTINGS));
        elements.add(new StringStringPair("header.users", USERS));
        elements.add(new StringStringPair("header.virtualSerialPorts", VIRTUAL_SERIAL_PORTS));
        elements.add(new StringStringPair("header.roles", ROLES));
        elements.add(new StringStringPair("header.permissions", PERMISSIONS));

        for (EmportDefinition def : ModuleRegistry.getDefinitions(EmportDefinition.class)) {
            if(def.getInView())
                elements.add(new StringStringPair(def.getDescriptionKey(), def.getElementId()));
        }

        return elements;
    }

    /**
     * Get a map of desired export data.
     *
     * @param exportElements if null full export is returned
     * @return
     */
    public static Map<String, Object> createExportDataMap(String[] exportElements){
        if(exportElements == null)
            exportElements = getAllExportNames();

        Map<String, Object> data = new LinkedHashMap<>();

        if (ArrayUtils.contains(exportElements, DATA_SOURCES))
            data.put(DATA_SOURCES, DataSourceDao.getInstance().getAll());
        if (ArrayUtils.contains(exportElements, DATA_POINTS))
            data.put(DATA_POINTS, DataPointDao.getInstance().getAll());
        if (ArrayUtils.contains(exportElements, USERS))
            data.put(USERS, UserDao.getInstance().getAll());
        if (ArrayUtils.contains(exportElements, MAILING_LISTS))
            data.put(MAILING_LISTS, MailingListDao.getInstance().getAll());
        if (ArrayUtils.contains(exportElements, PUBLISHERS))
            data.put(PUBLISHERS, PublisherDao.getInstance().getAll());
        if (ArrayUtils.contains(exportElements, PUBLISHED_POINTS))
            data.put(PUBLISHED_POINTS, Common.getBean(PublishedPointDao.class).getAll());
        if (ArrayUtils.contains(exportElements, EVENT_HANDLERS))
            data.put(EVENT_HANDLERS, EventHandlerDao.getInstance().getAll());
        if (ArrayUtils.contains(exportElements, SYSTEM_SETTINGS))
            data.put(SYSTEM_SETTINGS, SystemSettingsDao.getInstance().getAllSystemSettingsAsCodes());
        if (ArrayUtils.contains(exportElements, VIRTUAL_SERIAL_PORTS))
            data.put(VIRTUAL_SERIAL_PORTS, VirtualSerialPortConfigDao.getInstance().getAll());
        if (ArrayUtils.contains(exportElements, JSON_DATA))
            data.put(JSON_DATA, JsonDataDao.getInstance().getAll());
        if (ArrayUtils.contains(exportElements, ROLES))
            data.put(ROLES, Common.getBean(RoleDao.class).getAll());

        if (ArrayUtils.contains(exportElements, PERMISSIONS)) {
            List<Map<String, MangoPermission>> permissions = new ArrayList<>();
            for(PermissionDefinition def : ModuleRegistry.getPermissionDefinitions().values()) {
                Map<String, MangoPermission> toExport = new HashMap<>();
                toExport.put(def.getPermissionTypeName(), def.getPermission());
                permissions.add(toExport);
            }
            data.put(PERMISSIONS, permissions);
        }

        //TODO Add EVENT_DETECTORS
        //TODO Write the ImportTask properly for EventDetectors...

        for (EmportDefinition def : ModuleRegistry.getDefinitions(EmportDefinition.class)) {
            if (ArrayUtils.contains(exportElements, def.getElementId()))
                data.put(def.getElementId(), def.getExportData());
        }

        return data;
    }

}
