/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package dev.latvian.mods.rhino;

import dev.latvian.mods.rhino.classdata.ConstructorInfo;
import dev.latvian.mods.rhino.classdata.MethodSignature;
import dev.latvian.mods.rhino.js.NumberJS;

import java.lang.reflect.Array;
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
	static final String javaClassPropertyName = "__javaObject__";

	public NativeJavaClass(Context cx, Scriptable scope, Class<?> cl) {
		super(cx, scope, cl, null);
	}

	@Override
	public String getClassName() {
		return "JavaClass";
	}

	@Override
	public boolean has(Context cx, String name, Scriptable start) {
		return classData.getMember(name, true) != null || javaClassPropertyName.equals(name);
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

		var m = classData.getMember(name, true);

		if (m != null) {
			return m.actuallyGet(cx, start, null, null);
		}

		Scriptable scope = ScriptableObject.getTopLevelScope(start);
		WrapFactory wrapFactory = cx.getWrapFactory();

		if (javaClassPropertyName.equals(name)) {
			return wrapFactory.wrap(cx, scope, javaObject, ScriptRuntime.ClassClass);
		}

		// experimental:  look for nested classes by appending $name to
		// current class' name.
		Class<?> nestedClass = findNestedClass(getClassObject(), name);
		if (nestedClass != null) {
			Scriptable nestedValue = wrapFactory.wrapJavaClass(cx, scope, nestedClass);
			nestedValue.setParentScope(this);
			return nestedValue;
		}

		throw classData.reportMemberNotFound(cx, name);
	}

	@Override
	public void put(Context cx, String name, Scriptable start, Object value) {
		var m = classData.getMember(name, true);

		if (m != null) {
			m.actuallySet(cx, start, null, value);
		}
	}

	@Override
	public Object[] getIds(Context cx) {
		return classData.getMembers(true).keySet().toArray();
	}

	public Class<?> getClassObject() {
		return (Class<?>) super.unwrap();
	}

	@Override
	public Object getDefaultValue(Context cx, Class<?> hint) {
		if (hint == null || hint == ScriptRuntime.StringClass) {
			return this.toString();
		}
		if (hint == ScriptRuntime.BooleanClass) {
			return Boolean.TRUE;
		}
		if (hint == ScriptRuntime.NumberClass) {
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
			Class<?> c = getClassObject();
			do {
				if (p instanceof Wrapper) {
					Object o = ((Wrapper) p).unwrap();
					if (c.isInstance(o)) {
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
		Class<?> classObject = getClassObject();
		int modifiers = classObject.getModifiers();
		if (!(Modifier.isInterface(modifiers) || Modifier.isAbstract(modifiers))) {
			var cp = classData.constructor(cx.getSharedData(scope), args, MethodSignature.ofArgs(args));

			if (cp.isSet()) {
				// Found the constructor, so try invoking it.
				return constructSpecific(cx, scope, args, cp.get());
			} else {
				String sig = MethodSignature.scriptSignature(args);
				throw Context.reportRuntimeError2(cx, "msg.no.java.ctor", classObject.getName(), sig);
			}
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
		throw Context.reportRuntimeError2(cx, "msg.cant.instantiate", msg, classObject.getName());
	}

	static Scriptable constructSpecific(Context cx, Scriptable scope, Object[] args, ConstructorInfo ctor) {
		Object instance = constructInternal(cx, scope, args, ctor);
		// we need to force this to be wrapped, because construct _has_
		// to return a scriptable
		Scriptable topLevel = ScriptableObject.getTopLevelScope(scope);
		return cx.getWrapFactory().wrapNewObject(cx, topLevel, instance);
	}

	static Object constructInternal(Context cx, Scriptable scope, Object[] args, ConstructorInfo ctor) {
		Class<?>[] argTypes = ctor.signature.types;

		if (ctor.isVarArgs()) {
			// marshall the explicit parameter
			Object[] newArgs = new Object[argTypes.length];
			for (int i = 0; i < argTypes.length - 1; i++) {
				newArgs[i] = Context.jsToJava(cx, args[i], argTypes[i]);
			}

			Object varArgs;

			// Handle special situation where a single variable parameter
			// is given and it is a Java or ECMA array.
			if (args.length == argTypes.length && (args[args.length - 1] == null || args[args.length - 1] instanceof NativeJavaList)) {
				// convert the ECMA array into a native array
				varArgs = Context.jsToJava(cx, args[args.length - 1], argTypes[argTypes.length - 1]);
			} else {
				// marshall the variable parameter
				Class<?> componentType = argTypes[argTypes.length - 1].getComponentType();
				varArgs = Array.newInstance(componentType, args.length - argTypes.length + 1);
				for (int i = 0; i < Array.getLength(varArgs); i++) {
					Object value = Context.jsToJava(cx, args[argTypes.length - 1 + i], componentType);
					Array.set(varArgs, i, value);
				}
			}

			// add varargs
			newArgs[argTypes.length - 1] = varArgs;
			// replace the original args with the new one
			args = newArgs;
		} else {
			Object[] origArgs = args;
			for (int i = 0; i < args.length; i++) {
				Object arg = args[i];
				Object x = Context.jsToJava(cx, arg, argTypes[i]);
				if (x != arg) {
					if (args == origArgs) {
						args = origArgs.clone();
					}
					args[i] = x;
				}
			}
		}

		try {
			return ctor.newInstance(args);
		} catch (Exception ex) {
			throw Context.throwAsScriptRuntimeEx(ex);
		}
	}

	@Override
	public String toString() {
		return "[JavaClass " + getClassObject().getName() + "]";
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
	public boolean hasInstance(Context cx, Scriptable value) {

		if (value instanceof Wrapper && !(value instanceof NativeJavaClass)) {
			Object instance = ((Wrapper) value).unwrap();

			return getClassObject().isInstance(instance);
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
