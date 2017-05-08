/*
 * Created on 3-Aug-2006
 */
package com.serotonin.web.taglib;

import java.io.IOException;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.TagSupport;

import com.serotonin.web.i18n.I18NUtils;
import com.serotonin.web.util.PagingDataForm;

public class PageNumberTag extends TagSupport {
    private static final long serialVersionUID = -1;

    private PagingDataForm paging;
    private boolean rows = true;
    private String pageLabelKey;
    private String ofLabelKey;
    private String rowsLabelKey;
    private String noRowsLabelKey;
    
    public void setRows(boolean rows) {
        this.rows = rows;
    }
    public void setPaging(PagingDataForm paging) {
        this.paging = paging;
    }
    public void setPageLabelKey(String pageLabelKey) {
        this.pageLabelKey = pageLabelKey;
    }
    public void setOfLabelKey(String ofLabelKey) {
        this.ofLabelKey = ofLabelKey;
    }
    public void setRowsLabelKey(String rowsLabelKey) {
        this.rowsLabelKey = rowsLabelKey;
    }
    public void setNoRowsLabelKey(String noRowsLabelKey) {
        this.noRowsLabelKey = noRowsLabelKey;
    }
    
    @Override
    public int doStartTag() throws JspException {
        try {
            if (paging.getNumberOfItems() > 0) {
                I18NUtils.writeLabel(pageContext, pageLabelKey, "Page");
                pageContext.getOut().write(" ");
                pageContext.getOut().write(Integer.toString(paging.getPage() + 1));
                pageContext.getOut().write(" ");
                I18NUtils.writeLabel(pageContext, ofLabelKey, "of");
                pageContext.getOut().write(" ");
                pageContext.getOut().write(Integer.toString(paging.getNumberOfPages()));
                if (rows) {
                    pageContext.getOut().write(" (");
                    pageContext.getOut().write(Integer.toString(paging.getOffset() + 1));
                    pageContext.getOut().write(" - ");
                    if (paging.getNumberOfItems() < paging.getOffset() + paging.getItemsPerPage())
                        pageContext.getOut().write(Integer.toString(paging.getNumberOfItems()));
                    else
                        pageContext.getOut().write(Integer.toString(paging.getOffset() + paging.getItemsPerPage()));
                    pageContext.getOut().write(" ");
                    I18NUtils.writeLabel(pageContext, ofLabelKey, "of");
                    pageContext.getOut().write(" ");
                    pageContext.getOut().write(Integer.toString(paging.getNumberOfItems()));
                    pageContext.getOut().write(" ");
                    I18NUtils.writeLabel(pageContext, rowsLabelKey, "rows");
                    pageContext.getOut().write(")");
                }
                pageContext.getOut().flush();
            }
            else if (rows)
                I18NUtils.writeLabel(pageContext, noRowsLabelKey, "No rows");
        }
        catch (IOException e) {
            throw new JspException("Error writing page info", e);
        }
        
        return SKIP_BODY;
    }
    
    @Override
    public void release() {
        super.release();
        paging = null;
        rows = true;
    }
}
