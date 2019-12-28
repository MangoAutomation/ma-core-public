/**
 * Copyright (C) 2019  Infinite Automation Software. All rights reserved.
 */
package com.infiniteautomation.mango.spring.service;

import org.junit.Test;

import com.infiniteautomation.mango.util.exception.NotFoundException;
import com.serotonin.m2m2.db.dao.AbstractDao;
import com.serotonin.m2m2.vo.AbstractVO;

/**
 * Base class to test the AbstractVO service layer implementations
 * 
 * @author Terry Packer
 *
 */
public abstract class AbstractVOServiceTest<VO extends AbstractVO<?>, DAO extends AbstractDao<VO>, SERVICE extends AbstractVOService<VO,DAO>> extends AbstractBasicVOServiceTest<VO, DAO, SERVICE> {

    public AbstractVOServiceTest() {
        
    }
    
    public AbstractVOServiceTest(boolean enableWebDb, int webDbPort) {
        super(enableWebDb, webDbPort);
    }

    @Test
    public void testUpdateViaXid() {
        runTest(() -> {
            VO vo = insertNewVO();
            VO fromDb = service.get(vo.getId(), true, systemSuperadmin);
            assertVoEqual(vo, fromDb);
            
            VO updated = updateVO(vo);
            service.update(vo.getXid(), updated, true, systemSuperadmin);
            fromDb = service.get(vo.getXid(), true, systemSuperadmin);
            assertVoEqual(updated, fromDb);            
        });
    }
    
    @Test(expected = NotFoundException.class)
    public void testDeleteViaXid() {
        runTest(() -> {
            VO vo = insertNewVO();
            VO fromDb = service.get(vo.getId(), true, systemSuperadmin);
            assertVoEqual(vo, fromDb);
            service.delete(vo.getXid(), systemSuperadmin);
            service.get(vo.getXid(), true, systemSuperadmin);            
        });
    }
    
}
