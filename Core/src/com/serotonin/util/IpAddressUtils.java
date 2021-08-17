/*
    Copyright (C) 2006-2007 Serotonin Software Technologies Inc.
 	@author Matthew Lohbihler
 */
package com.serotonin.util;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.regex.Pattern;

/**
 * @author Matthew Lohbihler
 */
public class IpAddressUtils {
    private static final Pattern IPV6_REGEX = Pattern.compile("[0-9a-fA-F*]{1,4}:[0-9a-fA-F*]{1,4}:[0-9a-fA-F*]{1,4}:[0-9a-fA-F*]{1,4}"+
            ":[0-9a-fA-F*]{1,4}:[0-9a-fA-F*]{1,4}:[0-9a-fA-F*]{1,4}:[0-9a-fA-F*]{1,4}");
    private static final int IPV4_BASE = 10;
    private static final int IPV6_BASE = 16;
    
    public static boolean ipWhiteListCheck(String allowedIp, String remoteIp) throws IpWhiteListException {
        String[] remoteIpParts = remoteIp.split("\\.");
        if (remoteIpParts.length != 4) {
            remoteIpParts = remoteIp.split(":");
            if(remoteIpParts.length != 8)
                throw new IpWhiteListException("Invalid remote IP address: "+ remoteIp);
            return ipv6WhiteListCheckImpl(allowedIp, remoteIp, remoteIpParts);
        }
        return ipWhiteListCheckImpl(allowedIp, remoteIp, remoteIpParts);
    }
    
    public static boolean ipWhiteListCheck(String[] allowedIps, String remoteIp) throws IpWhiteListException {
        InetAddress address;
        try {
            address = InetAddress.getByName(remoteIp);
        } catch (UnknownHostException e) {
            return false;
        }
        
        String cleanIp = address.getHostAddress();
        if (address instanceof Inet6Address) {
            String [] remoteIpParts = cleanIp.split(":");
            if(remoteIpParts.length != 8)
                throw new IpWhiteListException("Invalid remote IP address: "+ remoteIp);

            for (int i=0; i<allowedIps.length; i++) {
                if (isIpv6(allowedIps[i]) && ipv6WhiteListCheckImpl(allowedIps[i], cleanIp, remoteIpParts))
                    return true;
            }
        } else if (address instanceof Inet4Address) {
            String[] remoteIpParts = cleanIp.split("\\.");
            if(remoteIpParts.length != 4)
                throw new IpWhiteListException("Invalid remote IP address: "+ remoteIp);

            for (int i=0; i<allowedIps.length; i++) {
                if (!isIpv6(allowedIps[i]) && ipWhiteListCheckImpl(allowedIps[i], cleanIp, remoteIpParts))
                    return true;
            }
        }
        return false;
    }
    
    private static boolean ipWhiteListCheckImpl(String allowedIp, String remoteIp, String[] remoteIpParts)
            throws IpWhiteListException {
        String[] allowedIpParts = allowedIp.split("\\.");
        if (allowedIpParts.length != 4)
            throw new IpWhiteListException("Invalid allowed IP address: "+ allowedIp);
            
        return validateIpPart(allowedIpParts[0], remoteIpParts[0], allowedIp, remoteIp, IPV4_BASE) &&
                validateIpPart(allowedIpParts[1], remoteIpParts[1], allowedIp, remoteIp, IPV4_BASE) &&
                validateIpPart(allowedIpParts[2], remoteIpParts[2], allowedIp, remoteIp, IPV4_BASE) &&
                validateIpPart(allowedIpParts[3], remoteIpParts[3], allowedIp, remoteIp, IPV4_BASE);
    }
    
    private static boolean ipv6WhiteListCheckImpl(String allowedIp, String remoteIp, String[] remoteIpParts)
            throws IpWhiteListException {
        String[] allowedIpParts = allowedIp.split(":");
        if (allowedIpParts.length != 8)
            throw new IpWhiteListException("Invalid allowed IPv6 address: "+ allowedIp);
        
        return validateIpPart(allowedIpParts[0], remoteIpParts[0], allowedIp, remoteIp, IPV6_BASE) &&
               validateIpPart(allowedIpParts[1], remoteIpParts[1], allowedIp, remoteIp, IPV6_BASE) &&
               validateIpPart(allowedIpParts[2], remoteIpParts[2], allowedIp, remoteIp, IPV6_BASE) &&
               validateIpPart(allowedIpParts[3], remoteIpParts[3], allowedIp, remoteIp, IPV6_BASE) &&
               validateIpPart(allowedIpParts[4], remoteIpParts[4], allowedIp, remoteIp, IPV6_BASE) &&
               validateIpPart(allowedIpParts[5], remoteIpParts[5], allowedIp, remoteIp, IPV6_BASE) &&
               validateIpPart(allowedIpParts[6], remoteIpParts[6], allowedIp, remoteIp, IPV6_BASE) &&
               validateIpPart(allowedIpParts[7], remoteIpParts[7], allowedIp, remoteIp, IPV6_BASE);
    }
    
    private static boolean validateIpPart(String allowed, String remote, String allowedIp, String remoteIp, int base)
            throws IpWhiteListException {
        if ("*".equals(allowed))
            return true;
        
        int dash = allowed.indexOf('-');
        try {
            if (dash == -1)
                return Integer.parseInt(allowed, base) == Integer.parseInt(remote, base);
            
            int from = Integer.parseInt(allowed.substring(0, dash), base);
            int to = Integer.parseInt(allowed.substring(dash + 1), base);
            int rem = Integer.parseInt(remote, base);
            
            return from <= rem && rem <= to;
        }
        catch (NumberFormatException e) {
            throw new IpWhiteListException("Integer parsing error. allowed="+ allowedIp + ", remote="+ remoteIp + ", base="+ base);
        }
    }
    
    private static boolean isIpv6(String address) {
        return address != null && IPV6_REGEX.matcher(address).matches();
    }
    
    public static String checkIpMask(String ip) {
        String[] ipParts = ip.split("\\.");
        if (ipParts.length != 4) {
            ipParts = ip.split(":");
            if(ipParts.length != 8)
                return "IPv4 address must have 4 parts, IPv6 address must have 8 parts";
            else {
                String message;
                for(int i = 0; i < 8; i+=1) {
                    message = checkIpv6MaskPart(ipParts[i]);
                    if(message != null)
                        return message;
                }
                return null;
            }
        }
        
        String message = checkIpMaskPart(ipParts[0]);
        if (message != null)
            return message;
        message = checkIpMaskPart(ipParts[1]);
        if (message != null)
            return message;
        message = checkIpMaskPart(ipParts[2]);
        if (message != null)
            return message;
        message = checkIpMaskPart(ipParts[3]);
        if (message != null)
            return message;
        
        return null;
    }
    
    private static String checkIpMaskPart(String part) {
        if ("*".equals(part))
            return null;
        
        int dash = part.indexOf('-');
        try {
            if (dash == -1) {
                int value = Integer.parseInt(part, IPV4_BASE);
                if (value < 0 || value > 255)
                    return "Value out of range in '"+ part +"'";
            }
            else {
                int from = Integer.parseInt(part.substring(0, dash), IPV4_BASE);
                if (from < 0 || from > 255)
                    return "'From' value out of range in '"+ part +"'";
                
                int to = Integer.parseInt(part.substring(dash + 1), IPV4_BASE);
                if (to < 0 || to > 255)
                    return "'To' value out of range in '"+ part +"'";
                
                if (from > to)
                    return "'From' value is greater than 'To' value in '"+ part +"'";
            }
        }
        catch (NumberFormatException e) {
            return "Integer parsing error in '"+ part +"'";
        }
        
        return null;
    }
    
    private static String checkIpv6MaskPart(String part) {
        if ("*".equals(part))
            return null;
        
        int dash = part.indexOf('-');
        try {
            if (dash == -1) {
                int value = Integer.parseInt(part, IPV6_BASE);
                if (value < 0 || value > 65535)
                    return "Value out of range in '"+ part +"'";
            } else {
                int from = Integer.parseInt(part.substring(0, dash), IPV6_BASE);
                if (from < 0 || from > 65535)
                    return "'From' value out of range in '"+ part +"'";
                
                int to = Integer.parseInt(part.substring(dash + 1), IPV6_BASE);
                if (to < 0 || to > 65535)
                    return "'To' value out of range in '"+ part +"'";
                
                if (from > to)
                    return "'From' value is greater than 'To' value in '"+ part +"'";
            }
        } catch (NumberFormatException e) {
            return "Integer parsing error in '"+ part +"'";
        }
        
        return null;
    }
    
    public static byte[] toIpAddress(String addr) throws IllegalArgumentException { //TODO IPv6
        if (addr == null)
            throw new IllegalArgumentException("Invalid address: (null)");
        
        String[] parts = addr.split("\\.");
        if (parts.length != 4)
            throw new IllegalArgumentException("IP address must have 4 parts");
        
        byte[] ip = new byte[4];
        for (int i=0; i<4; i++) {
            try {
                int part = Integer.parseInt(parts[i]);
                if (part < 0 || part > 255)
                    throw new IllegalArgumentException("Value out of range in '"+ parts[i] +"'");
                ip[i] = (byte)part;
            }
            catch (NumberFormatException e) {
                throw new IllegalArgumentException("Integer parsing error in '"+ parts[i] +"'");
            }
        }
        
        return ip;
    }
    
    public static String toIpString(byte[] b) throws IllegalArgumentException { //TODO IPv6
        if (b.length != 4)
            throw new IllegalArgumentException("IP address must have 4 parts");
        
        StringBuilder sb = new StringBuilder();
        sb.append(b[0] & 0xff);
        for (int i=1; i<b.length; i++)
            sb.append('.').append(b[i] & 0xff);
        return sb.toString();
    }
}
