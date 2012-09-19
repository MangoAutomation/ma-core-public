/*
    Copyright (C) 2006-2011 Serotonin Software Technologies Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.serotonin.m2m2.Common;
import com.serotonin.util.XmlUtilsTS;

/**
 * @author Matthew Lohbihler
 */
public class DocumentationManifest {
    private static final Log LOG = LogFactory.getLog(DocumentationManifest.class);
    private final Map<String, DocumentationItem> items = new HashMap<String, DocumentationItem>();

    public void parseManifestFile(String directory) throws Exception {
        // Read the documentation manifest file.
        File manifest = new File(Common.M2M2_HOME + "/" + directory, "manifest.xml");

        try {
            Document document = XmlUtilsTS.parse(manifest);

            Element root = document.getDocumentElement();
            for (Element item : XmlUtilsTS.getChildElements(root, "item")) {
                DocumentationItem di = new DocumentationItem(item.getAttribute("id"), item.getAttribute("key"),
                        directory, item.getAttribute("filename"));

                for (Element relation : XmlUtilsTS.getChildElements(item, "relation"))
                    di.addRelated(relation.getAttribute("id"));

                items.put(di.getId(), di);
            }
        }
        catch (FileNotFoundException e) {
            LOG.error("Documentation manifest file not found: " + manifest.getPath());
        }
    }

    public DocumentationItem getItem(String id) {
        return items.get(id);
    }
}
