/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.infiniteautomation.mango.spring.service;

import java.util.HashSet;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import com.infiniteautomation.mango.spring.components.RunAs;
import com.infiniteautomation.mango.spring.events.DaoEvent;
import com.infiniteautomation.mango.spring.events.DaoEventType;
import com.infiniteautomation.mango.util.exception.NotFoundException;
import com.infiniteautomation.mango.util.exception.ValidationException;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.dao.DataPointDao;
import com.serotonin.m2m2.db.dao.PublisherDao;
import com.serotonin.m2m2.i18n.ProcessResult;
import com.serotonin.m2m2.rt.publish.PublisherRT;
import com.serotonin.m2m2.vo.DataPointVO;
import com.serotonin.m2m2.vo.permission.PermissionException;
import com.serotonin.m2m2.vo.permission.PermissionHolder;
import com.serotonin.m2m2.vo.publish.PublishedPointVO;
import com.serotonin.m2m2.vo.publish.PublisherVO;
import com.serotonin.m2m2.vo.role.RoleVO;

/**
 * @author Terry Packer
 *
 */
@Service
public class PublisherService extends AbstractVOService<PublisherVO<? extends PublishedPointVO>, PublisherDao> {

    private final RunAs runAs;

    @Autowired
    public PublisherService(PublisherDao dao, ServiceDependencies dependencies, RunAs runAs) {
        super(dao, dependencies);
        this.runAs = runAs;
    }

    @Override
    public boolean hasCreatePermission(PermissionHolder user, PublisherVO<? extends PublishedPointVO> vo) {
        return permissionService.hasAdminRole(user);
    }

    @Override
    public boolean hasEditPermission(PermissionHolder user, PublisherVO<? extends PublishedPointVO> vo) {
        return permissionService.hasAdminRole(user);
    }

    @Override
    public boolean hasReadPermission(PermissionHolder user, PublisherVO<? extends PublishedPointVO> vo) {
        return permissionService.hasAdminRole(user);
    }

    @EventListener
    protected void handleRoleEvent(DaoEvent<? extends RoleVO> event) {
        if (event.getType() == DaoEventType.DELETE) {//So we don't have to restart it
            for (PublisherRT<?> rt : Common.runtimeManager.getRunningPublishers()) {
                rt.handleRoleEvent(event);
            }
        }
    }

    @Override
    public PublisherVO<? extends PublishedPointVO> insert(PublisherVO<? extends PublishedPointVO> vo)
            throws PermissionException, ValidationException {

        PublisherVO<? extends PublishedPointVO> result = super.insert(vo);
        if (result.isEnabled()) {
            Common.runtimeManager.startPublisher(result);
        }

        return result;
    }

    @Override
    protected PublisherVO<? extends PublishedPointVO> update(PublisherVO<? extends PublishedPointVO> existing, PublisherVO<? extends PublishedPointVO> vo) throws PermissionException, ValidationException {
        ensureEditPermission(Common.getUser(), existing);
        Common.runtimeManager.stopPublisher(existing.getId());
        PublisherVO<? extends PublishedPointVO> result = super.update(existing, vo);
        if (result.isEnabled()) {
            Common.runtimeManager.startPublisher(result);
        }
        return result;
    }


    @Override
    protected PublisherVO<? extends PublishedPointVO> delete(PublisherVO<? extends PublishedPointVO> vo) throws PermissionException, NotFoundException {
        ensureDeletePermission(Common.getUser(), vo);
        Common.runtimeManager.stopPublisher(vo.getId());
        PublisherVO<? extends PublishedPointVO> result = super.delete(vo);
        runAs.runAs(runAs.systemSuperadmin(), () -> {
            Common.eventManager.cancelEventsForPublisher(result.getId());
        });
        return result;
    }

    /**
     * @param xid
     * @param enabled
     * @param restart
     */
    public void restart(String xid, boolean enabled, boolean restart) {
        PublisherVO<? extends PublishedPointVO> vo = get(xid);
        PublisherVO<? extends PublishedPointVO> existing = (PublisherVO<? extends PublishedPointVO>) vo.copy();
        ensureEditPermission(Common.getUser(), vo);
        if (enabled && restart) {
            vo.setEnabled(true);
            update(existing, vo);
        } else if(vo.isEnabled() != enabled) {
            vo.setEnabled(enabled);
            update(existing, vo);
        }
    }

    @Override
    public ProcessResult validate(PublisherVO<? extends PublishedPointVO> vo, PermissionHolder user) {
        ProcessResult response = commonValidation(vo, user);
        vo.getDefinition().validate(response, vo, user);
        return response;
    }

    @Override
    public ProcessResult validate(PublisherVO<? extends PublishedPointVO> existing, PublisherVO<? extends PublishedPointVO> vo,
            PermissionHolder user) {
        ProcessResult response = commonValidation(vo, user);
        vo.getDefinition().validate(response, existing, vo, user);
        return response;
    }

    private ProcessResult commonValidation(PublisherVO<? extends PublishedPointVO> vo, PermissionHolder user) {
        ProcessResult response = super.validate(vo, user);
        if (vo.isSendSnapshot()) {
            if (vo.getSnapshotSendPeriods() <= 0)
                response.addContextualMessage("snapshotSendPeriods", "validate.greaterThanZero");
            if(!Common.TIME_PERIOD_CODES.isValidId(vo.getSnapshotSendPeriodType(), Common.TimePeriods.MILLISECONDS, Common.TimePeriods.DAYS,
                    Common.TimePeriods.WEEKS, Common.TimePeriods.MONTHS, Common.TimePeriods.YEARS))
                response.addContextualMessage("snapshotSendPeriodType", "validate.invalidValue");
        }

        if (vo.getCacheWarningSize() < 1)
            response.addContextualMessage("cacheWarningSize", "validate.greaterThanZero");

        if (vo.getCacheDiscardSize() <= vo.getCacheWarningSize())
            response.addContextualMessage("cacheDiscardSize", "validate.publisher.cacheDiscardSize");

        Set<Integer> set = new HashSet<>();

        for (PublishedPointVO point : vo.getPoints()) {
            int pointId = point.getDataPointId();
            //Does this point even exist?

            if (set.contains(pointId)) {
                DataPointVO dp = DataPointDao.getInstance().get(pointId);
                response.addContextualMessage("points", "validate.publisher.duplicatePoint", dp.getExtendedName(), dp.getXid());
            } else {
                String dpXid = DataPointDao.getInstance().getXidById(pointId);
                if (dpXid == null) {
                    response.addContextualMessage("points", "validate.publisher.missingPoint", pointId);
                } else {
                    set.add(pointId);
                }
            }
        }
        return response;
    }

}
