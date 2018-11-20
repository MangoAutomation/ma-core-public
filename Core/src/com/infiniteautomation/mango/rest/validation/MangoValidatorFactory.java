/**
 * Copyright (C) 2018  Infinite Automation Software. All rights reserved.
 */
package com.infiniteautomation.mango.rest.validation;

import javax.validation.ConstraintValidatorFactory;
import javax.validation.Validator;
import javax.validation.spi.ConfigurationState;

import org.hibernate.validator.internal.engine.ValidatorFactoryImpl;
import org.hibernate.validator.internal.engine.ValidatorImpl;

/**
 * @author Terry Packer
 *
 */
public class MangoValidatorFactory extends ValidatorFactoryImpl {
    
    /**
     * @param configurationState
     */
    public MangoValidatorFactory(ConfigurationState configurationState) {
        super(configurationState);
    }
    
    @Override
    public Validator getValidator() {
        return new MangoValidatorWrapper((ValidatorImpl)super.getValidator());
    }
}
