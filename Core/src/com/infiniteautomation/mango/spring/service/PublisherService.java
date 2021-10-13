/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.infiniteautomation.mango.spring.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import com.infiniteautomation.mango.spring.components.RunAs;
import com.infiniteautomation.mango.spring.events.DaoEvent;
import com.infiniteautomation.mango.spring.events.DaoEventType;
import com.infiniteautomation.mango.util.exception.NotFoundException;
import com.infiniteautomation.mango.util.exception.ValidationException;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.dao.PublisherDao;
import com.serotonin.m2m2.i18n.ProcessResult;
import com.serotonin.m2m2.rt.publish.PublisherRT;
import com.serotonin.m2m2.vo.permission.PermissionException;
import com.serotonin.m2m2.vo.permission.PermissionHolder;
import com.serotonin.m2m2.vo.publish.PublisherVO;
import com.serotonin.m2m2.vo.role.RoleVO;

/**
 * Publisher access, currently only superadmin has read/edit/create permission
 * @author Terry Packer
 *
 */
@Service
public class PublisherService extends AbstractVOService<PublisherVO, PublisherDao> {

    private final RunAs runAs;

    @Autowired
    public PublisherService(PublisherDao dao, ServiceDependencies dependencies, RunAs runAs) {
        super(dao, dependencies);
        this.runAs = runAs;
    }

    @Override
    public boolean hasCreatePermission(PermissionHolder user, PublisherVO vo) {
        return permissionService.hasAdminRole(user);
    }

    @Override
    public boolean hasEditPermission(PermissionHolder user, PublisherVO vo) {
        return permissionService.hasAdminRole(user);
    }

    @Override
    public boolean hasReadPermission(PermissionHolder user, PublisherVO vo) {
        return permissionService.hasAdminRole(user);
    }

    @EventListener
    protected void handleRoleEvent(DaoEvent<? extends RoleVO> event) {
        if (event.getType() == DaoEventType.DELETE) {//So we don't have to restart it
            for (PublisherRT rt : Common.runtimeManager.getRunningPublishers()) {
                rt.handleRoleEvent(event);
            }
        }
    }

    @Override
    public PublisherVO insert(PublisherVO vo)
            throws PermissionException, ValidationException {

        PublisherVO result = super.insert(vo);
        if (result.isEnabled()) {
            Common.runtimeManager.startPublisher(result);
        }

        return result;
    }

    @Override
    protected PublisherVO update(PublisherVO existing, PublisherVO vo) throws PermissionException, ValidationException {
        ensureEditPermission(Common.getUser(), existing);
        Common.runtimeManager.stopPublisher(existing.getId());
        PublisherVO result = super.update(existing, vo);
        if (result.isEnabled()) {
            Common.runtimeManager.startPublisher(result);
        }
        return result;
    }


    @Override
    protected PublisherVO delete(PublisherVO vo) throws PermissionException, NotFoundException {
        ensureDeletePermission(Common.getUser(), vo);
        Common.runtimeManager.stopPublisher(vo.getId());
        PublisherVO result = super.delete(vo);
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
        PublisherVO vo = get(xid);
        PublisherVO existing = (PublisherVO) vo.copy();
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
    public ProcessResult validate(PublisherVO vo) {
        ProcessResult response = commonValidation(vo);
        vo.getDefinition().validate(response, vo);
        return response;
    }

    @Override
    public ProcessResult validate(PublisherVO existing, PublisherVO vo) {
        ProcessResult response = commonValidation(vo);
        vo.getDefinition().validate(response, existing, vo);
        return response;
    }

    private ProcessResult commonValidation(PublisherVO vo) {
        ProcessResult response = super.validate(vo);
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

        return response;
    }

}
