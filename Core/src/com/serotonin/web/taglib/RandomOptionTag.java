package com.serotonin.web.taglib;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.BodyTagSupport;

public class RandomOptionTag extends BodyTagSupport {
    private static final long serialVersionUID = -1;
    
    /**
     * Property set in the tag.
     */
    private int weight = 1;

    public void setWeight(int weight) {
        this.weight = weight;
    }
    
    int getWeight() {
        return weight;
    }
    
    public int doStartTag() throws JspException {
        if (weight < 1)
            throw new JspException("RandomOptionTag weight must be greater than zero");
        
        RandomTag randomTag = (RandomTag)findAncestorWithClass(this, RandomTag.class);
        if (randomTag == null)
            throw new JspException("RandomOptionTag instances must be contained in a RandomTag instance");
        
        if (randomTag.isDeclareChildren()) {
            // If we are in the summing phase, add this to the list of children and skip the body.
            randomTag.addChild(this);
            return SKIP_BODY;
        }
        
        if (randomTag.evaluateChild())
            return EVAL_BODY_INCLUDE;
        
        return SKIP_BODY;
    }

    public void release() {
        super.release();
        weight = 1;
    }
}
