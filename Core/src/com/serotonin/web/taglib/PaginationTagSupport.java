/*
 * Created on 3-Aug-2006
 */
package com.serotonin.web.taglib;

import java.util.Enumeration;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.tagext.TagSupport;

import com.serotonin.web.util.PagingDataForm;

public class PaginationTagSupport extends TagSupport {
    private static final long serialVersionUID = -1;
    
    protected PagingDataForm paging;
    protected String prefix = "";
    
    /**
     * @param paging The paging to set.
     */
    public void setPaging(PagingDataForm paging) {
        this.paging = paging;
    }
    /**
     * @param prefix The prefix to set.
     */
    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }
    
    protected String getBaseHref(List<String> excludeParamNames) {
        HttpServletRequest request = (HttpServletRequest) pageContext.getRequest();
        
        StringBuffer baseHref = new StringBuffer();
        baseHref.append(request.getAttribute("javax.servlet.forward.request_uri"));
        
        boolean first = true;
        Enumeration paramNames = pageContext.getRequest().getParameterNames();
        String name;
        while (paramNames.hasMoreElements()) {
            name = (String)paramNames.nextElement();
            if (!excludeParamNames.contains(name)) {
                if (first) {
                    baseHref.append('?');
                    first = false;
                }
                else
                    baseHref.append("&amp;");
                
                // Take values from the paging object where possible.
                if (paging != null) {
                    if ((prefix+"page").equals(name))
                        baseHref.append(name).append('=').append(Integer.toString(paging.getPage()));
                    else if ((prefix+"numberOfPages").equals(name))
                        baseHref.append(name).append('=').append(Integer.toString(paging.getNumberOfPages()));
                    else if ((prefix+"offset").equals(name))
                        baseHref.append(name).append('=').append(Integer.toString(paging.getOffset()));
                    else if ((prefix+"itemsPerPage").equals(name))
                        baseHref.append(name).append('=').append(Integer.toString(paging.getItemsPerPage()));
                    else if ((prefix+"sortField").equals(name))
                        baseHref.append(name).append('=').append(paging.getSortField());
                    else if ((prefix+"sortDesc").equals(name))
                        baseHref.append(name).append('=').append(Boolean.toString(paging.getSortDesc()));
                    else
                        baseHref.append(name).append('=').append(request.getParameter(name));
                }
                else
                    baseHref.append(name).append('=').append(request.getParameter(name));
            }
        }
        
        // In the anticipation of more parameters, add the correct delimter.
        if (first)
            baseHref.append('?');
        else
            baseHref.append("&amp;");
        
        return baseHref.toString();
    }
    
    public void release() {
        super.release();
        paging = null;
        prefix = "";
    }
}
