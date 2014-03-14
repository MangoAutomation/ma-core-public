/*
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.view;

import java.awt.Container;
import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.Toolkit;
import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.Constants;
import com.serotonin.m2m2.module.Module;
import com.serotonin.m2m2.module.ModuleRegistry;

public class ViewGraphicLoader {
    private static final Log LOG = LogFactory.getLog(ViewGraphicLoader.class);

    //    private static final String GRAPHICS_PATH = "graphics";
    private static final String INFO_FILE_NAME = "info.txt";

    private static final String IGNORE_THUMBS = "Thumbs.db";

    private final String path = Common.MA_HOME + "/" + Constants.DIR_WEB;
    private List<ViewGraphic> viewGraphics;

    public List<ViewGraphic> loadViewGraphics() {
        viewGraphics = new ArrayList<ViewGraphic>();

        for (Module module : ModuleRegistry.getModules()) {
            if (module.getGraphicsDir() != null)
                loadModuleGraphics(new File(path + module.getWebPath(), module.getGraphicsDir()), module.getName());
        }

        return viewGraphics;
    }

    private void loadModuleGraphics(File graphicsPath, String moduleName) {
        File[] dirs = graphicsPath.listFiles();
        for (File dir : dirs) {
            try {
                if (dir.isDirectory() && !dir.getName().startsWith("."))
                    loadDirectory(dir, moduleName + ".");
            }
            catch (Exception e) {
                LOG.warn("Failed to load image set at " + dir, e);
            }
        }
    }

    private void loadDirectory(File dir, String baseId) throws Exception {
        String id = baseId + dir.getName();
        String name = id;
        String typeStr = "imageSet";
        int width = -1;
        int height = -1;
        int textX = 5;
        int textY = 5;

        File[] files = dir.listFiles();
        Arrays.sort(files);
        List<String> imageFiles = new ArrayList<String>();
        for (File file : files) {
            if (file.getName().startsWith(".")) {
                // ignore '.' files and directories
            }
            else if (file.isDirectory())
                loadDirectory(file, id + ".");
            else if (IGNORE_THUMBS.equalsIgnoreCase(file.getName())) {
                // no op
            }
            else if (INFO_FILE_NAME.equalsIgnoreCase(file.getName())) {
                // Info file
                Properties props = new Properties();
                props.load(new FileInputStream(file));

                name = getProperty(props, "name", name);
                typeStr = getProperty(props, "type", "imageSet");
                width = getIntProperty(props, "width", width);
                height = getIntProperty(props, "height", height);
                textX = getIntProperty(props, "text.x", textX);
                textY = getIntProperty(props, "text.y", textY);
            }
            else {
                // Image file. Subtract the load path from the image path
                String imagePath = file.getPath().substring(path.length());
                // Replace Windows-style '\' path separators with '/'
                imagePath = imagePath.replaceAll("\\\\", "/");
                imageFiles.add(imagePath);
            }
        }

        if (!imageFiles.isEmpty()) {
            if (width == -1 || height == -1) {
                String imagePath = path + imageFiles.get(0);
                Image image = Toolkit.getDefaultToolkit().getImage(imagePath);
                MediaTracker tracker = new MediaTracker(new Container());
                tracker.addImage(image, 0);
                tracker.waitForID(0);

                if (width == -1)
                    width = image.getWidth(null);
                if (height == -1)
                    height = image.getHeight(null);

                if (width == -1 || height == -1)
                    throw new Exception("Unable to derive image dimensions from " + imagePath);
            }

            String[] imageFileArr = imageFiles.toArray(new String[imageFiles.size()]);
            ViewGraphic g;
            if ("imageSet".equals(typeStr))
                g = new ImageSet(id, name, imageFileArr, width, height, textX, textY);
            else if ("dynamic".equals(typeStr))
                g = new DynamicImage(id, name, imageFileArr[0], width, height, textX, textY);
            else
                throw new Exception("Invalid type: " + typeStr);

            viewGraphics.add(g);
        }
    }

    private String getProperty(Properties props, String key, String defaultValue) {
        String prop = (String) props.get(key);
        if (prop == null)
            return defaultValue;
        return prop;
    }

    private int getIntProperty(Properties props, String key, int defaultValue) {
        String prop = (String) props.get(key);
        if (prop == null)
            return defaultValue;
        try {
            return Integer.parseInt(prop);
        }
        catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}
