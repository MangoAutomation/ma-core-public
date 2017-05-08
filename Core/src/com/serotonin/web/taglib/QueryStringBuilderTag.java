package com.serotonin.web.taglib;

import javax.servlet.jsp.tagext.TagSupport;

import org.apache.commons.lang3.StringUtils;

public class QueryStringBuilderTag extends TagSupport {
    private static final long serialVersionUID = -1;

    private String var;
    private String key;
    private String value;
    private boolean omitIfEmpty;

    public void setVar(String var) {
        this.var = var;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public void setOmitIfEmpty(boolean omitIfEmpty) {
        this.omitIfEmpty = omitIfEmpty;
    }

    @Override
    public int doEndTag() {
        String queryString = (String) pageContext.getRequest().getAttribute(var);

        if (!omitIfEmpty || !StringUtils.isBlank(value)) {
            StringBuilder sb = new StringBuilder();
            if (queryString != null)
                sb.append(queryString);

            if (StringUtils.isBlank(queryString))
                sb.append('?');
            else
                sb.append('&');
            sb.append(key).append('=').append(value);

            queryString = sb.toString();
        }

        if (queryString != null)
            pageContext.getRequest().setAttribute(var, queryString);

        return EVAL_PAGE;
    }

    @Override
    public void release() {
        super.release();
        var = null;
        key = null;
        value = null;
        omitIfEmpty = false;
    }
}
