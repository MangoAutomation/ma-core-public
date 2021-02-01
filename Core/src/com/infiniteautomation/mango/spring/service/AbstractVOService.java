/**
 * Copyright (C) 2018  Infinite Automation Software. All rights reserved.
 */
package com.infiniteautomation.mango.spring.service;

import org.apache.commons.lang3.StringUtils;
import org.jooq.Record;
import org.jooq.Table;

import com.infiniteautomation.mango.util.exception.NotFoundException;
import com.infiniteautomation.mango.util.exception.ValidationException;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.dao.AbstractVOAccess;
import com.serotonin.m2m2.i18n.ProcessResult;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.vo.AbstractVO;
import com.serotonin.m2m2.vo.permission.PermissionException;
import com.serotonin.m2m2.vo.permission.PermissionHolder;
import com.serotonin.validation.StringValidation;

/**
 * Base Service
 *
 * @author Terry Packer
 *
 */
public abstract class AbstractVOService<T extends AbstractVO, R extends Record, TABLE extends Table<R>, DAO extends AbstractVOAccess<T,R,TABLE>> extends AbstractBasicVOService<T,R,TABLE,DAO> {

    /**
     *
     * @param dao
     * @param permissionService
     */
    public AbstractVOService(DAO dao, PermissionService permissionService) {
        super(dao, permissionService);
    }

    /**
     * Validate a VO
     * @param vo
     * @param user
     * @return
     */
    @Override
    public ProcessResult validate(T vo, PermissionHolder user) {
        ProcessResult result = new ProcessResult();
        if (StringUtils.isBlank(vo.getXid()))
            result.addContextualMessage("xid", "validate.required");
        else if (StringValidation.isLengthGreaterThan(vo.getXid(), 100))
            result.addMessage("xid", new TranslatableMessage("validate.notLongerThan", 100));
        else if (!isXidUnique(vo.getXid(), vo.getId()))
            result.addContextualMessage("xid", "validate.xidUsed");

        if (StringUtils.isBlank(vo.getName()))
            result.addContextualMessage("name", "validate.required");
        else if (StringValidation.isLengthGreaterThan(vo.getName(), 255))
            result.addMessage("name", new TranslatableMessage("validate.notLongerThan", 255));
        return result;
    }

    protected boolean isXidUnique(String xid, int id){
        return dao.isXidUnique(xid,id);
    }

    /**
     *
     * @param xid
     * @return
     * @throws NotFoundException
     * @throws PermissionException
     */
    public T get(String xid) throws NotFoundException, PermissionException {
        T vo = dao.getByXid(xid);
        if(vo == null)
            throw new NotFoundException();
        PermissionHolder user = Common.getUser();
        ensureReadPermission(user, vo);
        return vo;
    }

    /**
     *
     * @param vo
     * @return
     * @throws PermissionException
     * @throws ValidationException
     */
    @Override
    public T insert(T vo) throws PermissionException, ValidationException {
        PermissionHolder user = Common.getUser();
        //Ensure they can create
        ensureCreatePermission(user, vo);

        //Ensure id is not set
        if(vo.getId() != Common.NEW_ID) {
            ProcessResult result = new ProcessResult();
            result.addContextualMessage("id", "validate.invalidValue");
            throw new ValidationException(result);
        }

        //Generate an Xid if necessary
        if(StringUtils.isEmpty(vo.getXid()))
            vo.setXid(dao.generateUniqueXid());

        ensureValid(vo, user);
        dao.insert(vo);
        return vo;
    }

    /**
     * Update a vo
     * @param existingXid
     * @param vo
     * @return
     * @throws PermissionException
     * @throws ValidationException
     * @throws NotFoundException
     */
    public T update(String existingXid, T vo) throws PermissionException, ValidationException, NotFoundException {
        return update(get(existingXid), vo);
    }


    /**
     * Delete a VO and its relational data
     * @param xid
     * @return
     * @throws PermissionException
     * @throws NotFoundException
     */
    public T delete(String xid) throws PermissionException, NotFoundException {
        T vo = get(xid);
        return delete(vo);
    }

    public String generateUniqueXid() {
        return dao.generateUniqueXid();
    }

}
