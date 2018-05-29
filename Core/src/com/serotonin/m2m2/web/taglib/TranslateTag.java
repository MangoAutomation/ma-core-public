/*
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.web.taglib;

import java.io.IOException;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.TagSupport;

import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.web.mvc.controller.ControllerUtils;
import com.serotonin.web.taglib.Functions;

public class TranslateTag extends TagSupport {
    private static final long serialVersionUID = 1L;

    private TranslatableMessage message;
    private String key;
    private boolean escapeQuotes;
    private boolean escapeDQuotes;

    public void setMessage(TranslatableMessage message) {
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
            s = message.translate(ControllerUtils.getTranslations(pageContext));
        else if (key != null)
            s = ControllerUtils.getTranslations(pageContext).translate(key);

        if (s != null) {
            if (escapeQuotes)
                s = Functions.quotEncode(s);
            if (escapeDQuotes)
                s = Functions.dquotEncode(s);

            //We do not want it encoding html
            s = Functions.escapeLessThan(s);
            
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
