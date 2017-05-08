package com.serotonin.messaging;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;

/**
 * First, instatiate with the port number. Then add a data consumer, or create a message control and pass this as the
 * transport (which will make the message control the data consumer). This class supports running in its own thread
 * (start) or an external one (run), say from a thread pool. Both approaches are delegated to the socket listener. In
 * either case, stop the transport with the stop method (or just stop the message control).
 * 
 * @author Matthew Lohbihler
 */
public class UdpTransport implements Transport, Runnable {
    private static final int DEFAULT_RECEIVE_TIMEOUT = 500;

    private volatile boolean running = true;
    private final String host;
    private final int port;
    private final DatagramSocket datagramSocket;
    private DataConsumer consumer;

    public UdpTransport(String host, int port) throws SocketException {
        this.host = host;
        this.port = port;
        datagramSocket = new DatagramSocket(port);
        datagramSocket.setSoTimeout(DEFAULT_RECEIVE_TIMEOUT);
    }

    public void start(String threadName) {
        Thread thread = new Thread(this, threadName);
        thread.setDaemon(true);
        thread.start();
    }

    public void stop() {
        running = false;
    }

    public void setConsumer(DataConsumer consumer) {
        this.consumer = consumer;
    }

    public void removeConsumer() {
        consumer = null;
        stop();
    }

    public void write(byte[] data) throws IOException {
        write(data, data.length);
    }

    public void write(byte[] data, int len) throws IOException {
        DatagramPacket packet = new DatagramPacket(data, data.length, InetAddress.getByName(host), port);
        datagramSocket.send(packet);
    }

    public void run() {
        DatagramPacket datagramPacket = new DatagramPacket(new byte[1028], 1028);

        while (running) {
            try {
                datagramSocket.receive(datagramPacket);
                consumer.data(datagramPacket.getData(), datagramPacket.getLength());
                datagramPacket = new DatagramPacket(new byte[1028], 1028);
            }
            catch (SocketTimeoutException e) {
                // no op
            }
            catch (IOException e) {
                consumer.handleIOException(e);
            }
        }
    }
}
