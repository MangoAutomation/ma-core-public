/**
 * Copyright (C) 2019  Infinite Automation Software. All rights reserved.
 */
package com.infiniteautomation.mango.spring.service;

import java.util.HashSet;
import java.util.ListIterator;
import java.util.Objects;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import com.infiniteautomation.mango.spring.db.PublisherTableDefinition;
import com.infiniteautomation.mango.util.exception.NotFoundException;
import com.infiniteautomation.mango.util.exception.ValidationException;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.dao.DataPointDao;
import com.serotonin.m2m2.db.dao.PublisherDao;
import com.serotonin.m2m2.db.dao.RoleDao.RoleDeletedDaoEvent;
import com.serotonin.m2m2.i18n.ProcessResult;
import com.serotonin.m2m2.rt.publish.PublisherRT;
import com.serotonin.m2m2.vo.DataPointVO;
import com.serotonin.m2m2.vo.permission.PermissionException;
import com.serotonin.m2m2.vo.permission.PermissionHolder;
import com.serotonin.m2m2.vo.publish.PublishedPointVO;
import com.serotonin.m2m2.vo.publish.PublisherVO;

/**
 * @author Terry Packer
 *
 */
@Service
public class PublisherService extends AbstractVOService<PublisherVO<? extends PublishedPointVO>, PublisherTableDefinition, PublisherDao> {

    @Autowired
    public PublisherService(PublisherDao dao, PermissionService permissionService) {
        super(dao, permissionService);
    }

    @Override
    public boolean hasCreatePermission(PermissionHolder user, PublisherVO<? extends PublishedPointVO> vo) {
        return user.hasAdminRole();
    }

    @Override
    public boolean hasEditPermission(PermissionHolder user, PublisherVO<? extends PublishedPointVO> vo) {
        return user.hasAdminRole();
    }

    @Override
    public boolean hasReadPermission(PermissionHolder user, PublisherVO<? extends PublishedPointVO> vo) {
        return user.hasAdminRole();
    }

    @Override
    @EventListener
    protected void handleRoleDeletedEvent(RoleDeletedDaoEvent event) {
        //So we don't have to restart it
        for(PublisherRT<?> rt : Common.runtimeManager.getRunningPublishers()) {
            rt.handleRoleDeletedEvent(event);
        }
    }

    @Override
    public PublisherVO<? extends PublishedPointVO> insert(PublisherVO<? extends PublishedPointVO> vo)
            throws PermissionException, ValidationException {
        PermissionHolder user = Common.getUser();
        Objects.requireNonNull(user, "Permission holder must be set in security context");

        //Ensure they can create a list
        ensureCreatePermission(user, vo);

        //Ensure we don't presume to exist
        if(vo.getId() != Common.NEW_ID) {
            ProcessResult result = new ProcessResult();
            result.addContextualMessage("id", "validate.invalidValue");
            throw new ValidationException(result);
        }

        //Generate an Xid if necessary
        if(StringUtils.isEmpty(vo.getXid()))
            vo.setXid(dao.generateUniqueXid());

        ensureValid(vo, user);
        Common.runtimeManager.savePublisher(vo);

        return vo;
    }

    @Override
    public PublisherVO<? extends PublishedPointVO> update(PublisherVO<? extends PublishedPointVO> existing, PublisherVO<? extends PublishedPointVO> vo) throws PermissionException, ValidationException {
        PermissionHolder user = Common.getUser();
        Objects.requireNonNull(user, "Permission holder must be set in security context");

        ensureEditPermission(user, existing);

        //Ensure matching data source types
        if(!StringUtils.equals(existing.getDefinition().getPublisherTypeName(), vo.getDefinition().getPublisherTypeName())) {
            ProcessResult result = new ProcessResult();
            result.addContextualMessage("definition.publisherTypeName", "validate.publisher.incompatiblePublisherType");
            throw new ValidationException(result);
        }

        vo.setId(existing.getId());
        ensureValid(existing, vo, user);
        Common.runtimeManager.savePublisher(vo);
        return vo;
    }


    @Override
    public PublisherVO<? extends PublishedPointVO> delete(PublisherVO<? extends PublishedPointVO> vo)
            throws PermissionException, NotFoundException {
        PermissionHolder user = Common.getUser();
        Objects.requireNonNull(user, "Permission holder must be set in security context");

        ensureDeletePermission(user, vo);
        Common.runtimeManager.deletePublisher(vo.getId());
        return vo;
    }

    /**
     * @param xid
     * @param enabled
     * @param restart
     */
    public void restart(String xid, boolean enabled, boolean restart) {
        PublisherVO<? extends PublishedPointVO>  vo = get(xid);
        PermissionHolder user = Common.getUser();
        Objects.requireNonNull(user, "Permission holder must be set in security context");

        ensureEditPermission(user, vo);
        if (enabled && restart) {
            vo.setEnabled(true);
            Common.runtimeManager.savePublisher(vo); //saving will restart it
        } else if(vo.isEnabled() != enabled) {
            vo.setEnabled(enabled);
            Common.runtimeManager.savePublisher(vo);
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
        ListIterator<? extends PublishedPointVO> it = vo.getPoints().listIterator();

        while(it.hasNext()) {
            PublishedPointVO point = it.next();
            int pointId = point.getDataPointId();
            //Does this point even exist?

            if (set.contains(pointId)) {
                DataPointVO dp = DataPointDao.getInstance().get(pointId);
                response.addContextualMessage("points", "validate.publisher.duplicatePoint", dp.getExtendedName(), dp.getXid());
            }
            else{
                String dpXid = DataPointDao.getInstance().getXidById(pointId);
                if(dpXid == null) {
                    response.addContextualMessage("points", "validate.publisher.missingPoint", pointId);
                }else {
                    set.add(pointId);
                }
            }
        }
        return response;
    }

}
