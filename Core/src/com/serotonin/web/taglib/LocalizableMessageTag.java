/*
    Copyright (C) 2006-2007 Serotonin Software Technologies Inc.
 	@author Matthew Lohbihler
 */
package com.serotonin.web.taglib;

import java.io.IOException;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.TagSupport;

import com.serotonin.web.i18n.I18NUtils;
import com.serotonin.web.i18n.LocalizableMessage;

/**
 * @author Matthew Lohbihler
 */
public class LocalizableMessageTag extends TagSupport {
    private static final long serialVersionUID = 1L;

    private LocalizableMessage message;
    private String key;
    private boolean escapeQuotes;
    private boolean escapeDQuotes;

    public void setMessage(LocalizableMessage message) {
        this.message = message;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public void setEscapeQuotes(boolean escapeQuotes) {
        this.escapeQuotes = escapeQuotes;
    }

    public void setEscapeDQuotes(boolean escapeDQuotes) {
        this.escapeDQuotes = escapeDQuotes;
    }

    @Override
    public int doEndTag() throws JspException {
        String s = null;

        if (message != null)
            s = I18NUtils.getMessage(pageContext, message);
        else if (key != null)
            s = I18NUtils.getMessage(pageContext, key);

        if (s != null) {
            if (escapeQuotes)
                s = Functions.quotEncode(s);
            if (escapeDQuotes)
                s = Functions.dquotEncode(s);

            try {
                pageContext.getOut().write(s);
            }
            catch (IOException e) {
                throw new JspException(e);
            }
        }

        return EVAL_PAGE;
    }
}
