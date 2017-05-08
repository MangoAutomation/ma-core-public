/*
    Copyright (C) 2006-2007 Serotonin Software Technologies Inc.
 	@author Matthew Lohbihler
 */
package com.serotonin.web.taglib;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.TagSupport;

import org.apache.commons.lang3.StringUtils;

/**
 * @author Matthew Lohbihler
 */
public class MapTag extends TagSupport {
    private static final long serialVersionUID = -1;

    private String var;
    private Map<String, Object> map;

    public void setVar(String var) {
        this.var = var;
    }

    public void addMapEntry(String key, Object value) {
        map.put(key, value);
    }

    public Map<String, Object> getMap() {
        return map;
    }

    @Override
    public int doStartTag() throws JspException {
        map = new HashMap<String, Object>();

        if (StringUtils.isBlank(var)) {
            // If no var was given, assume that this map should be added to an ancestor list.
            ListTag listTag = (ListTag) findAncestorWithClass(this, ListTag.class);
            if (listTag == null)
                throw new JspException("If no 'var' attribute is given, this Map must have a ListEntry tags ancestor");

            listTag.addListEntry(map);
        }
        else
            pageContext.getRequest().setAttribute(var, map);

        return EVAL_BODY_INCLUDE;
    }

    @Override
    public void release() {
        super.release();
        var = null;
        map = null;
    }
}
