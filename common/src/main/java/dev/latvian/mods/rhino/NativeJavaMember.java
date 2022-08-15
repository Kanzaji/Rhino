/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package dev.latvian.mods.rhino;

import dev.latvian.mods.rhino.js.TypeJS;
import dev.latvian.mods.rhino.js.prototype.CastType;
import dev.latvian.mods.rhino.js.prototype.PrototypeJS;

/**
 * This class reflects Java methods into the JavaScript environment and
 * handles overloading of methods.
 *
 * @author Mike Shaver
 * @see NativeJavaClass
 */

public class NativeJavaMember extends BaseFunction {
	public final PrototypeJS prototypeJS;
	public final Object selfObject;
	public final String memberKey;

	public NativeJavaMember(PrototypeJS prototypeJS, Object selfObject, String memberKey) {
		this.prototypeJS = prototypeJS;
		this.selfObject = selfObject;
		this.memberKey = memberKey;
	}

	@Override
	public String getFunctionName() {
		return String.valueOf(memberKey);
	}

	@Override
	public String toString() {
		return "[JavaMember " + memberKey + " of " + prototypeJS + "]";
	}

	@Override
	public Object call(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
		var cxjs = new ContextJS(cx, scope);
		return prototypeJS.invoke(cxjs, selfObject, memberKey, args, CastType.WRAP);
	}

	@Override
	public Object getDefaultValue(ContextJS cx, TypeJS hint) {
		if (hint == TypeJS.FUNCTION) {
			return this;
		}

		Object rval = prototypeJS.getValue(cx, selfObject, memberKey, CastType.UNWRAP);
		// Class<?> type = member.getType();
		// rval = cx.getWrapFactory().wrap(cx, this, rval, type);

		if (rval == this) {
			throw new RuntimeException("wtf");
		}

		if (rval instanceof Scriptable s) {
			rval = s.getDefaultValue(cx, hint);
		}

		if (hint == TypeJS.STRING) {
			return String.valueOf(rval);
		} else if (hint == TypeJS.NUMBER) {
			return 1;
		}

		return rval;
	}
}

