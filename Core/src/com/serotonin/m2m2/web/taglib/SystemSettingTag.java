/*
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.web.taglib;

import java.io.IOException;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.TagSupport;

import com.serotonin.m2m2.db.dao.SystemSettingsDao;
import com.serotonin.web.taglib.Functions;

public class SystemSettingTag extends TagSupport {
    private static final long serialVersionUID = 1L;

    private String key;
    private String defaultValue;
    private boolean escapeQuotes;
    private boolean escapeDQuotes;

    public void setKey(String key) {
        this.key = key;
    }

    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    public void setEscapeQuotes(boolean escapeQuotes) {
        this.escapeQuotes = escapeQuotes;
    }

    public void setEscapeDQuotes(boolean escapeDQuotes) {
        this.escapeDQuotes = escapeDQuotes;
    }

    @Override
    public int doEndTag() throws JspException {
        String value = SystemSettingsDao.instance.getValue(key, defaultValue);

        if (value != null) {
            if (escapeQuotes)
                value = Functions.quotEncode(value);
            if (escapeDQuotes)
                value = Functions.dquotEncode(value);

            try {
                pageContext.getOut().write(value);
            }
            catch (IOException e) {
                throw new JspException(e);
            }
        }

        return EVAL_PAGE;
    }
}
