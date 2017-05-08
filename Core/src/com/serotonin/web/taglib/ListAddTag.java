/*
    Copyright (C) 2006-2007 Serotonin Software Technologies Inc.
 	@author Matthew Lohbihler
 */
package com.serotonin.web.taglib;

import java.util.List;

import javax.servlet.jsp.tagext.TagSupport;

/**
 * @author Matthew Lohbihler
 */
public class ListAddTag extends TagSupport {
    private static final long serialVersionUID = -1;

    private List<Object> list;
    private Object element;

    public void setList(List<Object> list) {
        this.list = list;
    }

    public void setElement(Object element) {
        this.element = element;
    }

    @Override
    public int doStartTag() {
        list.add(element);
        return EVAL_BODY_INCLUDE;
    }

    @Override
    public void release() {
        super.release();
        list = null;
        element = null;
    }
}
