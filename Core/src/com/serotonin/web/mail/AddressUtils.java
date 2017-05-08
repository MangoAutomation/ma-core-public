/*
    Copyright (C) 2006-2007 Serotonin Software Technologies Inc.
 	@author Matthew Lohbihler
 */
package com.serotonin.web.mail;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

import org.apache.commons.lang3.StringUtils;

/**
 * @author Matthew Lohbihler
 */
public class AddressUtils {
    private static final String EMAIL_REGEX = ".+?@.+?\\..+";

    public static boolean isValidEmailAddress(String s) {
        if (StringUtils.isBlank(s))
            return false;

        try {
            new InternetAddress(s);
        }
        catch (AddressException e) {
            return false;
        }

        Pattern pattern = Pattern.compile(EMAIL_REGEX);
        Matcher matcher = pattern.matcher(s);
        return matcher.find();
    }
}
