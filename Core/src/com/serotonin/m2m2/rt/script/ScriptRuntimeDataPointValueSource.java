/**
 * Copyright (C) 2019  Infinite Automation Software. All rights reserved.
 */
package com.serotonin.m2m2.rt.script;

import java.io.Closeable;
import java.io.IOException;
import java.util.List;

import javax.script.Bindings;

import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.DataTypes;
import com.serotonin.m2m2.rt.dataImage.DataPointListener;
import com.serotonin.m2m2.rt.dataImage.IDataPointValueSource;
import com.serotonin.m2m2.rt.dataImage.PointValueTime;
import com.serotonin.m2m2.rt.dataImage.SetPointSource;
import com.serotonin.m2m2.vo.DataPointVO;

/**
 * 
 * Script Data Point that will install/remove itself to/from the script context when open/close is called.
 * 
 * TODO allow this point to fire context update events
 * 
 * @author Terry Packer
 *
 */
public class ScriptRuntimeDataPointValueSource implements IDataPointValueSource, DataPointListener, Closeable {

    private final String varName;
    private final int dataPointId;
    private final Bindings engineScope;
    private volatile IDataPointValueSource runtime;
    
    public ScriptRuntimeDataPointValueSource(ScriptContextVariable variable, Bindings engineScope) {
        this.varName = variable.getVariableName();
        this.dataPointId = variable.getDataPointId();
        this.engineScope = engineScope;
    }

    public void open() {
        Common.runtimeManager.addDataPointListener(this.dataPointId, this);
    }
    
    @Override
    public void close() throws IOException {
        Common.runtimeManager.removeDataPointListener(this.dataPointId, this);
    }
    
    @Override
    public List<PointValueTime> getLatestPointValues(int limit) {
        IDataPointValueSource local = runtime;
        if(local != null)
            return getLatestPointValues(limit);
        else
            return null;
    }

    @Override
    public void updatePointValue(PointValueTime newValue) {
        IDataPointValueSource local = runtime;
        if(local != null)
            local.updatePointValue(newValue);
    }

    @Override
    public void updatePointValue(PointValueTime newValue, boolean async) {
        IDataPointValueSource local = runtime;
        if(local != null)
            local.updatePointValue(newValue, async);
    }

    @Override
    public void setPointValue(PointValueTime newValue, SetPointSource source) {
        IDataPointValueSource local = runtime;
        if(local != null)
            local.setPointValue(newValue, source);
    }

    @Override
    public PointValueTime getPointValue() {
        IDataPointValueSource local = runtime;
        if(local != null)
            return local.getPointValue();
        else
            return null;
    }

    @Override
    public PointValueTime getPointValueBefore(long time) {
        IDataPointValueSource local = runtime;
        if(local != null)
            return local.getPointValueBefore(time);
        else
            return null;
    }

    @Override
    public PointValueTime getPointValueAfter(long time) {
        IDataPointValueSource local = runtime;
        if(local != null)
            return local.getPointValueAfter(time);
        else
            return null;
    }

    @Override
    public List<PointValueTime> getPointValues(long since) {
        IDataPointValueSource local = runtime;
        if(local != null)
            return local.getPointValues(since);
        else
            return null;
    }

    @Override
    public List<PointValueTime> getPointValuesBetween(long from, long to) {
        IDataPointValueSource local = runtime;
        if(local != null)
            return local.getPointValuesBetween(from, to);
        else
            return null;
    }

    @Override
    public PointValueTime getPointValueAt(long time) {
        IDataPointValueSource local = runtime;
        if(local != null)
            return local.getPointValueAt(time);
        else
            return null;
    }

    @Override
    public int getDataTypeId() {
        IDataPointValueSource local = runtime;
        if(local != null)
            return local.getDataTypeId();
        else
            return DataTypes.UNKNOWN;
    }

    @Override
    public DataPointWrapper getDataPointWrapper(AbstractPointWrapper wrapper) {
        IDataPointValueSource local = runtime;
        if(local != null)
            return new DataPointWrapper(local.getVO(), wrapper);
        else
            return null;
    }

    @Override
    public DataPointVO getVO() {
        IDataPointValueSource local = runtime;
        if(local != null)
            return local.getVO();
        else
            return null;
    }
    
    //
    //
    //  Data Point Listener Interface
    //
    //
    
    @Override
    public String getListenerName() {
        return "Script point listener " + varName;
    }

    @Override
    public void pointInitialized() {
        this.runtime = Common.runtimeManager.getDataPoint(dataPointId);
        this.engineScope.put(varName, runtime);
    }

    @Override
    public void pointUpdated(PointValueTime newValue) {
        
    }

    @Override
    public void pointChanged(PointValueTime oldValue, PointValueTime newValue) {
        
    }

    @Override
    public void pointSet(PointValueTime oldValue, PointValueTime newValue) {
        
    }

    @Override
    public void pointBackdated(PointValueTime value) {
        
    }

    @Override
    public void pointTerminated() {
        this.engineScope.remove(varName);
        this.runtime = null;
    }

    @Override
    public void pointLogged(PointValueTime value) {
           
    }

}
