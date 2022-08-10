/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package dev.latvian.mods.rhino;

import dev.latvian.mods.rhino.classdata.ClassData;
import dev.latvian.mods.rhino.classdata.MethodSignature;
import dev.latvian.mods.rhino.util.Deletable;
import dev.latvian.mods.rhino.util.JavaIteratorWrapper;
import dev.latvian.mods.rhino.util.wrap.TypeWrapperFactory;

import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * This class reflects non-Array Java objects into the JavaScript environment.  It
 * reflect fields directly, and uses NativeJavaMethod objects to reflect (possibly
 * overloaded) methods.<p>
 *
 * @author Mike Shaver
 * @see NativeJavaClass
 */

public class NativeJavaObject implements Scriptable, SymbolScriptable, Wrapper {
	private static final Object COERCED_INTERFACE_KEY = "Coerced Interface";

	/**
	 * The prototype of this object.
	 */
	protected Scriptable prototype;

	/**
	 * The parent scope of this object.
	 */
	protected Scriptable parent;

	protected transient Object javaObject;

	protected transient ClassData classData;
	protected transient Map<String, Object> customMembers;

	public NativeJavaObject(Context cx, Scriptable scope, Object javaObject, Class<?> type) {
		this.parent = scope;
		this.javaObject = javaObject;
		this.classData = ClassData.of(cx, scope, type);
		this.customMembers = null;
		initMembers(cx);
	}

	protected void initMembers(Context cx) {
	}

	protected void addCustomMember(String name, Object fm) {
		if (customMembers == null) {
			customMembers = new HashMap<>();
		}

		customMembers.put(name, fm);
	}

	protected void addCustomFunction(String name, CustomFunction.Func func, Class<?>... argTypes) {
		addCustomMember(name, new CustomFunction(name, func, argTypes));
	}

	protected void addCustomFunction(String name, CustomFunction.NoArgFunc func) {
		addCustomFunction(name, func, CustomFunction.NO_ARGS);
	}

	public void addCustomProperty(String name, CustomProperty getter) {
		addCustomMember(name, getter);
	}

	@Override
	public boolean has(Context cx, String name, Scriptable start) {
		return customMembers != null && customMembers.containsKey(name) || classData.getMember(name, false) != null;
	}

	@Override
	public boolean has(Context cx, int index, Scriptable start) {
		return false;
	}

	@Override
	public boolean has(Context cx, Symbol key, Scriptable start) {
		return javaObject instanceof Iterable<?> && SymbolKey.ITERATOR.equals(key);
	}

	@Override
	public Object get(Context cx, String name, Scriptable start) {
		if (customMembers != null) {
			Object result = customMembers.get(name);

			if (result != null) {
				if (result instanceof CustomProperty) {
					Object r = ((CustomProperty) result).get();

					if (r == null) {
						return Undefined.instance;
					}

					Object r1 = cx.getWrapFactory().wrap(cx, this, r, r.getClass());

					if (r1 instanceof Scriptable) {
						return ((Scriptable) r1).getDefaultValue(cx, null);
					}

					return r1;
				}

				return result;
			}
		}

		var m = classData.getMember(name, false);
		return m == null ? Scriptable.NOT_FOUND : m.actuallyGet(cx, start, javaObject, m.getType());
	}

	@Override
	public Object get(Context cx, Symbol key, Scriptable start) {
		if (javaObject instanceof Iterable<?> && SymbolKey.ITERATOR.equals(key)) {
			return new JavaIteratorWrapper(((Iterable<?>) javaObject).iterator());
		}

		// Native Java objects have no Symbol members
		return Scriptable.NOT_FOUND;
	}

	@Override
	public Object get(Context cx, int index, Scriptable start) {
		throw classData.reportMemberNotFound(cx, Integer.toString(index));
	}

	@Override
	public void put(Context cx, String name, Scriptable start, Object value) {
		var m = classData.getMember(name, false);

		// We could be asked to modify the value of a property in the
		// prototype. Since we can't add a property to a Java object,
		// we modify it in the prototype rather than copy it down.
		if (m != null) {
			m.actuallySet(cx, start, javaObject, value);
		} else if (prototype != null) {
			prototype.put(cx, name, prototype, value);
		} else {
			addCustomMember(name, value);
		}
	}

	@Override
	public void put(Context cx, Symbol symbol, Scriptable start, Object value) {
		// We could be asked to modify the value of a property in the
		// prototype. Since we can't add a property to a Java object,
		// we modify it in the prototype rather than copy it down.
		String name = symbol.toString();
		var m = classData.getMember(name, false);

		if (m != null) {
			m.actuallySet(cx, start, javaObject, value);
		} else if (prototype instanceof SymbolScriptable) {
			((SymbolScriptable) prototype).put(cx, symbol, prototype, value);
		} else {
			addCustomMember(name, value);
		}
	}

	@Override
	public void put(Context cx, int index, Scriptable start, Object value) {
		throw classData.reportMemberNotFound(cx, Integer.toString(index));
	}

	@Override
	public boolean hasInstance(Context cx, Scriptable value) {
		// This is an instance of a Java class, so always return false
		return false;
	}

	@Override
	public void delete(Context cx, Scriptable scope, String name) {
		if (customMembers != null) {
			Object result = customMembers.get(name);
			if (result != null) {
				Deletable.deleteObject(result);
				return;
			}
		}

		var m = classData.getMember(name, false);

		if (m != null) {
			Deletable.deleteObject(m.actuallyGet(cx, scope, name, null));
		}
	}

	@Override
	public void delete(Context cx, Scriptable scope, Symbol key) {
	}

	@Override
	public void delete(Context cx, Scriptable scope, int index) {
	}

	@Override
	public Scriptable getPrototype(Context cx) {
		if (prototype == null && javaObject instanceof String) {
			return TopLevel.getBuiltinPrototype(cx, ScriptableObject.getTopLevelScope(parent), TopLevel.Builtins.String);
		}
		return prototype;
	}

	/**
	 * Sets the prototype of the object.
	 */
	@Override
	public void setPrototype(Context cx, Scriptable m) {
		prototype = m;
	}

	/**
	 * Returns the parent (enclosing) scope of the object.
	 */
	@Override
	public Scriptable getParentScope() {
		return parent;
	}

	/**
	 * Sets the parent (enclosing) scope of the object.
	 */
	@Override
	public void setParentScope(Scriptable m) {
		parent = m;
	}

	@Override
	public Object[] getIds(Context cx) {
		if (customMembers != null) {
			Object[] c = customMembers.keySet().toArray();
			Object[] m = classData.getMembers(false).keySet().toArray();
			Object[] result = new Object[c.length + m.length];
			System.arraycopy(c, 0, result, 0, c.length);
			System.arraycopy(m, 0, result, c.length, m.length);
			return result;
		}

		return classData.getMembers(false).keySet().toArray(ScriptRuntime.EMPTY_OBJECTS);
	}

	@Override
	public Object unwrap() {
		return javaObject;
	}

	@Override
	public String getClassName() {
		return "JavaObject";
	}

	@Override
	public Object getDefaultValue(Context cx, Class<?> hint) {
		Object value;
		if (hint == null) {
			if (javaObject instanceof Boolean) {
				hint = ScriptRuntime.BooleanClass;
			}
			if (javaObject instanceof Number) {
				hint = ScriptRuntime.NumberClass;
			}
		}
		if (hint == null || hint == ScriptRuntime.StringClass) {
			value = javaObject.toString();
		} else {
			String converterName;
			if (hint == ScriptRuntime.BooleanClass) {
				converterName = "booleanValue";
			} else if (hint == ScriptRuntime.NumberClass) {
				converterName = "doubleValue";
			} else {
				throw Context.reportRuntimeError0(cx, "msg.default.value");
			}
			Object converterObject = get(cx, converterName, this);
			if (converterObject instanceof Function f) {
				value = f.call(Context.getContext(), f.getParentScope(), this, ScriptRuntime.EMPTY_OBJECTS);
			} else {
				if (hint == ScriptRuntime.NumberClass && javaObject instanceof Boolean) {
					boolean b = (Boolean) javaObject;
					value = b ? ScriptRuntime.wrapNumber(1.0) : ScriptRuntime.zeroObj;
				} else {
					value = javaObject.toString();
				}
			}
		}
		return value;
	}

	/**
	 * Determine whether we can/should convert between the given type and the
	 * desired one.  This should be superceded by a conversion-cost calculation
	 * function, but for now I'll hide behind precedent.
	 */
	public static boolean canConvert(Context cx, Object fromObj, Class<?> to) {
		return getConversionWeight(cx, fromObj, Wrapper.unwrapped(fromObj), to) < CONVERSION_NONE;
	}

	private static final int JSTYPE_UNDEFINED = 0; // undefined type
	private static final int JSTYPE_NULL = 1; // null
	private static final int JSTYPE_BOOLEAN = 2; // boolean
	private static final int JSTYPE_NUMBER = 3; // number
	private static final int JSTYPE_STRING = 4; // string
	private static final int JSTYPE_JAVA_CLASS = 5; // JavaClass
	private static final int JSTYPE_JAVA_OBJECT = 6; // JavaObject
	private static final int JSTYPE_JAVA_ARRAY = 7; // JavaArray
	private static final int JSTYPE_OBJECT = 8; // Scriptable

	static final byte CONVERSION_TRIVIAL = 1;
	static final byte CONVERSION_NONTRIVIAL = 0;
	static final byte CONVERSION_NONE = 99;

	/**
	 * Derive a ranking based on how "natural" the conversion is.
	 * The special value CONVERSION_NONE means no conversion is possible,
	 * and CONVERSION_NONTRIVIAL signals that more type conformance testing
	 * is required.
	 * Based on
	 * <a href="http://www.mozilla.org/js/liveconnect/lc3_method_overloading.html">
	 * "preferred method conversions" from Live Connect 3</a>
	 */
	static int getConversionWeight(Context cx, Object fromObj, Object unwrappedFromObj, Class<?> to) {
		var wrapperFactory = cx.getSharedData().hasTypeWrappers() ? cx.getSharedData().getTypeWrappers().getWrapperFactory(cx.getSharedData(), to, unwrappedFromObj) : null;

		if (wrapperFactory != null) {
			return CONVERSION_NONTRIVIAL;
		}

		int fromCode = getJSTypeCode(fromObj);

		switch (fromCode) {

			case JSTYPE_UNDEFINED:
				if (to == ScriptRuntime.StringClass || to == ScriptRuntime.ObjectClass) {
					return 1;
				}
				break;

			case JSTYPE_NULL:
				if (!to.isPrimitive()) {
					return 1;
				}
				break;

			case JSTYPE_BOOLEAN:
				// "boolean" is #1
				if (to == Boolean.TYPE) {
					return 1;
				} else if (to == ScriptRuntime.BooleanClass) {
					return 2;
				} else if (to == ScriptRuntime.ObjectClass) {
					return 3;
				} else if (to == ScriptRuntime.StringClass) {
					return 4;
				}
				break;

			case JSTYPE_NUMBER:
				if (to.isPrimitive()) {
					if (to == Double.TYPE) {
						return 1;
					} else if (to != Boolean.TYPE) {
						return 1 + getSizeRank(to);
					}
				} else {
					if (to == ScriptRuntime.StringClass) {
						// native numbers are #1-8
						return 9;
					} else if (to == ScriptRuntime.ObjectClass) {
						return 10;
					} else if (ScriptRuntime.NumberClass.isAssignableFrom(to)) {
						// "double" is #1
						return 2;
					}
				}
				break;

			case JSTYPE_STRING:
				if (to == ScriptRuntime.StringClass) {
					return 1;
				} else if (to.isInstance(fromObj)) {
					return 2;
				} else if (to.isPrimitive()) {
					if (to == Character.TYPE) {
						return 3;
					} else if (to != Boolean.TYPE) {
						return 4;
					}
				}
				break;

			case JSTYPE_JAVA_CLASS:
				if (to == ScriptRuntime.ClassClass) {
					return 1;
				} else if (to == ScriptRuntime.ObjectClass) {
					return 3;
				} else if (to == ScriptRuntime.StringClass) {
					return 4;
				}
				break;

			case JSTYPE_JAVA_OBJECT:
			case JSTYPE_JAVA_ARRAY:
				Object javaObj = fromObj;
				if (javaObj instanceof Wrapper) {
					javaObj = ((Wrapper) javaObj).unwrap();
				}
				if (to.isInstance(javaObj)) {
					return CONVERSION_NONTRIVIAL;
				}
				if (to == ScriptRuntime.StringClass) {
					return 2;
				} else if (to.isPrimitive() && to != Boolean.TYPE) {
					return (fromCode == JSTYPE_JAVA_ARRAY) ? CONVERSION_NONE : 2 + getSizeRank(to);
				}
				break;

			case JSTYPE_OBJECT:
				// Other objects takes #1-#3 spots
				if (to != ScriptRuntime.ObjectClass && to.isInstance(fromObj)) {
					// No conversion required, but don't apply for java.lang.Object
					return 1;
				}
				if (to.isArray()) {
					if (fromObj instanceof NativeJavaList) {
						// This is a native array conversion to a java array
						// Array conversions are all equal, and preferable to object
						// and string conversion, per LC3.
						return 2;
					}
				} else if (to == ScriptRuntime.ObjectClass) {
					return 3;
				} else if (to == ScriptRuntime.StringClass) {
					return 4;
				} else if (to == ScriptRuntime.DateClass) {
					if (fromObj instanceof NativeDate) {
						// This is a native date to java date conversion
						return 1;
					}
				} else if (to.isInterface()) {

					if (fromObj instanceof NativeFunction) {
						// See comments in createInterfaceAdapter
						return 1;
					}
					if (fromObj instanceof NativeObject) {
						return 2;
					}
					return 12;
				} else if (to.isPrimitive() && to != Boolean.TYPE) {
					return 4 + getSizeRank(to);
				}
				break;
		}

		return CONVERSION_NONE;
	}

	static int getSizeRank(Class<?> aType) {
		if (aType == Double.TYPE) {
			return 1;
		} else if (aType == Float.TYPE) {
			return 2;
		} else if (aType == Long.TYPE) {
			return 3;
		} else if (aType == Integer.TYPE) {
			return 4;
		} else if (aType == Short.TYPE) {
			return 5;
		} else if (aType == Character.TYPE) {
			return 6;
		} else if (aType == Byte.TYPE) {
			return 7;
		} else if (aType == Boolean.TYPE) {
			return CONVERSION_NONE;
		} else {
			return 8;
		}
	}

	private static int getJSTypeCode(Object value) {
		if (value == null) {
			return JSTYPE_NULL;
		} else if (value == Undefined.instance) {
			return JSTYPE_UNDEFINED;
		} else if (value instanceof CharSequence) {
			return JSTYPE_STRING;
		} else if (value instanceof Number) {
			return JSTYPE_NUMBER;
		} else if (value instanceof Boolean) {
			return JSTYPE_BOOLEAN;
		} else if (value instanceof Scriptable) {
			if (value instanceof NativeJavaClass) {
				return JSTYPE_JAVA_CLASS;
			} else if (value instanceof NativeJavaList) {
				return JSTYPE_JAVA_ARRAY;
			} else if (value instanceof Wrapper) {
				return JSTYPE_JAVA_OBJECT;
			} else {
				return JSTYPE_OBJECT;
			}
		} else if (value instanceof Class) {
			return JSTYPE_JAVA_CLASS;
		} else {
			Class<?> valueClass = value.getClass();
			if (valueClass.isArray()) {
				return JSTYPE_JAVA_ARRAY;
			}
			return JSTYPE_JAVA_OBJECT;
		}
	}

	/**
	 * Type-munging for field setting and method invocation.
	 * Conforms to LC3 specification
	 */
	static Object coerceTypeImpl(Context cx, Class<?> type, Object value) {
		if (value == null || value.getClass() == type) {
			return value;
		}

		Object unwrappedValue = Wrapper.unwrapped(value);
		TypeWrapperFactory<?> typeWrapper = cx.getSharedData().hasTypeWrappers() ? cx.getSharedData().getTypeWrappers().getWrapperFactory(cx.getSharedData(), type, unwrappedValue) : null;

		if (typeWrapper != null) {
			return typeWrapper.wrap(unwrappedValue);
		}

		switch (getJSTypeCode(value)) {

			case JSTYPE_NULL:
				// raise error if type.isPrimitive()
				if (type.isPrimitive()) {
					return reportConversionError(cx, value, type);
				}
				return null;

			case JSTYPE_UNDEFINED:
				if (type == ScriptRuntime.StringClass || type == ScriptRuntime.ObjectClass) {
					return "undefined";
				}
				return reportConversionError(cx, "undefined", type, value);

			case JSTYPE_BOOLEAN:
				// Under LC3, only JS Booleans can be coerced into a Boolean value
				if (type == Boolean.TYPE || type == ScriptRuntime.BooleanClass || type == ScriptRuntime.ObjectClass) {
					return value;
				} else if (type == ScriptRuntime.StringClass) {
					return value.toString();
				} else {
					return reportConversionError(cx, value, type);
				}
			case JSTYPE_NUMBER:
				if (type == ScriptRuntime.StringClass) {
					return ScriptRuntime.toString(cx, value);
				} else if (type == ScriptRuntime.ObjectClass) {
					return coerceToNumber(cx, Double.TYPE, value);
				} else if ((type.isPrimitive() && type != Boolean.TYPE) || ScriptRuntime.NumberClass.isAssignableFrom(type)) {
					return coerceToNumber(cx, type, value);
				} else {
					return reportConversionError(cx, value, type);
				}

			case JSTYPE_STRING:
				if (type == ScriptRuntime.StringClass || type.isInstance(value)) {
					return value.toString();
				} else if (type == Character.TYPE || type == ScriptRuntime.CharacterClass) {
					// Special case for converting a single char string to a
					// character
					// Placed here because it applies *only* to JS strings,
					// not other JS objects converted to strings
					if (((CharSequence) value).length() == 1) {
						return ((CharSequence) value).charAt(0);
					}
					return coerceToNumber(cx, type, value);
				} else if ((type.isPrimitive() && type != Boolean.TYPE) || ScriptRuntime.NumberClass.isAssignableFrom(type)) {
					return coerceToNumber(cx, type, value);
				} else {
					return reportConversionError(cx, value, type);
				}

			case JSTYPE_JAVA_CLASS:
				if (type == ScriptRuntime.ClassClass || type == ScriptRuntime.ObjectClass) {
					return unwrappedValue;
				} else if (type == ScriptRuntime.StringClass) {
					return unwrappedValue.toString();
				} else {
					return reportConversionError(cx, unwrappedValue, type);
				}

			case JSTYPE_JAVA_OBJECT:
			case JSTYPE_JAVA_ARRAY:
				if (type.isPrimitive()) {
					if (type == Boolean.TYPE) {
						return reportConversionError(cx, unwrappedValue, type);
					}
					return coerceToNumber(cx, type, unwrappedValue);
				}
				if (type == ScriptRuntime.StringClass) {
					return unwrappedValue.toString();
				}
				if (type.isInstance(unwrappedValue)) {
					return unwrappedValue;
				}
				return reportConversionError(cx, unwrappedValue, type);
			case JSTYPE_OBJECT:
				if (type == ScriptRuntime.StringClass) {
					return ScriptRuntime.toString(cx, value);
				} else if (type.isPrimitive()) {
					if (type == Boolean.TYPE) {
						return reportConversionError(cx, value, type);
					}
					return coerceToNumber(cx, type, value);
				} else if (type.isInstance(value)) {
					return value;
				} else if (type == ScriptRuntime.DateClass && value instanceof NativeDate) {
					double time = ((NativeDate) value).getJSTimeValue();
					// XXX: This will replace NaN by 0
					return new Date((long) time);
				} else if (type.isArray() && value instanceof NativeJavaList array) {
					// Make a new java array, and coerce the JS array components
					// to the target (component) type.
					long length = array.getLength();
					Class<?> arrayType = type.getComponentType();
					Object Result = Array.newInstance(arrayType, (int) length);
					for (int i = 0; i < length; ++i) {
						try {
							Array.set(Result, i, coerceTypeImpl(cx, arrayType, array.get(cx, i, array)));
						} catch (EvaluatorException ee) {
							return reportConversionError(cx, value, type);
						}
					}

					return Result;
				} else if (value instanceof Wrapper) {
					if (type.isInstance(unwrappedValue)) {
						return unwrappedValue;
					}
					return reportConversionError(cx, unwrappedValue, type);
				} else if (type.isInterface() && (value instanceof NativeObject || value instanceof NativeFunction || value instanceof ArrowFunction)) {
					// Try to use function/object as implementation of Java interface.
					return createInterfaceAdapter(type, (ScriptableObject) value);
				} else {
					return reportConversionError(cx, value, type);
				}
		}


		return value;
	}

	public static Object createInterfaceAdapter(Class<?> type, ScriptableObject so) {
		// XXX: Currently only instances of ScriptableObject are
		// supported since the resulting interface proxies should
		// be reused next time conversion is made and generic
		// Callable has no storage for it. Weak references can
		// address it but for now use this restriction.

		Object key = Kit.makeHashKeyFromPair(COERCED_INTERFACE_KEY, type);
		Object old = so.getAssociatedValue(key);
		if (old != null) {
			// Function was already wrapped
			return old;
		}
		Context cx = Context.getContext();
		Object glue = InterfaceAdapter.create(cx, type, so);
		// Store for later retrieval
		glue = so.associateValue(key, glue);
		return glue;
	}

	private static Object coerceToNumber(Context cx, Class<?> type, Object value) {
		Class<?> valueClass = value.getClass();

		// Character
		if (type == Character.TYPE || type == ScriptRuntime.CharacterClass) {
			if (valueClass == ScriptRuntime.CharacterClass) {
				return value;
			}
			return (char) toInteger(cx, value, ScriptRuntime.CharacterClass, Character.MIN_VALUE, Character.MAX_VALUE);
		}

		// Double, Float
		if (type == ScriptRuntime.ObjectClass || type == ScriptRuntime.DoubleClass || type == Double.TYPE) {
			return valueClass == ScriptRuntime.DoubleClass ? value : Double.valueOf(toDouble(cx, value));
		}

		if (type == ScriptRuntime.FloatClass || type == Float.TYPE) {
			if (valueClass == ScriptRuntime.FloatClass) {
				return value;
			}
			double number = toDouble(cx, value);
			if (Double.isInfinite(number) || Double.isNaN(number) || number == 0.0) {
				return (float) number;
			}

			double absNumber = Math.abs(number);
			if (absNumber < Float.MIN_VALUE) {
				return (number > 0.0) ? +0.0f : -0.0f;
			} else if (absNumber > Float.MAX_VALUE) {
				return (number > 0.0) ? Float.POSITIVE_INFINITY : Float.NEGATIVE_INFINITY;
			} else {
				return (float) number;
			}
		}

		// Integer, Long, Short, Byte
		if (type == ScriptRuntime.IntegerClass || type == Integer.TYPE) {
			if (valueClass == ScriptRuntime.IntegerClass) {
				return value;
			}
			return (int) toInteger(cx, value, ScriptRuntime.IntegerClass, Integer.MIN_VALUE, Integer.MAX_VALUE);
		}

		if (type == ScriptRuntime.LongClass || type == Long.TYPE) {
			if (valueClass == ScriptRuntime.LongClass) {
				return value;
			}
			/* Long values cannot be expressed exactly in doubles.
			 * We thus use the largest and smallest double value that
			 * has a value expressible as a long value. We build these
			 * numerical values from their hexidecimal representations
			 * to avoid any problems caused by attempting to parse a
			 * decimal representation.
			 */
			final double max = Double.longBitsToDouble(0x43dfffffffffffffL);
			final double min = Double.longBitsToDouble(0xc3e0000000000000L);
			return toInteger(cx, value, ScriptRuntime.LongClass, min, max);
		}

		if (type == ScriptRuntime.ShortClass || type == Short.TYPE) {
			if (valueClass == ScriptRuntime.ShortClass) {
				return value;
			}
			return (short) toInteger(cx, value, ScriptRuntime.ShortClass, Short.MIN_VALUE, Short.MAX_VALUE);
		}

		if (type == ScriptRuntime.ByteClass || type == Byte.TYPE) {
			if (valueClass == ScriptRuntime.ByteClass) {
				return value;
			}
			return (byte) toInteger(cx, value, ScriptRuntime.ByteClass, Byte.MIN_VALUE, Byte.MAX_VALUE);
		}

		return toDouble(cx, value);
	}


	private static double toDouble(Context cx, Object value) {
		if (value instanceof Number) {
			return ((Number) value).doubleValue();
		} else if (value instanceof String) {
			return ScriptRuntime.toNumber(cx, (String) value);
		} else if (value instanceof Scriptable) {
			if (value instanceof Wrapper) {
				// XXX: optimize tail-recursion?
				return toDouble(cx, ((Wrapper) value).unwrap());
			}
			return ScriptRuntime.toNumber(cx, value);
		} else {
			Method meth;
			try {
				meth = value.getClass().getMethod("doubleValue", (Class[]) null);
			} catch (NoSuchMethodException e) {
				meth = null;
			} catch (SecurityException e) {
				meth = null;
			}
			if (meth != null) {
				try {
					return ((Number) meth.invoke(value, (Object[]) null)).doubleValue();
				} catch (IllegalAccessException e) {
					// XXX: ignore, or error message?
					reportConversionError(cx, value, Double.TYPE);
				} catch (InvocationTargetException e) {
					// XXX: ignore, or error message?
					reportConversionError(cx, value, Double.TYPE);
				}
			}
			return ScriptRuntime.toNumber(cx, value.toString());
		}
	}

	private static long toInteger(Context cx, Object value, Class<?> type, double min, double max) {
		double d = toDouble(cx, value);

		if (Double.isInfinite(d) || Double.isNaN(d)) {
			// Convert to string first, for more readable message
			reportConversionError(cx, ScriptRuntime.toString(cx, value), type);
		}

		if (d > 0.0) {
			d = Math.floor(d);
		} else {
			d = Math.ceil(d);
		}

		if (d < min || d > max) {
			// Convert to string first, for more readable message
			reportConversionError(cx, ScriptRuntime.toString(cx, value), type);
		}
		return (long) d;
	}

	static Object reportConversionError(Context cx, Object value, Class<?> type) {
		return reportConversionError(cx, value, type, value);
	}

	static Object reportConversionError(Context cx, Object value, Class<?> type, Object stringValue) {
		// It uses String.valueOf(value), not value.toString() since
		// value can be null, bug 282447.
		throw Context.reportRuntimeError2(cx, "msg.conversion.not.allowed", String.valueOf(stringValue), MethodSignature.javaSignature(type));
	}

	@Override
	public String toString() {
		return String.valueOf(javaObject);
	}
}
