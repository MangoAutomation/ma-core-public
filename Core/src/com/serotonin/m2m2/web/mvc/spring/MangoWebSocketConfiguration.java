/**
 * Copyright (C) 2016 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.web.mvc.spring;

import java.util.List;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.session.SessionDestroyedEvent;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistration;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.handler.PerConnectionWebSocketHandler;

import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.module.ModuleRegistry;
import com.serotonin.m2m2.module.PerConnectionWebSocketDefinition;
import com.serotonin.m2m2.module.WebSocketDefinition;
import com.serotonin.m2m2.web.mvc.websocket.MangoWebSocketConfigurer;

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
public class MangoWebSocketConfiguration extends MangoWebSocketConfigurer  {

    @Autowired
    private ConfigurableApplicationContext context;
    
    @Autowired
    private ConfigurableListableBeanFactory beanFactory;

    @PostConstruct
    public void postConstruct() {
        // SessionDestroyedEvent from root context are not propagated to the child web context. Register as a listener
        // on the parent.
        ConfigurableApplicationContext parent = (ConfigurableApplicationContext) context.getParent();
        
        ApplicationListener<SessionDestroyedEvent> listener = this::sessionDestroyed;
        parent.addApplicationListener(listener);
    }

    public static class MangoSessionDestroyedEvent extends ApplicationEvent {
        private static final long serialVersionUID = 1L;
        private final SessionDestroyedEvent event;

        public MangoSessionDestroyedEvent(SessionDestroyedEvent event) {
            super(event.getSource());
            this.event = event;
        }

        public SessionDestroyedEvent getOriginalEvent() {
            return event;
        }
    }
    
    public void sessionDestroyed(SessionDestroyedEvent event) {
        // cant just publish same event as this would cause a stack overflow, child events propagate to the parent
        this.context.publishEvent(new MangoSessionDestroyedEvent(event));
    }

    @Bean
    public List<WebSocketDefinition> websocketDefinitions() {
        return ModuleRegistry.getDefinitions(WebSocketDefinition.class);
    }
    
	@Override
	public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {

		//Setup Allowed Origins for CORS requests
		boolean hasOrigins = false;
		String[] origins = null;
		if (Common.envProps.getBoolean("rest.cors.enabled", false)) {
			hasOrigins = true;
			origins = Common.envProps.getStringArray("rest.cors.allowedOrigins", ",", new String[0]);
		}

		for (WebSocketDefinition def : websocketDefinitions()) {
			WebSocketHandler handler;

			if (def instanceof PerConnectionWebSocketDefinition) {
			    PerConnectionWebSocketHandler perConnection = new PerConnectionWebSocketHandler(((PerConnectionWebSocketDefinition) def).getHandlerClass());
			    beanFactory.initializeBean(perConnection, PerConnectionWebSocketHandler.class.getName());
			    handler = perConnection;
			} else {
			    handler = def.getHandlerInstance();
                beanFactory.autowireBean(handler);
                handler = (WebSocketHandler) beanFactory.initializeBean(handler, handler.getClass().getName());
			}
		    
            WebSocketHandlerRegistration registration = registry.addHandler(handler, def.getUrl())
                    .setHandshakeHandler(handshakeHandler())
                    .addInterceptors(handshakeIterceptor());
            
            if(hasOrigins)
                registration.setAllowedOrigins(origins);
		}
	}
}
