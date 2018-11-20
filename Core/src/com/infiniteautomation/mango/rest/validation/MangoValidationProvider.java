/**
 * Copyright (C) 2018  Infinite Automation Software. All rights reserved.
 */
package com.infiniteautomation.mango.rest.validation;

import javax.validation.ValidatorFactory;
import javax.validation.spi.ConfigurationState;

import org.hibernate.validator.HibernateValidator;

/**
 * This gives us the ability to wrap the Validator
 * @author Terry Packer
 *
 */
public class MangoValidationProvider extends HibernateValidator {

    @Override
    public ValidatorFactory buildValidatorFactory(ConfigurationState configurationState) {
        return new MangoValidatorFactory( configurationState );
    }
}
