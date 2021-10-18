/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.email;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

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
 * @author Jared Wiltshire
 */
public class UsedImagesDirective implements TemplateDirectiveModel {

    /**
     * Prevent repeated Files.exists() calls for the same string.
     */
    private final Map<String, Path> pathCache = new HashMap<>();

    /**
     * Map of image path to content-ID (cid)
     */
    private final Map<Path, UUID> imageList = new HashMap<>();

    public Map<Path, UUID> getImageList() {
        return imageList;
    }

    @Override
    public void execute(Environment env, @SuppressWarnings("rawtypes") Map params, TemplateModel[] loopVars,
            TemplateDirectiveBody body) throws TemplateException, IOException {

        Object logo = params.get("logo");
        Object src = params.get("src");

        Path imagePath;
        if (logo instanceof TemplateScalarModel && Boolean.parseBoolean(((TemplateScalarModel) logo).getAsString())) {
            imagePath = getImagePath("logo.png");
        } else if (src instanceof TemplateScalarModel) {
            imagePath = getImagePath(((TemplateScalarModel) src).getAsString());
        } else {
            throw new TemplateModelException("Must supply logo=\"true\" or a valid image path via src=\"\"");
        }

        UUID contentId = imageList.computeIfAbsent(imagePath, p -> UUID.randomUUID());
        env.getOut().write(contentId.toString());
    }

    private Path getImagePath(String templatePath) {
        return pathCache.computeIfAbsent(templatePath, str -> {
            Path overridePath = Common.OVERRIDES_WEB.resolve("images").resolve(str).normalize();
            return Files.exists(overridePath) ? overridePath : Common.WEB.resolve("images").resolve(str).normalize();
        });
    }
}
