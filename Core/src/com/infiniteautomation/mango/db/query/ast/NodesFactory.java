/*
 * The MIT License
 *
 * Copyright 2013-2014 Jakub Jirutka <jakub@jirutka.cz>.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.infiniteautomation.mango.db.query.ast;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Factory that creates {@link Node} instances for the parser.
 */
public class NodesFactory {

    private final Map<String, ComparisonOperator> comparisonOperators;
    private final Map<String, Function> functions;


    public NodesFactory(Set<ComparisonOperator> operators, Set<Function> functions) {

        comparisonOperators = new HashMap<>(operators.size());
        for (ComparisonOperator op : operators) {
            for (String sym : op.getSymbols()) {
                comparisonOperators.put(sym, op);
            }
        }
        
        this.functions = new HashMap<>(functions.size());
        for (Function fn : functions) {
            for (String name : fn.getNames()) {
                this.functions.put(name, fn);
            }
        }
    }

    /**
     * Creates a specific {@link LogicalNode} instance for the specified operator and with the
     * given children nodes.
     *
     * @param operator The logical operator to create a node for.
     * @param children Children nodes, i.e. operands.
     * @return A subclass of the {@link LogicalNode} according to the specified operator.
     */
    public LogicalNode createLogicalNode(LogicalOperator operator, List<Node> children) {
        switch (operator) {
            case AND : return new AndNode(children);
            case OR  : return new OrNode(children);

            // this normally can't happen
            default  : throw new IllegalStateException("Unknown operator: " + operator);
        }
    }

    /**
     * Creates a {@link ComparisonNode} instance with the given parameters.
     *
     * @param operatorToken A textual representation of the comparison operator to be found in the
     *                      set of supported {@linkplain ComparisonOperator operators}.
     * @param selector The selector that specifies the left side of the comparison.
     * @param arguments A list of arguments that specifies the right side of the comparison.
     *
     * @throws UnknownOperatorException If no operator for the specified operator token exists.
     */
    public ComparisonNode createComparisonNode(
            String operatorToken, String selector, List<String> arguments) throws UnknownOperatorException {

        ComparisonOperator op = comparisonOperators.get(operatorToken);
        if (op != null) {
            return new ComparisonNode(op, selector, arguments);
        } else {
            throw new UnknownOperatorException(operatorToken);
        }
    }
    
    public FunctionNode createFunctionNode(
            String functionToken, List<String> arguments) throws UnknownFunctionException {

        Function fn = functions.get(functionToken);
        if (fn != null) {
            return new FunctionNode(fn, arguments);
        } else {
            throw new UnknownOperatorException(functionToken);
        }
    }
}
