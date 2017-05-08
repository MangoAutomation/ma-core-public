/*
    Copyright (C) 2006-2007 Serotonin Software Technologies Inc.
 	@author Matthew Lohbihler
 */
package com.serotonin.web.taglib;

import java.util.HashMap;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.TagSupport;

/**
 * @author Matthew Lohbihler
 */
public class MapEntryTag extends TagSupport {
    private static final long serialVersionUID = -1;

    private String mapVar;
    private String key;
    private Object value;

    public void setMapVar(String mapVar) {
        this.mapVar = mapVar;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    @Override
    public int doStartTag() throws JspException {
        if (mapVar != null) {
            @SuppressWarnings("unchecked")
            HashMap<String, Object> map = (HashMap<String, Object>) pageContext.getRequest().getAttribute(mapVar);
            map.put(key, value);
        }
        else {
            MapTag mapTag = (MapTag) findAncestorWithClass(this, MapTag.class);
            if (mapTag == null)
                throw new JspException("MapEntry tags must be used within a map tag");
            mapTag.addMapEntry(key, value);
        }

        return EVAL_BODY_INCLUDE;
    }

    @Override
    public void release() {
        super.release();
        mapVar = null;
        key = null;
        value = null;
    }
}
