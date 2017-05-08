/*
    Copyright (C) 2006-2007 Serotonin Software Technologies Inc.
 	@author Matthew Lohbihler
 */
package com.serotonin.web.taglib;

import java.util.List;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.TagSupport;

import org.apache.commons.lang3.StringUtils;

/**
 * @author Matthew Lohbihler
 */
public class ListEntryTag extends TagSupport {
    private static final long serialVersionUID = -1;

    private Object value;
    private String listVar;

    public void setValue(Object value) {
        this.value = value;
    }

    public void setListVar(String listVar) {
        this.listVar = listVar;
    }

    @SuppressWarnings("unchecked")
    @Override
    public int doStartTag() throws JspException {
        if (StringUtils.isBlank(listVar)) {
            ListTag listTag = (ListTag) findAncestorWithClass(this, ListTag.class);
            if (listTag == null)
                throw new JspException("ListEntry tags must be used within a list tag");
            listTag.addListEntry(value);
        }
        else {
            List<Object> list = (List<Object>) pageContext.getRequest().getAttribute(listVar);
            if (list == null)
                throw new JspException("A list with the var name '" + listVar + "' was not found");
            list.add(value);
        }

        return EVAL_BODY_INCLUDE;
    }

    @Override
    public void release() {
        super.release();
        value = null;
    }
}
