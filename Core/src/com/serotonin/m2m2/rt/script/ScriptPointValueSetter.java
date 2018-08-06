package com.serotonin.m2m2.rt.script;

import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.rt.dataImage.IDataPointValueSource;
import com.serotonin.m2m2.vo.permission.Permissions;

public abstract class ScriptPointValueSetter {
    protected ScriptPermissions permissions;

    public ScriptPointValueSetter(ScriptPermissions permissions) {
        this.permissions = permissions;
    }

    //Ensure points are settable and the setter has permissions
    public void set(IDataPointValueSource point, Object value, long timestamp, String annotation) {

        if(!point.getVO().getPointLocator().isSettable())
            return;

        if(permissions != null && !Permissions.hasDataPointSetPermission(permissions.getDataPointSetPermissions(), point.getVO()))
            throw new ScriptPermissionsException(new TranslatableMessage("script.set.permissionDenied", point.getVO().getXid()));

        setImpl(point, value, timestamp, annotation);
    }

    protected abstract void setImpl(IDataPointValueSource point, Object value, long timestamp, String annotation);
}
