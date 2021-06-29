/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */

package com.serotonin.m2m2.rt.dataSource;

import java.text.DecimalFormat;
import java.util.regex.Pattern;

import org.junit.Assert;
import org.junit.Test;

import com.serotonin.m2m2.DataTypes;
import com.serotonin.m2m2.MangoTestBase;
import com.serotonin.m2m2.i18n.TranslatableException;
import com.serotonin.m2m2.rt.dataImage.types.DataValue;
import com.serotonin.m2m2.view.text.TextRenderer;

/**
 * Test the data source utils class
 * @author Terry Packer
 */
public class DataSourceUtilsTest extends MangoTestBase {

    @Test
    public void testHexRendering() throws TranslatableException {

        Pattern valuePattern = Pattern.compile("result=([0-9]*)");
        int valueGroup = 1;
        TextRenderer renderer = null;
        DecimalFormat valueFormat = new DecimalFormat("00");
        DataValue value = DataSourceUtils.getValue(valuePattern, valueGroup, "result=15" , DataTypes.NUMERIC,
                "false", renderer, valueFormat, "Test Point Name");

        Double expected = Double.valueOf(15);
        Assert.assertEquals(expected.toString(), Double.toString(value.getDoubleValue()));

    }

}
