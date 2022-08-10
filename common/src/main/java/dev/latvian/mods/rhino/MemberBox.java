/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package dev.latvian.mods.rhino;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 * Wrapper class for Method and Constructor instances to cache
 * getParameterTypes() results, recover from IllegalAccessException
 * in some cases and provide serialization support.
 *
 * @author Igor Bukanov
 */

final class MemberBox {
	private transient Member memberObject;
	transient Class<?>[] argTypes;
	transient Object delegateTo;
	transient boolean vararg;

	Method method() {
		return (Method) memberObject;
	}

	boolean isMethod() {
		return memberObject instanceof Method;
	}

	boolean isStatic() {
		return Modifier.isStatic(memberObject.getModifiers());
	}

	String getName() {
		return memberObject.getName();
	}

	Class<?> getDeclaringClass() {
		return memberObject.getDeclaringClass();
	}

	@Override
	public String toString() {
		return memberObject.toString();
	}

	Object invoke(Object target, Object[] args) {
		Method method = method();
		try {
			return method.invoke(target, args);
		} catch (InvocationTargetException ite) {
			// Must allow ContinuationPending exceptions to propagate unhindered
			Throwable e = ite;
			do {
				e = ((InvocationTargetException) e).getTargetException();
			} while ((e instanceof InvocationTargetException));
			throw Context.throwAsScriptRuntimeEx(e);
		} catch (Exception ex) {
			throw Context.throwAsScriptRuntimeEx(ex);
		}
	}
}

