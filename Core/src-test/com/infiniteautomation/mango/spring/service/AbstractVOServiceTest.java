/**
 * Copyright (C) 2019  Infinite Automation Software. All rights reserved.
 */
package com.infiniteautomation.mango.spring.service;

import org.junit.Test;

import com.infiniteautomation.mango.spring.db.AbstractTableDefinition;
import com.infiniteautomation.mango.util.exception.NotFoundException;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.dao.AbstractDao;
import com.serotonin.m2m2.vo.AbstractVO;

/**
 * Base class to test the AbstractVO service layer implementations
 *
 * @author Terry Packer
 *
 */
public abstract class AbstractVOServiceTest<VO extends AbstractVO<?>, TABLE extends AbstractTableDefinition, DAO extends AbstractDao<VO,TABLE>, SERVICE extends AbstractVOService<VO,TABLE,DAO>> extends AbstractBasicVOServiceTest<VO, TABLE, DAO, SERVICE> {

    public AbstractVOServiceTest() {

    }

    public AbstractVOServiceTest(boolean enableWebDb, int webDbPort) {
        super(enableWebDb, webDbPort);
    }

    @Test
    public void testUpdateViaXid() {
        runTest(() -> {
            Common.setUser(systemSuperadmin);
            try {
                VO vo = insertNewVO();
                VO fromDb = service.get(vo.getId());
                assertVoEqual(vo, fromDb);

                VO updated = updateVO(vo);
                service.update(vo.getXid(), updated);
                fromDb = service.get(updated.getXid());
                assertVoEqual(updated, fromDb);
            }finally {
                Common.removeUser();
            }
        });
    }

    @Test(expected = NotFoundException.class)
    public void testDeleteViaXid() {
        runTest(() -> {
            Common.setUser(systemSuperadmin);
            try {
                VO vo = insertNewVO();
                VO fromDb = service.get(vo.getId());
                assertVoEqual(vo, fromDb);
                service.delete(vo.getXid());
                service.get(vo.getXid());
            }finally {
                Common.removeUser();
            }
        });
    }

}
