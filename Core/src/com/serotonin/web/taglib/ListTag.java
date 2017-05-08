/*
    Copyright (C) 2006-2007 Serotonin Software Technologies Inc.
 	@author Matthew Lohbihler
 */
package com.serotonin.web.taglib;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.jsp.tagext.TagSupport;

/**
 * @author Matthew Lohbihler
 */
public class ListTag extends TagSupport {
    private static final long serialVersionUID = -1;

    private String var;
    private List<Object> list;

    public void setVar(String var) {
        this.var = var;
    }

    public void addListEntry(Object value) {
        list.add(value);
    }

    @Override
    public int doStartTag() {
        list = new ArrayList<Object>();
        pageContext.getRequest().setAttribute(var, list);
        return EVAL_BODY_INCLUDE;
    }

    @Override
    public void release() {
        super.release();
        var = null;
    }
}
