/*
    Copyright (C) 2006-2009 Serotonin Software Technologies Inc.
 	@author Matthew Lohbihler
 */
package com.serotonin.util;

/**
 * @author Matthew Lohbihler
 */
public class DelimeterSet {
    private final String left;
    private final String right;
    
    public DelimeterSet(String left, String right) {
        this.left = left;
        this.right = right;
    }
    
    public String getToken(String s) {
        if (s == null)
            return null;
        
        int leftPos = s.indexOf(left);
        if (leftPos == -1)
            return null;
        
        int rightPos = s.indexOf(right, leftPos + left.length());
        if (rightPos == -1)
            return null;
        
        return s.substring(leftPos + left.length(), rightPos);
    }
    
    
    
    public int getIntToken(String s) {
        String token = getToken(s);
        if (token == null)
            return -1;
        try {
            return Integer.parseInt(token);
        }
        catch (NumberFormatException e) {
            return -1;
        }
    }
    
    public String wrap(Object o) {
        return left + o.toString() + right;
    }
    
    public String replace(String s, Object o) {
        if (s == null)
            return null;
        
        int leftPos = s.indexOf(left);
        if (leftPos == -1)
            return s;
        
        int rightPos = s.indexOf(right, leftPos + left.length());
        if (rightPos == -1)
            return s;
        
        return s.substring(0, leftPos + left.length()) + o.toString() + s.substring(rightPos);
    }
}
