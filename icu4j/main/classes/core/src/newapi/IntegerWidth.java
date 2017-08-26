// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html#License
package newapi;

@SuppressWarnings("unused")
public class IntegerWidth {

    private static final IntegerWidth DEFAULT = new IntegerWidth(1, -1);

    final int minInt;
    final int maxInt;

    private IntegerWidth(int minInt, int maxInt) {
        this.minInt = minInt;
        this.maxInt = maxInt;
    }

    public static IntegerWidth zeroFillTo(int minInt) {
        if (minInt == 1) {
            return DEFAULT;
        } else if (minInt >= 0 && minInt < Rounder.MAX_VALUE) {
            return new IntegerWidth(minInt, -1);
        } else {
            throw new IllegalArgumentException("Integer digits must be between 0 and " + Rounder.MAX_VALUE);
        }
    }

    public IntegerWidth truncateAt(int maxInt) {
        if (maxInt == this.maxInt) {
            return this;
        } else if (maxInt >= 0 && maxInt < Rounder.MAX_VALUE) {
            return new IntegerWidth(minInt, maxInt);
        } else if (maxInt == -1) {
            return new IntegerWidth(minInt, maxInt);
        } else {
            throw new IllegalArgumentException("Integer digits must be between 0 and " + Rounder.MAX_VALUE);
        }
    }
}