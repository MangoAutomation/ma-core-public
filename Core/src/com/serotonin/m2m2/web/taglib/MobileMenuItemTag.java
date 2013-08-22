/**
 * Copyright (C) 2013 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
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

/**
 * @author Terry Packer
 *
 */
public class MobileMenuItemTag extends TagSupport{
    private static final long serialVersionUID = 1L;

    private MenuItemDefinition def;
    private String href;
    private String id;
    private String png;
    private String src;
    private String key;

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
                _onclick = null;
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
                quote(sb, "<li dojoType", "dojox.mobile.IconItem");
                quote(sb,"label",text);
                quote(sb,"id",text + "_icon");
                quote(sb,"icon",_src);
                quote(sb,"href",_href); //Could use url and then load fragments via a controller
                quote(sb,"transition","slide");
                sb.append(">");
                sb.append("</li>");

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

