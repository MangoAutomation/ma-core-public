/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.email;

import java.io.IOException;
import java.util.Map;

import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.i18n.Translations;

import freemarker.core.Environment;
import freemarker.ext.beans.BeanModel;
import freemarker.template.TemplateDirectiveBody;
import freemarker.template.TemplateDirectiveModel;
import freemarker.template.TemplateException;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateScalarModel;

/**
 * @author Matthew Lohbihler
 */
public class MessageFormatDirective implements TemplateDirectiveModel {
    private final Translations translations;

    public MessageFormatDirective(Translations translations) {
        this.translations = translations;
    }

    @Override
    public void execute(Environment env, @SuppressWarnings("rawtypes") Map params, TemplateModel[] loopVars,
            TemplateDirectiveBody body) throws TemplateException, IOException {
        TemplateModel key = (TemplateModel) params.get("key");

        String out;
        if (key == null) {
            // No key. Look for a message.
            BeanModel model = (BeanModel) params.get("message");
            if (model == null) {
                if (params.containsKey("message"))
                    // The parameter is there, but the value is null.
                    out = "";
                else
                    // The parameter wasn't given
                    throw new TemplateModelException("One of key or message must be provided");
            }
            else {
                TranslatableMessage message = (TranslatableMessage) model.getWrappedObject();
                if (message == null)
                    out = "";
                else
                    out = message.translate(translations);
            }
        }
        else {
            if (key instanceof TemplateScalarModel)
                out = translations.translate(((TemplateScalarModel) key).getAsString());
            else
                throw new TemplateModelException("key must be a string");
        }

        env.getOut().write(out);
    }
}
