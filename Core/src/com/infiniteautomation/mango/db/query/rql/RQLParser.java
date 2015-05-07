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
package com.infiniteautomation.mango.db.query.rql;

import java.util.Arrays;
import java.util.Set;

import com.infiniteautomation.mango.db.query.ast.ComparisonOperator;
import com.infiniteautomation.mango.db.query.ast.Function;
import com.infiniteautomation.mango.db.query.ast.Node;
import com.infiniteautomation.mango.db.query.ast.NodesFactory;

/*
 * Example Usage -
 * Node rootNode = new RQLParser().parse(query);
   String result = rootNode.accept(new RqlToText());
 */

public final class RQLParser {
    private final NodesFactory nodesFactory;
    
    /**
     * Creates a new instance of {@code RQLParser} with the default set of comparison operators.
     */
    public RQLParser() {
        this.nodesFactory = new NodesFactory(RQLDefaults.operators(), RQLDefaults.functions());
    }

    /**
     * Creates a new instance of {@code RQLParser} that supports only the specified comparison
     * operators.
     *
     * @param operators A set of supported comparison operators. Must not be <tt>null</tt> or empty.
     */
    public RQLParser(Set<ComparisonOperator> operators, Set<Function> functions) {
        if (operators == null || operators.isEmpty()) {
            throw new IllegalArgumentException("operators must not be null or empty");
        }
        if (functions == null || functions.isEmpty()) {
            throw new IllegalArgumentException("functions must not be null or empty");
        }
        this.nodesFactory = new NodesFactory(operators, functions);
    }

    /**
     * Parses the RQL expression and returns AST.
     *
     * @param query The query expression to parse.
     * @return A root of the parsed AST.
     *
     * @throws RQLParserException If some exception occurred during parsing, i.e. the
     *          {@code query} is syntactically invalid.
     * @throws IllegalArgumentException If the {@code query} is <tt>null</tt>.
     */
    public Node parse(String query) throws RQLParserException {
        if (query == null) {
            throw new IllegalArgumentException("query must not be null");
        }
        
        // implementation here
        
        return nodesFactory.createComparisonNode("eq", "name", Arrays.asList(new String[] {"Jared"}));
    }
}
