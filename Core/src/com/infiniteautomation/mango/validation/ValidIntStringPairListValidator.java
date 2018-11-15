/**
 * Copyright (C) 2018  Infinite Automation Software. All rights reserved.
 */
package com.infiniteautomation.mango.validation;

import java.util.ArrayList;
import java.util.List;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import com.serotonin.db.pair.IntStringPair;

/**
 * Validate to ensure a data point xid exists
 * 
 * @author Terry Packer
 *
 */
public class ValidIntStringPairListValidator implements ConstraintValidator<ValidIntStringPairList, List<IntStringPair>>{
    
    boolean isScriptContext;
    
    @Override
    public void initialize(ValidIntStringPairList constraintAnnotation) {
        this.isScriptContext = constraintAnnotation.isScriptContext();
    }
    
    @Override
    public boolean isValid(List<IntStringPair> list, ConstraintValidatorContext context) {
        if(list == null) {
            return true;
        }else if(isScriptContext) {
            
            List<String> varNameSpace = new ArrayList<String>();
            for(IntStringPair cxt : list) {
                
                
                String varName = cxt.getValue();
                if (varNameSpace.contains(varName))
                    return false;
                varNameSpace.add(varName);
            }
            
            return true;
        }else {
            return true;
        }
    }

}
