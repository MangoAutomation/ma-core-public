/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.infiniteautomation.mango.util.script;

import java.util.function.BiConsumer;

import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.rt.dataImage.DataPointListener;
import com.serotonin.m2m2.rt.dataImage.DataPointRT;
import com.serotonin.m2m2.rt.dataImage.PointValueTime;
import com.serotonin.m2m2.rt.script.ScriptContextVariable;
import com.serotonin.m2m2.vo.DataPointVO;

/**
 * 
 * Script Data Point that aids in context management
 * 
 * Ensure to call open and then close as to not leave listeners in the runtime.
 * 
 * @author Terry Packer
 *
 */
public class ScriptContextPointListener implements DataPointListener {

    protected String pointXid;
    protected final ScriptContextVariable variable;
    //Called with true when point is initialized, false when it is not
    protected final BiConsumer<ScriptContextPointListener, Boolean> pointStateHandler;
    
    protected volatile DataPointRT runtime;
    
    public ScriptContextPointListener(ScriptContextVariable variable, String pointXid, BiConsumer<ScriptContextPointListener, Boolean> pointStateHandler) {
        this.pointXid = pointXid;
        this.variable = variable;
        this.pointStateHandler = pointStateHandler;
    }

    public void open() {
        this.runtime = Common.runtimeManager.getDataPoint(variable.getDataPointId());
        Common.runtimeManager.addDataPointListener(this.variable.getDataPointId(), this);
    }
    
    public void close() {
        Common.runtimeManager.removeDataPointListener(this.variable.getDataPointId(), this);
    }
    
    //
    //
    //  Data Point Listener Interface
    //
    //
    @Override
    public String getListenerName() {
        return "Script context point listener xid: " + pointXid + " var: " + this.variable.getVariableName();
    }

    @Override
    public void pointInitialized() {
        DataPointRT rt = Common.runtimeManager.getDataPoint(variable.getDataPointId());
        if(rt != null) {
            this.runtime = rt;
            this.pointXid = rt.getVO().getXid();
            if(this.pointStateHandler != null)
                this.pointStateHandler.accept(this, true);
        }
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
    public void pointTerminated(DataPointVO vo) {
        if(this.pointStateHandler != null)
            this.pointStateHandler.accept(this, false);
        this.runtime = null;
    }

    @Override
    public void pointLogged(PointValueTime value) {
           
    }
    
    public boolean isRunning() {
        return this.runtime != null;
    }
    
    public DataPointRT getRuntime() {
        return this.runtime;
    }
    
    public ScriptContextVariable getVariable() {
        return this.variable;
    }
    
    public String getPointXid() {
        return this.pointXid;
    }
}
