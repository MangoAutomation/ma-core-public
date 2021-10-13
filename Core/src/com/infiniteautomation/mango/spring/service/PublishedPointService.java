/*
 * Copyright (C) 2021 RadixIot LLC. All rights reserved.
 */

package com.infiniteautomation.mango.spring.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.dao.DataPointDao;
import com.serotonin.m2m2.db.dao.PublishedPointDao;
import com.serotonin.m2m2.db.dao.PublisherDao;
import com.serotonin.m2m2.i18n.ProcessResult;
import com.serotonin.m2m2.module.ModuleRegistry;
import com.serotonin.m2m2.module.PublisherDefinition;
import com.serotonin.m2m2.vo.DataPointVO;
import com.serotonin.m2m2.vo.permission.PermissionHolder;
import com.serotonin.m2m2.vo.publish.PublishedPointVO;
import com.serotonin.m2m2.vo.publish.PublisherVO;

/**
 * Published point access, currently only superadmin has read/edit/create permission
 * @author Terry Packer
 */
@Service
public class PublishedPointService extends AbstractVOService<PublishedPointVO, PublishedPointDao> {

    private final PublisherDao publisherDao;
    private final DataPointDao dataPointDao;
    private final PublisherService publisherService;

    /**
     *
     * @param publishedPointDao
     * @param dependencies
     * @param publisherDao
     * @param dataPointDao
     * @param publisherService
     */
    @Autowired
    public PublishedPointService(PublishedPointDao publishedPointDao,
                                 ServiceDependencies dependencies,
                                 PublisherDao publisherDao,
                                 DataPointDao dataPointDao,
                                 PublisherService publisherService) {
        super(publishedPointDao, dependencies);
        this.publisherDao = publisherDao;
        this.dataPointDao = dataPointDao;
        this.publisherService = publisherService;
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
            result.addContextualMessage("publisherId", "validate.publisher.missingPublisher", vo.getPublisherId());
        }
        //Ensure data point exists
        String dataPointXid = dataPointDao.getXidById(vo.getDataPointId());
        if(dataPointXid == null) {
            result.addContextualMessage("dataPointId", "validate.publisher.missingPoint", vo.getDataPointId());
        }else {
            List<PublishedPointVO> points = dao.getPublishedPoints(vo.getPublisherId());
            for (PublishedPointVO point : points) {
                if (point.getDataPointId() == vo.getDataPointId() && point.getId() != vo.getId()) {
                    //This point is already published by another point
                    DataPointVO dp = dataPointDao.get(vo.getDataPointId());
                    result.addContextualMessage("dataPointId", "validate.publisher.duplicatePoint", dataPointXid);
                }
            }
        }

        return result;
    }

    public void restart(String xid, boolean enabled, boolean restart) {
        //TODO Published Points - implement RT
    }

    /**
     * Replace all points on a publisher with these points
     * @param vo
     * @param pointVos
     */
    public void replacePoints(PublisherVO vo, List<PublishedPointVO> pointVos) {
        publisherService.ensureEditPermission(Common.getUser(), vo);
        //Validate all points
        for(PublishedPointVO point : pointVos) {
            ensureValid(point, Common.getUser());
        }
        dao.replacePoints(vo.getId(), pointVos);
    }
}
