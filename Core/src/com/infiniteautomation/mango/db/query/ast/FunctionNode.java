/**
 * Copyright (C) 2015 Infinite Automation Systems. All rights reserved.
 * http://infiniteautomation.com/
 */
package com.infiniteautomation.mango.db.query.ast;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Jared Wiltshire
 */
public class FunctionNode extends AbstractNode {
    private final Function name;
    private final List<String> arguments;
    
    public FunctionNode(Function name, List<String> arguments) {
        Assert.notNull(name, "name must not be null");
        Assert.notEmpty(arguments, "arguments list must not be empty");
        Assert.isTrue(name.isMultiValue() || arguments.size() == 1,
                "name %s expects single argument, but multiple values given", name);
        
        this.name = name;
        this.arguments = new ArrayList<>(arguments);
    }

    /* (non-Javadoc)
     * @see com.infiniteautomation.mango.db.query.ast.Node#accept(com.infiniteautomation.mango.db.query.ast.ASTVisitor, java.lang.Object)
     */
    @Override
    public <R, A> R accept(ASTVisitor<R, A> visitor, A param) {
        return visitor.visit(this, param);
    }

    public Function getName() {
        return name;
    }

    public List<String> getArguments() {
        return arguments;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result
                + ((arguments == null) ? 0 : arguments.hashCode());
        result = prime * result + ((name == null) ? 0 : name.hashCode());
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
        FunctionNode other = (FunctionNode) obj;
        if (arguments == null) {
            if (other.arguments != null)
                return false;
        } else if (!arguments.equals(other.arguments))
            return false;
        if (name == null) {
            if (other.name != null)
                return false;
        } else if (!name.equals(other.name))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "FunctionNode [name=" + name + ", arguments=" + arguments + "]";
    }

}
