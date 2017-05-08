/*
    Copyright (C) 2006-2007 Serotonin Software Technologies Inc.
 	@author Matthew Lohbihler
 */
package com.serotonin.web.taglib;

import java.io.IOException;
import java.text.MessageFormat;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.tagext.BodyTagSupport;

/**
 * @author Matthew Lohbihler
 */
public class ResolveMacrosTag extends BodyTagSupport {
    private static final long serialVersionUID = -1;

    private String pattern;
    private Object param;
    
    public void setPattern(String pattern) {
        this.pattern = pattern;
    }
    public void setParam(Object param) {
        this.param = param;
    }
    
    @Override
    public int doEndTag() throws JspException {
        String p = pattern;
        if (pattern == null)
            p = bodyContent.getString().trim();
        
        Object[] args;
        
        if (param == null)
            args = new Object[0];
        else if (param.getClass().isArray())
            args = (Object[])param;
        else
            args = new Object[] {param};
        
        try {
            JspWriter out = pageContext.getOut();
            out.write(MessageFormat.format(p, args));
        }
        catch (IOException e) {
            throw new JspException("Error while writing resolve macros tag", e);
        }
        
        return EVAL_PAGE;
    }
    
    @Override
    public void release() {
        super.release();
        pattern = null;
        param = null;
    }
}
