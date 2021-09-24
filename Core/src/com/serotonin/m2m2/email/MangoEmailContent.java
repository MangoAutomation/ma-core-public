/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.email;

import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.dao.SystemSettingsDao;
import com.serotonin.m2m2.i18n.Translations;
import com.serotonin.m2m2.util.ExportCodes;
import com.serotonin.web.mail.TemplateEmailContent;

import freemarker.template.Template;
import freemarker.template.TemplateException;

public class MangoEmailContent extends TemplateEmailContent {
    public static final int CONTENT_TYPE_BOTH = 0;
    public static final int CONTENT_TYPE_HTML = 1;
    public static final int CONTENT_TYPE_TEXT = 2;

    public static ExportCodes CONTENT_TYPE_CODES = new ExportCodes();
    static {
        CONTENT_TYPE_CODES.addElement(CONTENT_TYPE_BOTH, "HTML_AND_TEXT");
        CONTENT_TYPE_CODES.addElement(CONTENT_TYPE_HTML, "HTML");
        CONTENT_TYPE_CODES.addElement(CONTENT_TYPE_TEXT, "TEXT");
    }

    private final String defaultSubject;
    private final SubjectDirective subjectDirective;

    public MangoEmailContent(String handlerXid, String rawTemplate, Map<String, Object> model, Translations translations,
            String defaultSubject) throws TemplateException, IOException {
        super(StandardCharsets.UTF_8);

        int type = SystemSettingsDao.getInstance().getIntValue(SystemSettingsDao.EMAIL_CONTENT_TYPE);

        this.defaultSubject = defaultSubject;
        this.subjectDirective = new SubjectDirective(translations);

        model.put("fmt", new MessageFormatDirective(translations));
        model.put("subject", subjectDirective);

        Template template = new Template(handlerXid, new StringReader(rawTemplate), Common.freemarkerConfiguration);

        if (type == CONTENT_TYPE_HTML || type == CONTENT_TYPE_BOTH)
            setHtmlTemplate(template, model);

        if (type == CONTENT_TYPE_TEXT || type == CONTENT_TYPE_BOTH)
            setPlainTemplate(template, model);
    }

    public MangoEmailContent(String templateName, Map<String, Object> model, Translations translations,
            String defaultSubject, Charset encoding) throws TemplateException, IOException {
        super(encoding);

        int type = SystemSettingsDao.getInstance().getIntValue(SystemSettingsDao.EMAIL_CONTENT_TYPE);

        this.defaultSubject = defaultSubject;
        this.subjectDirective = new SubjectDirective(translations);

        model.put("fmt", new MessageFormatDirective(translations));
        model.put("subject", subjectDirective);

        if (type == CONTENT_TYPE_HTML || type == CONTENT_TYPE_BOTH)
            setHtmlTemplate(getTemplate(templateName, true), model);

        if (type == CONTENT_TYPE_TEXT || type == CONTENT_TYPE_BOTH)
            setPlainTemplate(getTemplate(templateName, false), model);
    }

    public String getSubject() {
        String subject = subjectDirective.getSubject();
        if (subject == null)
            return defaultSubject;
        return subject;
    }

    private Template getTemplate(String name, boolean html) throws IOException {
        if (html)
            name = "html/" + name + ".ftl";
        else
            name = "text/" + name + ".ftl";

        return Common.freemarkerConfiguration.getTemplate(name);
    }
}
