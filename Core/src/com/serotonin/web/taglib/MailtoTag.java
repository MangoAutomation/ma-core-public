package com.serotonin.web.taglib;

import java.io.IOException;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.tagext.TagSupport;

public class MailtoTag extends TagSupport {
    private static final long serialVersionUID = -1;
    
    private String styleClass;
    private String email;
    private String subject;
    
    /**
     * @param email The email to set.
     */
    public void setEmail(String email) {
        this.email = email;
    }
    /**
     * @param styleClass The styleClass to set.
     */
    public void setStyleClass(String styleClass) {
        this.styleClass = styleClass;
    }
    /**
     * @param subject The subject to set.
     */
    public void setSubject(String subject) {
        this.subject = subject;
    }
    
    public int doStartTag() throws JspException {
        String clazz = styleClass;
        if (clazz == null)
            clazz = "linkRed";
        
        String encodedEmail = encodeAddress(email);
        String address = "mailto:"+ encodedEmail;
        if (subject != null)
            address += "?subject="+ subject;
        try {
            JspWriter out = pageContext.getOut();
            out.write("<a");
            Functions.printAttribute(out, "class", clazz);
            Functions.printAttribute(out, "href", address);
            out.write(">");
            out.write(encodedEmail);
            out.write("</a>");
        }
        catch (IOException e) {
            throw new JspException("Error while writing online help tag", e);
        }
        
        return SKIP_BODY;
    }

    private static String encodeAddress(String addr) {
        StringBuilder sb = new StringBuilder();
        for (int i=0; i<addr.length(); i++)
            sb.append("&#").append((int)addr.charAt(i)).append(";");
        return sb.toString();
    }
}
