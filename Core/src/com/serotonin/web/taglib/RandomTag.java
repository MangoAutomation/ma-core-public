package com.serotonin.web.taglib;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.jsp.tagext.TagSupport;

import com.serotonin.util.StringUtils;

public class RandomTag extends TagSupport {
    private static final long serialVersionUID = -1;

    /**
     * The number of option tags that should be selected for display.
     */
    private int count = 1;

    public void setCount(int count) {
        this.count = count;
    }

    /**
     * True if we are in the child collection phase.
     */
    private boolean declareChildren = true;

    boolean isDeclareChildren() {
        return declareChildren;
    }

    private final List<RandomOptionProxy> children = new ArrayList<RandomOptionProxy>();
    private int currentChildIndex;

    void addChild(RandomOptionTag child) {
        RandomOptionProxy proxy = new RandomOptionProxy();
        proxy.weight = child.getWeight();
        children.add(proxy);
    }

    boolean evaluateChild() {
        RandomOptionProxy proxy = children.get(currentChildIndex);
        boolean result = proxy.evaluate;
        proxy.evaluate = false;
        currentChildIndex++;
        return result;
    }

    @Override
    public int doStartTag() {
        declareChildren = true;
        return EVAL_BODY_INCLUDE;
    }

    @Override
    public int doAfterBody() {
        if (declareChildren) {
            declareChildren = false;
            if (count > children.size())
                count = children.size();
        }

        if (count > 0) {
            // Select a child and run the body again.
            selectChild();
            currentChildIndex = 0;
            return EVAL_BODY_AGAIN;
        }

        // We're done.
        return SKIP_BODY;
    }

    @Override
    public int doEndTag() {
        count = 1;
        declareChildren = true;
        children.clear();

        return EVAL_PAGE;
    }

    private void selectChild() {
        // Sum the weights.
        int totalWeight = 0;
        RandomOptionProxy child;
        for (int i = 0; i < children.size(); i++) {
            child = children.get(i);
            if (!child.used)
                totalWeight += child.weight;
        }

        // Get a random number.
        int randomValue = StringUtils.RANDOM.nextInt(totalWeight);

        // Pick the child corresponding to the random number.
        for (int i = 0; i < children.size(); i++) {
            child = children.get(i);
            if (child.used)
                continue;

            if (randomValue < child.weight) {
                child.evaluate = true;
                child.used = true;
                break;
            }
            randomValue -= child.weight;
        }

        count--;
    }

    class RandomOptionProxy {
        int weight;
        boolean evaluate;
        boolean used;
    }
}
