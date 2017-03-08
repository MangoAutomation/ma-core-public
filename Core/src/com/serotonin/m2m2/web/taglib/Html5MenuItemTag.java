/*
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.web.taglib;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.TagSupport;

import com.serotonin.m2m2.module.MenuItemDefinition;
import com.serotonin.m2m2.module.MenuItemVetoDefinition;
import com.serotonin.m2m2.module.ModuleRegistry;
import com.serotonin.m2m2.web.mvc.controller.ControllerUtils;

public class Html5MenuItemTag extends TagSupport {
    private static final long serialVersionUID = 1L;

    private MenuItemDefinition def;
    private String href;
    private String id;
    private String png;
    private String src;
    private String key;
    private String onclick;

    public void setDef(MenuItemDefinition def) {
        this.def = def;
    }

    @Override
    public void setId(String id) {
        this.id = id;
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

    public void setOnclick(String onclick) {
        this.onclick = onclick;
    }

    @Override
    public int doEndTag() throws JspException {
        HttpServletRequest request = (HttpServletRequest) pageContext.getRequest();
        HttpServletResponse response = (HttpServletResponse) pageContext.getResponse();

        if (def == null || def.isVisible(request, response)) {
            String _id;
            String _href;
            String _onclick;
            String _target;
            String _key;
            String _src;

            if (def != null) {
                _id = def.getId(request, response);
                _href = def.getHref(request, response);
                _onclick = def.getOnclick(request, response);
                _target = def.getTarget(request, response);
                _key = def.getTextKey(request, response);
                _src = def.getImagePath(request, response);
            }
            else {
                _id = id;
                _href = href;
                _onclick = onclick;
                _target = null;
                _key = key;
                _src = src;
                if (png != null)
                    _src = "/images/" + png + ".png";
            }

            // Check for veto of the display of the menu item.
            boolean visible = true;
            for (MenuItemVetoDefinition veto : ModuleRegistry.getDefinitions(MenuItemVetoDefinition.class)) {
                if (!veto.isVisible(_id, request, response)) {
                    visible = false;
                    break;
                }
            }

            if (visible) {
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
                quote(sb, "alt",  text);
                quote(sb, "title", text);
                sb.append("/></a>");

                try {
                    pageContext.getOut().write(sb.toString());
                }
                catch (IOException e) {
                    throw new JspException(e);
                }
            }
        }

        return EVAL_PAGE;
    }

    private void quote(StringBuilder sb, String name, String value) {
        if (value != null)
            sb.append(" ").append(name).append("=\"").append(value).append('"');
    }
}
