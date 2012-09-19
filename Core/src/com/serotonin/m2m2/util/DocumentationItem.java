/*
    Copyright (C) 2006-2011 Serotonin Software Technologies Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.util;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Matthew Lohbihler
 */
public class DocumentationItem {
    private final String id;
    private final String key;
    private final String basePath;
    private final String filename;
    private final List<String> related = new ArrayList<String>();

    public DocumentationItem(String id, String key, String basePath, String filename) {
        this.id = id;
        this.key = key;
        this.basePath = basePath;
        this.filename = filename;
    }

    public void addRelated(String id) {
        related.add(id);
    }

    public String getId() {
        return id;
    }

    public String getKey() {
        return key;
    }

    public String getBasePath() {
        return basePath;
    }

    public String getFilename() {
        return filename;
    }

    public List<String> getRelated() {
        return related;
    }
}
