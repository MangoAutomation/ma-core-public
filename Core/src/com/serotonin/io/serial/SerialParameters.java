package com.serotonin.io.serial;


/**
 * @author Matthew Lohbihler
 * 
 */
public class SerialParameters {
    private String commPortId;
    private String portOwnerName;
    private int baudRate = -1;
    private int flowControlIn = SerialPortProxy.FLOWCONTROL_NONE;
    private int flowControlOut = SerialPortProxy.FLOWCONTROL_NONE;
    private int dataBits = SerialPortProxy.DATABITS_8;
    private int stopBits = SerialPortProxy.STOPBITS_1;
    private int parity = SerialPortProxy.PARITY_NONE;

    private int recieveTimeout = 0; //ms to wait before timeout

    public int getBaudRate() {
        return baudRate;
    }

    public void setBaudRate(int baudRate) {
        this.baudRate = baudRate;
    }

    public String getCommPortId() {
        return commPortId;
    }

    public void setCommPortId(String commPortId) {
        this.commPortId = commPortId;
    }

    public int getDataBits() {
        return dataBits;
    }

    public void setDataBits(int dataBits) {
        this.dataBits = dataBits;
    }

    public int getFlowControlIn() {
        return flowControlIn;
    }

    public void setFlowControlIn(int flowControlIn) {
        this.flowControlIn = flowControlIn;
    }

    public int getFlowControlOut() {
        return flowControlOut;
    }

    public void setFlowControlOut(int flowControlOut) {
        this.flowControlOut = flowControlOut;
    }

    public int getParity() {
        return parity;
    }

    public void setParity(int parity) {
        this.parity = parity;
    }

    public int getStopBits() {
        return stopBits;
    }

    public void setStopBits(int stopBits) {
        this.stopBits = stopBits;
    }

    public String getPortOwnerName() {
        return portOwnerName;
    }

    public void setPortOwnerName(String portOwnerName) {
        this.portOwnerName = portOwnerName;
    }

    public int getRecieveTimeout() {
        return recieveTimeout;
    }

    public void setRecieveTimeout(int recieveTimeout) {
        this.recieveTimeout = recieveTimeout;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + baudRate;
        result = prime * result + ((commPortId == null) ? 0 : commPortId.hashCode());
        result = prime * result + dataBits;
        result = prime * result + flowControlIn;
        result = prime * result + flowControlOut;
        result = prime * result + parity;
        result = prime * result + ((portOwnerName == null) ? 0 : portOwnerName.hashCode());
        result = prime * result + recieveTimeout;
        result = prime * result + stopBits;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        SerialParameters other = (SerialParameters) obj;
        if (baudRate != other.baudRate)
            return false;
        if (commPortId == null) {
            if (other.commPortId != null)
                return false;
        }
        else if (!commPortId.equals(other.commPortId))
            return false;
        if (dataBits != other.dataBits)
            return false;
        if (flowControlIn != other.flowControlIn)
            return false;
        if (flowControlOut != other.flowControlOut)
            return false;
        if (parity != other.parity)
            return false;
        if (portOwnerName == null) {
            if (other.portOwnerName != null)
                return false;
        }
        else if (!portOwnerName.equals(other.portOwnerName))
            return false;
        if (recieveTimeout != other.recieveTimeout)
            return false;
        if (stopBits != other.stopBits)
            return false;
        return true;
    }
}
