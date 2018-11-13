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
public class DataPointXidExistsValidator implements ConstraintValidator<DataPointXidExists, String>{
    
    @Override
    public boolean isValid(String xid, ConstraintValidatorContext context) {
        return DataPointDao.getInstance().getIdByXid(xid) != null;
    }

}
