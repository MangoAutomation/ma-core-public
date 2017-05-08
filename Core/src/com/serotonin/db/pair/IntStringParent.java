package com.serotonin.db.pair;

import java.util.ArrayList;
import java.util.List;

public class IntStringParent extends IntStringPair {
    private static final long serialVersionUID = -1;

    private final List<IntStringPair> children = new ArrayList<IntStringPair>();

    public IntStringParent(int key, String value) {
        super(key, value);
    }

    public void addChild(IntStringPair child) {
        children.add(child);
    }

    public List<IntStringPair> getChildren() {
        return children;
    }
}
