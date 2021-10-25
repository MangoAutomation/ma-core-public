/*
 * Copyright (C) 2021 RadixIot LLC. All rights reserved.
 */

package com.serotonin.m2m2.db.dao;

import org.junit.Before;

import com.serotonin.m2m2.MangoTestBase;
import com.serotonin.m2m2.vo.AbstractVO;

/**
 * Base test for DAO Tests
 *
 * @param <VO>
 * @param <DAO>
 */
public abstract class AbstractVoDaoTest<VO extends AbstractVO, DAO extends AbstractVoDao> extends MangoTestBase {

    protected DAO dao;

    @Before
    public void setupDaoTest() {
        dao = getDao();
    }

    /**
     * Get the DAO for testing
     * @return
     */
    abstract DAO getDao();

    /**
     * Create a new VO
     * @return
     */
    abstract VO newVO();

    /**
     * Modify the fields of the VO to ensure they are saved in the database
     * @param toUpdate
     * @return
     */
    abstract VO updateVO(VO toUpdate);

    /**
     * Assert that the 2 are equal
     * @param expected
     * @param actual
     */
    abstract void assertVoEqual(VO expected, VO actual);
}
