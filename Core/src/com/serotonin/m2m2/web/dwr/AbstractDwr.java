/*
 * Copyright (C) 2013 Deltamation Software. All rights reserved.
 * @author Jared Wiltshire
 */

package com.serotonin.m2m2.web.dwr;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;

import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.dao.AbstractDao;
import com.serotonin.m2m2.i18n.ProcessResult;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.vo.AbstractVO;
import com.serotonin.m2m2.web.dwr.util.DwrPermission;

/**
 * Copyright (C) 2013 Deltamation Software. All rights reserved.
 * @author Jared Wiltshire
 */
public abstract class AbstractDwr<VO extends AbstractVO<?>, DAO extends AbstractDao<VO>> extends AbstractBasicDwr<VO, DAO> {
    // Used for:
    // JSON export of individual VOs, whole sets of objects are handled separately
    // keyNameErrors is the div where errors will be shown TODO is this still true?
    protected String keyName;
    
    /**
     * If set then Json Exports will appear under this keyname
     * e.g.
     * "topLevelKeyName":{
     *   "keyName":[
     *     {
     *       "prop1":"value1",
     *       "prop2":"value2"
     *     }
     *   ]
     * }
     */
    protected String topLevelKeyName;
    
    public AbstractDwr(DAO dao, String keyName) {
        super(dao);
        this.keyName = keyName;
    }
    
    public AbstractDwr(DAO dao, String keyName, String topLevelKeyName) {
        this(dao, keyName);
        this.topLevelKeyName = topLevelKeyName;
    }

    /**
     * Save the VO
     * 
     * Conversion for the VO must be added by extending DwrConversionDefinition
     * 
     * @return
     */
    @DwrPermission(admin = true)
    public ProcessResult save(VO vo) {
        ProcessResult response = new ProcessResult();
        if (vo.getXid() == null) {
            vo.setXid(dao.generateUniqueXid());
        }
        vo.validate(response);
        if(!response.getHasMessages()) {
            try {
                dao.save(vo);
            } catch(Exception e) {
                // Handle the exceptions.
                LOG.error(e);
                
                String context = vo.getName();
                if (context == null) {
                    context = vo.getXid();
                }
                if (context == null) {
                    context = Integer.toString(vo.getId());
                }
                
                if(e instanceof DuplicateKeyException)
                    response.addContextualMessage(context, "table.edit.alreadyExists");
                else
                    response.addContextualMessage(context, "table.edit.unableToSave", e.getMessage());
            }
        }
        response.addData("vo", vo);
        response.addData("id", vo.getId()); //Add in case it fails
        return response;
    }
    
    /**
     * Save the VO AND FDAO Data
     * 
     * Conversion for the VO must be added by extending DwrConversionDefinition
     * 
     * @return
     */
    @DwrPermission(admin = true)
    public ProcessResult saveFull(VO vo) { // TODO combine with save()
        ProcessResult response = new ProcessResult();
        if (vo.getXid() == null) {
            vo.setXid(dao.generateUniqueXid());
        }
        vo.validate(response);
        if(!response.getHasMessages()) {
            try {
                dao.saveFull(vo);
            } catch(Exception e) {
                // Handle the exceptions.
                LOG.error(e);
                
                String context = vo.getName();
                if (context == null) {
                    context = vo.getXid();
                }
                if (context == null) {
                    context = vo.getXid();
                }
                if (context == null) {
                    context = Integer.toString(vo.getId());
                }
                
                if(e instanceof DuplicateKeyException)
                    response.addContextualMessage(context, "table.edit.alreadyExists");
                else
                    response.addContextualMessage(context, "table.edit.unableToSave", e.getMessage());
            }
        }
        response.addData("vo", vo);
        response.addData("id", vo.getId()); //Add in case it fails
        return response;
    }

    /**
     * Delete a VO
     * @param id
     * @return
     */
    @DwrPermission(admin = true)
    public ProcessResult remove(int id) {
        ProcessResult response = new ProcessResult();
        try {
            dao.delete(id);
        } catch(Exception e) {
            // Handle the exceptions.
            LOG.error(e);
            VO vo = dao.get(id);
            if(e instanceof DataIntegrityViolationException)
                response.addContextualMessage(vo.getName(), "table.edit.unableToDeleteDueToConstraints");
            else
                response.addContextualMessage(vo.getName(), "table.edit.unableToDelete", e.getMessage());
        }
        
        response.addData("id", id);
        return response;
    }
    
    /**
     * Delete a VO and its associated points
     * @param id
     * @return
     */
    @DwrPermission(admin = true)
    public ProcessResult removeIncludingPoints(int id) {
        return remove(id);
    }
    
    /**
     * Export a VO
     * @param id
     * @return
     */
    @DwrPermission(user = true)
    public String jsonExport(int id) {
        Map<String, Object> data = new LinkedHashMap<String, Object>();
        List<VO> vos = new ArrayList<VO>();
        //Get the Full VO for the export
        vos.add(dao.getFull(id));
        data.put(keyName, vos);
        
        if (topLevelKeyName != null) {
            Map<String, Object> topData = new LinkedHashMap<String, Object>();
            topData.put(topLevelKeyName, data);
            data = topData;
        }
        
        return EmportDwr.export(data, 3);
    }

    @DwrPermission(user = true)
    public ProcessResult getCopy(int id) {

        //Get a Full Copy
        VO vo = dao.getFull(id);
        ProcessResult response = new ProcessResult();

        String name = StringUtils.abbreviate(
                TranslatableMessage.translate(getTranslations(), "common.copyPrefix", vo.getName()), 40);

        //Setup the Copy
        VO copy = (VO)vo.copy();
        copy.setId(Common.NEW_ID);
        copy.setName(name);
        copy.setXid(dao.generateUniqueXid());
        response.addData("vo", copy);

        //Don't Validate it, that will be done on save
        
        return response;
    }

    /* (non-Javadoc)
     * @see com.deltamation.mango.downtime.web.AbstractBasicDwr#get(int)
     */
    @DwrPermission(user = true)
    @Override
    public ProcessResult get(int id) {
        VO vo;
    
        if(id == Common.NEW_ID){
            vo = dao.getNewVo();
        }else{
            vo = dao.get(id);
        }
        ProcessResult response = new ProcessResult();
        response.addData("vo", vo);
        
        return response;
    }
    
    /* (non-Javadoc)
     * @see com.deltamation.mango.downtime.web.AbstractBasicDwr#getFull(int)
     */
    @DwrPermission(user = true)
    @Override
    public ProcessResult getFull(int id) {
        VO vo;
    
        if(id == Common.NEW_ID){
            vo = dao.getNewVo();
        }else{
            vo = dao.getFull(id);
        }
        ProcessResult response = new ProcessResult();
        response.addData("vo", vo);
        
        return response;
    }
}
