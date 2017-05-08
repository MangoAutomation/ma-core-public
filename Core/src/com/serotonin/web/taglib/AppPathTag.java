/*
    Copyright (C) 2006-2007 Serotonin Software Technologies Inc.
 	@author Matthew Lohbihler
 */
package com.serotonin.web.taglib;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.tagext.TagSupport;

/**
 * @author Matthew Lohbihler
 */
public class AppPathTag extends TagSupport {
    private static final long serialVersionUID = -1;
    
    public int doEndTag() throws JspException {
        HttpServletRequest request = (HttpServletRequest)pageContext.getRequest();
        String path = request.getContextPath();
        
        JspWriter out = pageContext.getOut();
        try {
            out.write(path);
            out.write("/");
        }
        catch (IOException e) {
            throw new JspException("Error writing tag content", e);
        }
        
        return EVAL_PAGE;
    }
}
