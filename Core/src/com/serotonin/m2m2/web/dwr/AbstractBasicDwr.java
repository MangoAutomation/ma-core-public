/*
 * Copyright (C) 2013 Deltamation Software. All rights reserved.
 * @author Jared Wiltshire
 */

package com.serotonin.m2m2.web.dwr;

import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;

import com.infiniteautomation.mango.db.query.SortOption;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.dao.AbstractDao;
import com.serotonin.m2m2.db.dao.ResultsWithTotal;
import com.serotonin.m2m2.i18n.ProcessResult;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.vo.AbstractVO;
import com.serotonin.m2m2.vo.permission.PermissionException;
import com.serotonin.m2m2.web.dwr.util.DwrPermission;

/**
 * 
 * 
 * 
 * Copyright (C) 2013 Deltamation Software. All rights reserved.
 * @author Jared Wiltshire, Terry Packer
 */
public abstract class AbstractBasicDwr<VO extends AbstractVO<?>, DAO extends AbstractDao<VO>> extends ModuleDwr {
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
        throw new PermissionException(new TranslatableMessage("common.default", "Subclass DWRs must implement method to use"), Common.getHttpUser());
    }
    
    /**
     * Get a VO with FK values populated
     * @param id
     * @return
     */
    @DwrPermission(user = true)
    public ProcessResult getFull(int id) {
        throw new PermissionException(new TranslatableMessage("common.default", "Subclass DWRs must implement method to use"), Common.getHttpUser());
    }
    
    /**
     * Load a list of VOs
     * @return
     */
    @DwrPermission(user = true)
    public ProcessResult load() {
        throw new PermissionException(new TranslatableMessage("common.default", "Subclass DWRs must implement method to use"), Common.getHttpUser());
    }
    
    /**
     * Load a list of VOs
     * with the FK values populated
     * @return
     */
    @DwrPermission(user = true)
    public ProcessResult loadFull() {
        throw new PermissionException(new TranslatableMessage("common.default", "Subclass DWRs must implement method to use"), Common.getHttpUser());
    }
    
    /**
     * Load a list of VOs
     * @return
     */
    @DwrPermission(user = true)
    public ProcessResult dojoQuery(Map<String, String> query, List<SortOption> sort, Integer start, Integer count, boolean or) {
        throw new PermissionException(new TranslatableMessage("common.default", "Subclass DWRs must implement method to use"), Common.getHttpUser());
    }
}
