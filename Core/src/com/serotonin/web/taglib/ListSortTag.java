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

import org.apache.commons.lang3.StringUtils;

import com.serotonin.web.i18n.I18NUtils;

public class ListSortTag extends PaginationUrlTag {
    private static final long serialVersionUID = -1;

    // Fields that need to be provided.
    private String label;
    private String labelKey;
    private String field;

    // Fields that can be defaulted from web.xml.
    private String styleClass;
    private String upImageSrc;
    private String upImageWidth;
    private String upImageHeight;
    private String upImageAlt;
    private String upImageAltKey;
    private String downImageSrc;
    private String downImageWidth;
    private String downImageHeight;
    private String downImageAlt;
    private String downImageAltKey;
    private String imageAlign;

    public void setField(String field) {
        this.field = field;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public void setLabelKey(String labelKey) {
        this.labelKey = labelKey;
    }

    public void setStyleClass(String styleClass) {
        this.styleClass = styleClass;
    }

    public void setDownImageAlt(String downImageAlt) {
        this.downImageAlt = downImageAlt;
    }

    public void setDownImageAltKey(String downImageAltKey) {
        this.downImageAltKey = downImageAltKey;
    }

    public void setDownImageHeight(String downImageHeight) {
        this.downImageHeight = downImageHeight;
    }

    public void setDownImageSrc(String downImageSrc) {
        this.downImageSrc = downImageSrc;
    }

    public void setDownImageWidth(String downImageWidth) {
        this.downImageWidth = downImageWidth;
    }

    public void setUpImageAlt(String upImageAlt) {
        this.upImageAlt = upImageAlt;
    }

    public void setUpImageAltKey(String upImageAltKey) {
        this.upImageAltKey = upImageAltKey;
    }

    public void setUpImageHeight(String upImageHeight) {
        this.upImageHeight = upImageHeight;
    }

    public void setUpImageSrc(String upImageSrc) {
        this.upImageSrc = upImageSrc;
    }

    public void setUpImageWidth(String upImageWidth) {
        this.upImageWidth = upImageWidth;
    }

    public void setImageAlign(String imageAlign) {
        this.imageAlign = imageAlign;
    }

    @Override
    public int doStartTag() throws JspException {
        setDefaults();
        JspWriter out = pageContext.getOut();
        List<String> excludeList = new ArrayList<String>();
        excludeList.add(prefix + "sortField");
        excludeList.add(prefix + "sortDesc");
        addExcludeParams(excludeList);

        try {
            out.write("<a href=\"");
            out.write(getBaseHref(excludeList));
            out.write(prefix);
            out.write("sortField=");
            out.write(field);
            out.write("&amp;");
            out.write(prefix);
            out.write("sortDesc=");
            if (field != null && field.equals(paging.getSortField()))
                out.write(Boolean.toString(!paging.getSortDesc()));
            else
                out.write(Boolean.toString(false));
            out.write("\"");

            Functions.printAttribute(out, "class", styleClass);

            out.write(">");
            if (StringUtils.isBlank(labelKey))
                out.write(label);
            else
                out.write(I18NUtils.getMessage(pageContext, labelKey));
            out.write("</a>&nbsp;");

            if (field != null && field.equals(paging.getSortField())) {
                out.write("<img");
                if (paging.getSortDesc()) {
                    Functions.printAttribute(out, "src", downImageSrc);
                    Functions.printAttribute(out, "width", downImageWidth);
                    Functions.printAttribute(out, "height", downImageHeight);
                    if (downImageAltKey != null)
                        Functions.printAttribute(out, "alt", I18NUtils.getMessage(pageContext, downImageAltKey));
                    else
                        Functions.printAttribute(out, "alt", downImageAlt);
                }
                else {
                    Functions.printAttribute(out, "src", upImageSrc);
                    Functions.printAttribute(out, "width", upImageWidth);
                    Functions.printAttribute(out, "height", upImageHeight);
                    if (upImageAltKey != null)
                        Functions.printAttribute(out, "alt", I18NUtils.getMessage(pageContext, upImageAltKey));
                    else
                        Functions.printAttribute(out, "alt", upImageAlt);
                }
                Functions.printAttribute(out, "align", imageAlign);
                Functions.printAttribute(out, "border", "0");
                out.write("/>");
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
        label = null;
        labelKey = null;
        field = null;
        styleClass = null;
        upImageSrc = null;
        upImageWidth = null;
        upImageHeight = null;
        upImageAlt = null;
        downImageSrc = null;
        downImageWidth = null;
        downImageHeight = null;
        downImageAlt = null;
        imageAlign = null;
    }

    private void setDefaults() {
        ServletContext ctx = pageContext.getServletContext();
        if (styleClass == null)
            styleClass = ctx.getInitParameter("com.serotonin.web.taglib.ListSortTag.styleClass");
        if (upImageSrc == null)
            upImageSrc = ctx.getInitParameter("com.serotonin.web.taglib.ListSortTag.upImage.src");
        if (upImageWidth == null)
            upImageWidth = ctx.getInitParameter("com.serotonin.web.taglib.ListSortTag.upImage.width");
        if (upImageHeight == null)
            upImageHeight = ctx.getInitParameter("com.serotonin.web.taglib.ListSortTag.upImage.height");
        if (upImageAlt == null)
            upImageAlt = ctx.getInitParameter("com.serotonin.web.taglib.ListSortTag.upImage.alt");
        if (upImageAltKey == null)
            upImageAltKey = ctx.getInitParameter("com.serotonin.web.taglib.ListSortTag.upImage.altKey");
        if (downImageSrc == null)
            downImageSrc = ctx.getInitParameter("com.serotonin.web.taglib.ListSortTag.downImage.src");
        if (downImageWidth == null)
            downImageWidth = ctx.getInitParameter("com.serotonin.web.taglib.ListSortTag.downImage.width");
        if (downImageHeight == null)
            downImageHeight = ctx.getInitParameter("com.serotonin.web.taglib.ListSortTag.downImage.height");
        if (downImageAlt == null)
            downImageAlt = ctx.getInitParameter("com.serotonin.web.taglib.ListSortTag.downImage.alt");
        if (downImageAltKey == null)
            downImageAltKey = ctx.getInitParameter("com.serotonin.web.taglib.ListSortTag.downImage.altKey");
        if (imageAlign == null)
            imageAlign = ctx.getInitParameter("com.serotonin.web.taglib.ListSortTag.imageAlign");
    }
}
