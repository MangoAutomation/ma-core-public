/**
 * Copyright (C) 2020  Infinite Automation Software. All rights reserved.
 */

package com.infiniteautomation.mango.spring.service;

import com.infiniteautomation.mango.permission.MangoPermission;
import com.infiniteautomation.mango.spring.eventMulticaster.PropagatingEvent;
import com.infiniteautomation.mango.util.exception.NotFoundException;
import com.infiniteautomation.mango.util.exception.ValidationException;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.dao.SystemPermissionDao;
import com.serotonin.m2m2.i18n.ProcessResult;
import com.serotonin.m2m2.module.PermissionDefinition;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Objects;

/**
 *
 * @author Terry Packer
 */
@Service
public class SystemPermissionService {

    private final SystemPermissionDao dao;
    private final PermissionService permissionService;
    private final RoleService roleService;
    private final UsersService userService;
    private final ApplicationEventPublisher eventPublisher;

    @Autowired
    public SystemPermissionService(SystemPermissionDao dao,
                                   PermissionService permissionService,
                                   RoleService roleService,
                                   UsersService userService,
                                   ApplicationEventPublisher eventPublisher) {
        this.dao = dao;
        this.permissionService = permissionService;
        this.roleService = roleService;
        this.userService = userService;
        this.eventPublisher = eventPublisher;
    }

    /**
     *
     * @param permission
     * @param def
     * @throws ValidationException
     */
    public void update(MangoPermission permission, PermissionDefinition def) throws ValidationException {
        Objects.requireNonNull(def, "Permission definition cannot be null");
        permissionService.ensureAdminRole(Common.getUser());

        ProcessResult validation = new ProcessResult();
        if (permission == null) {
            validation.addContextualMessage("permission", "validate.required");
            throw new ValidationException(validation);
        }

        permission.getRoles().stream().flatMap(Collection::stream).forEach(role -> {
            try {
                roleService.get(role.getXid());
            } catch(NotFoundException e) {
                validation.addGenericMessage("validate.role.notFound", role.getXid());
            }
        });

        if (validation.getHasMessages()) {
            throw new ValidationException(validation);
        }

        //TODO Mango 4.0 Transaction rollback etc?
        dao.update(def.getPermissionTypeName(), def.getPermission(), permission);
        def.setPermission(permission);
        this.eventPublisher.publishEvent(new SystemPermissionUpdated(def));
    }

    public class SystemPermissionUpdated extends ApplicationEvent implements PropagatingEvent {
        private final PermissionDefinition permissionDefinition;

        private SystemPermissionUpdated(PermissionDefinition permissionDefinition) {
            super(SystemPermissionService.this);
            this.permissionDefinition = permissionDefinition;
        }

        public PermissionDefinition getPermissionDefinition() {
            return permissionDefinition;
        }
    }
}
