/*
    Copyright (C) 2006-2011 Serotonin Software Technologies Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
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

    /**
     * This is the mapping of document ids to document items.
     */
    private final Map<String, DocumentationItem> items = new HashMap<String, DocumentationItem>();

    /**
     * This is the list of directories in which document content can be found.
     */
    private final List<String> directories = new ArrayList<String>();

    public void parseManifestFile(String directory) throws Exception {
        directories.add(directory);

        // Read the documentation manifest file.
        File manifest = new File(Common.MA_HOME + "/" + directory, "manifest.xml");

        try {
            Document document = XmlUtilsTS.parse(manifest);

            Element root = document.getDocumentElement();
            for (Element item : XmlUtilsTS.getChildElements(root, "item")) {
                DocumentationItem di = new DocumentationItem(item.getAttribute("id"), item.getAttribute("key"),
                        item.getAttribute("filename"));

                for (Element relation : XmlUtilsTS.getChildElements(item, "relation"))
                    di.addRelated(relation.getAttribute("id"));

                items.put(di.getId(), di);
            }
        }
        catch (FileNotFoundException e) {
            LOG.info("Documentation manifest file not found: " + manifest.getPath());
        }
    }

    public DocumentationItem getItem(String id) {
        return items.get(id);
    }

    /**
     * This is a recursive function that tried to locate document content files by locale. It should initially be
     * called with as maybe arguments as available. Recursive calls will get more generalized by removing arguments
     * when there is no content match.
     * 
     * @param item
     *            the document item to locate
     * @param language
     *            the 2 character language code, optional
     * @param country
     *            the 2 character country specifier, optional
     * @param variant
     *            the 2 character region variant, optional
     * @return the content file. Will not be null, but the file may not exist.
     */
    public File getDocumentationFile(DocumentationItem item, String language, String country, String variant) {
        StringBuilder sb = new StringBuilder();

        if (!StringUtils.isBlank(language)) {
            sb.append('/').append(language);
            if (!StringUtils.isBlank(country)) {
                sb.append('/').append(country);
                if (!StringUtils.isBlank(variant)) {
                    sb.append('/').append(variant);
                    sb.append('/').append(item.getFilename());
                    File file = findDocumentFile(sb.toString());
                    if (file.exists())
                        return file;
                    return getDocumentationFile(item, language, country, null);
                }

                sb.append('/').append(item.getFilename());
                File file = findDocumentFile(sb.toString());
                if (file.exists())
                    return file;
                return getDocumentationFile(item, language, null, null);
            }

            sb.append('/').append(item.getFilename());
            File file = findDocumentFile(sb.toString());
            if (file.exists())
                return file;
            return getDocumentationFile(item, null, null, null);
        }

        sb.append('/').append(item.getFilename());
        return findDocumentFile(sb.toString());
    }

    /**
     * Searches through the list of content directories for the given file name.
     * 
     * @return a file object. Will not be null, but the file may not exist.
     */
    private File findDocumentFile(String filename) {
        // We traverse the directory list in reverse order so that modules can override the core.
        File file = null;
        for (int i = directories.size() - 1; i >= 0; i--) {
            file = new File(Common.MA_HOME + "/" + directories.get(i), filename);
            if (file.exists())
                return file;
        }
        return file;
    }
}
