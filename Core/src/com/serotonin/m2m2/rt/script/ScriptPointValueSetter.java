/*
    Copyright (C) 2019 Infinite Automation Systems Inc. All rights reserved.
    @author Terry Packer
 */
package com.serotonin.m2m2.rt.script;

import com.infiniteautomation.mango.spring.service.PermissionService;
import com.infiniteautomation.mango.util.LazyInitSupplier;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.rt.dataImage.IDataPointValueSource;
import com.serotonin.m2m2.vo.permission.PermissionHolder;

/**
 *
 * @author Terry Packer
 *
 */
public abstract class ScriptPointValueSetter {

    private final LazyInitSupplier<PermissionService> permissionService = new LazyInitSupplier<>(() -> {
        return Common.getBean(PermissionService.class);
    });

    protected PermissionHolder permissions;

    public ScriptPointValueSetter(PermissionHolder permissions) {
        this.permissions = permissions;
    }

    //Ensure points are settable and the setter has permissions
    public void set(IDataPointValueSource point, Object value, long timestamp, String annotation) throws ScriptPermissionsException {

        if(!point.getVO().getPointLocator().isSettable())
            return;

        if(!permissionService.get().hasPermission(permissions, point.getVO().getSetPermission()))
            throw new ScriptPermissionsException(new TranslatableMessage("script.set.permissionDenied", point.getVO().getXid()));

        setImpl(point, value, timestamp, annotation);
    }

    protected abstract void setImpl(IDataPointValueSource point, Object value, long timestamp, String annotation);
}
