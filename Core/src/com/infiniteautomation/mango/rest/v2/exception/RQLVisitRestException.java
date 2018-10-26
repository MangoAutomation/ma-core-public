/**
 * Copyright (C) 2017 Infinite Automation Software. All rights reserved.
 *
 */
package com.infiniteautomation.mango.rest.v2.exception;

import org.springframework.http.HttpStatus;

import com.infiniteautomation.mango.db.query.RQLToCondition.RQLVisitException;
import com.serotonin.m2m2.i18n.TranslatableMessage;

import net.jazdw.rql.parser.ASTNode;

/**
 * @author Jared Wiltshire
 */
public class RQLVisitRestException extends AbstractRestV2Exception {

    private static final long serialVersionUID = 1L;
    private final ASTNode node;

    public RQLVisitRestException(RQLVisitException cause) {
        super(HttpStatus.BAD_REQUEST, MangoRestErrorCode.RQL_VISIT_ERROR, new TranslatableMessage("common.invalidRql"), cause.getCause());
        this.node = cause.getNode();
    }

    public ASTNode getNode() {
        return node;
    }

}
