/*
 * Copyright (C) 2021 RadixIot LLC. All rights reserved.
 */

package com.infiniteautomation.mango.spring.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.serotonin.m2m2.db.dao.DataPointDao;
import com.serotonin.m2m2.db.dao.PublishedPointDao;
import com.serotonin.m2m2.db.dao.PublisherDao;
import com.serotonin.m2m2.i18n.ProcessResult;
import com.serotonin.m2m2.module.ModuleRegistry;
import com.serotonin.m2m2.module.PublisherDefinition;
import com.serotonin.m2m2.vo.permission.PermissionHolder;
import com.serotonin.m2m2.vo.publish.PublishedPointVO;

/**
 * Published point access, currently only superadmin has read/edit/create permission
 * @author Terry Packer
 */
@Service
public class PublishedPointService extends AbstractVOService<PublishedPointVO, PublishedPointDao> {

    private final PublisherDao publisherDao;
    private final DataPointDao dataPointDao;

    /**
     * @param publishedPointDao
     * @param dependencies
     * @param publisherDao
     * @param dataPointDao
     */
    @Autowired
    public PublishedPointService(PublishedPointDao publishedPointDao,
                                 ServiceDependencies dependencies,
                                 PublisherDao publisherDao,
                                 DataPointDao dataPointDao) {
        super(publishedPointDao, dependencies);
        this.publisherDao = publisherDao;
        this.dataPointDao = dataPointDao;
    }

    @Override
    public boolean hasEditPermission(PermissionHolder user, PublishedPointVO vo) {
        return permissionService.hasAdminRole(user);
    }

    @Override
    public boolean hasReadPermission(PermissionHolder user, PublishedPointVO vo) {
        return permissionService.hasAdminRole(user);
    }

    @Override
    public ProcessResult validate(PublishedPointVO vo, PermissionHolder user) {
        ProcessResult result = commonValidation(vo, user);
        PublisherDefinition<?> definition = ModuleRegistry.getPublisherDefinition(vo.getPublisherTypeName());
        //Ensure the definition exists
        if(definition == null) {
            result.addContextualMessage("publisherTypeName", "validate.invalidValue");
            return result;
        }
        definition.validate(result, vo, user);
        return result;
    }

    @Override
    public ProcessResult validate(PublishedPointVO existing, PublishedPointVO vo, PermissionHolder user) {
        ProcessResult result = commonValidation(vo, user);
        PublisherDefinition<?> definition = ModuleRegistry.getPublisherDefinition(vo.getPublisherTypeName());
        //Ensure the definition exists
        if(definition == null) {
            result.addContextualMessage("publisherTypeName", "validate.invalidValue");
            return result;
        }
        definition.validate(result, existing, vo, user);
        return result;
    }

    private ProcessResult commonValidation(PublishedPointVO vo, PermissionHolder user) {
        ProcessResult result = super.validate(vo, user);

        //Ensure publisher exists
        if(publisherDao.getXidById(vo.getPublisherId()) == null) {
            result.addContextualMessage("publisherId", "validate.invalidValue");
        }
        //Ensure data point exists
        if(dataPointDao.getXidById(vo.getDataPointId()) == null) {
            result.addContextualMessage("dataPointId", "validate.invalidValue");
        }
        return result;
    }


}
