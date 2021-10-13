/*
 * Copyright (C) 2021 RadixIot LLC. All rights reserved.
 */

package com.infiniteautomation.mango.emport;

import com.infiniteautomation.mango.spring.service.DataPointService;
import com.infiniteautomation.mango.spring.service.DataSourceService;
import com.infiniteautomation.mango.spring.service.EventDetectorsService;
import com.infiniteautomation.mango.spring.service.EventHandlerService;
import com.infiniteautomation.mango.spring.service.JsonDataService;
import com.infiniteautomation.mango.spring.service.MailingListService;
import com.infiniteautomation.mango.spring.service.PublishedPointService;
import com.infiniteautomation.mango.spring.service.PublisherService;
import com.infiniteautomation.mango.spring.service.RoleService;
import com.infiniteautomation.mango.spring.service.SystemPermissionService;
import com.infiniteautomation.mango.spring.service.UsersService;
import com.serotonin.m2m2.i18n.Translations;

public class ImportTaskDependencies {
    private final Translations translations;
    private final RoleService roleService;
    private final UsersService usersService;
    private final MailingListService mailingListService;
    private final DataSourceService dataSourceService;
    private final DataPointService dataPointService;
    private final PublisherService publisherService;
    private final PublishedPointService publishedPointService;
    private final EventHandlerService eventHandlerService;
    private final JsonDataService jsonDataService;
    private final EventDetectorsService eventDetectorService;
    private final SystemPermissionService permissionService;

    public ImportTaskDependencies(Translations translations, RoleService roleService, UsersService usersService, MailingListService mailingListService, DataSourceService dataSourceService, DataPointService dataPointService, PublisherService publisherService, PublishedPointService publishedPointService, EventHandlerService eventHandlerService, JsonDataService jsonDataService, EventDetectorsService eventDetectorService, SystemPermissionService permissionService) {
        this.translations = translations;
        this.roleService = roleService;
        this.usersService = usersService;
        this.mailingListService = mailingListService;
        this.dataSourceService = dataSourceService;
        this.dataPointService = dataPointService;
        this.publisherService = publisherService;
        this.publishedPointService = publishedPointService;
        this.eventHandlerService = eventHandlerService;
        this.jsonDataService = jsonDataService;
        this.eventDetectorService = eventDetectorService;
        this.permissionService = permissionService;
    }

    public Translations getTranslations() {
        return translations;
    }

    public RoleService getRoleService() {
        return roleService;
    }

    public UsersService getUsersService() {
        return usersService;
    }

    public MailingListService getMailingListService() {
        return mailingListService;
    }

    public DataSourceService getDataSourceService() {
        return dataSourceService;
    }

    public DataPointService getDataPointService() {
        return dataPointService;
    }

    public PublisherService getPublisherService() {
        return publisherService;
    }

    public PublishedPointService getPublishedPointService() {
        return publishedPointService;
    }

    public EventHandlerService getEventHandlerService() {
        return eventHandlerService;
    }

    public JsonDataService getJsonDataService() {
        return jsonDataService;
    }

    public EventDetectorsService getEventDetectorService() {
        return eventDetectorService;
    }

    public SystemPermissionService getPermissionService() {
        return permissionService;
    }
}
