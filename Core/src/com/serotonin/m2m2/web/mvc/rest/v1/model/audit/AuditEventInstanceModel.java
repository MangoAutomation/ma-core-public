/**
 * Copyright (C) 2016 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.web.mvc.rest.v1.model.audit;

import com.serotonin.m2m2.vo.event.audit.AuditEventInstanceVO;
import com.serotonin.m2m2.web.mvc.rest.v1.model.AbstractBasicVoModel;

/**
 * @author Terry Packer
 *
 */
public class AuditEventInstanceModel extends AbstractBasicVoModel<AuditEventInstanceVO>{

	/**
	 * @param data
	 */
	public AuditEventInstanceModel(AuditEventInstanceVO data) {
		super(data);
	}
	
	public AuditEventInstanceModel(){
		super(new AuditEventInstanceVO());
	}

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.web.mvc.rest.v1.model.AbstractVoModel#getModelType()
	 */
	@Override
	public String getModelType() {
		return AuditEventInstanceModelDefinition.TYPE_NAME;
	}

}
