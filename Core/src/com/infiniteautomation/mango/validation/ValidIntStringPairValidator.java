/**
 * Copyright (C) 2018  Infinite Automation Software. All rights reserved.
 */
package com.infiniteautomation.mango.validation;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import org.apache.commons.lang3.StringUtils;

import com.serotonin.db.pair.IntStringPair;
import com.serotonin.m2m2.util.VarNames;

/**
 * Validate to ensure a data point xid exists
 * 
 * @author Terry Packer
 *
 */
public class ValidIntStringPairValidator implements ConstraintValidator<ValidIntStringPair, IntStringPair>{
    
    boolean isScriptContextVariable;
    
    @Override
    public void initialize(ValidIntStringPair constraintAnnotation) {
        this.isScriptContextVariable = constraintAnnotation.isScriptContextVariable();
    }
    
    @Override
    public boolean isValid(IntStringPair pair, ConstraintValidatorContext context) {
        if(pair == null) {
            return true;
        }else if(isScriptContextVariable) {
            boolean valid = true;
            ValidDataPointValidator dpValidator = new ValidDataPointValidator();
            if(!dpValidator.isValid(pair.getKey(), context)) {
                context.buildConstraintViolationWithTemplate("event.script.contextPointMissing")
                    .addPropertyNode("key").addConstraintViolation();
                valid = false;
            }

            String varName = pair.getValue();
            if (StringUtils.isBlank(varName)) {
                context.buildConstraintViolationWithTemplate("validate.allVarNames")
                    .addPropertyNode("value").addConstraintViolation();
                valid = false;
            }

            if (!VarNames.validateVarName(varName)) {
                context.buildConstraintViolationWithTemplate("validate.invalidVarName")
                    .addPropertyNode("value").addConstraintViolation();
                valid = false;
            }
            return valid;
        }else {
            return true;
        }
    }

}
