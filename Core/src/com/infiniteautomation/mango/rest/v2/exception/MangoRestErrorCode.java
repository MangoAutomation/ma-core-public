/**
 * Copyright (C) 2017 Infinite Automation Software. All rights reserved.
 *
 */
package com.infiniteautomation.mango.rest.v2.exception;

/**
 * Error Codes for Mango Rest Errors
 *
 * 0-999 Series for Module Custom Errors
 *
 * 4000 Series are for Bad Request Http Status
 *
 * 5000 Series are for Internal Server Error Http Status
 *
 *
 * @author Terry Packer
 */
public enum MangoRestErrorCode implements IMangoRestErrorCode {

    //**** 0000 - 999 Reserved for Modules ****

    //***** 4000 Series *****

    RQL_PARSE_FAILURE(4001),

    /**
     * this is specifically caused by a VO validation method failing, as opposed to BAD_REQUEST which is more generic
     */
    VALIDATION_FAILED(4002),
    ALREADY_EXISTS(4003),
    ACCESS_DENIED(4004),

    /**
     * HTTP 400 bad request technically means "invalid syntax",
     * if we send MangoRestErrorCode.BAD_REQUEST too it indicates its more of a problem inside the JSON structure rather than a
     * syntactic error
     */
    BAD_REQUEST(4005),

    IP_RATE_LIMITED(4006),
    USER_RATE_LIMITED(4007),
    GENERIC_AUTHENTICATION_FAILED(4008),
    ACCOUNT_DISABLED(4009),
    CREDENTIALS_EXPIRED(4010),
    BAD_CREDENTIALS(4011),
    PASSWORD_CHANGE_FAILED(4012),
    RQL_VISIT_ERROR(4013),

    //***** 5000 Series *****
    GENERIC_500(5000);

    private final int code;

    private MangoRestErrorCode(int code){
        this.code = code;
    }

    @Override
    public int getCode(){
        return this.code;
    }
}
