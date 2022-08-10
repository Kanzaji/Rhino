/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

// API class

package dev.latvian.mods.rhino;

/**
 * Java reflection of JavaScript exceptions.
 * Instances of this class are thrown by the JavaScript 'throw' keyword.
 *
 * @author Mike McCabe
 */
public class JavaScriptException extends RhinoException {
	/**
	 * Create a JavaScript exception wrapping the given JavaScript value
	 *
	 * @param value the JavaScript value thrown.
	 */
	public JavaScriptException(Context cx, Object value, String sourceName, int lineNumber) {
		recordErrorOrigin(sourceName, lineNumber, null, 0);
		this.value = value;
		// Fill in fileName and lineNumber automatically when not specified
		// explicitly, see Bugzilla issue #342807
		if (value instanceof NativeError error) {
			if (!error.has(cx, "fileName", error)) {
				error.put(cx, "fileName", error, sourceName);
			}
			if (!error.has(cx, "lineNumber", error)) {
				error.put(cx, "lineNumber", error, lineNumber);
			}
			// set stack property, see bug #549604
			error.setStackProvider(cx, this);
		}
	}

	@Override
	public String details(Context cx) {
		if (value == null) {
			return "null";
		} else if (value instanceof NativeError) {
			return value.toString();
		}
		try {
			return ScriptRuntime.toString(cx, value);
		} catch (RuntimeException rte) {
			// ScriptRuntime.toString may throw a RuntimeException
			if (value instanceof Scriptable) {
				return ScriptRuntime.defaultObjectToString((Scriptable) value);
			}
			return value.toString();
		}
	}

	/**
	 * @return the value wrapped by this exception
	 */
	public Object getValue() {
		return value;
	}

	private final Object value;
}
