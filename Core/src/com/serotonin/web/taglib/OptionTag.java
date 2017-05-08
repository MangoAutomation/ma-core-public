/*
 * Created on 26-Jul-2006
 */
package com.serotonin.web.taglib;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspTagException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.tagext.BodyTagSupport;

public class OptionTag extends BodyTagSupport {
    private static final long serialVersionUID = -1;

    private String value; // value of option

    /**
     * @return Returns the value.
     */
    public String getValue() {
        return value;
    }

    /**
     * @param value
     *            The value to set.
     */
    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public int doStartTag() {
        // It seems to be necessary to clear the body as release doesn't do
        // this?
        if (getBodyContent() != null) {
            getBodyContent().clearBody();
        }
        return EVAL_BODY_BUFFERED;
    }

    @Override
    public int doEndTag() throws JspException {
        try {
            String content = getBodyContent() != null ? getBodyContent().getString() : null;

            // get what we need from the page
            JspWriter out = pageContext.getOut();

            // start building up the tag
            out.print("<option");
            Functions.printAttribute(out, "value", value);

            // Find the select parent, and check if this is a selected value.
            SelectTag selectTag = (SelectTag) findAncestorWithClass(this, SelectTag.class);
            if (selectTag != null) {
                if (value != null) {
                    if (value.equals(selectTag.getValue()))
                        out.print(" selected=\"selected\"");
                }
                else if (content != null) {
                    if (content.equals(selectTag.getValue()))
                        out.print(" selected=\"selected\"");
                }
            }

            out.print(">");

            if (content != null)
                out.print(content);

            out.print("</option>");
        }
        catch (Exception ex) {
            throw new JspTagException(ex.getMessage());
        }
        return EVAL_PAGE;
    }

    @Override
    public void release() {
        super.release();
        value = null;
    }
}
