/*
 * Copyright (c) 2018, 2022, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * The Universal Permissive License (UPL), Version 1.0
 *
 * Subject to the condition set forth below, permission is hereby granted to any
 * person obtaining a copy of this software, associated documentation and/or
 * data (collectively the "Software"), free of charge and under any and all
 * copyright rights in the Software, and any and all patent rights owned or
 * freely licensable by each licensor hereunder covering either (i) the
 * unmodified Software as contributed to or provided by such licensor, or (ii)
 * the Larger Works (as defined below), to deal in both
 *
 * (a) the Software, and
 *
 * (b) any piece of software and/or hardware listed in the lrgrwrks.txt file if
 * one is included with the Software each a "Larger Work" to which the Software
 * is contributed by such licensors),
 *
 * without restriction, including without limitation the rights to copy, create
 * derivative works of, display, perform, and distribute the Software and make,
 * use, sell, offer for sale, import, export, have made, and have sold the
 * Software and the Larger Work(s), and to sublicense the foregoing rights on
 * either these or other terms.
 *
 * This license is subject to the following condition:
 *
 * The above copyright notice and either this complete permission notice or at a
 * minimum a reference to the UPL must be included in all copies or substantial
 * portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.oracle.truffle.js.nodes.access;

import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.runtime.BigInt;
import com.oracle.truffle.js.runtime.JSConfig;
import com.oracle.truffle.js.runtime.SafeInteger;
import com.oracle.truffle.js.runtime.Symbol;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;

/**
 * Checks whether the argument is of type Object (JS or foreign), i.e., not a primitive value.
 */
@GenerateUncached
@ImportStatic({JSConfig.class})
public abstract class IsObjectNode extends JavaScriptBaseNode {

    public abstract boolean executeBoolean(Object operand);

    @Specialization(guards = {"isJSNull(operand)"})
    protected static boolean doNull(@SuppressWarnings("unused") Object operand) {
        return false;
    }

    @Specialization(guards = {"isUndefined(operand)"})
    protected static boolean doUndefined(@SuppressWarnings("unused") Object operand) {
        return false;
    }

    @Specialization
    protected static boolean doBoolean(@SuppressWarnings("unused") boolean operand) {
        return false;
    }

    @Specialization
    protected static boolean doInt(@SuppressWarnings("unused") int operand) {
        return false;
    }

    @Specialization
    protected static boolean doLong(@SuppressWarnings("unused") long operand) {
        return false;
    }

    @Specialization
    protected static boolean doLargeInt(@SuppressWarnings("unused") SafeInteger operand) {
        return false;
    }

    @Specialization
    protected static boolean doDouble(@SuppressWarnings("unused") double operand) {
        return false;
    }

    @Specialization
    protected static boolean doSymbol(@SuppressWarnings("unused") Symbol operand) {
        return false;
    }

    @Specialization
    protected static boolean doBigInt(@SuppressWarnings("unused") BigInt operand) {
        return false;
    }

    @Specialization
    protected static boolean doTString(@SuppressWarnings("unused") TruffleString operand) {
        return false;
    }

    @SuppressWarnings("unused")
    @Specialization(guards = {"isJSObject(operand)"})
    protected static boolean doIsObject(JSDynamicObject operand) {
        return true;
    }

    @Specialization(guards = {"isForeignObject(operand)"}, limit = "InteropLibraryLimit")
    protected static boolean doForeignObject(Object operand,
                    @CachedLibrary("operand") InteropLibrary interop) {
        if (interop.isNull(operand)) {
            return false;
        } else if (interop.isBoolean(operand)) {
            return false;
        } else if (interop.isString(operand)) {
            return false;
        } else if (interop.isNumber(operand)) {
            return false;
        }
        return true;
    }

    public static IsObjectNode create() {
        return IsObjectNodeGen.create();
    }

    public static IsObjectNode getUncached() {
        return IsObjectNodeGen.getUncached();
    }
}
