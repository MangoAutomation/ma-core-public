/**
 * Copyright (C) 2020  Infinite Automation Software. All rights reserved.
 */

package com.infiniteautomation.mango.spring.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.infiniteautomation.mango.db.tables.Audit;
import com.infiniteautomation.mango.db.tables.records.AuditRecord;
import com.serotonin.m2m2.db.dao.AuditEventDao;
import com.serotonin.m2m2.i18n.ProcessResult;
import com.serotonin.m2m2.vo.event.audit.AuditEventInstanceVO;
import com.serotonin.m2m2.vo.permission.PermissionHolder;

/**
 *
 * @author Terry Packer
 */
@Service
public class AuditEventService extends AbstractBasicVOService<AuditEventInstanceVO, AuditEventDao> {

    @Autowired
    public AuditEventService(AuditEventDao dao, PermissionService permissionService) {
        super(dao, permissionService);
    }

    @Override
    public ProcessResult validate(AuditEventInstanceVO vo, PermissionHolder user) {
        return new ProcessResult();
    }

    @Override
    public boolean hasEditPermission(PermissionHolder user, AuditEventInstanceVO vo) {
        return false;
    }

    @Override
    public boolean hasReadPermission(PermissionHolder user, AuditEventInstanceVO vo) {
        return permissionService.hasAdminRole(user);
    }

}
