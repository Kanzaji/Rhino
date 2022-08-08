/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package dev.latvian.mods.rhino;

import java.util.List;

public final class NativeArrayIterator extends ES6Iterator {
	public enum ArrayIteratorType {
		ENTRIES, KEYS, VALUES
	}

	private static final String ITERATOR_TAG = "ArrayIterator";

	private ArrayIteratorType type;

	static void init(Context cx, ScriptableObject scope, boolean sealed) {
		init(cx, scope, sealed, new NativeArrayIterator(), ITERATOR_TAG);
	}

	/**
	 * Only for constructing the prototype object.
	 */
	private NativeArrayIterator() {
		super();
	}

	public NativeArrayIterator(Context cx, Scriptable scope, Scriptable arrayLike, ArrayIteratorType type) {
		super(cx, scope, ITERATOR_TAG);
		this.index = 0;
		this.arrayLike = arrayLike;
		this.type = type;
	}

	@Override
	public String getClassName() {
		return "Array Iterator";
	}

	@Override
	protected boolean isDone(Context cx, Scriptable scope) {
		return index >= ScriptRuntime.getLengthProperty(cx, arrayLike, false);
	}

	@Override
	protected Object nextValue(Context cx, Scriptable scope) {
		if (type == ArrayIteratorType.KEYS) {
			return index++;
		}

		Object value = arrayLike.get(cx, index, arrayLike);
		if (value == NOT_FOUND) {
			value = Undefined.instance;
		}

		if (type == ArrayIteratorType.ENTRIES) {
			value = cx.newArray(scope, List.of(index, value));
		}

		index++;
		return value;
	}

	@Override
	protected String getTag() {
		return ITERATOR_TAG;
	}

	private Scriptable arrayLike;
	private int index;
}

