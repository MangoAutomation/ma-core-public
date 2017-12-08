/**
 * @copyright 2017 {@link http://infiniteautomation.com|Infinite Automation Systems, Inc.} All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.dao;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.MangoTestBase;

/**
 *
 * @author Terry Packer
 */
public class NumericPointValueDaoTest extends MangoTestBase{
    
    private NumericPointValueDaoTestHelper helper;

    @Before
    public void before() {
        super.before();
        this.helper = new NumericPointValueDaoTestHelper(Common.databaseProxy.newPointValueDao());
        this.helper.before();
    }
    @After
    public void after() {
        this.helper.after();
    }
    
    @Test
    public void testLatestMultiplePointValuesNoLimit() {
        this.helper.testLatestMultiplePointValuesNoLimit();
    }
    @Test
    public void testLatestMultiplePointValuesOrderByIdNoLimit() {
        this.helper.testLatestMultiplePointValuesOrderByIdNoLimit();
    }
    @Test
    public void testLatestMultiplePointValuesLimit() {
        this.helper.testLatestMultiplePointValuesLimit();
    }
    @Test
    public void testLatestMultiplePointValuesOrderByIdLimit() {
        this.helper.testLatestMultiplePointValuesOrderByIdLimit();
    }

    //Values Between
    
    @Test
    public void testRangeMultiplePointValuesNoLimit() {
        this.helper.testRangeMultiplePointValuesNoLimit();
    }
    @Test
    public void testRangeMultiplePointValuesOrderByIdNoLimit() {
        this.helper.testRangeMultiplePointValuesOrderByIdNoLimit();
    }
    @Test
    public void testRangeMultiplePointValuesLimit() {
        this.helper.testRangeMultiplePointValuesLimit();
    }
    @Test
    public void testRangeMultiplePointValuesOrderByIdLimit() {
        this.helper.testRangeMultiplePointValuesOrderByIdLimit();
    }
 
    //Bookend
    
    @Test
    public void testBookendMultiplePointValuesNoLimit() {
        this.helper.testBookendMultiplePointValuesNoLimit();
    }
    @Test
    public void testBookendMultiplePointValuesOrderByIdNoLimit() {
        this.helper.testBookendMultiplePointValuesOrderByIdNoLimit();
    }
    //TODO This test is broken @Test
    public void testBookendMultiplePointValuesLimit() {
        this.helper.testBookendMultiplePointValuesLimit();
    }
    
    //TODO This test is broken @Test
    public void testBookendMultiplePointValuesOrderByIdLimit() {
        this.helper.testBookendMultiplePointValuesOrderByIdLimit();
    }
    
    
}
