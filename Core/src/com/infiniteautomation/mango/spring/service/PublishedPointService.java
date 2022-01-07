/*
 * Copyright (C) 2021 RadixIot LLC. All rights reserved.
 */

package com.infiniteautomation.mango.spring.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import com.infiniteautomation.mango.spring.events.DaoEvent;
import com.infiniteautomation.mango.spring.events.DaoEventType;
import com.infiniteautomation.mango.util.exception.NotFoundException;
import com.infiniteautomation.mango.util.exception.ValidationException;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.dao.DataPointDao;
import com.serotonin.m2m2.db.dao.PublishedPointDao;
import com.serotonin.m2m2.db.dao.PublisherDao;
import com.serotonin.m2m2.i18n.ProcessResult;
import com.serotonin.m2m2.module.ModuleRegistry;
import com.serotonin.m2m2.module.PublisherDefinition;
import com.serotonin.m2m2.rt.RuntimeManager;
import com.serotonin.m2m2.vo.DataPointVO;
import com.serotonin.m2m2.vo.permission.PermissionException;
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
    public PublishedPointVO insert(PublishedPointVO vo) throws PermissionException, ValidationException {
        super.insert(vo);

        if (vo.isEnabled()) {
            // the published point is ready to be started
            getRuntimeManager().startPublishedPoint(vo);
        }

        return vo;
    }

    @Override
    protected PublishedPointVO update(PublishedPointVO existing, PublishedPointVO vo) throws PermissionException, ValidationException {
        PermissionHolder user = Common.getUser();

        ensureEditPermission(user, existing);

        vo.setId(existing.getId());

        ensureValid(existing, vo);

        getRuntimeManager().stopPublishedPoint(vo.getId());
        dao.update(existing, vo);

        if (vo.isEnabled()) {
            getRuntimeManager().startPublishedPoint(vo);
        }

        return vo;
    }

    @Override
    protected PublishedPointVO delete(PublishedPointVO vo) throws PermissionException, NotFoundException {
        PermissionHolder user = Common.getUser();

        ensureDeletePermission(user, vo);

        getRuntimeManager().stopPublishedPoint(vo.getId());
        dao.delete(vo);

        return vo;
    }

    @Override
    public ProcessResult validate(PublishedPointVO vo) {
        ProcessResult result = super.validate(vo);

        // Ensure the definition exists
        PublisherDefinition<? extends PublisherVO> definition = ModuleRegistry.getPublisherDefinition(vo.getPublisherTypeName());
        if (definition == null) {
            result.addContextualMessage("publisherTypeName", "validate.invalidValue");
        }

        // Check the publisher exists
        PublisherVO pub = publisherDao.get(vo.getPublisherId());
        if (pub == null) {
            result.addContextualMessage("publisherId", "validate.publisher.missingPublisher", vo.getPublisherId());
        } else {
            vo.setPublisherXid(pub.getXid());
        }

        // Ensure data point exists
        String dataPointXid = dataPointDao.getXidById(vo.getDataPointId());
        if (dataPointXid == null) {
            result.addContextualMessage("dataPointId", "validate.publisher.missingPoint", vo.getDataPointId());
        } else {
            vo.setDataPointXid(dataPointXid);
        }

        if (definition != null && pub != null) {
            definition.validate(result, vo, pub);
        }
        return result;
    }

    @Override
    public ProcessResult validate(PublishedPointVO existing, PublishedPointVO vo) {
        ProcessResult result = super.validate(existing, vo);

        // Ensure the definition exists
        PublisherDefinition<? extends PublisherVO> definition = ModuleRegistry.getPublisherDefinition(vo.getPublisherTypeName());
        if (definition == null) {
            result.addContextualMessage("publisherTypeName", "validate.invalidValue");
        }

        // ensure the publisher type is not being changed
        if (!existing.getPublisherTypeName().equals(vo.getPublisherTypeName())) {
            result.addContextualMessage("publisherId", "validate.publishedPoints.cantChangeType");
        }

        // Check the publisher exists
        PublisherVO pub = publisherDao.get(vo.getPublisherId());
        if (pub == null) {
            result.addContextualMessage("publisherId", "validate.publisher.missingPublisher", vo.getPublisherId());
        } else {
            vo.setPublisherXid(pub.getXid());

            // don't allow moving to a different publisher
            if (existing.getPublisherId() != vo.getPublisherId()) {
                result.addContextualMessage("publisherId", "validate.publishedPoints.cantChangePublisher");
            }
        }

        // Ensure data point exists
        String dataPointXid = dataPointDao.getXidById(vo.getDataPointId());
        if (dataPointXid == null) {
            result.addContextualMessage("dataPointId", "validate.publisher.missingPoint", vo.getDataPointId());
        } else {
            vo.setDataPointXid(dataPointXid);

            // don't allow changing the point being published
            if (existing.getDataPointId() != vo.getDataPointId()) {
                result.addContextualMessage("dataPointId", "validate.publishedPoints.cantChangeDataPoint");
            }
        }

        if (definition != null && pub != null) {
            definition.validate(result, existing, vo, pub);
        }
        return result;
    }

    /**
     * Enable/Disable/Restart a published point
     *
     * @param xid     - xid of point to restart
     * @param enabled - Enable or disable the published point
     * @param restart - Restart the published point, enabled must equal true
     */
    public boolean setPublishedPointState(String xid, boolean enabled, boolean restart) throws NotFoundException, PermissionException {
        PermissionHolder user = Common.getUser();

        PublishedPointVO vo = get(xid);
        ensureEditPermission(user, vo);
        return setPublishedPointState(vo, enabled, restart);
    }

    /**
     * Set the state helper method
     *
     * @param vo      - The point to restart
     * @param enabled - Enable or disable the published point
     * @param restart - Restart the published point, enabled must equal true (will start a stopped point)
     * @return - true if the state changed
     */
    protected boolean setPublishedPointState(PublishedPointVO vo, boolean enabled, boolean restart) {
        vo.setEnabled(enabled);
        boolean publisherRunning = getRuntimeManager().isPublisherRunning(vo.getPublisherId());

        if (!publisherRunning) {
            //We must check its state in the DB
            boolean enabledInDB = dao.isEnabled(vo.getId());
            if (enabledInDB && !enabled) {
                dao.saveEnabledColumn(vo);
                return true;
            } else if (!enabledInDB && enabled) {
                dao.saveEnabledColumn(vo);
                return true;
            }
        } else {
            boolean running = getRuntimeManager().isPublishedPointRunning(vo.getId());
            if (running && !enabled) {
                //Running, so stop it
                getRuntimeManager().stopPublishedPoint(vo.getId());
                dao.saveEnabledColumn(vo);
                return true;
            } else if (!running && enabled) {
                //Not running, so start it
                dao.saveEnabledColumn(vo);
                getRuntimeManager().startPublishedPoint(vo);
                return true;
            } else if (enabled && restart) {
                //May be running or not, will either start or restart it (stopping a non running point will do nothing which is ok)
                getRuntimeManager().stopPublishedPoint(vo.getId());
                getRuntimeManager().startPublishedPoint(vo);
                return false;
            }
        }
        return false;
    }

    /**
     * Replace all points on a publisher with these points, must have read and edit permission for this publisher
     *
     */
    public void replacePoints(int publisherId, List<PublishedPointVO> pointVos) throws NotFoundException, PermissionException, ValidationException {
        replacePoints(publisherService.get(publisherId), pointVos);
    }

    protected void replacePoints(PublisherVO vo, List<PublishedPointVO> pointVos) throws NotFoundException, PermissionException, ValidationException {
        publisherService.ensureEditPermission(Common.getUser(), vo);
        //Validate all points
        for (PublishedPointVO point : pointVos) {
            ensureValid(point);
        }
        dao.replacePoints(vo.getId(), pointVos);
    }

    private RuntimeManager getRuntimeManager() {
        return Common.runtimeManager;
    }

    /**
     * Get the points for a publisher, must have read permission for publisher
     *
     */
    public List<PublishedPointVO> getPublishedPoints(int id) throws NotFoundException, PermissionException {
        return getPublishedPoints(publisherService.get(id));
    }

    protected List<PublishedPointVO> getPublishedPoints(PublisherVO vo) throws NotFoundException, PermissionException {
        return dao.getPublishedPoints(vo.getId());
    }

    /**
     * Published points are deleted on cascade by a data point
     *  ensure they are removed from the runtime and DAO notifications are sent
     */
    @EventListener
    protected void handleDataPointEvent(DaoEvent<? extends DataPointVO> event) {
        if (event.getType() == DaoEventType.DELETE) {
            //Stop the point
            List<PublishedPointVO> stopped = getRuntimeManager().stopPublishedPointsForDataPoint(event.getVo().getId());
            for(PublishedPointVO vo : stopped) {
                //Send websocket messages etc.
                dao.notifyPointDeleted(vo);
            }
        }
    }
}
