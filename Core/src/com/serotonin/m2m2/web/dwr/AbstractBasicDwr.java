/*
 * Copyright (C) 2013 Deltamation Software. All rights reserved.
 * @author Jared Wiltshire
 */

package com.serotonin.m2m2.web.dwr;

import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;

import com.serotonin.m2m2.db.dao.AbstractBasicDao;
import com.serotonin.m2m2.db.dao.ResultsWithTotal;
import com.serotonin.m2m2.db.dao.SortOption;
import com.serotonin.m2m2.i18n.ProcessResult;
import com.serotonin.m2m2.web.dwr.util.DwrPermission;

/**
 * 
 * 
 * 
 * Copyright (C) 2013 Deltamation Software. All rights reserved.
 * @author Jared Wiltshire, Terry Packer
 */
public abstract class AbstractBasicDwr<VO, DAO extends AbstractBasicDao<VO>> extends ModuleDwr {
    protected Log LOG;
    protected DAO dao;
    
    public AbstractBasicDwr(DAO dao) {
        this.dao = dao;
    }

    
    /**
     * Get a VO 
     * @param id
     * @return
     */
    @DwrPermission(user = true)
    public ProcessResult get(int id) {
        VO vo = dao.get(id);
        ProcessResult response = new ProcessResult();
        response.addData("vo", vo);
        
        return response;
    }
    
    /**
     * Get a VO with FK values populated
     * @param id
     * @return
     */
    @DwrPermission(user = true)
    public ProcessResult getFull(int id) {
        VO vo = dao.getFull(id);
        ProcessResult response = new ProcessResult();
        response.addData("vo", vo);
        
        return response;
    }
    
    /**
     * Load a list of VOs
     * @return
     */
    @DwrPermission(user = true)
    public ProcessResult load() {
        ProcessResult response = new ProcessResult();
        
        List<VO> voList = dao.getAll();
        response.addData("list", voList);
                
        return response;
    }
    
    /**
     * Load a list of VOs
     * with the FK values populated
     * @return
     */
    @DwrPermission(user = true)
    public ProcessResult loadFull() {
        ProcessResult response = new ProcessResult();
        
        List<VO> voList = dao.getAllFull();
        response.addData("list", voList);
                
        return response;
    }
    
    /**
     * Load a list of VOs
     * @return
     */
    @DwrPermission(user = true)
    public ProcessResult dojoQuery(Map<String, String> query, List<SortOption> sort, Integer start, Integer count, boolean or) {
        ProcessResult response = new ProcessResult();
        
        ResultsWithTotal results = dao.dojoQuery(query, sort, start, count, or);
        response.addData("list", results.getResults());
        response.addData("total", results.getTotal());
        
        return response;
    }
}
