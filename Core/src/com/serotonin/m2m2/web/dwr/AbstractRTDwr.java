/*
    Copyright (C) 2013 Deltamation Software All rights reserved.
    @author Terry Packer
 */
package com.serotonin.m2m2.web.dwr;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;

import com.serotonin.m2m2.db.dao.AbstractDao;
import com.serotonin.m2m2.i18n.ProcessResult;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.rt.AbstractRT;
import com.serotonin.m2m2.rt.AbstractRTM;
import com.serotonin.m2m2.vo.AbstractActionVO;
import com.serotonin.m2m2.web.dwr.util.DwrPermission;

/**
 * Copyright (C) 2013 Deltamation Software. All Rights Reserved.
 * @author Terry Packer
 *
 */
public abstract class AbstractRTDwr<VO extends AbstractActionVO<?>, 
                      DAO extends AbstractDao<VO>,
                      RT extends AbstractRT<VO>,
                      RTM extends AbstractRTM<VO,RT,DAO>> extends AbstractDwr<VO, DAO>{
    //Runtime Manager Definition
    protected RTM runtimeManager;

    public AbstractRTDwr(DAO dao, String keyName, RTM runtimeManager){
        super(dao, keyName);
        this.runtimeManager = runtimeManager;
    }
    
    public AbstractRTDwr(DAO dao, String keyName, RTM runtimeManager, String topLevelKeyName){
        super(dao, keyName, topLevelKeyName);
        this.runtimeManager = runtimeManager;
    }
    
    @DwrPermission(admin = true)
    public ProcessResult toggle(int id) {

        ProcessResult response = new ProcessResult();
        VO mon = dao.get(id);
        
        if(mon != null){
	        mon.setEnabled(!mon.isEnabled());
	
	    	 // Validate
	        mon.validate(response);
	
	        if (!response.getHasMessages()) {
	            runtimeManager.save(mon);
	        }
	 
	        response.addData("enabled", mon.isEnabled());
	        response.addData("id", mon.getId());
        }
        return response;
    }  
    
    @DwrPermission(admin = true)
    @Override
    public ProcessResult remove(int id) {
        ProcessResult response = new ProcessResult();
        try{
            runtimeManager.delete(id);
        }catch(Exception e){
            //Handle the exceptions.
            LOG.error(e); 
            //TODO Clean up and generify these messages to some central place
            if(e instanceof DataIntegrityViolationException)
                response.addMessage(this.keyName + "Errors", new TranslatableMessage("dsEdit.unableToDeleteDueToConstraints"));
            else
                response.addMessage(this.keyName + "Errors", new TranslatableMessage("dsEdit.unableToDelete"));

        }
        
        response.addData("id", id);
        return response;
    }
    /**
     * Save the Process
     * @return
     */
    @DwrPermission(admin = true)
    @Override
    public ProcessResult save(VO vo) {
        ProcessResult response = new ProcessResult();
        vo.validate(response);
        if(!response.getHasMessages()){
            //Save it
            try{
                runtimeManager.save(vo);
            }catch(Exception e){
                //Handle the exceptions.
                LOG.error(e); //TODO Clean up and generify these messages to some central place
                if(e instanceof DuplicateKeyException)
                    response.addMessage(this.keyName + "Errors", new TranslatableMessage("dsEdit.alreadyExists"));
                else
                    response.addMessage(this.keyName + "Errors", new TranslatableMessage("dsEdit.unableToSave"));
            }
        }
        response.addData("vo", vo);
        response.addData("id", vo.getId()); //In case there are errors
        return response;
    }
    
    @DwrPermission(admin = true)
    @Override
    public ProcessResult saveFull(VO vo) {
        return this.save(vo);
    }
    
    @SuppressWarnings("unchecked")
    @DwrPermission(user = true)
    @Override
    public ProcessResult getCopy(int id) {
        ProcessResult response = super.getCopy(id);
        ((AbstractActionVO<?>) response.getData().get("vo")).setEnabled(false); //Ensure he isn't running
        //Don't Validate it, that will be done on save
        
        return response;
    }
}
