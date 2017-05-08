package com.serotonin.util;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

/**
 * @author Matthew Lohbihler
 * 
 */
public class PortScan {
    public static void main(String[] args) {
        InetAddress addr;
        try {
            addr = InetAddress.getByName("192.168.0.20");
        }
        catch (UnknownHostException e) {
            e.printStackTrace();
            return;
        }
        int port = 1;

        while (port < 65536) {
            new SocketCheck(addr, port, port + 100).start();
            port += 100;
        }
    }

    static class SocketCheck extends Thread {
        InetAddress addr;
        int start, end;

        SocketCheck(InetAddress addr, int start, int end) {
            this.addr = addr;
            this.start = start;
            this.end = end;
            if (end >= 65536)
                end = 65536;
        }

        @Override
        public void run() {
            int port = start;
            Socket socket = null;
            while (port < end) {
                try {
                    socket = new Socket(addr, port);
                    System.out.println("Success on port " + port);
                }
                catch (IOException e) {
                    // no op
                }
                finally {
                    try {
                        if (socket != null)
                            socket.close();
                    }
                    catch (IOException e) {
                        // no op
                    }
                }
                port++;
            }
        }
    }
}
