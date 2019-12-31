/**
 * Copyright (C) 2019  Infinite Automation Software. All rights reserved.
 */
package com.serotonin.m2m2.vo;

import com.infiniteautomation.mango.spring.service.PermissionService;
import com.serotonin.m2m2.i18n.ProcessResult;
import com.serotonin.m2m2.vo.permission.PermissionHolder;

/**
 * @author Terry Packer
 *
 */
public interface ChangeValidatable<T extends AbstractBasicVO> {

    /**
     * Validate a new VO
     * @param result
     * @param service
     * @param savingUser
     */
    public abstract void validate(ProcessResult result, PermissionService service, PermissionHolder savingUser);
    
    /**
     * Validate using the information from the current existing VO
     * @param result
     * @param existing
     * @param service
     * @param savingUser
     */
    default public void validate(ProcessResult result, T existing, PermissionService service, PermissionHolder savingUser) {
        validate(result, service, savingUser);
    }
    
}
