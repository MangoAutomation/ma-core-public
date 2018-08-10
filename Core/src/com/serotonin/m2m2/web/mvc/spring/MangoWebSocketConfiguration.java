/**
 * Copyright (C) 2018 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.web.mvc.spring;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistration;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.handler.PerConnectionWebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.dao.AbstractBasicDao;
import com.serotonin.m2m2.module.ModuleRegistry;
import com.serotonin.m2m2.module.PerConnectionWebSocketDefinition;
import com.serotonin.m2m2.module.WebSocketDefinition;
import com.serotonin.m2m2.web.mvc.websocket.DaoNotificationWebSocketHandler;
import com.serotonin.m2m2.web.mvc.websocket.MangoWebSocketHandshakeHandler;

/**
 *
 * Web Socket Configuration
 *
 * CORS allowed origins in env.properties are set here
 * WebSocketHandlers module definitions are registered here
 *
 * @author Terry Packer
 *
 */
@Configuration
@EnableWebSocket
@ComponentScan(basePackages = {"com.serotonin.m2m2.web.mvc.websocket"})
public class MangoWebSocketConfiguration implements WebSocketConfigurer {

    @Autowired
    private ConfigurableListableBeanFactory beanFactory;

    @Autowired
    private HandshakeInterceptor handshakeInterceptor;

    @Autowired
    private MangoWebSocketHandshakeHandler handshakeHandler;
    
    
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {

        //Setup Allowed Origins for CORS requests
        boolean hasOrigins = false;
        String[] origins = null;
        if (Common.envProps.getBoolean("rest.cors.enabled", false)) {
            hasOrigins = true;
            origins = Common.envProps.getStringArray("rest.cors.allowedOrigins", ",", new String[0]);
        }

        for (WebSocketDefinition def : ModuleRegistry.getDefinitions(WebSocketDefinition.class)) {
            WebSocketHandler handler;
            if (def instanceof PerConnectionWebSocketDefinition) {
                PerConnectionWebSocketHandler perConnection = new PerConnectionWebSocketHandler(((PerConnectionWebSocketDefinition) def).getHandlerClass());
                beanFactory.initializeBean(perConnection, PerConnectionWebSocketHandler.class.getName());
                handler = perConnection;
            } else {
                handler = (WebSocketHandler)beanFactory.getBean(def.getWebSocketHandlerBeanName());
                if(handler instanceof DaoNotificationWebSocketHandler<?>) {
                    //Register us with the Dao as the bean is ready to be used
                    DaoNotificationWebSocketHandler daoHandler = (DaoNotificationWebSocketHandler<?>)handler;
                    AbstractBasicDao<?> dao = (AbstractBasicDao<?>)beanFactory.getBean(daoHandler.getDaoBeanName());
                    dao.setHandler(daoHandler);
                }
            }
            WebSocketHandlerRegistration registration = registry.addHandler(handler, def.getUrl())
                    .setHandshakeHandler(handshakeHandler)
                    .addInterceptors(handshakeInterceptor);

            if(hasOrigins)
                registration.setAllowedOrigins(origins);
        }
    }
}
