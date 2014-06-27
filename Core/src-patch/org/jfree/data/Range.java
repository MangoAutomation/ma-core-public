package org.jfree.data;

import java.io.Serializable;

/**
 * Represents an immutable range of values.
 */
public strictfp class Range implements Serializable {

    /** For serialization. */
    private static final long serialVersionUID = -906333695431863380L;

    /** The lower bound of the range. */
    private final double lower;

    /** The upper bound of the range. */
    private final double upper;

    /**
     * Creates a new range.
     * 
     * @param lower
     *            the lower bound (must be <= upper bound).
     * @param upper
     *            the upper bound (must be >= lower bound).
     */
    public Range(double lower, double upper) {
        if (lower > upper) {
            String msg = "Range(double, double): require lower (" + lower + ") <= upper (" + upper + ").";
            throw new IllegalArgumentException(msg);
        }
        this.lower = lower;
        this.upper = upper;
    }

    /**
     * Returns the lower bound for the range.
     * 
     * @return The lower bound.
     */
    public double getLowerBound() {
        return this.lower;
    }

    /**
     * Returns the upper bound for the range.
     * 
     * @return The upper bound.
     */
    public double getUpperBound() {
        return this.upper;
    }

    /**
     * Returns the length of the range.
     * 
     * @return The length.
     */
    public double getLength() {
        return this.upper - this.lower;
    }

    /**
     * Returns the central value for the range.
     * 
     * @return The central value.
     */
    public double getCentralValue() {
        return this.lower / 2.0 + this.upper / 2.0;
    }

    /**
     * Returns <code>true</code> if the range contains the specified value and <code>false</code> otherwise.
     * 
     * @param value
     *            the value to lookup.
     * 
     * @return <code>true</code> if the range contains the specified value.
     */
    public boolean contains(double value) {
        return (value >= this.lower && value <= this.upper);
    }

    /**
     * Returns <code>true</code> if the range intersects with the specified
     * range, and <code>false</code> otherwise.
     * 
     * @param b0
     *            the lower bound (should be <= b1).
     * @param b1
     *            the upper bound (should be >= b0).
     * 
     * @return A boolean.
     */
    public boolean intersects(double b0, double b1) {
        if (b0 <= this.lower) {
            return (b1 > this.lower);
        }
        else {
            return (b0 < this.upper && b1 >= b0);
        }
    }

    /**
     * Returns <code>true</code> if the range intersects with the specified
     * range, and <code>false</code> otherwise.
     * 
     * @param range
     *            another range (<code>null</code> not permitted).
     * 
     * @return A boolean.
     * 
     * @since 1.0.9
     */
    public boolean intersects(Range range) {
        return intersects(range.getLowerBound(), range.getUpperBound());
    }

    /**
     * Returns the value within the range that is closest to the specified
     * value.
     * 
     * @param value
     *            the value.
     * 
     * @return The constrained value.
     */
    public double constrain(double value) {
        double result = value;
        if (!contains(value)) {
            if (value > this.upper) {
                result = this.upper;
            }
            else if (value < this.lower) {
                result = this.lower;
            }
        }
        return result;
    }

    /**
     * Creates a new range by combining two existing ranges.
     * <P>
     * Note that:
     * <ul>
     * <li>either range can be <code>null</code>, in which case the other range is returned;</li>
     * <li>if both ranges are <code>null</code> the return value is <code>null</code>.</li>
     * </ul>
     * 
     * @param range1
     *            the first range (<code>null</code> permitted).
     * @param range2
     *            the second range (<code>null</code> permitted).
     * 
     * @return A new range (possibly <code>null</code>).
     */
    public static Range combine(Range range1, Range range2) {
        if (range1 == null) {
            return range2;
        }
        else {
            if (range2 == null) {
                return range1;
            }
            else {
                double l;
                if (Double.isNaN(range1.getLowerBound()))
                    l = range2.getLowerBound();
                else if (Double.isNaN(range2.getLowerBound()))
                    l = range1.getLowerBound();
                else
                    l = Math.min(range1.getLowerBound(), range2.getLowerBound());

                double u;
                if (Double.isNaN(range1.getUpperBound()))
                    u = range2.getUpperBound();
                else if (Double.isNaN(range2.getUpperBound()))
                    u = range1.getUpperBound();
                else
                    u = Math.max(range1.getUpperBound(), range2.getUpperBound());

                return new Range(l, u);
            }
        }
    }

    /**
     * Returns a range that includes all the values in the specified <code>range</code> AND the specified
     * <code>value</code>.
     * 
     * @param range
     *            the range (<code>null</code> permitted).
     * @param value
     *            the value that must be included.
     * 
     * @return A range.
     * 
     * @since 1.0.1
     */
    public static Range expandToInclude(Range range, double value) {
        if (range == null) {
            return new Range(value, value);
        }
        if (value < range.getLowerBound()) {
            return new Range(value, range.getUpperBound());
        }
        else if (value > range.getUpperBound()) {
            return new Range(range.getLowerBound(), value);
        }
        else {
            return range;
        }
    }

    /**
     * Creates a new range by adding margins to an existing range.
     * 
     * @param range
     *            the range (<code>null</code> not permitted).
     * @param lowerMargin
     *            the lower margin (expressed as a percentage of the
     *            range length).
     * @param upperMargin
     *            the upper margin (expressed as a percentage of the
     *            range length).
     * 
     * @return The expanded range.
     */
    public static Range expand(Range range, double lowerMargin, double upperMargin) {
        if (range == null) {
            throw new IllegalArgumentException("Null 'range' argument.");
        }
        double length = range.getLength();
        double lower = range.getLowerBound() - length * lowerMargin;
        double upper = range.getUpperBound() + length * upperMargin;
        if (lower > upper) {
            lower = lower / 2.0 + upper / 2.0;
            upper = lower;
        }
        return new Range(lower, upper);
    }

    /**
     * Shifts the range by the specified amount.
     * 
     * @param base
     *            the base range (<code>null</code> not permitted).
     * @param delta
     *            the shift amount.
     * 
     * @return A new range.
     */
    public static Range shift(Range base, double delta) {
        return shift(base, delta, false);
    }

    /**
     * Shifts the range by the specified amount.
     * 
     * @param base
     *            the base range (<code>null</code> not permitted).
     * @param delta
     *            the shift amount.
     * @param allowZeroCrossing
     *            a flag that determines whether or not the
     *            bounds of the range are allowed to cross
     *            zero after adjustment.
     * 
     * @return A new range.
     */
    public static Range shift(Range base, double delta, boolean allowZeroCrossing) {
        if (base == null) {
            throw new IllegalArgumentException("Null 'base' argument.");
        }
        if (allowZeroCrossing) {
            return new Range(base.getLowerBound() + delta, base.getUpperBound() + delta);
        }
        else {
            return new Range(shiftWithNoZeroCrossing(base.getLowerBound(), delta), shiftWithNoZeroCrossing(
                    base.getUpperBound(), delta));
        }
    }

    /**
     * Returns the given <code>value</code> adjusted by <code>delta</code> but
     * with a check to prevent the result from crossing <code>0.0</code>.
     * 
     * @param value
     *            the value.
     * @param delta
     *            the adjustment.
     * 
     * @return The adjusted value.
     */
    private static double shiftWithNoZeroCrossing(double value, double delta) {
        if (value > 0.0) {
            return Math.max(value + delta, 0.0);
        }
        else if (value < 0.0) {
            return Math.min(value + delta, 0.0);
        }
        else {
            return value + delta;
        }
    }

    /**
     * Scales the range by the specified factor.
     * 
     * @param base
     *            the base range (<code>null</code> not permitted).
     * @param factor
     *            the scaling factor (must be non-negative).
     * 
     * @return A new range.
     * 
     * @since 1.0.9
     */
    public static Range scale(Range base, double factor) {
        if (base == null) {
            throw new IllegalArgumentException("Null 'base' argument.");
        }
        if (factor < 0) {
            throw new IllegalArgumentException("Negative 'factor' argument.");
        }
        return new Range(base.getLowerBound() * factor, base.getUpperBound() * factor);
    }

    /**
     * Tests this object for equality with an arbitrary object.
     * 
     * @param obj
     *            the object to test against (<code>null</code> permitted).
     * 
     * @return A boolean.
     */
    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Range)) {
            return false;
        }
        Range range = (Range) obj;
        if (!(this.lower == range.lower)) {
            return false;
        }
        if (!(this.upper == range.upper)) {
            return false;
        }
        return true;
    }

    /**
     * Returns a hash code.
     * 
     * @return A hash code.
     */
    @Override
    public int hashCode() {
        int result;
        long temp;
        temp = Double.doubleToLongBits(this.lower);
        result = (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(this.upper);
        result = 29 * result + (int) (temp ^ (temp >>> 32));
        return result;
    }

    /**
     * Returns a string representation of this Range.
     * 
     * @return A String "Range[lower,upper]" where lower=lower range and
     *         upper=upper range.
     */
    @Override
    public String toString() {
        return ("Range[" + this.lower + "," + this.upper + "]");
    }

}
