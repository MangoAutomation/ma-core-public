/**
 * Copyright (C) 2018  Infinite Automation Software. All rights reserved.
 */
package com.infiniteautomation.mango.validation;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import javax.validation.Path;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.validator.internal.engine.constraintvalidation.ConstraintValidatorContextImpl;

import com.infiniteautomation.mango.rest.validation.MangoValidatorFactory;
import com.serotonin.m2m2.db.dao.DataPointDao;

/**
 * Validate to ensure a data point xid exists
 * 
 * @author Terry Packer
 *
 */
public class ValidDataPointValidator implements ConstraintValidator<ValidDataPoint, Object>{
    //TODO This logic only works for the workflow of validate model --> validate VO --> Save
    //   it won't work for validating a list of models
    private static final ThreadLocal<List<String>> validatedPaths = new ThreadLocal<>();

    private String voProperty;
    
    @Override
    public void initialize(ValidDataPoint constraintAnnotation) {
        this.voProperty = constraintAnnotation.voProperty();
    }
    
    @Override
    public boolean isValid(Object id, ConstraintValidatorContext context) {
        if(context instanceof ConstraintValidatorContextImpl) {
            Field f;
            try {
                if(!StringUtils.isEmpty(this.voProperty)) {
                    //Validating something for a model
                    List<String> validated = validatedPaths.get();
                    if(validated == null) {
                        validated = new ArrayList<>();
                    }
                    validated.add(voProperty);
                    validatedPaths.set(validated);
                }else {
                    //Potentially a VO, did we already validate this?
                    f = context.getClass().getDeclaredField("basePath");
                    f.setAccessible(true);
                    Path validatedPath = (Path)f.get(context);
                    String propertyName = validatedPath.iterator().next().getName();
                    List<String> validated = validatedPaths.get();
                    if(validated != null) {
                        for(String valid : validated) {
                            if(propertyName.equals(valid))
                                return true;
                        }
                    }
                }
            } catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        if(id instanceof String)
            return DataPointDao.getInstance().getIdByXid((String)id) != null;
        else if(id instanceof Integer)
            return DataPointDao.getInstance().getXidById((Integer)id) != null;
        else
            return false;
    }

}
