/*
    Copyright (C) 2006-2007 Serotonin Software Technologies Inc.
 	@author Matthew Lohbihler
 */
package com.serotonin.util;

/**
 * @author Matthew Lohbihler
 */
public class Tuple<E1 extends Object, E2 extends Object> {
    private E1 element1;
    private E2 element2;

    public Tuple() {
        // no op
    }

    public Tuple(E1 element1, E2 element2) {
        this.element1 = element1;
        this.element2 = element2;
    }

    public E1 getElement1() {
        return element1;
    }

    public void setElement1(E1 element1) {
        this.element1 = element1;
    }

    public E2 getElement2() {
        return element2;
    }

    public void setElement2(E2 element2) {
        this.element2 = element2;
    }
}
