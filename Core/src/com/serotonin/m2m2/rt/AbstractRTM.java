/*
    Copyright (C) 2013 Deltamation Software All rights reserved.
    @author Terry Packer
 */
package com.serotonin.m2m2.rt;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.logging.Log;

import com.serotonin.m2m2.db.dao.AbstractDao;
import com.serotonin.m2m2.module.RuntimeManagerDefinition;
import com.serotonin.m2m2.vo.AbstractActionVO;

/**
 * Copyright (C) 2013 Deltamation Software. All Rights Reserved.
 * @author Terry Packer
 *
 */
public abstract class AbstractRTM<VO extends AbstractActionVO<VO>, RT extends AbstractRT<VO>, DAO extends AbstractDao<VO>>
    extends RuntimeManagerDefinition {
    protected Log LOG;
    private int initializationPriority;
    
    // List of Running RTs
    private final Map<Integer, RT> running = new HashMap<Integer, RT>();
    
    /**
     * Construct Me
     * @param initializationPriority
     */
    public AbstractRTM(int initializationPriority) {
        this.initializationPriority = initializationPriority;
    }
    
    /* (non-Javadoc)
     * @see com.serotonin.m2m2.module.RuntimeManagerDefinition#getInitializationPriority()
     */
    @Override
    public int getInitializationPriority() {
        return initializationPriority;
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.module.RuntimeManagerDefinition#terminate()
     */
    @Override
    public void terminate() {
        Iterator<Integer> it = running.keySet().iterator();
        while (it.hasNext()) {
            stop(it.next());
        }
    }
    
    /* (non-Javadoc)
     * @see com.serotonin.m2m2.module.RuntimeManagerDefinition#initialize(boolean)
     */
    @Override
    public void initialize(boolean safe) {
        for (VO vo : getDao().getAll()) {
            if (vo.isEnabled() && !safe) {
                start(getRt(vo));
            }
        }
    }
    
    /**
     * Saves a MonitorVO by stopping it first then starting it
     * if it is enabled
     * @param vo
     */
    public void save(VO vo) {
        // If the monitor is running, stop it
        stop(vo.getId());

        getDao().save(vo);

        // If the monitor is enabled, start it
        if (vo.isEnabled()) {
            start(getRt(vo));
        }
    }

    public void delete(int id) {
        stop(id);
        getDao().delete(id);
    }
    

    private void start(RT rt) {
        synchronized (running) {
            if (running.containsKey(rt.getVo().getId())) {
                return;
            }
            rt.initialize();
            running.put(rt.getVo().getId(), rt);
            LOG.info(rt.getVo().getName() + " started");
        }
    }

    /**
     * Stops a downtime monitor
     * @param id
     */
    private void stop(int id) {
        synchronized (running) {
            if (running.containsKey(id)) {
                RT rt = running.remove(id);
                rt.terminate();
                LOG.info(rt.getVo().getName() + " stopped");
            }
        }
    }
    
    /**
     * Create an RT Type from the VO
     * @param vo
     * @return
     */
    public abstract RT getRt(VO vo);
    
    /**
     * Kludge to allow using the Dao and not having 
     * to have it set a construction time as the DB Layer
     * isn't ready when this object is created
     * @return
     */
    public abstract DAO getDao();
}
