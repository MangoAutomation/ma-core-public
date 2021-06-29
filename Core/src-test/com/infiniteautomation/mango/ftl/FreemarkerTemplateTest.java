/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.infiniteautomation.mango.ftl;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

import org.junit.Before;
import org.junit.Test;

import freemarker.cache.TemplateLoader;
import freemarker.core.ParseException;
import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapper;
import freemarker.template.MalformedTemplateNameException;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateNotFoundException;
import freemarker.template.Version;

/**
 * @author Terry Packer
 *
 */
public class FreemarkerTemplateTest {

    protected final String dateFormatTemplateName = "dateFormatExample";
    protected final String listExampleTemplateName = "listExample";
    
    protected Map<String, String> templateStore = new HashMap<>();
    
    @Before
    public void setup() {
        templateStore.put(listExampleTemplateName, "<#list tags?keys as key> ${key} = ${tags[key]} </#list>");
        templateStore.put(dateFormatTemplateName, "${time?number_to_datetime?iso_utc}");
    }
    
    @Test
    public void testDateFormat() throws TemplateNotFoundException, MalformedTemplateNameException, ParseException, IOException, TemplateException {
        Version version = new Version("2.3.23");
        Configuration cfg = new Configuration(version);
        cfg.setTemplateLoader(new TemplateLoader() {

            @Override
            public Object findTemplateSource(String name) throws IOException {
                return templateStore.get(name);
            }

            @Override
            public long getLastModified(Object templateSource) {
                return 0;
            }

            @Override
            public Reader getReader(Object templateSource, String encoding) throws IOException {
                return new StringReader((String)templateSource);
            }

            @Override
            public void closeTemplateSource(Object templateSource) throws IOException {
                
            }
            
        });
        cfg.setObjectWrapper(new DefaultObjectWrapper(version));
        Template test = cfg.getTemplate(dateFormatTemplateName);
        
        Map<String, Object> model = new HashMap<>();
        
        long time = System.currentTimeMillis();
        model.put("time", time);
        StringWriter out = new StringWriter();
        test.process(model, out);
        String output = out.toString();
        
        SimpleDateFormat sdf = new SimpleDateFormat("yyy-MM-dd'T'HH:mm:ss'Z'");
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        String expected = sdf.format(new Date(time));
        assertEquals(expected, output);
    }
    
    @Test
    public void testList() throws TemplateNotFoundException, MalformedTemplateNameException, ParseException, IOException, TemplateException {
        Version version = new Version("2.3.23");
        Configuration cfg = new Configuration(version);
        cfg.setTemplateLoader(new TemplateLoader() {

            @Override
            public Object findTemplateSource(String name) throws IOException {
                return templateStore.get(name);
            }

            @Override
            public long getLastModified(Object templateSource) {
                return 0;
            }

            @Override
            public Reader getReader(Object templateSource, String encoding) throws IOException {
                return new StringReader((String)templateSource);
            }

            @Override
            public void closeTemplateSource(Object templateSource) throws IOException {
                
            }
            
        });
        cfg.setObjectWrapper(new DefaultObjectWrapper(version));
        Template test = cfg.getTemplate(listExampleTemplateName);
        
        Map<String, Object> model = new HashMap<>();
        
        Map<String,String> tags = new HashMap<>();
        tags.put("tag1", "value1");
        tags.put("tag2", "value2");
        model.put("tags", tags);
        
        StringWriter out = new StringWriter();
        test.process(model, out);
        String output = out.toString();
        String expected = " tag1 = value1  tag2 = value2 ";
        assertEquals(expected, output);
    }
    
}
