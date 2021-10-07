/*
 * Copyright (C) 2021 RadixIot LLC. All rights reserved.
 */

package com.infiniteautomation.mango.util.exception;

import static org.junit.Assert.*;

import java.util.Locale;

import org.junit.Before;
import org.junit.Test;

import com.infiniteautomation.mango.spring.service.DataSourceService;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.MangoTestBase;
import com.serotonin.m2m2.i18n.ProcessResult;
import com.serotonin.m2m2.i18n.TranslatableException;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.i18n.Translations;
import com.serotonin.m2m2.vo.dataSource.mock.MockDataSourceVO;


public class ValidationExceptionTest extends MangoTestBase {

    private DataSourceService service;
    private MockDataSourceVO vo;

    @Before
    public void initTest() {
        service = Common.getBean(DataSourceService.class);
        vo = this.createMockDataSource();
    }

    @Test
    public void getValidationResult() {
        try {
            service.insert(vo);
            fail("This should not be inserted");
        } catch (ValidationException validationException) {
            assertNotNull(validationException.getValidationResult());
        }
    }

    @Test
    public void getValidatedClass() {
        try {
            service.insert(vo);
            fail("This should not be inserted");
        } catch (ValidationException validationException) {
            assertNotNull(validationException.getValidatedClass());
            assertEquals(vo.getClass(), validationException.getValidatedClass());
        }
    }

    @Test
    public void testToString() {
        try {
            service.insert(vo);
            fail("This should not be inserted");
        } catch (ValidationException validationException) {
            String toStringExpected = validationException.getValidationErrorMessage(Translations.getTranslations(Locale.ENGLISH));
            validationException.getValidationErrorMessage(Translations.getTranslations(Locale.ENGLISH));
            assertTrue(validationException.toString().contains(toStringExpected.trim()));
        }
    }

    @Test
    public void getValidationErrorMessage() {
        try {
            service.insert(vo);
            fail("This should not be inserted");
        } catch (ValidationException validationException) {
            assertNotNull(validationException.getValidationErrorMessage(Common.getTranslations()));
        }
    }

    @Test
    public void checkMessageWithArguments() {
        TranslatableMessage m = new TranslatableMessage("literal", "This is a test");
        TranslatableException e = new TranslatableException(m);

        assertTrue(e.getTranslatableMessage().translate(Common.getTranslations()).contains("This is a test"));
        assertEquals(e.getTranslatableMessage().translate(Common.getTranslations()), m.translate(Common.getTranslations()));

        ProcessResult result = new ProcessResult();
        result.addContextualMessage("test", m);
        ValidationException ve = new ValidationException(result);
        assertTrue(ve.getValidationErrorMessage(Common.getTranslations()).contains(e.getTranslatableMessage().translate(Common.getTranslations())));
    }

}
