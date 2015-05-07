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

import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;

public final class Function {

    private static final Pattern NAME_PATTERN = Pattern.compile("=[a-zA-Z]*=|[><]=?|!=");

    private final String[] names;

    private final boolean multiValue;


    /**
     * @param names Textual representation of this operator (e.g. <tt>=gt=</tt>); the first item
     *          is primary representation, any others are alternatives. Must match
     *          <tt>=[a-zA-Z]*=|[><]=?|!=</tt>.
     * @param multiValue Whether this operator may be used with multiple arguments. This is then
     *          validated in {@link NodesFactory}.
     *
     * @throws IllegalArgumentException If the {@code names} is either <tt>null</tt>, empty,
     *          or contain illegal names.
     */
    public Function(String[] names, boolean multiValue) {
        Assert.notEmpty(names, "names must not be null or empty");
        for (String name : names) {
            Assert.isTrue(isValidFunctionName(name), "name must match: %s", NAME_PATTERN);
        }
        this.multiValue = multiValue;
        this.names = names.clone();
    }

    /**
     * @see #FunctionName(String[], boolean)
     */
    public Function(String name, boolean multiValue) {
        this(new String[]{name}, multiValue);
    }

    /**
     * @see #FunctionName(String[], boolean)
     */
    public Function(String name, String altName, boolean multiValue) {
        this(new String[]{name, altName}, multiValue);
    }

    /**
     * @see #FunctionName(String[], boolean)
     */
    public Function(String... names) {
        this(names, false);
    }


    /**
     * Returns the primary representation of this operator.
     */
    public String getName() {
        return names[0];
    }

    /**
     * Returns all representations of this operator. The first item is always the primary
     * representation.
     */
    public String[] getNames() {
        return names.clone();
    }

    /**
     * Whether this operator may be used with multiple arguments.
     */
    public boolean isMultiValue() {
        return multiValue;
    }


    /**
     * Whether the given string can represent an operator.
     * Note: Allowed names are limited by the RQL syntax (i.e. parser).
     */
    private boolean isValidFunctionName(String str) {
        return !StringUtils.isBlank(str) && NAME_PATTERN.matcher(str).matches();
    }


    @Override
    public String toString() {
        return getName();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Function)) return false;

        Function that = (Function) o;
        return getName().equals(that.getName());
    }

    @Override
    public int hashCode() {
        return getName().hashCode();
    }
}
