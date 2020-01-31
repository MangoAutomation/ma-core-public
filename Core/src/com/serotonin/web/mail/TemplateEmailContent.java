package com.serotonin.web.mail;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import freemarker.template.Template;
import freemarker.template.TemplateException;

public class TemplateEmailContent extends EmailContent {
    protected static final Map<Pattern, String> REPLACEMENT_EMPTY_TAG = new HashMap<Pattern, String>();
    static {
        REPLACEMENT_EMPTY_TAG.put(Pattern.compile("<br\\s*/>"), "\r\n");
        REPLACEMENT_EMPTY_TAG.put(Pattern.compile("&nbsp;"), " ");
    }

    public TemplateEmailContent(Charset encoding) {
        this.encoding = encoding;
    }

    public TemplateEmailContent(Template plainTpl, Template htmlTpl, Object model, Charset encoding)
            throws TemplateException, IOException {
        setPlainTemplate(plainTpl, model);
        setHtmlTemplate(htmlTpl, model);
        this.encoding = encoding;
    }

    public void setPlainTemplate(Template plainTpl, Object model) throws TemplateException, IOException {
        if (plainTpl != null) {
            StringWriter plain = new StringWriter();
            plainTpl.process(model, plain);
            plainContent = plain.toString();

            // Replace empty HTML tags
            for (Entry<Pattern, String> entry : REPLACEMENT_EMPTY_TAG.entrySet())
                plainContent = entry.getKey().matcher(plainContent).replaceAll(entry.getValue());
        }
    }

    public void setHtmlTemplate(Template htmlTpl, Object model) throws TemplateException, IOException {
        if (htmlTpl != null) {
            StringWriter html = new StringWriter();
            htmlTpl.process(model, html);
            htmlContent = html.toString();
        }
    }
}
