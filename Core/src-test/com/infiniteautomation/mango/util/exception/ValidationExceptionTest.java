/*
 * Copyright (C) 2021 RadixIot LLC. All rights reserved.
 */

package com.infiniteautomation.mango.util.exception;

import org.junit.Before;
import org.junit.Test;

import com.infiniteautomation.mango.spring.service.DataSourceService;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.MangoTestBase;
import com.serotonin.m2m2.vo.dataSource.mock.MockDataSourceVO;

import io.jsonwebtoken.lang.Assert;

public class ValidationExceptionTest extends MangoTestBase {

    private DataSourceService service;
    private MockDataSourceVO vo;

    @Before
    public void initTest(){
        service = Common.getBean(DataSourceService.class);
        vo = this.createMockDataSource();
    }

    @Test
    public void getValidationResult() {
        try {
            service.insert(vo);
        }catch (ValidationException validationException){
            Assert.notNull(validationException.getValidationResult());
        }
    }

    @Test
    public void getValidatedClass() {
        try {
            service.insert(vo);
        }catch (ValidationException validationException){
            //the validated class is null when validating the insertion of the VO
            Assert.isNull(validationException.getValidatedClass());
        }
    }

    @Test
    public void testToString() {
        try {
            service.insert(vo);
        }catch (ValidationException validationException){
            String toStringExpected = "Invalid value";
            Assert.hasText(toStringExpected,validationException.toString());
        }
    }

    @Test
    public void getValidationErrorMessage() {
        try {
            service.insert(vo);
        }catch (ValidationException validationException){
            Assert.notNull(validationException.getValidationErrorMessage(Common.getTranslations()));
        }
    }
}
