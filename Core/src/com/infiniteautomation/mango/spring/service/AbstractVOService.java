/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.infiniteautomation.mango.spring.service;

import java.util.concurrent.CompletionStage;

import org.apache.commons.lang3.StringUtils;

import com.infiniteautomation.mango.spring.events.DaoEventType;
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
public abstract class AbstractVOService<T extends AbstractVO, DAO extends AbstractVOAccess<T>> extends AbstractBasicVOService<T,DAO> {

    /**
     *
     * @param dao
     * @param dependencies dependencies for service
     */
    public AbstractVOService(DAO dao, ServiceDependencies dependencies) {
        super(dao, dependencies);
    }

    /**
     * Validate a VO
     * @param vo
     * @return
     */
    @Override
    public ProcessResult validate(T vo) {
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
            throw new ValidationException(result, vo.getClass());
        }

        //Generate an Xid if necessary
        if(StringUtils.isEmpty(vo.getXid()))
            vo.setXid(dao.generateUniqueXid());

        ensureValid(vo);
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

    protected static class AsyncOperation<T> extends BasicAsyncOperation<T> {
        final String xid;

        protected AsyncOperation(DaoEventType operation, String xid) {
            this(operation, xid, null);
        }

        protected AsyncOperation(DaoEventType operation, String xid, T vo) {
            super(operation, Common.NEW_ID, vo);
            this.xid = xid;
        }
    }

    @Override
    protected T doAsyncOperation(BasicAsyncOperation<T> operation) {
        if (operation instanceof AsyncOperation) {
            AsyncOperation<T> operationXid = (AsyncOperation<T>) operation;
            switch (operationXid.type) {
                case GET:
                    return get(operationXid.xid);
                case UPDATE:
                    return update(operationXid.xid, operationXid.vo);
                case DELETE:
                    return delete(operationXid.xid);
            }
        }
        return super.doAsyncOperation(operation);
    }

    public CompletionStage<T> getAsync(String xid) {
        return concurrentProcessor.add(new AsyncOperation<>(DaoEventType.GET, xid));
    }

    public CompletionStage<T> deleteAsync(String xid) {
        return concurrentProcessor.add(new AsyncOperation<>(DaoEventType.DELETE, xid));
    }

    public CompletionStage<T> updateAsync(String xid, T vo) {
        return concurrentProcessor.add(new AsyncOperation<>(DaoEventType.UPDATE, xid, vo));
    }
}
