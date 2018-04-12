/**
 * Copyright (C) 2015 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.web.mvc.websocket;

import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jetty.websocket.api.WebSocketException;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.PingMessage;
import org.springframework.web.socket.WebSocketSession;

import com.serotonin.m2m2.util.timeout.TimeoutClient;
import com.serotonin.m2m2.util.timeout.TimeoutTask;

/**
 * Class to test and disconnect WebSocket sessions if the Client goes away without notifying us
 *
 * Saves Ping/Pong state in Session attributes
 *
 * Not convinced to use the real time timer for this, perhaps it should be a WorkItem?
 *
 * @author Terry Packer
 *
 */
public 	class MangoPingPongTracker extends TimeoutClient {

    private final Log log = LogFactory.getLog(this.getClass());

    private WebSocketSession session;
    private int timeout;
    private TimeoutTask task;
    private volatile boolean isShutdown = false;

    public MangoPingPongTracker(WebSocketSession session, int timeout) {
        this.session = session;
        this.timeout = timeout;

        if (this.session.isOpen()) {
            this.sendPing();
        }
    }

    public void sendPing() {
        try {
            session.getAttributes().put(MangoWebSocketHandler.RECEIVED_PONG, Boolean.FALSE);
            session.sendMessage(new PingMessage());
        } catch (IOException | WebSocketException e) {
            if (log.isErrorEnabled()) {
                log.error("Error sending websocket ping", e);
            }
        } finally {
            // never reschedule the task if shutdown was called
            if (!this.isShutdown) {
                task = new TimeoutTask(this.timeout, this);
            }
        }
    }

    public void shutdown() {
        this.isShutdown = true;
        if (this.task != null) {
            this.task.cancel();
        }
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.util.timeout.TimeoutClient#scheduleTimeout(long)
     */
    @Override
    public void scheduleTimeout(long fireTime) {

        //Shut'er down if we are already dead
        if (!this.session.isOpen())
            return;

        Boolean receivedPong = (Boolean) this.session.getAttributes().get(MangoWebSocketHandler.RECEIVED_PONG);
        if (receivedPong) {
            this.sendPing();
        } else {
            try {
                session.close(new CloseStatus(CloseStatus.SESSION_NOT_RELIABLE.getCode(), "Didn't receive Pong from Endpoint within " + timeout + " ms."));
            } catch (IOException e) {
                if (log.isErrorEnabled()) {
                    log.error("Error closing websocket session after poing timeout", e);
                }
            }
        }
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.util.timeout.TimeoutClient#getName()
     */
    @Override
    public String getThreadName() {
        return "Mango ping pong tracker";
    }

    private static final String taskId = "MangoPingPongTracker-";
    /* (non-Javadoc)
     * @see com.serotonin.m2m2.util.timeout.TimeoutClient#getTaskId()
     */
    @Override
    public String getTaskId() {
        return taskId + session.getId();
    }
}