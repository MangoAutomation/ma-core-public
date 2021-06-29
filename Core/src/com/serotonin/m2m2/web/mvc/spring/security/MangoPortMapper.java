/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.web.mvc.spring.security;

import java.util.Arrays;
import java.util.List;

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.web.PortMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import com.serotonin.m2m2.db.dao.SystemSettingsDao;
import com.serotonin.m2m2.vo.systemSettings.SystemSettingsListener;

/**
 * Port mapper implementation to infer port mappings for Mango's HTTPS rest/web interface for mapping HTTP to HTTPS ports
 * 
 * Give priority to env mapping while deferring to the publicly resolvable URLs port, as a last ditch effort 
 * use the ssl.port env property
 * 
 * @author Terry Packer
 *
 */
@Component
public class MangoPortMapper implements PortMapper, SystemSettingsListener {
    
    private final Integer webPort;
    private final Integer sslPort;
    private Integer publicHttpsPort;
    
    @Autowired
    public MangoPortMapper(@Value("${web.port:8080}") Integer webPort, @Value("${ssl.port:8443}") Integer sslPort) {
        this.webPort = webPort;
        this.sslPort = sslPort;
    }
    
    @PostConstruct
    public void initialize() {
        this.publicHttpsPort = parseHttpsPort(SystemSettingsDao.instance.getValue(SystemSettingsDao.PUBLICLY_RESOLVABLE_BASE_URL));
    }
    
    @Override
    public Integer lookupHttpPort(Integer httpsPort) {
        return webPort;
    }
    
    @Override
    public Integer lookupHttpsPort(Integer httpPort) {
        //prefer env property mapping
        if(webPort.equals(httpPort)) {
            return sslPort;
        }else {
            return publicHttpsPort;
        }
    }

    @Override
    public void systemSettingsSaved(String key, String oldValue, String newValue) {
        publicHttpsPort = parseHttpsPort(newValue);
    }

    @Override
    public List<String> getKeys() {
        return Arrays.asList(SystemSettingsDao.PUBLICLY_RESOLVABLE_BASE_URL);
    }
    
    private Integer parseHttpsPort(String urlString) {
        if(StringUtils.isEmpty(urlString)) {
            return sslPort;
        }else {
            try {
                UriComponents uri = UriComponentsBuilder.fromHttpUrl(urlString).build();
                if("https".equals(uri.getScheme())) {
                    return uri.getPort() == -1 ? 443 : uri.getPort();
                }else {
                    return sslPort;
                }
            }catch(Exception e) {
                //invalid url?
                return sslPort;
            }
        }
    }
    
}
