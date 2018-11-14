/**
 * Copyright (C) 2018  Infinite Automation Software. All rights reserved.
 */
package com.infiniteautomation.mango.validation;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import com.serotonin.m2m2.db.dao.DataPointDao;

/**
 * Validate to ensure a data point xid exists
 * 
 * @author Terry Packer
 *
 */
public class ValidDataPointValidator implements ConstraintValidator<ValidDataPoint, Object>{
    
    @Override
    public boolean isValid(Object id, ConstraintValidatorContext context) {
        if(id instanceof String)
            return DataPointDao.getInstance().getIdByXid((String)id) != null;
        else if(id instanceof Integer)
            return DataPointDao.getInstance().getXidById((Integer)id) != null;
        else
            return false;
    }

}
