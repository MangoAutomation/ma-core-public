/*
 * Copyright (C) 2016 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.test;

import com.dumbster.smtp.SimpleSmtpServer;

/**
 * Simple SMTP Sever to test Mango Sending Emails on localhost
 * @author Terry Packer
 *
 */
public class DumbSMTPServer {
	
	public void runServer(){

		SimpleSmtpServer server = SimpleSmtpServer.start(20000);
		int emailCnt = 0;
		while(emailCnt < 100){
			try {
				Thread.sleep(1000);
//				Iterator<SmtpMessage> it = server.getReceivedEmail();
//				
//				while(server.getReceivedEmailSize() > emailCnt){
//					SmtpMessage msg = it.next();
//					System.out.println("" + msg.getBody());
//					emailCnt++;
//				}
				
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		server.stop();
	}

}
