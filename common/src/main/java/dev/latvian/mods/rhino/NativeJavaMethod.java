/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package dev.latvian.mods.rhino;

import dev.latvian.mods.rhino.classdata.BaseMember;
import org.jetbrains.annotations.Nullable;

/**
 * This class reflects Java methods into the JavaScript environment and
 * handles overloading of methods.
 *
 * @author Mike Shaver
 * @see NativeJavaClass
 */

public class NativeJavaMethod extends BaseFunction {
	public final BaseMember member;
	private final Object selfObject;
	private final String functionName;

	public NativeJavaMethod(BaseMember member, @Nullable Object selfObject, String functionName) {
		this.member = member;
		this.selfObject = selfObject;
		this.functionName = functionName;
	}

	@Override
	public String getFunctionName() {
		return functionName;
	}

	@Override
	public String toString() {
		return "[JavaMethod " + member + "]";
	}

	@Override
	public Object call(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
		Object retval = member.invokeJS(new ContextJS(cx, scope), selfObject, args);
		Class<?> staticType = member.getType();

		Object wrapped = cx.getWrapFactory().wrap(cx, scope, retval, staticType);

		if (wrapped == null && staticType == Void.TYPE) {
			wrapped = Undefined.instance;
		}

		return wrapped;
	}

	@Override
	public Object getDefaultValue(Context cx, Class<?> hint) {
		if (hint == ScriptRuntime.FunctionClass) {
			return this;
		}

		Object rval = member.getJS(new ContextJS(cx), selfObject);
		Class<?> type = member.getType();
		rval = cx.getWrapFactory().wrap(cx, this, rval, type);

		if (rval == this) {
			throw new RuntimeException("wtf");
		}

		if (hint == ScriptRuntime.StringClass) {
			return String.valueOf(rval);
		}

		if (rval instanceof Scriptable s) {
			// rval = s.getDefaultValue(cx, hint);
		}

		return rval;
	}
}

