package com.serotonin.web.taglib;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.tagext.TagSupport;

public class BrowserTypeTag extends TagSupport {
    private static final long serialVersionUID = -1;
    
    private String prefix;
    private String suffix;
    
    /**
     * @param end The end to set.
     */
    public void setSuffix(String suffix) {
        this.suffix = suffix;
    }
    /**
     * @param start The start to set.
     */
    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }
    
    
    public int doStartTag() throws JspException {
        
        String userAgent = ((HttpServletRequest)pageContext.getRequest()).getHeader("user-agent");
        String abbr;
        if (userAgent == null || userAgent.trim().length() == 0)
            abbr = null;
        else if (userAgent.contains("MSIE"))
            abbr = "ie";
        else if (userAgent.contains("Firefox"))
            abbr = "ff";
        else if (userAgent.contains("Netscape"))
            abbr = "ns";
        else
            abbr = null;
        
        if (abbr != null) {
            JspWriter out = pageContext.getOut();
            try {
                if (prefix != null)
                    out.write(prefix);
                out.write(abbr);
                if (suffix != null)
                    out.write(suffix);
            }
            catch (IOException e) {
                throw new JspException("Error writing tag content", e);
            }
        }
        
        return EVAL_BODY_INCLUDE;
    }
}
