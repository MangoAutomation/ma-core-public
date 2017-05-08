/*
    Copyright (C) 2006-2007 Serotonin Software Technologies Inc.
 	@author Matthew Lohbihler
 */
package com.serotonin.web.taglib;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspTagException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.tagext.TagSupport;

import org.apache.commons.lang3.StringUtils;

/**
 * @author Matthew Lohbihler
 */
public class RadioTag extends TagSupport {
    private static final long serialVersionUID = -1;

    private String name;
    private String value;
    private String selectedValue;
    private String styleClass;
    private String style;
    private String onchange;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getOnchange() {
        return onchange;
    }

    public void setOnchange(String onchange) {
        this.onchange = onchange;
    }

    public String getSelectedValue() {
        return selectedValue;
    }

    public void setSelectedValue(String selectedValue) {
        this.selectedValue = selectedValue;
    }

    public String getStyle() {
        return style;
    }

    public void setStyle(String style) {
        this.style = style;
    }

    public String getStyleClass() {
        return styleClass;
    }

    public void setStyleClass(String styleClass) {
        this.styleClass = styleClass;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public void release() {
        super.release();
        name = null;
        value = null;
        selectedValue = null;
        styleClass = null;
        onchange = null;
        style = null;
    }

    @Override
    public int doEndTag() throws JspException {
        try {
            JspWriter out = pageContext.getOut();

            // start building up the tag
            out.print("<input");
            Functions.printAttribute(out, "type", "radio");
            Functions.printAttribute(out, "id", id);
            Functions.printAttribute(out, "name", name);
            Functions.printAttribute(out, "value", value);
            Functions.printAttribute(out, "class", styleClass);
            Functions.printAttribute(out, "onchange", onchange);
            Functions.printAttribute(out, "style", style);
            if (StringUtils.equals(value, selectedValue))
                Functions.printAttribute(out, "checked", "checked");
            out.println("/>");
        }
        catch (Exception ex) {
            throw new JspTagException(ex.getMessage());
        }
        return EVAL_PAGE;
    }
}
