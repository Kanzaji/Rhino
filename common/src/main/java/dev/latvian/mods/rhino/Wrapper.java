/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

// API class

package dev.latvian.mods.rhino;

/**
 * Objects that can wrap other values for reflection in the JS environment
 * will implement Wrapper.
 * <p>
 * Wrapper defines a single method that can be called to unwrap the object.
 */

public interface Wrapper {

	/**
	 * Unwrap the object by returning the wrapped value.
	 *
	 * @return a wrapped value
	 */
	Object unwrap();

	static Object unwrapped(Object o) {
		Object o1 = o;

		while (o1 instanceof Wrapper w) {
			o1 = w.unwrap();
		}

		return o1;
	}

	static Object[] unwrappedArray(Object[] args) {
		Object[] original = args;

		for (int i = 0; i < args.length; i++) {
			Object o = unwrapped(args[i]);

			if (o != args[i]) {
				if (original == args) {
					args = args.clone();
				}

				args[i] = o;
			}
		}

		return args;
	}
}
