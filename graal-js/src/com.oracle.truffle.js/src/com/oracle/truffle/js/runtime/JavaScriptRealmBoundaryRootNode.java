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
package com.oracle.truffle.js.runtime;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.js.lang.JavaScriptLanguage;
import com.oracle.truffle.js.runtime.builtins.JSFunction;

/**
 * A RootNode for all cases where the body could throw and we would need to set the Realm to the
 * exception.
 *
 */
public abstract class JavaScriptRealmBoundaryRootNode extends JavaScriptRootNode {

    protected JavaScriptRealmBoundaryRootNode(JavaScriptLanguage lang, SourceSection sourceSection, FrameDescriptor frameDescriptor) {
        super(lang, sourceSection, frameDescriptor);
    }

    @Override
    public final Object execute(VirtualFrame frame) {
        final JSContext context = getLanguage().getJSContext();
        CompilerAsserts.partialEvaluationConstant(context);

        JSRealm functionRealm = null;
        final boolean enterRealm;
        if (context.neverCreatedChildRealms()) {
            // fast path: if there are no child realms we are guaranteed to be in the right realm
            assert getRealm() == JSFunction.getRealm(JSFrameUtil.getFunctionObject(frame));
            enterRealm = false;
        } else {
            // must enter function context if functionRealm != currentRealm
            functionRealm = JSFunction.getRealm(JSFrameUtil.getFunctionObject(frame));
            JSRealm currentRealm = getRealm();
            enterRealm = functionRealm != currentRealm;
        }

        JSRealm prevRealm = null;
        JSRealm mainRealm = null;
        if (enterRealm) {
            mainRealm = JSRealm.getMain(this);
            prevRealm = mainRealm.enterRealm(this, functionRealm);
        }

        try {
            return executeInRealm(frame);
        } catch (StackOverflowError ex) {
            CompilerDirectives.transferToInterpreter();
            throw Errors.createRangeErrorStackOverflow(ex, this);
        } finally {
            if (enterRealm) {
                mainRealm.leaveRealm(this, prevRealm);
            }
        }
    }

    protected abstract Object executeInRealm(VirtualFrame frame);

}
