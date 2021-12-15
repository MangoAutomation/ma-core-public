/*
 * Copyright (C) 2021 RadixIot LLC. All rights reserved.
 */

package com.serotonin.m2m2.db.dao;

import org.junit.Before;

import com.infiniteautomation.mango.util.AbstractRoleBasedTest;
import com.infiniteautomation.mango.util.AssertionUtils;
import com.serotonin.m2m2.vo.AbstractVO;

/**
 * Base test for DAO Tests
 *
 * @param <VO>
 * @param <DAO>
 */
public abstract class AbstractVoDaoTest<VO extends AbstractVO, DAO extends AbstractVoDao> extends AbstractRoleBasedTest implements AssertionUtils {

    protected DAO dao;

    @Before
    public void setupDaoTest() {
        setupRoles();
        dao = getDao();
    }

    /**
     * Get the DAO for testing
     */
    abstract DAO getDao();

    /**
     * Create a new VO
     */
    abstract VO newVO();

    /**
     * Modify the fields of the VO to ensure they are saved in the database
     */
    abstract VO updateVO(VO toUpdate);

    /**
     * Assert that the 2 are equal
     */
    abstract void assertVoEqual(VO expected, VO actual);
}
