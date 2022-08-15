/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package dev.latvian.mods.rhino;

import dev.latvian.mods.rhino.classdata.MethodSignature;
import dev.latvian.mods.rhino.js.NumberJS;
import dev.latvian.mods.rhino.js.TypeJS;
import dev.latvian.mods.rhino.js.UndefinedJS;
import dev.latvian.mods.rhino.js.prototype.CastType;
import dev.latvian.mods.rhino.js.prototype.PrototypeJS;

import java.lang.reflect.Modifier;

/**
 * This class reflects Java classes into the JavaScript environment, mainly
 * for constructors and static members.  We lazily reflect properties,
 * and currently do not guarantee that a single j.l.Class is only
 * reflected once into the JS environment, although we should.
 * The only known case where multiple reflections
 * are possible occurs when a j.l.Class is wrapped as part of a
 * method return or property access, rather than by walking the
 * Packages/java tree.
 *
 * @author Mike Shaver
 * @see NativeJavaObject
 */

public class NativeJavaClass extends NativeJavaObject implements Function {
	// Special property for getting the underlying Java class object.

	public NativeJavaClass(Scriptable scope, Class<?> cl) {
		super(scope, cl, cl);
	}

	@Override
	public String getClassName() {
		return "JavaClass";
	}

	@Override
	public boolean has(Context cx, String name, Scriptable start) {
		return getPrototypeJS(new ContextJS(cx, start)).hasValue(new ContextJS(cx, start), null, name);
	}

	@Override
	public Object get(Context cx, String name, Scriptable start) {
		// When used as a constructor, ScriptRuntime.newObject() asks
		// for our prototype to create an object of the correct type.
		// We don't really care what the object is, since we're returning
		// one constructed out of whole cloth, so we return null.
		if (name.equals("prototype")) {
			return null;
		}

		ContextJS cxjs = new ContextJS(cx, start);
		PrototypeJS p = getPrototypeJS(cxjs);

		Object value = p.getValue(cxjs, null, name, CastType.WRAP);

		if (value != UndefinedJS.PROTOTYPE) {
			return value;

			/*
			Scriptable scope = ScriptableObject.getTopLevelScope(start);
			WrapFactory wrapFactory = cx.getWrapFactory();

			// experimental:  look for nested classes by appending $name to
			// current class' name.
			Class<?> nestedClass = findNestedClass(getClassObject(), name);
			if (nestedClass != null) {
				Scriptable nestedValue = wrapFactory.wrapJavaClass(cx, scope, nestedClass);
				nestedValue.setParentScope(this);
				return nestedValue;
			}
			 */
		}

		return Scriptable.NOT_FOUND;
	}

	@Override
	public void put(Context cx, String name, Scriptable start, Object value) {
		var cxjs = new ContextJS(cx, start);
		getPrototypeJS(cxjs).setValue(cxjs, null, name, value, CastType.UNWRAP);
	}

	@Override
	public Object getDefaultValue(ContextJS cx, TypeJS hint) {
		if (hint == null || hint == TypeJS.STRING) {
			return staticType.getName();
		}
		if (hint == TypeJS.BOOLEAN) {
			return Boolean.TRUE;
		}
		if (hint == TypeJS.NUMBER) {
			return NumberJS.NaN;
		}
		return this;
	}

	@Override
	public Object call(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
		// If it looks like a "cast" of an object to this class type,
		// walk the prototype chain to see if there's a wrapper of a
		// object that's an instanceof this class.
		if (args.length == 1 && args[0] instanceof Scriptable p) {
			do {
				if (p instanceof Wrapper) {
					Object o = ((Wrapper) p).unwrap();
					if (staticType.isInstance(o)) {
						return p;
					}
				}
				p = p.getPrototype(cx);
			} while (p != null);
		}
		return construct(cx, scope, args);
	}

	@Override
	public Scriptable construct(Context cx, Scriptable scope, Object[] args) {
		int modifiers = staticType.getModifiers();
		if (!(Modifier.isInterface(modifiers) || Modifier.isAbstract(modifiers))) {
			var cxjs = new ContextJS(cx, scope);
			var cons = getPrototypeJS(cxjs).getConstructor();

			if (cons != null) {
				var inst = cons.invoke(cxjs, null, "<init>", args, CastType.WRAP);

				if (inst instanceof Scriptable s) {
					return s;
				}
			}

			String sig = MethodSignature.scriptSignature(args);
			throw Context.reportRuntimeError2(cx, "msg.no.java.ctor", staticType.getName(), sig);
		}
		if (args.length == 0) {
			throw Context.reportRuntimeError0(cx, "msg.adapter.zero.args");
		}
		Scriptable topLevel = ScriptableObject.getTopLevelScope(this);
		String msg = "";
		try {
			// use JavaAdapter to construct a new class on the fly that
			// implements/extends this interface/abstract class.
			Object v = topLevel.get(cx, "JavaAdapter", topLevel);
			if (v != NOT_FOUND) {
				Function f = (Function) v;
				// Args are (interface, js object)
				Object[] adapterArgs = {this, args[0]};
				return f.construct(cx, topLevel, adapterArgs);
			}
		} catch (Exception ex) {
			// fall through to error
			String m = ex.getMessage();
			if (m != null) {
				msg = m;
			}
		}
		throw Context.reportRuntimeError2(cx, "msg.cant.instantiate", msg, staticType.getName());
	}

	@Override
	public String toString() {
		return "[JavaClass " + staticType.getName() + "]";
	}

	/**
	 * Determines if prototype is a wrapped Java object and performs
	 * a Java "instanceof".
	 * Exception: if value is an instance of NativeJavaClass, it isn't
	 * considered an instance of the Java class; this forestalls any
	 * name conflicts between java.lang.Class's methods and the
	 * static methods exposed by a JavaNativeClass.
	 */
	@Override
	public boolean hasInstance(Context cx, Scriptable lhsScriptable) {

		if (lhsScriptable instanceof Wrapper && !(lhsScriptable instanceof NativeJavaClass)) {
			Object instance = ((Wrapper) lhsScriptable).unwrap();

			return staticType.isInstance(instance);
		}

		// value wasn't something we understand
		return false;
	}

	private static Class<?> findNestedClass(Class<?> parentClass, String name) {
		String nestedClassName = parentClass.getName() + '$' + name;
		ClassLoader loader = parentClass.getClassLoader();
		if (loader == null) {
			// ALERT: if loader is null, nested class should be loaded
			// via system class loader which can be different from the
			// loader that brought Rhino classes that Class.forName() would
			// use, but ClassLoader.getSystemClassLoader() is Java 2 only
			return Kit.classOrNull(nestedClassName);
		}
		return Kit.classOrNull(loader, nestedClassName);
	}
}
