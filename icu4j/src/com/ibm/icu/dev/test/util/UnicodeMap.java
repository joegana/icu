package com.ibm.icu.dev.test.util;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import com.ibm.icu.impl.Utility;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.UnicodeSetIterator;
/**
 * Class for mapping Unicode characters to values
 * Much smaller storage than using HashMap.
 * @author Davis
 */
// TODO Optimize using range map
public final class UnicodeMap {
    static final boolean ASSERTIONS = false;
    static final long GROWTH_PERCENT = 200; // 100 is no growth!
    static final long GROWTH_GAP = 10; // extra bump!

    private int length = 2;
    private int[] transitions = {0,0x110000,0,0,0,0,0,0,0,0};
    private Object[] values = new Object[10];
    {
        values[1] = "TERMINAL";
    }
    
    void _checkInvariants() {
        if (length < 2
          || length > transitions.length
          || transitions.length != values.length) {
              throw new IllegalArgumentException("Invariant failed: Lengths bad");
          }
        for (int i = 1; i < length-1; ++i) {
            if (equator.isEqual(values[i-1], values[i])) {
                throw new IllegalArgumentException("Invariant failed: values shared at " 
                    + "\t" + Utility.hex(i-1) + ": <" + values[i-1] + ">"
                    + "\t" + Utility.hex(i) + ": <" + values[i] + ">"
                    );
            }
        }
        if (transitions[0] != 0 || transitions[length-1] != 0x110000) {
            throw new IllegalArgumentException("Invariant failed: bounds set wrong");
        }
        for (int i = 1; i < length-1; ++i) {
            if (transitions[i-1] >= transitions[i]) {
                throw new IllegalArgumentException("Invariant failed: not monotonic"
                + "\t" + Utility.hex(i-1) + ": " + transitions[i-1]
                + "\t" + Utility.hex(i) + ": " + transitions[i]
                    );
            }
        }
    }
    
    public interface Equator {
        /**
          * Comparator function. If overridden, must handle case of null,
          * and compare any two objects in the array
          * @param a
          * @param b
          * @return
          */
         public boolean isEqual(Object a, Object b);
    }
    
    public static class SimpleEquator implements Equator {
        public boolean isEqual(Object a, Object b) {
            if (a == b) return true;
            if (a == null || b == null) return false;
            return a.equals(b);
        }
    }
    private Equator equator = new SimpleEquator();

    /**
     * Finds an index such that inversionList[i] <= codepoint < inversionList[i+1]
     * Assumes that 0 <= codepoint <= 0x10FFFF
     * @param codepoint
     * @return
     */
    private int findIndex(int c) {
        int lo = 0;
        int hi = length - 1;
        int i = (lo + hi) >>> 1;
        // invariant: c >= list[lo]
        // invariant: c < list[hi]
        while (i != lo) {
            if (c < transitions[i]) {
                hi = i;
            } else {
                lo = i;
            }
            i = (lo + hi) >>> 1;
        }
        if (ASSERTIONS) _checkFind(c, lo);
        return lo;
    }
    
    private void _checkFind(int codepoint, int value) {
        int other = _findIndex(codepoint);
        if (other != value) {
            throw new IllegalArgumentException("Invariant failed: binary search"
                + "\t" + Utility.hex(codepoint) + ": " + value
                + "\tshould be: " + other);            
        }
    }
    
    private int _findIndex(int codepoint) {
        // TODO use binary search
        for (int i = length-1; i > 0; --i) {
            if (transitions[i] <= codepoint) return i;
        }
        return 0;
    }
    
    /*
     * Try indexed lookup
     
    static final int SHIFT = 8;
    int[] starts = new int[0x10FFFF>>SHIFT]; // lowest transition index where codepoint>>x can be found
    boolean startsValid = false;
    private int findIndex(int codepoint) {
        if (!startsValid) {
            int start = 0;
            for (int i = 1; i < length; ++i) {
                
            }
        }
        for (int i = length-1; i > 0; --i) {
           if (transitions[i] <= codepoint) return i;
       }
       return 0;
   }
   */
   
    /**
     * Remove the items from index through index+count-1.
     * Logically reduces the size of the internal arrays.
     * @param index
     * @param count
     */
    private void removeAt(int index, int count) {
        for (int i = index + count; i < length; ++i) {
            transitions[i-count] = transitions[i];
            values[i-count] = values[i];
        }
        length -= count;
    }
    /**
     * Add a gap from index to index+count-1.
     * The values there are undefined, and must be set.
     * Logically grows arrays to accomodate. Actual growth is limited
     * @param index
     * @param count
     */
    private void insertGapAt(int index, int count) {
        int newLength = length + count;
        int[] oldtransitions = transitions;
        Object[] oldvalues = values;
        if (newLength > transitions.length) {
            int allocation = (int) (GROWTH_GAP + (newLength * GROWTH_PERCENT) / 100);
            transitions = new int[allocation];
            values = new Object[allocation];
            for (int i = 0; i < index; ++i) {
                transitions[i] = oldtransitions[i];
                values[i] = oldvalues[i];
            }
        } 
        for (int i = length - 1; i >= index; --i) {
            transitions[i+count] = oldtransitions[i];
            values[i+count] = oldvalues[i];
        }
        length = newLength;
    }
    
    /**
     * Associates code point with value. Removes any previous association.
     * @param codepoint
     * @param value
     * @return this, for chaining
     */
    private UnicodeMap _put(int codepoint, Object value) {
        int baseIndex = findIndex(codepoint);
        int limitIndex = baseIndex + 1;
        // cases are (a) value is already set
        if (equator.isEqual(values[baseIndex], value)) return this;
        int baseCP = transitions[baseIndex];
        int limitCP = transitions[limitIndex];
        // CASE: At very start of range
        if (baseCP == codepoint) {
            boolean connectsWithPrevious = 
                baseIndex != 0 && equator.isEqual(value, values[baseIndex-1]);               
                
            // CASE: Single codepoint range
            if (limitCP == codepoint + 1) {
                boolean connectsWithFollowing =
                    baseIndex < length - 1 && equator.isEqual(value, values[limitIndex]);
                // A1a connects with previous & following, so remove index
                if (connectsWithPrevious) {
                    if (connectsWithFollowing) {
                        removeAt(baseIndex, 2);
                        return this;
                    }
                    removeAt(baseIndex, 1); // extend previous
                    return this;
                } else if (connectsWithFollowing) {
                    removeAt(baseIndex, 1); // extend following backwards
                    transitions[baseIndex] = codepoint; 
                    return this;
                }
                // doesn't connect on either side, just reset
                values[baseIndex] = value;
                return this;
            }                   
            // A.1: start of multi codepoint range
            // if connects
            if (connectsWithPrevious) {
                ++transitions[baseIndex]; // extend previous
            } else {
                // otherwise insert new transition
                transitions[baseIndex] = codepoint+1; // fix following range
                insertGapAt(baseIndex, 1);
                values[baseIndex] = value;
                transitions[baseIndex] = codepoint;
            }
            return this;
        }
        // CASE: at end of range
        if (limitCP == codepoint + 1) {
            // if connects, just back up range
            boolean connectsWithFollowing =
                baseIndex < length - 1 && equator.isEqual(value, values[limitIndex]);

            if (connectsWithFollowing) {
                --transitions[limitIndex]; 
                return this;                
            } else {
                insertGapAt(limitIndex, 1);
                transitions[limitIndex] = codepoint;
                values[limitIndex] = value;
            }
            return this;
        }
        // CASE: in middle of range
        insertGapAt(++baseIndex,2);
        transitions[baseIndex] = codepoint;
        values[baseIndex] = value;
        transitions[++baseIndex] = codepoint + 1;
        values[baseIndex] = values[baseIndex-2]; // copy lower range values
        return this;
    }
    /**
     * Sets the codepoint value.
     * @param codepoint
     * @param value
     * @return
     */
    public UnicodeMap put(int codepoint, Object value) {
        if (codepoint < 0 || codepoint > 0x10FFFF) {
            throw new IllegalArgumentException("Codepoint out of range: " + codepoint);
        }
        _put(codepoint, value);
        if (ASSERTIONS) _checkInvariants();
        return this;
    }
    /**
     * Adds bunch o' codepoints; otherwise like put.
     * @param codepoints
     * @param value
     * @return this, for chaining
     */
    public UnicodeMap putAll(UnicodeSet codepoints, Object value) {
        // TODO optimize
        UnicodeSetIterator it = new UnicodeSetIterator(codepoints);
        while (it.next()) {
            _put(it.codepoint, value);
        }
        return this;
    }
    
    /**
     * Adds bunch o' codepoints; otherwise like add.
     * @param codepoints
     * @param value
     * @return this, for chaining
     */
    public UnicodeMap putAll(int startCodePoint, int endCodePoint, Object value) {
        if (startCodePoint < 0 || endCodePoint > 0x10FFFF) {
            throw new IllegalArgumentException("Codepoint out of range: "
             + Utility.hex(startCodePoint) + ".." + Utility.hex(endCodePoint));
        }
        // TODO optimize
        for (int i = startCodePoint; i <= endCodePoint; ++i) {
            _put(i, value);
        }
        return this;
    }
    /**
     * Add all the (main) values from a Unicode property
     * @param prop
     * @return
     */
    public UnicodeMap putAll(UnicodeProperty prop) {
        // TODO optimize
        for (int i = 0; i <= 0x10FFFF; ++i) {
            _put(i, prop.getValue(i));
        }
        return this;
    }
    
    /**
     * Set the currently unmapped Unicode code points to the given value.
     * @param value
     * @return
     */
    public UnicodeMap setMissing(Object value) {
        for (int i = 0; i < length; ++i) {
            if (values[i] == null) values[i] = value;
        }
        return this;
    }
    /**
     * Returns the set associated with a given value. Deposits into
     * result if it is not null. Remember to clear if you just want
     * the new values.
     * @param value
     * @param result
     * @return result
     */
    public UnicodeSet getSet(Object value, UnicodeSet result) {
        if (result == null) result = new UnicodeSet();
        for (int i = 0; i < length; ++i) {
            if (values[i] == value) result.add(transitions[i], transitions[i+1]);
        }
        return result;
    }
    /**
     * Returns the list of possible values. Deposits into
     * result if it is not null. Remember to clear if you just want
     * @param result
     * @return
     */
    public Collection getAvailableValues(Collection result) {
        if (result == null) result = new HashSet();
         for (int i = 0; i < length; ++i) {
            Object value = values[i];
            if (value == null) continue;
            if (result.contains(value)) continue;
            result.add(value);
        }
        return result;
    }
    /**
     * Gets the value associated with a given code point.
     * Returns null, if there is no such value.
     * @param codepoint
     * @return
     */
    public Object getValue(int codepoint) {
        if (codepoint < 0 || codepoint > 0x10FFFF) {
            throw new IllegalArgumentException("Codepoint out of range: " + codepoint);
        }
        return values[findIndex(codepoint)];
    }
    
    public String toString() {
        StringBuffer result = new StringBuffer();       
        for (int i = 0; i < length-1; ++i) {
            Object value = values[i];
            if (value == null) continue;
            int start = transitions[i];
            int end = transitions[i+1]-1;
            result.append(Utility.hex(start));
            if (start != end) result.append("..")
            .append(Utility.hex(end));
            result.append("\t=>")
            .append(values[i] == null ? "null" : values[i].toString())
            .append("\r\n");
        }
        return result.toString();
    }
}