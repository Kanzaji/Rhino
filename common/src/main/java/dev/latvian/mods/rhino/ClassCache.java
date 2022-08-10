/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package dev.latvian.mods.rhino;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Cache of generated classes and data structures to access Java runtime
 * from JavaScript.
 *
 * @author Igor Bukanov
 * @since Rhino 1.5 Release 5
 */
public class ClassCache {
	private static final Object AKEY = "ClassCache";
	private transient Map<JavaAdapter.JavaAdapterSignature, Class<?>> classAdapterCache;
	private transient Map<Class<?>, Object> interfaceAdapterCache;
	private int generatedClassSerial;
	private Scriptable associatedScope;

	/**
	 * Search for ClassCache object in the given scope.
	 * The method first calls
	 * {@link ScriptableObject#getTopLevelScope(Scriptable scope)}
	 * to get the top most scope and then tries to locate associated
	 * ClassCache object in the prototype chain of the top scope.
	 *
	 * @param scope scope to search for ClassCache object.
	 * @return previously associated ClassCache object or a new instance of
	 * ClassCache if no ClassCache object was found.
	 * @see #associate(ScriptableObject topScope)
	 */
	public static ClassCache get(Context cx, Scriptable scope) {
		ClassCache cache = (ClassCache) ScriptableObject.getTopScopeValue(cx, scope, AKEY);
		if (cache == null) {
			throw new RuntimeException("Can't find top level scope for " + "ClassCache.get");
		}
		return cache;
	}

	/**
	 * Associate ClassCache object with the given top-level scope.
	 * The ClassCache object can only be associated with the given scope once.
	 *
	 * @param topScope scope to associate this ClassCache object with.
	 * @return true if no previous ClassCache objects were embedded into
	 * the scope and this ClassCache were successfully associated
	 * or false otherwise.
	 * @see #get(Context, Scriptable scope)
	 */
	public boolean associate(ScriptableObject topScope) {
		if (topScope.getParentScope() != null) {
			// Can only associate cache with top level scope
			throw new IllegalArgumentException();
		}
		if (this == topScope.associateValue(AKEY, this)) {
			associatedScope = topScope;
			return true;
		}
		return false;
	}

	Map<JavaAdapter.JavaAdapterSignature, Class<?>> getInterfaceAdapterCacheMap() {
		if (classAdapterCache == null) {
			classAdapterCache = new ConcurrentHashMap<>(16, 0.75f, 1);
		}
		return classAdapterCache;
	}

	public final synchronized int newClassSerialNumber() {
		return ++generatedClassSerial;
	}

	Object getInterfaceAdapter(Class<?> cl) {
		return interfaceAdapterCache == null ? null : interfaceAdapterCache.get(cl);
	}

	synchronized void cacheInterfaceAdapter(Class<?> cl, Object iadapter) {
		if (interfaceAdapterCache == null) {
			interfaceAdapterCache = new ConcurrentHashMap<>(16, 0.75f, 1);
		}
		interfaceAdapterCache.put(cl, iadapter);
	}

	Scriptable getAssociatedScope() {
		return associatedScope;
	}
}
