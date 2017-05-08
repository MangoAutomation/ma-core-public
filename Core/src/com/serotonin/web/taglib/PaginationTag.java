/*
 * Created on 3-Aug-2006
 */
package com.serotonin.web.taglib;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletContext;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspWriter;

import com.serotonin.web.i18n.I18NUtils;

public class PaginationTag extends PaginationUrlTag {
    private static final long serialVersionUID = -1;

    private String delimeter = "&nbsp;";
    private boolean indices = true;
    private String previousLabelKey;
    private String nextLabelKey;
    
    // Fields that can be defaulted.
    private String styleClass;
    
    public void setDelimeter(String delimeter) {
        this.delimeter = delimeter;
    }
    public void setStyleClass(String styleClass) {
        this.styleClass = styleClass;
    }
    public void setIndices(boolean indices) {
        this.indices = indices;
    }
    public void setPreviousLabelKey(String previousLabelKey) {
        this.previousLabelKey = previousLabelKey;
    }
    public void setNextLabelKey(String nextLabelKey) {
        this.nextLabelKey = nextLabelKey;
    }
    
    @Override
    public int doStartTag() throws JspException {
        setDefaults();
        JspWriter out = pageContext.getOut();
        
        List<String> excludeList = new ArrayList<String>();
        excludeList.add(prefix +"page");
        addExcludeParams(excludeList);
        
        String baseHref = getBaseHref(excludeList);
        try {
            boolean prev = paging.getPage() > 0;
            boolean next = paging.getNumberOfPages() > paging.getPage() + 1;
            
            if (prev) {
                out.write("<a");
                Functions.printAttribute(out, "href",
                        baseHref + prefix +"page=" + Integer.toString(paging.getPage() - 1));
                Functions.printAttribute(out, "class", styleClass);
                out.write(">");
                I18NUtils.writeLabel(pageContext, previousLabelKey, "Previous");
                out.write("</a>");
            }
            
            if (indices && paging.getNumberOfItems() > 0) {
                if (prev)
                    out.write(delimeter);
                
                int start = paging.getPage() - 5;
                if (start < 0)
                    start = 0;
                int end = paging.getPage() + 5;
                if (end >= paging.getNumberOfPages())
                    end = paging.getNumberOfPages() - 1;
                
                for (int i=start; i<=end; i++) {
                    if (i > start)
                        out.write(delimeter);
                    
                    if (i == paging.getPage()) {
                        out.write("<strong>");
                        out.write(Integer.toString(i+1));
                        out.write("</strong>");
                    }
                    else {
                        out.write("<a");
                        Functions.printAttribute(out, "href", baseHref + prefix +"page=" + Integer.toString(i));
                        Functions.printAttribute(out, "class", styleClass);
                        out.write(">");
                        out.write(Integer.toString(i+1));
                        out.write("</a>");
                    }
                }
                
                if (next)
                    out.write(delimeter);
            }
            else {
                if (prev && next)
                    out.write(delimeter);
            }
            
            if (next) {
                out.write("<a");
                Functions.printAttribute(out, "href",
                        baseHref + prefix +"page=" + Integer.toString(paging.getPage() + 1));
                Functions.printAttribute(out, "class", styleClass);
                out.write(">");
                I18NUtils.writeLabel(pageContext, nextLabelKey, "Next");
                out.write("</a>");
            }
            
            out.flush();
        }
        catch (IOException e) {
            throw new JspException("Error writing page info", e);
        }
        
        return SKIP_BODY;
    }
    
    @Override
    public void release() {
        super.release();
        delimeter = "&nbsp;";
        indices = true;
        styleClass = null;
    }
    
    private void setDefaults() {
        ServletContext ctx = pageContext.getServletContext();
        if (styleClass == null)
            styleClass = ctx.getInitParameter("com.serotonin.web.taglib.PaginationTag.styleClass");
    }
}
