/**
 * Copyright (C) 2018  Infinite Automation Software. All rights reserved.
 */
package com.infiniteautomation.mango.ftl;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;

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

    protected Map<String, String> templateStore = new HashMap<>();
    
    @Before
    public void setup() {
        templateStore.put("listExample", "<#list tags?keys as key> m${key} = ${tags[key]} </#list>");
        templateStore.put("dateFormatExample", "${time?number_to_datetime?iso_local}");
    }
    
    public void testSimpleLogic() throws TemplateNotFoundException, MalformedTemplateNameException, ParseException, IOException, TemplateException {
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
                // TODO Auto-generated method stub
                
            }
            
        });
        cfg.setObjectWrapper(new DefaultObjectWrapper(version));
        Template test = cfg.getTemplate("dateFormatTest");
        
        Map<String, Object> model = new HashMap<>();
        model.put("test", "Test String");
        
        Map<String,String> tags = new HashMap<>();
        tags.put("tag1", "value1");
        tags.put("tag2", "value2");
        model.put("tags", tags);
        
        model.put("time", System.currentTimeMillis());
        StringWriter out = new StringWriter();
        test.process(model, out);
        System.out.println(out.toString());
    }
    
}
