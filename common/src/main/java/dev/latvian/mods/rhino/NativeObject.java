/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package dev.latvian.mods.rhino;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;

/**
 * This class implements the Object native object.
 * See ECMA 15.2.
 *
 * @author Norris Boyd
 */
public class NativeObject extends IdScriptableObject {
	private static final Object OBJECT_TAG = "Object";

	static void init(Context cx, Scriptable scope) {
		NativeObject obj = new NativeObject();
		obj.exportAsJSClass(cx, MAX_PROTOTYPE_ID, scope);
	}

	@Override
	public String getClassName() {
		return "Object";
	}

	/*
	@Override
	public String toString()
	{
		return ScriptRuntime.defaultObjectToString(this);
	}
	*/

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder("{");
		boolean first = true;

		for (Map.Entry<?, ?> entry : Collections.emptyMap().entrySet()) {
			if (first) {
				first = false;
			} else {
				sb.append(", ");
			}

			sb.append(entry.getKey());
			sb.append(": ");
			sb.append(entry.getValue());
		}

		return sb.append('}').toString();
	}

	@Override
	protected void fillConstructorProperties(Context cx, IdFunctionObject ctor) {
		addIdFunctionProperty(cx, ctor, OBJECT_TAG, ConstructorId_getPrototypeOf, "getPrototypeOf", 1);
		addIdFunctionProperty(cx, ctor, OBJECT_TAG, ConstructorId_setPrototypeOf, "setPrototypeOf", 2);
		addIdFunctionProperty(cx, ctor, OBJECT_TAG, ConstructorId_keys, "keys", 1);
		addIdFunctionProperty(cx, ctor, OBJECT_TAG, ConstructorId_entries, "entries", 1);
		addIdFunctionProperty(cx, ctor, OBJECT_TAG, ConstructorId_values, "values", 1);
		addIdFunctionProperty(cx, ctor, OBJECT_TAG, ConstructorId_getOwnPropertyNames, "getOwnPropertyNames", 1);
		addIdFunctionProperty(cx, ctor, OBJECT_TAG, ConstructorId_getOwnPropertySymbols, "getOwnPropertySymbols", 1);
		addIdFunctionProperty(cx, ctor, OBJECT_TAG, ConstructorId_getOwnPropertyDescriptor, "getOwnPropertyDescriptor", 2);
		addIdFunctionProperty(cx, ctor, OBJECT_TAG, ConstructorId_defineProperty, "defineProperty", 3);
		addIdFunctionProperty(cx, ctor, OBJECT_TAG, ConstructorId_isExtensible, "isExtensible", 1);
		addIdFunctionProperty(cx, ctor, OBJECT_TAG, ConstructorId_preventExtensions, "preventExtensions", 1);
		addIdFunctionProperty(cx, ctor, OBJECT_TAG, ConstructorId_defineProperties, "defineProperties", 2);
		addIdFunctionProperty(cx, ctor, OBJECT_TAG, ConstructorId_create, "create", 2);
		addIdFunctionProperty(cx, ctor, OBJECT_TAG, ConstructorId_isSealed, "isSealed", 1);
		addIdFunctionProperty(cx, ctor, OBJECT_TAG, ConstructorId_isFrozen, "isFrozen", 1);
		addIdFunctionProperty(cx, ctor, OBJECT_TAG, ConstructorId_seal, "seal", 1);
		addIdFunctionProperty(cx, ctor, OBJECT_TAG, ConstructorId_freeze, "freeze", 1);
		addIdFunctionProperty(cx, ctor, OBJECT_TAG, ConstructorId_assign, "assign", 2);
		addIdFunctionProperty(cx, ctor, OBJECT_TAG, ConstructorId_is, "is", 2);
		super.fillConstructorProperties(cx, ctor);
	}

	@Override
	protected void initPrototypeId(Context cx, int id) {
		String s;
		int arity;
		switch (id) {
			case Id_constructor -> {
				arity = 1;
				s = "constructor";
			}
			case Id_toString -> {
				arity = 0;
				s = "toString";
			}
			case Id_toLocaleString -> {
				arity = 0;
				s = "toLocaleString";
			}
			case Id_valueOf -> {
				arity = 0;
				s = "valueOf";
			}
			case Id_hasOwnProperty -> {
				arity = 1;
				s = "hasOwnProperty";
			}
			case Id_propertyIsEnumerable -> {
				arity = 1;
				s = "propertyIsEnumerable";
			}
			case Id_isPrototypeOf -> {
				arity = 1;
				s = "isPrototypeOf";
			}
			case Id_toSource -> {
				arity = 0;
				s = "toSource";
			}
			case Id___defineGetter__ -> {
				arity = 2;
				s = "__defineGetter__";
			}
			case Id___defineSetter__ -> {
				arity = 2;
				s = "__defineSetter__";
			}
			case Id___lookupGetter__ -> {
				arity = 1;
				s = "__lookupGetter__";
			}
			case Id___lookupSetter__ -> {
				arity = 1;
				s = "__lookupSetter__";
			}
			default -> throw new IllegalArgumentException(String.valueOf(id));
		}
		initPrototypeMethod(cx, OBJECT_TAG, id, s, arity);
	}

	@Override
	public Object execIdCall(IdFunctionObject f, Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
		if (!f.hasTag(OBJECT_TAG)) {
			return super.execIdCall(f, cx, scope, thisObj, args);
		}
		int id = f.methodId();
		switch (id) {
			case Id_constructor: {
				if (thisObj != null) {
					// BaseFunction.construct will set up parent, proto
					return f.construct(cx, scope, args);
				}
				if (args.length == 0 || args[0] == null || Undefined.isUndefined(args[0])) {
					return new NativeObject();
				}
				return ScriptRuntime.toObject(cx, scope, args[0]);
			}

			case Id_toLocaleString: {
				Object toString = getProperty(cx, thisObj, "toString");
				if (!(toString instanceof Callable fun)) {
					throw ScriptRuntime.notFunctionError(toString);
				}
				return fun.call(cx, scope, thisObj, ScriptRuntime.EMPTY_OBJECTS);
			}

			case Id_toString: {
				return ScriptRuntime.defaultObjectToString(thisObj);
			}

			case Id_valueOf:
				if (thisObj == null || Undefined.isUndefined(thisObj)) {
					throw ScriptRuntime.typeError0("msg." + (thisObj == null ? "null" : "undef") + ".to.object");
				}
				return thisObj;

			case Id_hasOwnProperty: {
				if (thisObj == null || Undefined.isUndefined(thisObj)) {
					throw ScriptRuntime.typeError0("msg." + (thisObj == null ? "null" : "undef") + ".to.object");
				}
				boolean result;
				Object arg = args.length < 1 ? Undefined.instance : args[0];
				if (arg instanceof Symbol) {
					result = ensureSymbolScriptable(thisObj).has(cx, (Symbol) arg, thisObj);
				} else {
					ScriptRuntime.StringIdOrIndex s = ScriptRuntime.toStringIdOrIndex(cx, arg);
					if (s.stringId == null) {
						result = thisObj.has(cx, s.index, thisObj);
					} else {
						result = thisObj.has(cx, s.stringId, thisObj);
					}
				}
				return ScriptRuntime.wrapBoolean(result);
			}

			case Id_propertyIsEnumerable: {
				if (thisObj == null || Undefined.isUndefined(thisObj)) {
					throw ScriptRuntime.typeError0("msg." + (thisObj == null ? "null" : "undef") + ".to.object");
				}

				boolean result;
				Object arg = args.length < 1 ? Undefined.instance : args[0];

				if (arg instanceof Symbol) {
					result = ((SymbolScriptable) thisObj).has(cx, (Symbol) arg, thisObj);
					if (result && thisObj instanceof ScriptableObject so) {
						int attrs = so.getAttributes(cx, (Symbol) arg);
						result = ((attrs & DONTENUM) == 0);
					}
				} else {
					ScriptRuntime.StringIdOrIndex s = ScriptRuntime.toStringIdOrIndex(cx, arg);
					// When checking if a property is enumerable, a missing property should return "false" instead of
					// throwing an exception.  See: https://github.com/mozilla/rhino/issues/415
					try {
						if (s.stringId == null) {
							result = thisObj.has(cx, s.index, thisObj);
							if (result && thisObj instanceof ScriptableObject so) {
								int attrs = so.getAttributes(cx, s.index);
								result = ((attrs & DONTENUM) == 0);
							}
						} else {
							result = thisObj.has(cx, s.stringId, thisObj);
							if (result && thisObj instanceof ScriptableObject so) {
								int attrs = so.getAttributes(cx, s.stringId);
								result = ((attrs & DONTENUM) == 0);
							}
						}
					} catch (EvaluatorException ee) {
						if (ee.getMessage().startsWith(ScriptRuntime.getMessage1("msg.prop.not.found", s.stringId == null ? Integer.toString(s.index) : s.stringId))) {
							result = false;
						} else {
							throw ee;
						}
					}
				}
				return ScriptRuntime.wrapBoolean(result);
			}

			case Id_isPrototypeOf: {
				if (thisObj == null || Undefined.isUndefined(thisObj)) {
					throw ScriptRuntime.typeError0("msg." + (thisObj == null ? "null" : "undef") + ".to.object");
				}

				boolean result = false;
				if (args.length != 0 && args[0] instanceof Scriptable v) {
					do {
						v = v.getPrototype(cx);
						if (v == thisObj) {
							result = true;
							break;
						}
					} while (v != null);
				}
				return ScriptRuntime.wrapBoolean(result);
			}

			case Id_toSource:
				return ScriptRuntime.defaultObjectToSource(cx, scope, thisObj, args);
			case Id___defineGetter__:
			case Id___defineSetter__: {
				if (args.length < 2 || !(args[1] instanceof Callable getterOrSetter)) {
					Object badArg = (args.length >= 2 ? args[1] : Undefined.instance);
					throw ScriptRuntime.notFunctionError(badArg);
				}
				if (!(thisObj instanceof ScriptableObject so)) {
					throw Context.reportRuntimeError2(cx, "msg.extend.scriptable", thisObj == null ? "null" : thisObj.getClass().getName(), String.valueOf(args[0]));
				}
				ScriptRuntime.StringIdOrIndex s = ScriptRuntime.toStringIdOrIndex(cx, args[0]);
				int index = s.stringId != null ? 0 : s.index;
				boolean isSetter = (id == Id___defineSetter__);
				so.setGetterOrSetter(cx, s.stringId, index, getterOrSetter, isSetter);
			}
			return Undefined.instance;

			case Id___lookupGetter__:
			case Id___lookupSetter__: {
				if (args.length < 1 || !(thisObj instanceof ScriptableObject so)) {
					return Undefined.instance;
				}

				ScriptRuntime.StringIdOrIndex s = ScriptRuntime.toStringIdOrIndex(cx, args[0]);
				int index = s.stringId != null ? 0 : s.index;
				boolean isSetter = (id == Id___lookupSetter__);
				Object gs;
				for (; ; ) {
					gs = so.getGetterOrSetter(s.stringId, index, isSetter);
					if (gs != null) {
						break;
					}
					// If there is no getter or setter for the object itself,
					// how about the prototype?
					Scriptable v = so.getPrototype(cx);
					if (v == null) {
						break;
					}
					if (v instanceof ScriptableObject) {
						so = (ScriptableObject) v;
					} else {
						break;
					}
				}
				if (gs != null) {
					return gs;
				}
			}
			return Undefined.instance;

			case ConstructorId_getPrototypeOf: {
				Object arg = args.length < 1 ? Undefined.instance : args[0];
				Scriptable obj = getCompatibleObject(cx, scope, arg);
				return obj.getPrototype(cx);
			}
			case ConstructorId_setPrototypeOf: {
				if (args.length < 2) {
					throw ScriptRuntime.typeError1("msg.incompat.call", "setPrototypeOf");
				}
				Scriptable proto = (args[1] == null) ? null : ensureScriptable(args[1]);
				if (proto instanceof Symbol) {
					throw ScriptRuntime.typeError1("msg.arg.not.object", ScriptRuntime.typeof(proto));
				}

				final Object arg0 = args[0];
				ScriptRuntimeES6.requireObjectCoercible(cx, arg0, f);
				if (!(arg0 instanceof ScriptableObject obj)) {
					return arg0;
				}
				if (!obj.isExtensible()) {
					throw ScriptRuntime.typeError0("msg.not.extensible");
				}

				// cycle detection
				Scriptable prototypeProto = proto;
				while (prototypeProto != null) {
					if (prototypeProto == obj) {
						throw ScriptRuntime.typeError1("msg.object.cyclic.prototype", obj.getClass().getSimpleName());
					}
					prototypeProto = prototypeProto.getPrototype(cx);
				}
				obj.setPrototype(cx, proto);
				return obj;
			}
			case ConstructorId_keys: {
				Object arg = args.length < 1 ? Undefined.instance : args[0];
				Scriptable obj = getCompatibleObject(cx, scope, arg);
				Object[] ids = obj.getIds(cx);
				for (int i = 0; i < ids.length; i++) {
					ids[i] = ScriptRuntime.toString(cx, ids[i]);
				}
				return cx.newArray(scope, ids);
			}
			case ConstructorId_entries: {
				Object arg = args.length < 1 ? Undefined.instance : args[0];
				Scriptable obj = getCompatibleObject(cx, scope, arg);
				Object[] ids = obj.getIds(cx);
				Object[] entries = new Object[ids.length];
				for (int i = 0; i < ids.length; i++) {
					Object[] entry = new Object[2];
					entry[0] = ScriptRuntime.toString(cx, ids[i]);
					entry[1] = obj.get(cx, entry[0].toString(), scope);
					entries[i] = cx.newArray(scope, entry);
				}
				return cx.newArray(scope, entries);
			}
			case ConstructorId_values: {
				Object arg = args.length < 1 ? Undefined.instance : args[0];
				Scriptable obj = getCompatibleObject(cx, scope, arg);
				Object[] ids = obj.getIds(cx);
				Object[] values = new Object[ids.length];
				for (int i = 0; i < ids.length; i++) {
					values[i] = obj.get(cx, ScriptRuntime.toString(cx, ids[i]), scope);
				}
				return cx.newArray(scope, values);
			}
			case ConstructorId_getOwnPropertyNames: {
				Object arg = args.length < 1 ? Undefined.instance : args[0];
				Scriptable s = getCompatibleObject(cx, scope, arg);
				ScriptableObject obj = ensureScriptableObject(s);
				Object[] ids = obj.getIds(cx, true, false);
				for (int i = 0; i < ids.length; i++) {
					ids[i] = ScriptRuntime.toString(cx, ids[i]);
				}
				return cx.newArray(scope, ids);
			}
			case ConstructorId_getOwnPropertySymbols: {
				Object arg = args.length < 1 ? Undefined.instance : args[0];
				Scriptable s = getCompatibleObject(cx, scope, arg);
				ScriptableObject obj = ensureScriptableObject(s);
				Object[] ids = obj.getIds(cx, true, true);
				ArrayList<Object> syms = new ArrayList<>();
				for (Object o : ids) {
					if (o instanceof Symbol) {
						syms.add(o);
					}
				}
				return cx.newArray(scope, syms);
			}
			case ConstructorId_getOwnPropertyDescriptor: {
				Object arg = args.length < 1 ? Undefined.instance : args[0];
				// TODO(norris): There's a deeper issue here if
				// arg instanceof Scriptable. Should we create a new
				// interface to admit the new ECMAScript 5 operations?
				Scriptable s = getCompatibleObject(cx, scope, arg);
				ScriptableObject obj = ensureScriptableObject(s);
				Object nameArg = args.length < 2 ? Undefined.instance : args[1];
				Scriptable desc = obj.getOwnPropertyDescriptor(cx, nameArg);
				return desc == null ? Undefined.instance : desc;
			}
			case ConstructorId_defineProperty: {
				Object arg = args.length < 1 ? Undefined.instance : args[0];
				ScriptableObject obj = ensureScriptableObject(arg);
				Object name = args.length < 2 ? Undefined.instance : args[1];
				Object descArg = args.length < 3 ? Undefined.instance : args[2];
				ScriptableObject desc = ensureScriptableObject(descArg);
				obj.defineOwnProperty(cx, name, desc);
				return obj;
			}
			case ConstructorId_isExtensible: {
				Object arg = args.length < 1 ? Undefined.instance : args[0];
				if (!(arg instanceof ScriptableObject)) {
					return Boolean.FALSE;
				}

				ScriptableObject obj = ensureScriptableObject(arg);
				return obj.isExtensible();
			}
			case ConstructorId_preventExtensions: {
				Object arg = args.length < 1 ? Undefined.instance : args[0];
				if (!(arg instanceof ScriptableObject)) {
					return arg;
				}

				ScriptableObject obj = ensureScriptableObject(arg);
				obj.preventExtensions();
				return obj;
			}
			case ConstructorId_defineProperties: {
				Object arg = args.length < 1 ? Undefined.instance : args[0];
				ScriptableObject obj = ensureScriptableObject(arg);
				Object propsObj = args.length < 2 ? Undefined.instance : args[1];
				Scriptable props = ScriptRuntime.toObject(cx, scope, propsObj);
				obj.defineOwnProperties(cx, scope, ensureScriptableObject(props));
				return obj;
			}
			case ConstructorId_create: {
				Object arg = args.length < 1 ? Undefined.instance : args[0];
				Scriptable obj = (arg == null) ? null : ensureScriptable(arg);

				ScriptableObject newObject = new NativeObject();
				newObject.setParentScope(scope);
				newObject.setPrototype(cx, obj);

				if (args.length > 1 && !Undefined.isUndefined(args[1])) {
					Scriptable props = ScriptRuntime.toObject(cx, scope, args[1]);
					newObject.defineOwnProperties(cx, scope, ensureScriptableObject(props));
				}

				return newObject;
			}
			case ConstructorId_isSealed: {
				Object arg = args.length < 1 ? Undefined.instance : args[0];
				if (!(arg instanceof ScriptableObject)) {
					return Boolean.TRUE;
				}

				ScriptableObject obj = ensureScriptableObject(arg);

				if (obj.isExtensible()) {
					return Boolean.FALSE;
				}

				for (Object name : obj.getAllIds(cx)) {
					Object configurable = obj.getOwnPropertyDescriptor(cx, name).get(cx, "configurable");
					if (Boolean.TRUE.equals(configurable)) {
						return Boolean.FALSE;
					}
				}

				return Boolean.TRUE;
			}
			case ConstructorId_isFrozen: {
				Object arg = args.length < 1 ? Undefined.instance : args[0];
				if (!(arg instanceof ScriptableObject)) {
					return Boolean.TRUE;
				}

				ScriptableObject obj = ensureScriptableObject(arg);

				if (obj.isExtensible()) {
					return Boolean.FALSE;
				}

				for (Object name : obj.getAllIds(cx)) {
					ScriptableObject desc = obj.getOwnPropertyDescriptor(cx, name);
					if (Boolean.TRUE.equals(desc.get(cx, "configurable"))) {
						return Boolean.FALSE;
					}
					if (isDataDescriptor(cx, desc) && Boolean.TRUE.equals(desc.get(cx, "writable"))) {
						return Boolean.FALSE;
					}
				}

				return Boolean.TRUE;
			}
			case ConstructorId_seal: {
				Object arg = args.length < 1 ? Undefined.instance : args[0];
				if (!(arg instanceof ScriptableObject)) {
					return arg;
				}

				ScriptableObject obj = ensureScriptableObject(arg);

				for (Object name : obj.getAllIds(cx)) {
					ScriptableObject desc = obj.getOwnPropertyDescriptor(cx, name);
					if (Boolean.TRUE.equals(desc.get(cx, "configurable"))) {
						desc.put(cx, "configurable", desc, Boolean.FALSE);
						obj.defineOwnProperty(cx, name, desc, false);
					}
				}
				obj.preventExtensions();

				return obj;
			}
			case ConstructorId_freeze: {
				Object arg = args.length < 1 ? Undefined.instance : args[0];
				if (!(arg instanceof ScriptableObject)) {
					return arg;
				}

				ScriptableObject obj = ensureScriptableObject(arg);

				for (Object name : obj.getIds(cx, true, true)) {
					ScriptableObject desc = obj.getOwnPropertyDescriptor(cx, name);
					if (isDataDescriptor(cx, desc) && Boolean.TRUE.equals(desc.get(cx, "writable"))) {
						desc.put(cx, "writable", desc, Boolean.FALSE);
					}
					if (Boolean.TRUE.equals(desc.get(cx, "configurable"))) {
						desc.put(cx, "configurable", desc, Boolean.FALSE);
					}
					obj.defineOwnProperty(cx, name, desc, false);
				}
				obj.preventExtensions();

				return obj;
			}

			case ConstructorId_assign: {
				if (args.length < 1) {
					throw ScriptRuntime.typeError1("msg.incompat.call", "assign");
				}
				Scriptable targetObj = ScriptRuntime.toObject(cx, thisObj, args[0]);
				for (int i = 1; i < args.length; i++) {
					if ((args[i] == null) || Undefined.isUndefined(args[i])) {
						continue;
					}
					Scriptable sourceObj = ScriptRuntime.toObject(cx, thisObj, args[i]);
					Object[] ids = sourceObj.getIds(cx);
					for (Object key : ids) {
						if (key instanceof String) {
							Object val = sourceObj.get(cx, (String) key, sourceObj);
							if ((val != NOT_FOUND) && !Undefined.isUndefined(val)) {
								targetObj.put(cx, (String) key, targetObj, val);
							}
						} else if (key instanceof Number) {
							int ii = ScriptRuntime.toInt32(cx, key);
							Object val = sourceObj.get(cx, ii, sourceObj);
							if ((val != NOT_FOUND) && !Undefined.isUndefined(val)) {
								targetObj.put(cx, ii, targetObj, val);
							}
						}
					}
				}
				return targetObj;
			}

			case ConstructorId_is: {
				Object a1 = args.length < 1 ? Undefined.instance : args[0];
				Object a2 = args.length < 2 ? Undefined.instance : args[1];
				return ScriptRuntime.wrapBoolean(ScriptRuntime.same(cx, a1, a2));
			}


			default:
				throw new IllegalArgumentException(String.valueOf(id));
		}
	}

	private static Scriptable getCompatibleObject(Context cx, Scriptable scope, Object arg) {
		Scriptable s = ScriptRuntime.toObject(cx, scope, arg);
		return ensureScriptable(s);
	}

	@Override
	protected int findPrototypeId(String s) {
		return switch (s) {
			case "constructor" -> Id_constructor;
			case "toString" -> Id_toString;
			case "toLocaleString" -> Id_toLocaleString;
			case "valueOf" -> Id_valueOf;
			case "hasOwnProperty" -> Id_hasOwnProperty;
			case "propertyIsEnumerable" -> Id_propertyIsEnumerable;
			case "isPrototypeOf" -> Id_isPrototypeOf;
			case "toSource" -> Id_toSource;
			case "__defineGetter__" -> Id___defineGetter__;
			case "__defineSetter__" -> Id___defineSetter__;
			case "__lookupGetter__" -> Id___lookupGetter__;
			case "__lookupSetter__" -> Id___lookupSetter__;
			default -> 0;
		};
	}

	private static final int ConstructorId_getPrototypeOf = -1;
	private static final int ConstructorId_keys = -2;
	private static final int ConstructorId_getOwnPropertyNames = -3;
	private static final int ConstructorId_getOwnPropertyDescriptor = -4;
	private static final int ConstructorId_defineProperty = -5;
	private static final int ConstructorId_isExtensible = -6;
	private static final int ConstructorId_preventExtensions = -7;
	private static final int ConstructorId_defineProperties = -8;
	private static final int ConstructorId_create = -9;
	private static final int ConstructorId_isSealed = -10;
	private static final int ConstructorId_isFrozen = -11;
	private static final int ConstructorId_seal = -12;
	private static final int ConstructorId_freeze = -13;
	private static final int ConstructorId_getOwnPropertySymbols = -14;
	private static final int ConstructorId_assign = -15;
	private static final int ConstructorId_is = -16;
	private static final int ConstructorId_setPrototypeOf = -17;
	private static final int ConstructorId_entries = -18;
	private static final int ConstructorId_values = -19;

	private static final int Id_constructor = 1;
	private static final int Id_toString = 2;
	private static final int Id_toLocaleString = 3;
	private static final int Id_valueOf = 4;
	private static final int Id_hasOwnProperty = 5;
	private static final int Id_propertyIsEnumerable = 6;
	private static final int Id_isPrototypeOf = 7;
	private static final int Id_toSource = 8;
	private static final int Id___defineGetter__ = 9;
	private static final int Id___defineSetter__ = 10;
	private static final int Id___lookupGetter__ = 11;
	private static final int Id___lookupSetter__ = 12;
	private static final int MAX_PROTOTYPE_ID = 12;
	// #/string_id_map#
}
