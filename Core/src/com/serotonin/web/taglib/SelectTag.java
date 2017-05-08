/*
 * Created on 26-Jul-2006
 */
package com.serotonin.web.taglib;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspTagException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.tagext.TagSupport;

public class SelectTag extends TagSupport {
    private static final long serialVersionUID = -1;

    private String id;
    private String name; // name of the select element
    private String value; // default value if none is found
    private String size; // select size
    private String styleClass;
    private String onchange;
    private String onclick;
    private String onblur;
    private String onmouseover;
    private String onmouseout;
    private String style;

    /**
     * @return Returns the id.
     */
    @Override
    public String getId() {
        return id;
    }

    /**
     * @param id The id to set.
     */
    @Override
    public void setId(String id) {
        this.id = id;
    }

    /**
     * @return Returns the name.
     */
    public String getName() {
        return name;
    }

    /**
     * @param name The name to set.
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return Returns the value.
     */
    public String getValue() {
        return value;
    }

    /**
     * @param value The value to set.
     */
    public void setValue(String value) {
        this.value = value;
    }

    /**
     * @return Returns the size.
     */
    public String getSize() {
        return size;
    }

    /**
     * @param size The size to set.
     */
    public void setSize(String size) {
        this.size = size;
    }

    /**
     * @return Returns the onchange.
     */
    public String getOnchange() {
        return onchange;
    }

    /**
     * @param onchange The onchange to set.
     */
    public void setOnchange(String onchange) {
        this.onchange = onchange;
    }
    public String getOnblur() {
        return onblur;
    }
    public void setOnblur(String onblur) {
        this.onblur = onblur;
    }
    public String getOnclick() {
        return onclick;
    }
    public void setOnclick(String onclick) {
        this.onclick = onclick;
    }
    public String getOnmouseover() {
        return onmouseover;
    }
    public void setOnmouseover(String onmouseover) {
        this.onmouseover = onmouseover;
    }
    public String getOnmouseout() {
        return onmouseout;
    }
    public void setOnmouseout(String onmouseout) {
        this.onmouseout = onmouseout;
    }

    /**
     * @return Returns the styleClass.
     */
    public String getStyleClass() {
        return styleClass;
    }

    /**
     * @param styleClass The styleClass to set.
     */
    public void setStyleClass(String styleClass) {
        this.styleClass = styleClass;
    }

    public String getStyle() {
        return style;
    }

    public void setStyle(String style) {
        this.style = style;
    }

    @Override
    public void release() {
        super.release();
        id = null;
        name = null;
        value = null;
        size = null;
        styleClass = null;
        onchange = null;
        onclick = null;
        onblur = null;
        onmouseover = null;
        onmouseout = null;
        style = null;
    }

    @Override
    public int doStartTag() throws JspException {
        try {
            JspWriter out = pageContext.getOut();

            // start building up the tag
            out.print("<select");
            Functions.printAttribute(out, "id", id);
            Functions.printAttribute(out, "name", name);
            Functions.printAttribute(out, "size", size);
            Functions.printAttribute(out, "class", styleClass);
            Functions.printAttribute(out, "onchange", onchange);
            Functions.printAttribute(out, "onclick", onclick);
            Functions.printAttribute(out, "onblur", onblur);
            Functions.printAttribute(out, "onmouseover", onmouseover);
            Functions.printAttribute(out, "onmouseout", onmouseout);
            Functions.printAttribute(out, "style", style);
            out.println(">");
        }
        catch (Exception ex) {
            throw new JspTagException(ex.getMessage());
        }
        return EVAL_BODY_INCLUDE;
    }

    @Override
    public int doEndTag() throws JspException {
        try {
            JspWriter out = pageContext.getOut();
            out.print("</select>");
        }
        catch (Exception ex) {
            throw new JspTagException(ex.getMessage());
        }
        return EVAL_PAGE;
    }
}
