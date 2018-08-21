/**
 * Copyright (C) 2018 Infinite Automation Software. All rights reserved.
 */
package com.infiniteautomation.mango.web.jsp.compiler;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.jasper.compiler.JDTCompiler;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;
import org.apache.tomcat.Jar;
import org.apache.tomcat.util.scan.JarFactory;

/**
 * Class to track JSP changes via Hash rather than modification time due to the problems with Zip
 * files and timestamps.
 * 
 * To enable this compiler set env property:
 *  web.jsp.compilerClassName=com.infiniteautomation.mango.web.jsp.compiler.MangoJDTCompiler
 * 
 * The JSP/Tag is presumed to be up to date on first read, if it changes on any subsequent 
 * read according to the Modification Re-Test policy then it will be compiled.
 * 
 * Compatible with Apache JSP 8.5.24.2
 * 
 * @author Terry Packer
 */
public class MangoJDTCompiler extends JDTCompiler {

    private final Log log = LogFactory.getLog(MangoJDTCompiler.class);
    private static final ConcurrentHashMap<String, byte[]> hashMap = new ConcurrentHashMap<>();

    @Override
    public boolean isOutDated() {
        if (jsw != null && (ctxt.getOptions().getModificationTestInterval() > 0)) {

            if (jsw.getLastModificationTest()
                    + (ctxt.getOptions().getModificationTestInterval() * 1000) > System
                            .currentTimeMillis()) {
                return false;
            }
            jsw.setLastModificationTest(System.currentTimeMillis());
        }

        File targetFile = new File(ctxt.getClassFileName());
        if (!targetFile.exists()) {
            return true;
        }
        boolean outdated = false;
        try(FileInputStream is = new FileInputStream(targetFile)){
             outdated = isOutdated(ctxt.getClassFileName(), is);
        } catch (Exception e) {
            if (log.isDebugEnabled())
                log.debug("Problem accessing resource. Treat as outdated.", e);
            return true;
        }

        if (!outdated) {
            // determine if source dependent files (e.g. includes using include
            // directives) have been changed.
            if (jsw == null) {
                return false;
            }

            Map<String, Long> depends = jsw.getDependants();
            if (depends == null) {
                return false;
            }

            Iterator<String> it = depends.keySet().iterator();
            while (it.hasNext()) {
                try {
                    String key = it.next();
                    if (key.startsWith("jar:jar:")) {
                        // Assume we constructed this correctly
                        int entryStart = key.lastIndexOf("!/");
                        String entry = key.substring(entryStart + 2);
                        try (Jar jar =
                                JarFactory.newInstance(new URL(key.substring(4, entryStart)))) {
                            if (isOutdated(key, jar.getInputStream(entry)))
                                return true;
                        }
                    } else {
                        URL includeUrl;
                        if (key.startsWith("jar:") || key.startsWith("file:")) {
                            includeUrl = new URL(key);
                        } else {
                            includeUrl = ctxt.getResource(key);
                        }
                        if (includeUrl == null) {
                            return true;
                        }
                        try(InputStream is = includeUrl.openStream()){
                            if(isOutdated(key, includeUrl.openStream()))
                                return true;
                        }

                    }
                } catch (Exception e) {
                    if (log.isDebugEnabled())
                        log.debug("Problem accessing resource. Treat as outdated.", e);
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * @param classFileName
     * @return
     */
    private boolean isOutdated(String key, InputStream is) {
        AtomicBoolean outdated = new AtomicBoolean();
        // File exists, is there a hashed value?
        hashMap.compute(key, (k, v) -> {
            try {
                byte[] newHash = computeHash(is);
                if (v == null) {
                    return newHash;
                } else {
                    if (!Arrays.equals(newHash, v))
                        outdated.set(true);
                    return newHash;
                }
            } catch (Exception e) {
                if (log.isDebugEnabled())
                    log.debug("Problem accessing resource. Treat as outdated.", e);
                return null;
            }
        });
        return outdated.get();
    }

    /**
     * Compute hash of existing file, if file DNE return null
     * 
     * @param file
     * @return
     * @throws NoSuchAlgorithmException
     * @throws IOException
     */
    private byte[] computeHash(InputStream is) throws NoSuchAlgorithmException, IOException {

        MessageDigest md = MessageDigest.getInstance("SHA1");
        byte[] dataBytes = new byte[1024];
        int nread = 0;
        while ((nread = is.read(dataBytes)) != -1) {
            md.update(dataBytes, 0, nread);
        }
        return md.digest();
    }

}
