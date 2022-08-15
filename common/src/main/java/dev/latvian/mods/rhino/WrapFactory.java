/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

// API class

package dev.latvian.mods.rhino;

import dev.latvian.mods.rhino.util.CustomJavaToJsWrapper;
import dev.latvian.mods.rhino.util.JavaSetWrapper;
import dev.latvian.mods.rhino.util.NativeArrayWrapper;

import java.util.List;
import java.util.Set;

/**
 * Embeddings that wish to provide their own custom wrappings for Java
 * objects may extend this class and call
 * {@link Context#setWrapFactory(WrapFactory)}
 * Once an instance of this class or an extension of this class is enabled
 * for a given context (by calling setWrapFactory on that context), Rhino
 * will call the methods of this class whenever it needs to wrap a value
 * resulting from a call to a Java method or an access to a Java field.
 *
 * @see Context#setWrapFactory(WrapFactory)
 * @since 1.5 Release 4
 */
public class WrapFactory {
	/**
	 * Wrap the object.
	 * <p>
	 * The value returned must be one of
	 * <UL>
	 * <LI>java.lang.Boolean</LI>
	 * <LI>java.lang.String</LI>
	 * <LI>java.lang.Number</LI>
	 * <LI>org.mozilla.javascript.Scriptable objects</LI>
	 * <LI>The value returned by Context.getUndefinedValue()</LI>
	 * <LI>null</LI>
	 * </UL>
	 *
	 * @param cx         the current Context for this thread
	 * @param scope      the scope of the executing script
	 * @param obj        the object to be wrapped. Note it can be null.
	 * @param staticType type hint. If security restrictions prevent to wrap
	 *                   object based on its class, staticType will be used instead.
	 * @return the wrapped value.
	 */
	public Object wrap(Context cx, Scriptable scope, Object obj, Class<?> staticType) {
		if (obj == null || obj == Undefined.instance || obj instanceof Scriptable) {
			return obj;
		} else if (staticType == Void.TYPE) {
			return Undefined.instance;
		} else if (staticType == Character.TYPE) {
			return (int) (Character) obj;
		} else if (staticType != null && staticType.isPrimitive()) {
			return obj;
		}

		Class<?> cls = obj.getClass();

		if (cls.isArray()) {
			return new NativeJavaList(scope, NativeArrayWrapper.of(obj));
		}

		return wrapAsJavaObject(cx, scope, obj, staticType);
	}

	/**
	 * Wrap Java object as Scriptable instance to allow full access to its
	 * methods and fields from JavaScript.
	 * <p>
	 * {@link #wrap(Context, Scriptable, Object, Class)} call this method
	 * when they can not convert <code>javaObject</code> to JavaScript primitive
	 * value or JavaScript array.
	 * <p>
	 * Subclasses can override the method to provide custom wrappers
	 * for Java objects.
	 *
	 * @param cx         the current Context for this thread
	 * @param scope      the scope of the executing script
	 * @param javaObject the object to be wrapped
	 * @param staticType type hint. If security restrictions prevent to wrap
	 *                   object based on its class, staticType will be used instead.
	 * @return the wrapped value which shall not be null
	 */
	public Scriptable wrapAsJavaObject(Context cx, Scriptable scope, Object javaObject, Class<?> staticType) {
		if (javaObject instanceof CustomJavaToJsWrapper w) {
			return w.convertJavaToJs(cx, scope, staticType);
		}

		CustomJavaToJsWrapper w = cx.getSharedData(scope).wrapCustomJavaToJs(javaObject);

		if (w != null) {
			return w.convertJavaToJs(cx, scope, staticType);
		}

		if (javaObject instanceof List list) {
			return new NativeJavaList(scope, list);
		} else if (javaObject instanceof Set<?> set) {
			return new NativeJavaList(scope, new JavaSetWrapper<>(set));
		}

		// TODO: Wrap Gson

		return new NativeJavaObject(scope, javaObject, javaObject.getClass());
	}
}
