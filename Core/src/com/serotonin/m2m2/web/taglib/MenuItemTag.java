/*
    Copyright (C) 2006-2011 Serotonin Software Technologies Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.web.taglib;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.TagSupport;

import com.serotonin.m2m2.module.MenuItemDefinition;
import com.serotonin.m2m2.web.mvc.controller.ControllerUtils;

public class MenuItemTag extends TagSupport {
    private static final long serialVersionUID = 1L;

    private MenuItemDefinition def;
    private String href;
    private String png;
    private String src;
    private String key;

    public void setDef(MenuItemDefinition def) {
        this.def = def;
    }

    public void setHref(String href) {
        this.href = href;
    }

    public void setPng(String png) {
        this.png = png;
    }

    public void setSrc(String src) {
        this.src = src;
    }

    public void setKey(String key) {
        this.key = key;
    }

    @Override
    public int doEndTag() throws JspException {
        HttpServletRequest request = (HttpServletRequest) pageContext.getRequest();
        HttpServletResponse response = (HttpServletResponse) pageContext.getResponse();

        if (def == null || def.isVisible(request, response)) {
            String _href;
            String _onclick;
            String _target;
            String _key;
            String _id;
            String _src;

            if (def != null) {
                _href = def.getHref(request, response);
                _onclick = def.getOnclick(request, response);
                _target = def.getTarget(request, response);
                _key = def.getTextKey(request, response);
                _id = def.getId(request, response);
                _src = def.getImagePath(request, response);
            }
            else {
                _href = href;
                _onclick = null;
                _target = null;
                _key = key;
                _id = null;
                _src = src;
                if (png != null)
                    _src = "/images/" + png + ".png";
            }

            String text = ControllerUtils.getTranslations(pageContext).translate(_key);
            text = com.serotonin.web.taglib.Functions.quotEncode(text);

            StringBuilder sb = new StringBuilder();

            sb.append("<a");
            quote(sb, "href", _href);
            quote(sb, "onclick", _onclick);
            quote(sb, "target", _target);
            sb.append("><img");
            quote(sb, "id", _id);
            quote(sb, "src", _src);
            quote(sb, "class", "ptr");
            quote(sb, "onmouseout", "if (typeof hMD == 'function') hMD();");
            quote(sb, "onmouseover", "if (typeof hMD == 'function') hMD('" + text + "', this);");
            sb.append("/></a>");

            try {
                pageContext.getOut().write(sb.toString());
            }
            catch (IOException e) {
                throw new JspException(e);
            }
        }

        return EVAL_PAGE;
    }

    private void quote(StringBuilder sb, String name, String value) {
        if (value != null)
            sb.append(" ").append(name).append("=\"").append(value).append('"');
    }
}
