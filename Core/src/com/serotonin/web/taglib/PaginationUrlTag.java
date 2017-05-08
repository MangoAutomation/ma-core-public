/*
 * Created on 3-Aug-2006
 */
package com.serotonin.web.taglib;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.jsp.JspException;

public class PaginationUrlTag extends PaginationTagSupport {
    private static final long serialVersionUID = -1;
    
    private String excludeParams;
    
    /**
     * @param excludeParams The excludeParams to set.
     */
    public void setExcludeParams(String excludeParams) {
        this.excludeParams = excludeParams;
    }
    
    public int doStartTag() throws JspException {
        List<String> exclude = new ArrayList<String>();
        if (excludeParams != null)
            addExcludeParams(exclude);
        
        try {
            pageContext.getOut().write(getBaseHref(exclude));
        }
        catch (IOException e) {
            throw new JspException("Error writing page info", e);
        }
        
        return SKIP_BODY;
    }
    
    protected void addExcludeParams(List<String> excludeList) {
        if (excludeParams != null) {
            String[] bits = excludeParams.split(",");
            for (int i=0; i<bits.length; i++)
                excludeList.add(bits[i]);
        }
    }
    
    public void release() {
        super.release();
        excludeParams = null;
    }
}
