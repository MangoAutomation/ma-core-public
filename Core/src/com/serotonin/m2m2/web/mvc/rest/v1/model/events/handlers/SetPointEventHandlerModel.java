/**
 * Copyright (C) 2017 Infinite Automation Software. All rights reserved.
 *
 */
package com.serotonin.m2m2.web.mvc.rest.v1.model.events.handlers;

import org.apache.commons.lang3.StringUtils;

import com.serotonin.m2m2.db.dao.DataPointDao;
import com.serotonin.m2m2.vo.DataPointVO;
import com.serotonin.m2m2.vo.event.SetPointEventHandlerVO;

/**
 * 
 * @author Terry Packer
 */
public class SetPointEventHandlerModel extends AbstractEventHandlerModel<SetPointEventHandlerVO>{

	private String targetPointXid;
	private String activePointXid;
	private String inactivePointXid;
	
	/**
	 * @param data
	 */
	public SetPointEventHandlerModel(SetPointEventHandlerVO data) {
		super(data);
		
		DataPointVO vo = DataPointDao.instance.get(data.getTargetPointId());
		if(vo != null)
			this.targetPointXid = vo.getXid();
		
		if(data.getActiveAction() == SetPointEventHandlerVO.SET_ACTION_POINT_VALUE){
			vo = DataPointDao.instance.get(data.getActivePointId());
			if(vo != null)
				this.activePointXid = vo.getXid();
		}
		if(data.getInactiveAction() == SetPointEventHandlerVO.SET_ACTION_POINT_VALUE){
			vo = DataPointDao.instance.get(data.getInactivePointId());
			if(vo != null)
				this.inactivePointXid = vo.getXid();
		}
	
	}
	
	public SetPointEventHandlerModel(){
		super(new SetPointEventHandlerVO());
	}
	
    public String getTargetPointXid() {
        return this.targetPointXid;
    }

    public void setTargetPointXid(String targetPointXid) {
        this.targetPointXid = targetPointXid;
    }

    public String getActiveAction() {
        return SetPointEventHandlerVO.SET_ACTION_CODES.getCode(this.data.getActiveAction());
    }

    public void setActiveAction(String activeAction) {
        this.data.setActiveAction(SetPointEventHandlerVO.SET_ACTION_CODES.getId(activeAction));
    }

    public String getInactiveAction() {
        return SetPointEventHandlerVO.SET_ACTION_CODES.getCode(this.data.getInactiveAction());
    }

    public void setInactiveAction(String inactiveAction) {
        this.data.setInactiveAction(SetPointEventHandlerVO.SET_ACTION_CODES.getId(inactiveAction));
    }
    
    public String getActiveValueToSet() {
        return this.data.getActiveValueToSet();
    }

    public void setActiveValueToSet(String activeValueToSet) {
        this.data.setActiveValueToSet(activeValueToSet);
    }

    public String getActivePointXid() {
        return this.activePointXid;
    }

    public void setActivePointXid(String activePointXid) {
        this.activePointXid = activePointXid;
    }

    public String getInactiveValueToSet() {
        return this.data.getInactiveValueToSet();
    }

    public void setInactiveValueToSet(String inactiveValueToSet) {
        this.data.setInactiveValueToSet(inactiveValueToSet);
    }

    public String getInactivePointXid() {
        return this.inactivePointXid;
    }

    public void setInactivePointXid(String inactivePointXid) {
        this.inactivePointXid = inactivePointXid;
    }
    
    /* (non-Javadoc)
     * @see com.serotonin.m2m2.web.mvc.rest.v1.model.AbstractRestModel#getData()
     */
    @Override
    public SetPointEventHandlerVO getData() {
    	DataPointVO vo;
    	
	    //Set the IDs if necessary
    	if(!StringUtils.isEmpty(this.targetPointXid)){
    		vo = DataPointDao.instance.getByXid(targetPointXid);
    		if(vo != null)
    			this.data.setTargetPointId(vo.getId());
    	}
    	
		if(data.getActiveAction() == SetPointEventHandlerVO.SET_ACTION_POINT_VALUE && !StringUtils.isEmpty(activePointXid)){
			vo = DataPointDao.instance.getByXid(activePointXid);
			if(vo != null)
				data.setActivePointId(vo.getId());
		}
		if(data.getInactiveAction() == SetPointEventHandlerVO.SET_ACTION_POINT_VALUE && !StringUtils.isEmpty(inactivePointXid)){
			vo = DataPointDao.instance.getByXid(inactivePointXid);
			if(vo != null)
				data.setInactivePointId(vo.getId());
		}
    	
    	return this.data;
    }
}
