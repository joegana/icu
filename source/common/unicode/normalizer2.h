/*
*******************************************************************************
*
*   Copyright (C) 2009, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
*******************************************************************************
*   file name:  normalizer2.h
*   encoding:   US-ASCII
*   tab size:   8 (not used)
*   indentation:4
*
*   created on: 2009nov22
*   created by: Markus W. Scherer
*/

#ifndef __NORMALIZER2_H__
#define __NORMALIZER2_H__

#include "unicode/utypes.h"

// TODO: add to list of C++ header files

#if !UCONFIG_NO_NORMALIZATION

#include "unicode/unistr.h"
#include "unicode/unorm.h"

// TODO: Copy \file comments from unorm.h and/or normlzr.h.
// TODO: Move UNORM2_ enums to new unormalizer2.h.

/**
 * Constants for normalization modes.
 * @draft ICU 4.4
 */
typedef enum {
    /**
     * Decomposition followed by composition.
     * Same as standard NFC when using an "nfc" instance.
     * Same as standard NFKC when using an "nfkc" instance.
     * @draft ICU 4.4
     */
    UNORM2_COMPOSE,
    /**
     * Map, and reorder canonically.
     * Same as standard NFD when using an "nfc" instance.
     * Same as standard NFKD when using an "nfkc" instance.
     * @draft ICU 4.4
     */
    UNORM2_DECOMPOSE,
    /**
     * "Fast C or D" form.
     * Further decomposition <i>without reordering</i>
     * would yield the same form as DECOMPOSE.
     * Not a standard Unicode normalization form.
     * Not a unique form: Different FCD strings can be canonically equivalent.
     * For details see http://www.unicode.org/notes/tn5/#FCD
     * @draft ICU 4.4
     */
    UNORM2_FCD,
    /**
     * Compose only contiguously.
     * Also known as "FCC" or "Fast C Contiguous".
     * The result will often but not always be in NFC.
     * The result will conform to FCD which is useful for processing.
     * Not a standard Unicode normalization form.
     * For details see http://www.unicode.org/notes/tn5/#FCC
     * @draft ICU 4.4
     */
    UNORM2_COMPOSE_CONTIGUOUS,
    /** One more than the highest normalization mode constant. @draft ICU 4.4 */
    // TODO: needed? error checking?? UNORM2_MODE_COUNT
} UNormalization2Mode;

U_NAMESPACE_BEGIN

class U_COMMON_API Normalizer2 : public UObject {
public:
    /**
     * Returns a Normalizer2 instance which uses the specified data file
     * (packageName/name similar to ucnv_openPackage() and ures_open()/ResourceBundle)
     * and which composes or decomposes text according to the specified mode.
     * Returns an unmodifiable singleton instance.
     *
     * Use packageName=NULL for data files that are part of ICU's own data.
     * Use name="nfc" and UNORM2_COMPOSE/UNORM2_DECOMPOSE for Unicode standard NFC/NFD.
     * Use name="nfkc" and UNORM2_COMPOSE/UNORM2_DECOMPOSE for Unicode standard NFKC/NFKD.
     * Use name="nfkc_cf" and UNORM2_COMPOSE for Unicode standard NFKC_CF=NFKC_Casefold.
     *
     * @param errorCode Standard ICU error code. Its input value must
     *                  pass the U_SUCCESS() test, or else the function returns
     *                  immediately. Check for U_FAILURE() on output or use with
     *                  function chaining. (See User Guide for details.)
     * @return the requested Normalizer2, if successful
     * @draft ICU 4.4
     */
    static Normalizer2 *
    getInstance(const char *packageName,
                const char *name,
                UNormalization2Mode mode,
                UErrorCode &errorCode);

    /**
     * Returns the normalized form of the source string.
     * @param src source string
     * @param errorCode Standard ICU error code. Its input value must
     *                  pass the U_SUCCESS() test, or else the function returns
     *                  immediately. Check for U_FAILURE() on output or use with
     *                  function chaining. (See User Guide for details.)
     * @return normalized src
     * @draft ICU 4.4
     */
    UnicodeString
    normalize(const UnicodeString &src, UErrorCode &errorCode) const {
        UnicodeString result;
        normalize(src, result, errorCode);
        return result;
    }
    /**
     * Writes the normalized form of the source string to the destination string
     * (replacing its contents) and returns the destination string.
     * @param src source string
     * @param dest destination string; its contents is replaced with normalized src
     * @param errorCode Standard ICU error code. Its input value must
     *                  pass the U_SUCCESS() test, or else the function returns
     *                  immediately. Check for U_FAILURE() on output or use with
     *                  function chaining. (See User Guide for details.)
     * @return dest
     * @draft ICU 4.4
     */
    virtual UnicodeString &
    normalize(const UnicodeString &src,
              UnicodeString &dest,
              UErrorCode &errorCode) const = 0;
    /**
     * Appends the normalized form of the second string to the first string
     * (merging them at the boundary) and returns the first string.
     * The result is normalized if the first string was normalized.
     * @param first string, should be normalized
     * @param second string, will be normalized
     * @param errorCode Standard ICU error code. Its input value must
     *                  pass the U_SUCCESS() test, or else the function returns
     *                  immediately. Check for U_FAILURE() on output or use with
     *                  function chaining. (See User Guide for details.)
     * @return first
     * @draft ICU 4.4
     */
    virtual UnicodeString &
    normalizeSecondAndAppend(UnicodeString &first,
                             const UnicodeString &second,
                             UErrorCode &errorCode) const = 0;
    /**
     * Appends the second string to the first string
     * (merging them at the boundary) and returns the first string.
     * The result is normalized if both the strings were normalized.
     * @param first string, should be normalized
     * @param second string, should be normalized
     * @param errorCode Standard ICU error code. Its input value must
     *                  pass the U_SUCCESS() test, or else the function returns
     *                  immediately. Check for U_FAILURE() on output or use with
     *                  function chaining. (See User Guide for details.)
     * @return first
     * @draft ICU 4.4
     */
    virtual UnicodeString &
    append(UnicodeString &first,
           const UnicodeString &second,
           UErrorCode &errorCode) const = 0;

    /**
     * Tests if the string is normalized.
     * Internally, in cases where the quickCheck() method would return "maybe"
     * (which is only possible for the two COMPOSE modes) this method
     * resolves to "yes" or "no" to provide a definitive result,
     * at the cost of doing more work in those cases.
     * @param s input string
     * @param errorCode Standard ICU error code. Its input value must
     *                  pass the U_SUCCESS() test, or else the function returns
     *                  immediately. Check for U_FAILURE() on output or use with
     *                  function chaining. (See User Guide for details.)
     * @return TRUE if s is normalized
     * @draft ICU 4.4
     */
    virtual UBool
    isNormalized(const UnicodeString &s, UErrorCode &errorCode) const = 0;

    /**
     * Tests if the string is normalized.
     * For the two COMPOSE modes, the result could be "maybe" in cases that
     * would take a little more work to resolve definitively.
     * Use spanQuickCheckYes() and normalizeSecondAndAppend() for a faster
     * combination of quick check + normalization, to avoid
     * re-checking the "yes" prefix.
     * @param s input string
     * @param errorCode Standard ICU error code. Its input value must
     *                  pass the U_SUCCESS() test, or else the function returns
     *                  immediately. Check for U_FAILURE() on output or use with
     *                  function chaining. (See User Guide for details.)
     * @return UNormalizationCheckResult
     * @draft ICU 4.4
     */
    virtual UNormalizationCheckResult
    quickCheck(const UnicodeString &s, UErrorCode &errorCode) const = 0;

    /**
     * Returns the end of the normalized substring of the input string starting
     * at the specified index.
     * In other words, with <code>end=spanQuickCheckYes(s, start, ec);</code>
     * the substring <code>UnicodeString(s, start, end-start)</code>
     * will pass the quick check with a "yes" result.
     *
     * The returned end index is usually one or more characters before the
     * "no" or "maybe" character: The end index is at a normalization boundary,
     * so that the portions of the string before it and after it
     * can be handled independently.
     *
     * When the goal is a normalized string and most input strings are expected
     * to be normalized already, then call this method,
     * and if it returns a prefix shorter than the input string,
     * copy that prefix and use normalizeSecondAndAppend() for the remainder.
     * @param s input string
     * @param start starting index in the string for the quick check
     * @param errorCode Standard ICU error code. Its input value must
     *                  pass the U_SUCCESS() test, or else the function returns
     *                  immediately. Check for U_FAILURE() on output or use with
     *                  function chaining. (See User Guide for details.)
     * @return UNormalizationCheckResult
     * @draft ICU 4.4
     */
    virtual int32_t
    spanQuickCheckYes(const UnicodeString &s, int32_t start, UErrorCode &errorCode) const = 0;

    /**
     * ICU "poor man's RTTI", returns a UClassID for this class.
     * @returns a UClassID for this class.
     * @draft ICU 4.4
     */
    static UClassID U_EXPORT2 getStaticClassID();

    /**
     * ICU "poor man's RTTI", returns a UClassID for the actual class.
     * @return a UClassID for the actual class.
     * @draft ICU 4.4
     */
    virtual UClassID getDynamicClassID() const;

    // TODO: no copy, ==, etc.
};

U_NAMESPACE_END

#endif  // !UCONFIG_NO_NORMALIZATION
#endif  // __NORMALIZER2_H__
