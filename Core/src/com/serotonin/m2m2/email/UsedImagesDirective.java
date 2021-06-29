/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.email;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.serotonin.m2m2.Common;

import freemarker.core.Environment;
import freemarker.template.TemplateDirectiveBody;
import freemarker.template.TemplateDirectiveModel;
import freemarker.template.TemplateException;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateScalarModel;

/**
 * @author Matthew Lohbihler
 */
public class UsedImagesDirective implements TemplateDirectiveModel {
    private final List<String> imageList = new ArrayList<String>();

    public List<String> getImageList() {
        return imageList;
    }

    @Override
    public void execute(Environment env, @SuppressWarnings("rawtypes") Map params, TemplateModel[] loopVars,
            TemplateDirectiveBody body) throws TemplateException, IOException {
        boolean writeLogo = false;

        TemplateModel logo = (TemplateModel) params.get("logo");
        if (logo instanceof TemplateScalarModel) {
            String s = ((TemplateScalarModel) logo).getAsString();
            if (Boolean.parseBoolean(s)) {
                writeLogo = true;

                if (!imageList.contains(Common.APPLICATION_LOGO))
                    imageList.add(Common.APPLICATION_LOGO);
                env.getOut().write(Common.APPLICATION_LOGO);
            }
        }

        if (!writeLogo) {
            TemplateModel src = (TemplateModel) params.get("src");

            if (src instanceof TemplateScalarModel) {
                String s = "images/" + ((TemplateScalarModel) src).getAsString();
                if (!imageList.contains(s))
                    imageList.add(s);
                env.getOut().write(s);
            }
            else
                throw new TemplateModelException("key must be a string");
        }
    }
}
