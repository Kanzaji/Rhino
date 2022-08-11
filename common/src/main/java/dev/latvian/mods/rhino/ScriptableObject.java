/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

// API class

package dev.latvian.mods.rhino;

import dev.latvian.mods.rhino.classdata.BaseMember;
import dev.latvian.mods.rhino.classdata.ClassData;
import dev.latvian.mods.rhino.classdata.ConstructorInfo;
import dev.latvian.mods.rhino.classdata.DelegatedMember;
import dev.latvian.mods.rhino.classdata.MethodInfo;
import dev.latvian.mods.rhino.classdata.PublicClassData;
import dev.latvian.mods.rhino.util.Deletable;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

/**
 * This is the default implementation of the Scriptable interface. This
 * class provides convenient default behavior that makes it easier to
 * define host objects.
 * <p>
 * Various properties and methods of JavaScript objects can be conveniently
 * defined using methods of ScriptableObject.
 * <p>
 * Classes extending ScriptableObject must define the getClassName method.
 *
 * @author Norris Boyd
 * @see Scriptable
 */

public abstract class ScriptableObject implements Scriptable, SymbolScriptable, ConstProperties {
	/**
	 * The empty property attribute.
	 * <p>
	 * Used by getAttributes() and setAttributes().
	 *
	 * @see ScriptableObject#getAttributes(Context, String)
	 * @see ScriptableObject#setAttributes(Context, String, int)
	 */
	public static final int EMPTY = 0x00;

	/**
	 * Property attribute indicating assignment to this property is ignored.
	 *
	 * @see ScriptableObject
	 * #put(String, Scriptable, Object)
	 * @see ScriptableObject#getAttributes(Context, String)
	 * @see ScriptableObject#setAttributes(Context, String, int)
	 */
	public static final int READONLY = 0x01;

	/**
	 * Property attribute indicating property is not enumerated.
	 * <p>
	 * Only enumerated properties will be returned by getIds().
	 *
	 * @see ScriptableObject#getIds(Context)
	 * @see ScriptableObject#getAttributes(Context, String)
	 * @see ScriptableObject#setAttributes(Context, String, int)
	 */
	public static final int DONTENUM = 0x02;

	/**
	 * Property attribute indicating property cannot be deleted.
	 *
	 * @see ScriptableObject#delete(Context, Scriptable, String)
	 * @see ScriptableObject#getAttributes(Context, String)
	 * @see ScriptableObject#setAttributes(Context, String, int)
	 */
	public static final int PERMANENT = 0x04;

	/**
	 * Property attribute indicating that this is a const property that has not
	 * been assigned yet.  The first 'const' assignment to the property will
	 * clear this bit.
	 */
	public static final int UNINITIALIZED_CONST = 0x08;

	public static final int CONST = PERMANENT | READONLY | UNINITIALIZED_CONST;
	/**
	 * The prototype of this object.
	 */
	private Scriptable prototypeObject;

	/**
	 * The parent scope of this object.
	 */
	private Scriptable parentScopeObject;

	/**
	 * This holds all the slots. It may or may not be thread-safe, and may expand itself to
	 * a different data structure depending on the size of the object.
	 */
	private final transient SlotMapContainer slotMap;

	private volatile Map<Object, Object> associatedValues;

	private boolean isExtensible = true;

	protected static ScriptableObject buildDataDescriptor(Context cx, Scriptable scope, Object value, int attributes) {
		ScriptableObject desc = new NativeObject();
		ScriptRuntime.setBuiltinProtoAndParent(cx, desc, scope, TopLevel.Builtins.Object);
		desc.defineProperty(cx, "value", value, EMPTY);
		desc.defineProperty(cx, "writable", (attributes & READONLY) == 0, EMPTY);
		desc.defineProperty(cx, "enumerable", (attributes & DONTENUM) == 0, EMPTY);
		desc.defineProperty(cx, "configurable", (attributes & PERMANENT) == 0, EMPTY);
		return desc;
	}

	static void checkValidAttributes(int attributes) {
		final int mask = READONLY | DONTENUM | PERMANENT | UNINITIALIZED_CONST;
		if ((attributes & ~mask) != 0) {
			throw new IllegalArgumentException(String.valueOf(attributes));
		}
	}

	private static SlotMapContainer createSlotMap(int initialSize) {
		return new SlotMapContainer(initialSize);
	}

	public ScriptableObject() {
		slotMap = createSlotMap(0);
	}

	public ScriptableObject(Scriptable scope, Scriptable prototype) {
		if (scope == null) {
			throw new IllegalArgumentException();
		}

		parentScopeObject = scope;
		prototypeObject = prototype;
		slotMap = createSlotMap(0);
	}

	/**
	 * Gets the value that will be returned by calling the typeof operator on this object.
	 *
	 * @return default is "object" unless {@link #avoidObjectDetection()} is <code>true</code> in which
	 * case it returns "undefined"
	 */
	@Override
	public MemberType getTypeOf() {
		return avoidObjectDetection() ? MemberType.UNDEFINED : MemberType.OBJECT;
	}

	/**
	 * Return the name of the class.
	 * <p>
	 * This is typically the same name as the constructor.
	 * Classes extending ScriptableObject must implement this abstract
	 * method.
	 */
	@Override
	public abstract String getClassName();

	/**
	 * Returns true if the named property is defined.
	 *
	 * @param name  the name of the property
	 * @param start the object in which the lookup began
	 * @return true if and only if the property was found in the object
	 */
	@Override
	public boolean has(Context cx, String name, Scriptable start) {
		return null != slotMap.query(cx, name, 0);
	}

	/**
	 * Returns true if the property index is defined.
	 *
	 * @param index the numeric index for the property
	 * @param start the object in which the lookup began
	 * @return true if and only if the property was found in the object
	 */
	@Override
	public boolean has(Context cx, int index, Scriptable start) {
		return null != slotMap.query(cx, null, index);
	}

	/**
	 * A version of "has" that supports symbols.
	 */
	@Override
	public boolean has(Context cx, Symbol key, Scriptable start) {
		return null != slotMap.query(cx, key, 0);
	}

	/**
	 * Returns the value of the named property or NOT_FOUND.
	 * <p>
	 * If the property was created using defineProperty, the
	 * appropriate getter method is called.
	 *
	 * @param name  the name of the property
	 * @param start the object in which the lookup began
	 * @return the value of the property (may be null), or NOT_FOUND
	 */
	@Override
	public Object get(Context cx, String name, Scriptable start) {
		Slot slot = slotMap.query(cx, name, 0);
		if (slot == null) {
			return NOT_FOUND;
		}
		return slot.getValue(cx, start);
	}

	/**
	 * Returns the value of the indexed property or NOT_FOUND.
	 *
	 * @param index the numeric index for the property
	 * @param start the object in which the lookup began
	 * @return the value of the property (may be null), or NOT_FOUND
	 */
	@Override
	public Object get(Context cx, int index, Scriptable start) {
		Slot slot = slotMap.query(cx, null, index);
		if (slot == null) {
			return NOT_FOUND;
		}
		return slot.getValue(cx, start);
	}

	/**
	 * Another version of Get that supports Symbol keyed properties.
	 */
	@Override
	public Object get(Context cx, Symbol key, Scriptable start) {
		Slot slot = slotMap.query(cx, key, 0);
		if (slot == null) {
			return NOT_FOUND;
		}
		return slot.getValue(cx, start);
	}

	/**
	 * Sets the value of the named property, creating it if need be.
	 * <p>
	 * If the property was created using defineProperty, the
	 * appropriate setter method is called. <p>
	 * <p>
	 * If the property's attributes include READONLY, no action is
	 * taken.
	 * This method will actually set the property in the start
	 * object.
	 *
	 * @param name  the name of the property
	 * @param start the object whose property is being set
	 * @param value value to set the property to
	 */
	@Override
	public void put(Context cx, String name, Scriptable start, Object value) {
		if (putImpl(cx, name, 0, start, value)) {
			return;
		}

		if (start == this) {
			throw Kit.codeBug();
		}
		start.put(cx, name, start, value);
	}

	/**
	 * Sets the value of the indexed property, creating it if need be.
	 *
	 * @param index the numeric index for the property
	 * @param start the object whose property is being set
	 * @param value value to set the property to
	 */
	@Override
	public void put(Context cx, int index, Scriptable start, Object value) {
		if (putImpl(cx, null, index, start, value)) {
			return;
		}

		if (start == this) {
			throw Kit.codeBug();
		}
		start.put(cx, index, start, value);
	}

	/**
	 * Implementation of put required by SymbolScriptable objects.
	 */
	@Override
	public void put(Context cx, Symbol key, Scriptable start, Object value) {
		if (putImpl(cx, key, 0, start, value)) {
			return;
		}

		if (start == this) {
			throw Kit.codeBug();
		}
		ensureSymbolScriptable(start).put(cx, key, start, value);
	}

	/**
	 * Removes a named property from the object.
	 * <p>
	 * If the property is not found, or it has the PERMANENT attribute,
	 * no action is taken.
	 *
	 * @param name the name of the property
	 */
	@Override
	public void delete(Context cx, Scriptable scope, String name) {
		Slot s = slotMap.query(cx, name, 0);
		slotMap.remove(cx, name, 0);
		Deletable.deleteObject(s == null ? null : s.value);
	}

	/**
	 * Removes the indexed property from the object.
	 * <p>
	 * If the property is not found, or it has the PERMANENT attribute,
	 * no action is taken.
	 *
	 * @param index the numeric index for the property
	 */
	@Override
	public void delete(Context cx, Scriptable scope, int index) {
		Slot s = slotMap.query(cx, null, index);
		slotMap.remove(cx, null, index);
		Deletable.deleteObject(s == null ? null : s.value);
	}

	/**
	 * Removes an object like the others, but using a Symbol as the key.
	 */
	@Override
	public void delete(Context cx, Scriptable scope, Symbol key) {
		slotMap.remove(cx, key, 0);
	}

	/**
	 * Sets the value of the named const property, creating it if need be.
	 * <p>
	 * If the property was created using defineProperty, the
	 * appropriate setter method is called. <p>
	 * <p>
	 * If the property's attributes include READONLY, no action is
	 * taken.
	 * This method will actually set the property in the start
	 * object.
	 *
	 * @param name  the name of the property
	 * @param start the object whose property is being set
	 * @param value value to set the property to
	 */
	@Override
	public void putConst(Context cx, String name, Scriptable start, Object value) {
		if (putConstImpl(cx, name, 0, start, value, READONLY)) {
			return;
		}

		if (start == this) {
			throw Kit.codeBug();
		}
		if (start instanceof ConstProperties) {
			((ConstProperties) start).putConst(cx, name, start, value);
		} else {
			start.put(cx, name, start, value);
		}
	}

	@Override
	public void defineConst(Context cx, String name, Scriptable start) {
		if (putConstImpl(cx, name, 0, start, Undefined.instance, UNINITIALIZED_CONST)) {
			return;
		}

		if (start == this) {
			throw Kit.codeBug();
		}
		if (start instanceof ConstProperties) {
			((ConstProperties) start).defineConst(cx, name, start);
		}
	}

	/**
	 * Returns true if the named property is defined as a const on this object.
	 *
	 * @param name
	 * @return true if the named property is defined as a const, false
	 * otherwise.
	 */
	@Override
	public boolean isConst(Context cx, String name) {
		Slot slot = slotMap.query(cx, name, 0);
		if (slot == null) {
			return false;
		}
		return (slot.getAttributes() & (PERMANENT | READONLY)) == (PERMANENT | READONLY);

	}

	/**
	 * Get the attributes of a named property.
	 * <p>
	 * The property is specified by <code>name</code>
	 * as defined for <code>has</code>.<p>
	 *
	 * @param name the identifier for the property
	 * @return the bitset of attributes
	 * @throws EvaluatorException if the named property is not found
	 * @see ScriptableObject#has(Context, String, Scriptable)
	 * @see ScriptableObject#READONLY
	 * @see ScriptableObject#DONTENUM
	 * @see ScriptableObject#PERMANENT
	 * @see ScriptableObject#EMPTY
	 */
	public int getAttributes(Context cx, String name) {
		return findAttributeSlot(cx, name, 0, SlotAccess.QUERY).getAttributes();
	}

	/**
	 * Get the attributes of an indexed property.
	 *
	 * @param index the numeric index for the property
	 * @return the bitset of attributes
	 * @throws EvaluatorException if the named property is not found
	 *                            is not found
	 * @see ScriptableObject#has(Context, String, Scriptable)
	 * @see ScriptableObject#READONLY
	 * @see ScriptableObject#DONTENUM
	 * @see ScriptableObject#PERMANENT
	 * @see ScriptableObject#EMPTY
	 */
	public int getAttributes(Context cx, int index) {
		return findAttributeSlot(cx, null, index, SlotAccess.QUERY).getAttributes();
	}

	public int getAttributes(Context cx, Symbol sym) {
		return findAttributeSlot(cx, sym, SlotAccess.QUERY).getAttributes();
	}


	/**
	 * Set the attributes of a named property.
	 * <p>
	 * The property is specified by <code>name</code>
	 * as defined for <code>has</code>.<p>
	 * <p>
	 * The possible attributes are READONLY, DONTENUM,
	 * and PERMANENT. Combinations of attributes
	 * are expressed by the bitwise OR of attributes.
	 * EMPTY is the state of no attributes set. Any unused
	 * bits are reserved for future use.
	 *
	 * @param name       the name of the property
	 * @param attributes the bitset of attributes
	 * @throws EvaluatorException if the named property is not found
	 * @see Scriptable#has(Context, String, Scriptable)
	 * @see ScriptableObject#READONLY
	 * @see ScriptableObject#DONTENUM
	 * @see ScriptableObject#PERMANENT
	 * @see ScriptableObject#EMPTY
	 */
	public void setAttributes(Context cx, String name, int attributes) {
		findAttributeSlot(cx, name, 0, SlotAccess.MODIFY).setAttributes(attributes);
	}

	/**
	 * Set the attributes of an indexed property.
	 *
	 * @param index      the numeric index for the property
	 * @param attributes the bitset of attributes
	 * @throws EvaluatorException if the named property is not found
	 * @see Scriptable#has(Context, String, Scriptable)
	 * @see ScriptableObject#READONLY
	 * @see ScriptableObject#DONTENUM
	 * @see ScriptableObject#PERMANENT
	 * @see ScriptableObject#EMPTY
	 */
	public void setAttributes(Context cx, int index, int attributes) {
		findAttributeSlot(cx, null, index, SlotAccess.MODIFY).setAttributes(attributes);
	}

	/**
	 * Set attributes of a Symbol-keyed property.
	 */
	public void setAttributes(Context cx, Symbol key, int attributes) {
		findAttributeSlot(cx, key, SlotAccess.MODIFY).setAttributes(attributes);
	}

	/**
	 * XXX: write docs.
	 */
	public void setGetterOrSetter(Context cx, String name, int index, Callable getterOrSetter, boolean isSetter) {
		setGetterOrSetter(cx, name, index, getterOrSetter, isSetter, false);
	}

	private void setGetterOrSetter(Context cx, String name, int index, Callable getterOrSetter, boolean isSetter, boolean force) {
		if (name != null && index != 0) {
			throw new IllegalArgumentException(name);
		}

		final GetterSlot gslot;
		if (isExtensible()) {
			gslot = (GetterSlot) slotMap.get(cx, name, index, SlotAccess.MODIFY_GETTER_SETTER);
		} else {
			Slot slot = slotMap.query(cx, name, index);
			if (!(slot instanceof GetterSlot)) {
				return;
			}
			gslot = (GetterSlot) slot;
		}

		if (!force) {
			int attributes = gslot.getAttributes();
			if ((attributes & READONLY) != 0) {
				throw Context.reportRuntimeError1(cx, "msg.modify.readonly", name);
			}
		}
		if (isSetter) {
			gslot.setter = getterOrSetter;
		} else {
			gslot.getter = getterOrSetter;
		}
		gslot.value = Undefined.instance;
	}

	/**
	 * Get the getter or setter for a given property. Used by __lookupGetter__
	 * and __lookupSetter__.
	 *
	 * @param name     Name of the object. If nonnull, index must be 0.
	 * @param index    Index of the object. If nonzero, name must be null.
	 * @param isSetter If true, return the setter, otherwise return the getter.
	 * @return Null if the property does not exist. Otherwise returns either
	 * the getter or the setter for the property, depending on
	 * the value of isSetter (may be undefined if unset).
	 * @throws IllegalArgumentException if both name and index are nonnull
	 *                                  and nonzero respectively.
	 */
	public Object getGetterOrSetter(Context cx, String name, int index, boolean isSetter) {
		if (name != null && index != 0) {
			throw new IllegalArgumentException(name);
		}
		Slot slot = slotMap.query(cx, name, index);
		if (slot == null) {
			return null;
		}
		if (slot instanceof GetterSlot gslot) {
			Object result = isSetter ? gslot.setter : gslot.getter;
			return result != null ? result : Undefined.instance;
		}
		return Undefined.instance;
	}

	/**
	 * Returns whether a property is a getter or a setter
	 *
	 * @param name   property name
	 * @param index  property index
	 * @param setter true to check for a setter, false for a getter
	 * @return whether the property is a getter or a setter
	 */
	protected boolean isGetterOrSetter(Context cx, String name, int index, boolean setter) {
		Slot slot = slotMap.query(cx, name, index);
		if (slot instanceof GetterSlot) {
			if (setter && ((GetterSlot) slot).setter != null) {
				return true;
			}
			return !setter && ((GetterSlot) slot).getter != null;
		}
		return false;
	}

	/**
	 * Returns the prototype of the object.
	 */
	@Override
	public Scriptable getPrototype(Context cx) {
		return prototypeObject;
	}

	/**
	 * Sets the prototype of the object.
	 */
	@Override
	public void setPrototype(Context cx, Scriptable m) {
		prototypeObject = m;
	}

	/**
	 * Returns the parent (enclosing) scope of the object.
	 */
	@Override
	public Scriptable getParentScope() {
		return parentScopeObject;
	}

	/**
	 * Sets the parent (enclosing) scope of the object.
	 */
	@Override
	public void setParentScope(Scriptable m) {
		parentScopeObject = m;
	}

	/**
	 * Returns an array of ids for the properties of the object.
	 *
	 * <p>Any properties with the attribute DONTENUM are not listed. <p>
	 *
	 * @return an array of java.lang.Objects with an entry for every
	 * listed property. Properties accessed via an integer index will
	 * have a corresponding
	 * Integer entry in the returned array. Properties accessed by
	 * a String will have a String entry in the returned array.
	 */
	@Override
	public Object[] getIds(Context cx) {
		return getIds(cx, false, false);
	}

	/**
	 * Returns an array of ids for the properties of the object.
	 *
	 * <p>All properties, even those with attribute DONTENUM, are listed. <p>
	 *
	 * @return an array of java.lang.Objects with an entry for every
	 * listed property. Properties accessed via an integer index will
	 * have a corresponding
	 * Integer entry in the returned array. Properties accessed by
	 * a String will have a String entry in the returned array.
	 */
	@Override
	public Object[] getAllIds(Context cx) {
		return getIds(cx, true, false);
	}

	/**
	 * Implements the [[DefaultValue]] internal method.
	 *
	 * <p>Note that the toPrimitive conversion is a no-op for
	 * every type other than Object, for which [[DefaultValue]]
	 * is called. See ECMA 9.1.<p>
	 * <p>
	 * A <code>hint</code> of null means "no hint".
	 *
	 * @param typeHint the type hint
	 * @return the default value for the object
	 * <p>
	 * See ECMA 8.6.2.6.
	 */
	@Override
	public Object getDefaultValue(Context cx, Class<?> typeHint) {
		for (int i = 0; i < 2; i++) {
			boolean tryToString;
			if (typeHint == ScriptRuntime.StringClass) {
				tryToString = (i == 0);
			} else {
				tryToString = (i == 1);
			}

			String methodName;
			if (tryToString) {
				methodName = "toString";
			} else {
				methodName = "valueOf";
			}
			Object v = getProperty(cx, this, methodName);
			if (!(v instanceof Function fun)) {
				continue;
			}
			v = fun.call(cx, fun.getParentScope(), this, ScriptRuntime.EMPTY_OBJECTS);
			if (v != null) {
				if (!(v instanceof Scriptable)) {
					return v;
				}
				if (typeHint == ScriptRuntime.ScriptableClass || typeHint == ScriptRuntime.FunctionClass) {
					return v;
				}
				if (tryToString && v instanceof Wrapper) {
					// Let a wrapped java.lang.String pass for a primitive
					// string.
					Object u = ((Wrapper) v).unwrap();
					if (u instanceof String) {
						return u;
					}
				}
			}
		}
		// fall through to error
		String arg = (typeHint == null) ? "undefined" : typeHint.getName();
		throw ScriptRuntime.typeError1("msg.default.value", arg);
	}

	/**
	 * Implements the instanceof operator.
	 *
	 * <p>This operator has been proposed to ECMA.
	 *
	 * @param instance The value that appeared on the LHS of the instanceof
	 *                 operator
	 * @return true if "this" appears in value's prototype chain
	 */
	@Override
	public boolean hasInstance(Context cx, Scriptable instance) {
		// Default for JS objects (other than Function) is to do prototype
		// chasing.  This will be overridden in NativeFunction and non-JS
		// objects.

		return ScriptRuntime.jsDelegatesTo(cx, instance, this);
	}

	/**
	 * Emulate the SpiderMonkey (and Firefox) feature of allowing
	 * custom objects to avoid detection by normal "object detection"
	 * code patterns. This is used to implement document.all.
	 * See https://bugzilla.mozilla.org/show_bug.cgi?id=412247.
	 * This is an analog to JOF_DETECTING from SpiderMonkey; see
	 * https://bugzilla.mozilla.org/show_bug.cgi?id=248549.
	 * Other than this special case, embeddings should return false.
	 *
	 * @return true if this object should avoid object detection
	 * @since 1.7R1
	 */
	public boolean avoidObjectDetection() {
		return false;
	}

	/**
	 * Custom <code>==</code> operator.
	 * Must return {@link Scriptable#NOT_FOUND} if this object does not
	 * have custom equality operator for the given value,
	 * <code>Boolean.TRUE</code> if this object is equivalent to <code>value</code>,
	 * <code>Boolean.FALSE</code> if this object is not equivalent to
	 * <code>value</code>.
	 * <p>
	 * The default implementation returns Boolean.TRUE
	 * if <code>this == value</code> or {@link Scriptable#NOT_FOUND} otherwise.
	 * It indicates that by default custom equality is available only if
	 * <code>value</code> is <code>this</code> in which case true is returned.
	 */
	protected Object equivalentValues(Object value) {
		return (this == value) ? Boolean.TRUE : NOT_FOUND;
	}

	/**
	 * Defines JavaScript objects from a Java class, optionally
	 * allowing sealing and mapping of Java inheritance to JavaScript
	 * prototype-based inheritance.
	 * <p>
	 * Similar to <code>defineClass(Scriptable scope, Class clazz)</code>
	 * except that sealing and inheritance mapping are allowed. An object
	 * that is sealed cannot have properties added or removed. Note that
	 * sealing is not allowed in the current ECMA/ISO language specification,
	 * but is likely for the next version.
	 *
	 * @param scope          The scope in which to define the constructor.
	 * @param classData      The Java class to use to define the JavaScript objects
	 *                       and properties. The class must implement Scriptable.
	 * @param mapInheritance Whether or not to map Java inheritance to
	 *                       JavaScript prototype-based inheritance.
	 * @return the class name for the prototype of the specified class
	 * @throws IllegalAccessException    if access is not available
	 *                                   to a reflected class member
	 * @throws InstantiationException    if unable to instantiate
	 *                                   the named class
	 * @throws InvocationTargetException if an exception is thrown
	 *                                   during execution of methods of the named class
	 * @since 1.6R2
	 */
	public static <T extends Scriptable> String defineClass(Context cx, Scriptable scope, ClassData classData, boolean mapInheritance) throws Exception {
		BaseFunction ctor = buildClassCtor(cx, scope, classData, mapInheritance);
		if (ctor == null) {
			return null;
		}
		String name = ctor.getClassPrototype(cx).getClassName();
		defineProperty(cx, scope, name, ctor, ScriptableObject.DONTENUM);
		return name;
	}

	static <T extends Scriptable> BaseFunction buildClassCtor(Context cx, Scriptable scope, ClassData classData, boolean mapInheritance) throws Exception {
		// If we got here, there isn't an "init" method with the right
		// parameter types.

		var ctors = classData.publicClassData.getConstructors();
		ConstructorInfo protoCtor = null;
		for (var constructor : ctors) {
			if (constructor.signature.isEmpty()) {
				protoCtor = constructor;
				break;
			}
		}
		if (protoCtor == null) {
			throw Context.reportRuntimeError1(cx, "msg.zero.arg.ctor", classData.toString());
		}

		Scriptable proto = (Scriptable) protoCtor.newInstance(ScriptRuntime.EMPTY_OBJECTS);
		String className = proto.getClassName();

		// check for possible redefinition
		Object existing = getProperty(cx, getTopLevelScope(scope), className);
		if (existing instanceof BaseFunction) {
			Object existingProto = ((BaseFunction) existing).getPrototypeProperty(cx);
			if (existingProto != null && classData.publicClassData.type == existingProto.getClass()) {
				return (BaseFunction) existing;
			}
		}

		// Set the prototype's prototype, trying to map Java inheritance to JS
		// prototype-based inheritance if requested to do so.
		Scriptable superProto = null;
		if (mapInheritance) {
			Class<?> superClass = classData.publicClassData.type.getSuperclass();
			if (ScriptRuntime.ScriptableClass.isAssignableFrom(superClass) && !Modifier.isAbstract(superClass.getModifiers())) {
				Class<? extends Scriptable> superScriptable = extendsScriptable(superClass);
				String name = ScriptableObject.defineClass(cx, scope, ClassData.of(cx, scope, superScriptable), mapInheritance);
				if (name != null) {
					superProto = ScriptableObject.getClassPrototype(cx, scope, name);
				}
			}
		}
		if (superProto == null) {
			superProto = ScriptableObject.getObjectPrototype(cx, scope);
		}
		proto.setPrototype(cx, superProto);

		ConstructorInfo ctorMember = null;

		if (ctors.length == 1) {
			ctorMember = ctors[0];
		} else if (ctors.length == 2) {
			if (ctors[0].signature.isEmpty()) {
				ctorMember = ctors[1];
			} else if (ctors[1].signature.isEmpty()) {
				ctorMember = ctors[0];
			}
		}
		if (ctorMember == null) {
			throw Context.reportRuntimeError1(cx, "msg.ctor.multiple.parms", classData.publicClassData.type.getName());
		}

		FunctionObject ctor = new FunctionObject(cx, className, ctorMember, scope);
		if (ctor.isVarArgsMethod()) {
			throw Context.reportRuntimeError1(cx, "msg.varargs.ctor", ctorMember.getName());
		}
		ctor.initAsConstructor(cx, scope, proto);
		return ctor;
	}

	@SuppressWarnings({"unchecked"})
	private static <T extends Scriptable> Class<T> extendsScriptable(Class<?> c) {
		if (ScriptRuntime.ScriptableClass.isAssignableFrom(c)) {
			return (Class<T>) c;
		}
		return null;
	}

	/**
	 * Define a JavaScript property.
	 * <p>
	 * Creates the property with an initial value and sets its attributes.
	 *
	 * @param propertyName the name of the property to define.
	 * @param value        the initial value of the property
	 * @param attributes   the attributes of the JavaScript property
	 * @see Scriptable#put(Context, String, Scriptable, Object)
	 */
	public void defineProperty(Context cx, String propertyName, Object value, int attributes) {
		put(cx, propertyName, this, value);
		setAttributes(cx, propertyName, attributes);
	}

	/**
	 * A version of defineProperty that uses a Symbol key.
	 *
	 * @param key        symbol of the property to define.
	 * @param value      the initial value of the property
	 * @param attributes the attributes of the JavaScript property
	 */
	public void defineProperty(Context cx, Symbol key, Object value, int attributes) {
		put(cx, key, this, value);
		setAttributes(cx, key, attributes);
	}

	/**
	 * Utility method to add properties to arbitrary Scriptable object.
	 * If destination is instance of ScriptableObject, calls
	 * defineProperty there, otherwise calls put in destination
	 * ignoring attributes
	 *
	 * @param destination  ScriptableObject to define the property on
	 * @param propertyName the name of the property to define.
	 * @param value        the initial value of the property
	 * @param attributes   the attributes of the JavaScript property
	 */
	public static void defineProperty(Context cx, Scriptable destination, String propertyName, Object value, int attributes) {
		if (!(destination instanceof ScriptableObject so)) {
			destination.put(cx, propertyName, destination, value);
			return;
		}
		so.defineProperty(cx, propertyName, value, attributes);
	}

	/**
	 * Utility method to add properties to arbitrary Scriptable object.
	 * If destination is instance of ScriptableObject, calls
	 * defineProperty there, otherwise calls put in destination
	 * ignoring attributes
	 *
	 * @param destination  ScriptableObject to define the property on
	 * @param propertyName the name of the property to define.
	 */
	public static void defineConstProperty(Context cx, Scriptable destination, String propertyName) {
		if (destination instanceof ConstProperties cp) {
			cp.defineConst(cx, propertyName, destination);
		} else {
			defineProperty(cx, destination, propertyName, Undefined.instance, CONST);
		}
	}

	/**
	 * Define a JavaScript property with getter and setter side effects.
	 * <p>
	 * If the setter is not found, the attribute READONLY is added to
	 * the given attributes. <p>
	 * <p>
	 * The getter must be a method with zero parameters, and the setter, if
	 * found, must be a method with one parameter.<p>
	 *
	 * @param propertyName the name of the property to define. This name
	 *                     also affects the name of the setter and getter
	 *                     to search for. If the propertyId is "foo", then
	 *                     <code>clazz</code> will be searched for "getFoo"
	 *                     and "setFoo" methods.
	 * @param clazz        the Java class to search for the getter and setter
	 * @param attributes   the attributes of the JavaScript property
	 * @see Scriptable#put(Context, String, Scriptable, Object)
	 */
	public void defineProperty(Context cx, String propertyName, Class<?> clazz, int attributes) {
		int length = propertyName.length();
		if (length == 0) {
			throw new IllegalArgumentException();
		}
		char[] buf = new char[3 + length];
		propertyName.getChars(0, length, buf, 3);
		buf[3] = Character.toUpperCase(buf[3]);
		buf[0] = 'g';
		buf[1] = 'e';
		buf[2] = 't';
		String getterName = new String(buf);
		buf[0] = 's';
		String setterName = new String(buf);

		MethodInfo[] methods = PublicClassData.of(clazz).getDeclaredMethods();
		MethodInfo getter = FunctionObject.findSingleMethod(cx, methods, getterName);
		MethodInfo setter = FunctionObject.findSingleMethod(cx, methods, setterName);
		if (setter == null) {
			attributes |= ScriptableObject.READONLY;
		}
		defineProperty(cx, propertyName, null, getter, setter, attributes);
	}

	/**
	 * Define a JavaScript property.
	 * <p>
	 * Use this method only if you wish to define getters and setters for
	 * a given property in a ScriptableObject. To create a property without
	 * special getter or setter side effects, use
	 * <code>defineProperty(String,int)</code>.
	 * <p>
	 * If <code>setter</code> is null, the attribute READONLY is added to
	 * the given attributes.<p>
	 * <p>
	 * Several forms of getters or setters are allowed. In all cases the
	 * type of the value parameter can be any one of the following types:
	 * Object, String, boolean, Scriptable, byte, short, int, long, float,
	 * or double. The runtime will perform appropriate conversions based
	 * upon the type of the parameter (see description in FunctionObject).
	 * The first forms are nonstatic methods of the class referred to
	 * by 'this':
	 * <pre>
	 * Object getFoo();
	 * void setFoo(SomeType value);</pre>
	 * Next are static methods that may be of any class; the object whose
	 * property is being accessed is passed in as an extra argument:
	 * <pre>
	 * static Object getFoo(Scriptable obj);
	 * static void setFoo(Scriptable obj, SomeType value);</pre>
	 * Finally, it is possible to delegate to another object entirely using
	 * the <code>delegateTo</code> parameter. In this case the methods are
	 * nonstatic methods of the class delegated to, and the object whose
	 * property is being accessed is passed in as an extra argument:
	 * <pre>
	 * Object getFoo(Scriptable obj);
	 * void setFoo(Scriptable obj, SomeType value);</pre>
	 *
	 * @param propertyName the name of the property to define.
	 * @param delegateTo   an object to call the getter and setter methods on,
	 *                     or null, depending on the form used above.
	 * @param getter       the method to invoke to get the value of the property
	 * @param setter       the method to invoke to set the value of the property
	 * @param attributes   the attributes of the JavaScript property
	 */
	public void defineProperty(Context cx, String propertyName, Object delegateTo, BaseMember getter, BaseMember setter, int attributes) {
		BaseMember getterBox = getter;
		if (getter != null) {

			boolean delegatedForm;
			if (!getter.isStatic()) {
				delegatedForm = (delegateTo != null);
				getterBox = new DelegatedMember(delegateTo, getterBox);
			} else {
				delegatedForm = true;
				// Ignore delegateTo for static getter but store
				// non-null delegateTo indicator.
				getterBox = new DelegatedMember(Void.TYPE, getterBox);
			}

			String errorId = null;
			Class<?>[] parmTypes = getter.getSignature().types;
			if (parmTypes.length == 0) {
				if (delegatedForm) {
					errorId = "msg.obj.getter.parms";
				}
			} else if (parmTypes.length == 1) {
				Object argType = parmTypes[0];
				// Allow ScriptableObject for compatibility
				if (!(argType == ScriptRuntime.ScriptableClass || argType == ScriptRuntime.ScriptableObjectClass)) {
					errorId = "msg.bad.getter.parms";
				} else if (!delegatedForm) {
					errorId = "msg.bad.getter.parms";
				}
			} else {
				errorId = "msg.bad.getter.parms";
			}
			if (errorId != null) {
				throw Context.reportRuntimeError1(cx, errorId, getter.toString());
			}
		}

		BaseMember setterBox = setter;
		if (setter != null) {
			if (setter.getType() != Void.TYPE) {
				throw Context.reportRuntimeError1(cx, "msg.setter.return", setter.toString());
			}

			boolean delegatedForm;
			if (!setter.isStatic()) {
				delegatedForm = (delegateTo != null);
				setterBox = new DelegatedMember(delegateTo, setterBox);
			} else {
				delegatedForm = true;
				// Ignore delegateTo for static setter but store
				// non-null delegateTo indicator.
				setterBox = new DelegatedMember(Void.TYPE, setterBox);
			}

			String errorId = null;
			Class<?>[] parmTypes = setter.getSignature().types;
			if (parmTypes.length == 1) {
				if (delegatedForm) {
					errorId = "msg.setter2.expected";
				}
			} else if (parmTypes.length == 2) {
				Object argType = parmTypes[0];
				// Allow ScriptableObject for compatibility
				if (!(argType == ScriptRuntime.ScriptableClass || argType == ScriptRuntime.ScriptableObjectClass)) {
					errorId = "msg.setter2.parms";
				} else if (!delegatedForm) {
					errorId = "msg.setter1.parms";
				}
			} else {
				errorId = "msg.setter.parms";
			}
			if (errorId != null) {
				throw Context.reportRuntimeError1(cx, errorId, setter.toString());
			}
		}

		GetterSlot gslot = (GetterSlot) slotMap.get(cx, propertyName, 0, SlotAccess.MODIFY_GETTER_SETTER);
		gslot.setAttributes(attributes);
		gslot.getter = getterBox;
		gslot.setter = setterBox;
	}

	/**
	 * Defines one or more properties on this object.
	 *
	 * @param cx    the current Context
	 * @param props a map of property ids to property descriptors
	 */
	public void defineOwnProperties(Context cx, Scriptable scope, ScriptableObject props) {
		Object[] ids = props.getIds(cx, false, true);
		ScriptableObject[] descs = new ScriptableObject[ids.length];
		for (int i = 0, len = ids.length; i < len; ++i) {
			Object descObj = ScriptRuntime.getObjectElem(cx, props, ids[i]);
			ScriptableObject desc = ensureScriptableObject(descObj);
			checkPropertyDefinition(cx, desc);
			descs[i] = desc;
		}
		for (int i = 0, len = ids.length; i < len; ++i) {
			defineOwnProperty(cx, ids[i], descs[i]);
		}
	}

	/**
	 * Defines a property on an object.
	 *
	 * @param cx   the current Context
	 * @param id   the name/index of the property
	 * @param desc the new property descriptor, as described in 8.6.1
	 */
	public void defineOwnProperty(Context cx, Object id, Scriptable desc) {
		checkPropertyDefinition(cx, desc);
		defineOwnProperty(cx, id, desc, true);
	}

	/**
	 * Defines a property on an object.
	 * <p>
	 * Based on [[DefineOwnProperty]] from 8.12.10 of the spec.
	 *
	 * @param cx         the current Context
	 * @param id         the name/index of the property
	 * @param desc       the new property descriptor, as described in 8.6.1
	 * @param checkValid whether to perform validity checks
	 */
	protected void defineOwnProperty(Context cx, Object id, Scriptable desc, boolean checkValid) {

		Slot slot = getSlot(cx, id, SlotAccess.QUERY);
		boolean isNew = slot == null;

		if (checkValid) {
			ScriptableObject current = slot == null ? null : slot.getPropertyDescriptor(cx, this);
			checkPropertyChange(cx, id, current, desc);
		}

		boolean isAccessor = isAccessorDescriptor(cx, desc);
		final int attributes;

		if (slot == null) { // new slot
			slot = getSlot(cx, id, isAccessor ? SlotAccess.MODIFY_GETTER_SETTER : SlotAccess.MODIFY);
			attributes = applyDescriptorToAttributeBitset(cx, DONTENUM | READONLY | PERMANENT, desc);
		} else {
			attributes = applyDescriptorToAttributeBitset(cx, slot.getAttributes(), desc);
		}

		if (isAccessor) {
			if (!(slot instanceof GetterSlot)) {
				slot = getSlot(cx, id, SlotAccess.MODIFY_GETTER_SETTER);
			}

			GetterSlot gslot = (GetterSlot) slot;

			Object getter = getProperty(cx, desc, "get");
			if (getter != NOT_FOUND) {
				gslot.getter = getter;
			}
			Object setter = getProperty(cx, desc, "set");
			if (setter != NOT_FOUND) {
				gslot.setter = setter;
			}

			gslot.value = Undefined.instance;
			gslot.setAttributes(attributes);
		} else {
			if (slot instanceof GetterSlot && isDataDescriptor(cx, desc)) {
				slot = getSlot(cx, id, SlotAccess.CONVERT_ACCESSOR_TO_DATA);
			}

			Object value = getProperty(cx, desc, "value");
			if (value != NOT_FOUND) {
				slot.value = value;
			} else if (isNew) {
				slot.value = Undefined.instance;
			}
			slot.setAttributes(attributes);
		}
	}

	protected void checkPropertyDefinition(Context cx, Scriptable desc) {
		Object getter = getProperty(cx, desc, "get");
		if (getter != NOT_FOUND && getter != Undefined.instance && !(getter instanceof Callable)) {
			throw ScriptRuntime.notFunctionError(getter);
		}
		Object setter = getProperty(cx, desc, "set");
		if (setter != NOT_FOUND && setter != Undefined.instance && !(setter instanceof Callable)) {
			throw ScriptRuntime.notFunctionError(setter);
		}
		if (isDataDescriptor(cx, desc) && isAccessorDescriptor(cx, desc)) {
			throw ScriptRuntime.typeError0("msg.both.data.and.accessor.desc");
		}
	}

	protected void checkPropertyChange(Context cx, Object id, Scriptable current, Scriptable desc) {
		if (current == null) { // new property
			if (!isExtensible()) {
				throw ScriptRuntime.typeError0("msg.not.extensible");
			}
		} else {
			if (isFalse(cx, current.get(cx, "configurable", current))) {
				if (isTrue(cx, getProperty(cx, desc, "configurable"))) {
					throw ScriptRuntime.typeError1("msg.change.configurable.false.to.true", id);
				}
				if (isTrue(cx, current.get(cx, "enumerable", current)) != isTrue(cx, getProperty(cx, desc, "enumerable"))) {
					throw ScriptRuntime.typeError1("msg.change.enumerable.with.configurable.false", id);
				}
				boolean isData = isDataDescriptor(cx, desc);
				boolean isAccessor = isAccessorDescriptor(cx, desc);
				if (!isData && !isAccessor) {
					// no further validation required for generic descriptor
				} else if (isData && isDataDescriptor(cx, current)) {
					if (isFalse(cx, current.get(cx, "writable", current))) {
						if (isTrue(cx, getProperty(cx, desc, "writable"))) {
							throw ScriptRuntime.typeError1("msg.change.writable.false.to.true.with.configurable.false", id);
						}

						if (!sameValue(cx, current, getProperty(cx, desc, "value"), current.get(cx, "value", current))) {
							throw ScriptRuntime.typeError1("msg.change.value.with.writable.false", id);
						}
					}
				} else if (isAccessor && isAccessorDescriptor(cx, current)) {
					if (!sameValue(cx, current, getProperty(cx, desc, "set"), current.get(cx, "set", current))) {
						throw ScriptRuntime.typeError1("msg.change.setter.with.configurable.false", id);
					}

					if (!sameValue(cx, current, getProperty(cx, desc, "get"), current.get(cx, "get", current))) {
						throw ScriptRuntime.typeError1("msg.change.getter.with.configurable.false", id);
					}
				} else {
					if (isDataDescriptor(cx, current)) {
						throw ScriptRuntime.typeError1("msg.change.property.data.to.accessor.with.configurable.false", id);
					}
					throw ScriptRuntime.typeError1("msg.change.property.accessor.to.data.with.configurable.false", id);
				}
			}
		}
	}

	protected static boolean isTrue(Context cx, Object value) {
		return (value != NOT_FOUND) && ScriptRuntime.toBoolean(cx, value);
	}

	protected static boolean isFalse(Context cx, Object value) {
		return !isTrue(cx, value);
	}

	/**
	 * Implements SameValue as described in ES5 9.12, additionally checking
	 * if new value is defined.
	 *
	 * @param newValue     the new value
	 * @param currentValue the current value
	 * @return true if values are the same as defined by ES5 9.12
	 */
	protected boolean sameValue(Context cx, Scriptable scope, Object newValue, Object currentValue) {
		if (newValue == NOT_FOUND) {
			return true;
		}
		if (currentValue == NOT_FOUND) {
			currentValue = Undefined.instance;
		}
		// Special rules for numbers: NaN is considered the same value,
		// while zeroes with different signs are considered different.
		if (currentValue instanceof Number && newValue instanceof Number) {
			double d1 = ((Number) currentValue).doubleValue();
			double d2 = ((Number) newValue).doubleValue();
			if (Double.isNaN(d1) && Double.isNaN(d2)) {
				return true;
			}
			if (d1 == 0.0 && Double.doubleToLongBits(d1) != Double.doubleToLongBits(d2)) {
				return false;
			}
		}
		return ScriptRuntime.shallowEq(cx, scope, currentValue, newValue);
	}

	protected int applyDescriptorToAttributeBitset(Context cx, int attributes, Scriptable desc) {
		Object enumerable = getProperty(cx, desc, "enumerable");
		if (enumerable != NOT_FOUND) {
			attributes = ScriptRuntime.toBoolean(cx, enumerable) ? attributes & ~DONTENUM : attributes | DONTENUM;
		}

		Object writable = getProperty(cx, desc, "writable");
		if (writable != NOT_FOUND) {
			attributes = ScriptRuntime.toBoolean(cx, writable) ? attributes & ~READONLY : attributes | READONLY;
		}

		Object configurable = getProperty(cx, desc, "configurable");
		if (configurable != NOT_FOUND) {
			attributes = ScriptRuntime.toBoolean(cx, configurable) ? attributes & ~PERMANENT : attributes | PERMANENT;
		}

		return attributes;
	}

	/**
	 * Implements IsDataDescriptor as described in ES5 8.10.2
	 *
	 * @param desc a property descriptor
	 * @return true if this is a data descriptor.
	 */
	protected boolean isDataDescriptor(Context cx, Scriptable desc) {
		return hasProperty(cx, desc, "value") || hasProperty(cx, desc, "writable");
	}

	/**
	 * Implements IsAccessorDescriptor as described in ES5 8.10.1
	 *
	 * @param desc a property descriptor
	 * @return true if this is an accessor descriptor.
	 */
	protected boolean isAccessorDescriptor(Context cx, Scriptable desc) {
		return hasProperty(cx, desc, "get") || hasProperty(cx, desc, "set");
	}

	/**
	 * Implements IsGenericDescriptor as described in ES5 8.10.3
	 *
	 * @param desc a property descriptor
	 * @return true if this is a generic descriptor.
	 */
	protected boolean isGenericDescriptor(Context cx, Scriptable desc) {
		return !isDataDescriptor(cx, desc) && !isAccessorDescriptor(cx, desc);
	}

	protected static Scriptable ensureScriptable(Object arg) {
		if (!(arg instanceof Scriptable)) {
			throw ScriptRuntime.typeError1("msg.arg.not.object", ScriptRuntime.typeof(arg));
		}
		return (Scriptable) arg;
	}

	protected static SymbolScriptable ensureSymbolScriptable(Object arg) {
		if (!(arg instanceof SymbolScriptable)) {
			throw ScriptRuntime.typeError1("msg.object.not.symbolscriptable", ScriptRuntime.typeof(arg));
		}
		return (SymbolScriptable) arg;
	}

	protected static ScriptableObject ensureScriptableObject(Object arg) {
		if (!(arg instanceof ScriptableObject)) {
			throw ScriptRuntime.typeError1("msg.arg.not.object", ScriptRuntime.typeof(arg));
		}
		return (ScriptableObject) arg;
	}

	/**
	 * Search for names in a class, adding the resulting methods
	 * as properties.
	 *
	 * <p> Uses reflection to find the methods of the given names. Then
	 * FunctionObjects are constructed from the methods found, and
	 * are added to this object as properties with the given names.
	 *
	 * @param names      the names of the Methods to add as function properties
	 * @param classData  the class to search for the Methods
	 * @param attributes the attributes of the new properties
	 * @see FunctionObject
	 */
	public void defineFunctionProperties(Context cx, String[] names, ClassData classData, int attributes) {
		var methods = classData.publicClassData.getDeclaredMethods();
		for (String name : names) {
			MethodInfo m = FunctionObject.findSingleMethod(cx, methods, name);
			if (m == null) {
				throw Context.reportRuntimeError2(cx, "msg.method.not.found", name, classData.toString());
			}
			FunctionObject f = new FunctionObject(cx, name, m, this);
			defineProperty(cx, name, f, attributes);
		}
	}

	/**
	 * Get the Object.prototype property.
	 * See ECMA 15.2.4.
	 *
	 * @param scope an object in the scope chain
	 */
	public static Scriptable getObjectPrototype(Context cx, Scriptable scope) {
		return TopLevel.getBuiltinPrototype(cx, getTopLevelScope(scope), TopLevel.Builtins.Object);
	}

	/**
	 * Get the Function.prototype property.
	 * See ECMA 15.3.4.
	 *
	 * @param scope an object in the scope chain
	 */
	public static Scriptable getFunctionPrototype(Context cx, Scriptable scope) {
		return TopLevel.getBuiltinPrototype(cx, getTopLevelScope(scope), TopLevel.Builtins.Function);
	}

	public static Scriptable getGeneratorFunctionPrototype(Context cx, Scriptable scope) {
		return TopLevel.getBuiltinPrototype(cx, getTopLevelScope(scope), TopLevel.Builtins.GeneratorFunction);
	}

	public static Scriptable getArrayPrototype(Context cx, Scriptable scope) {
		return TopLevel.getBuiltinPrototype(cx, getTopLevelScope(scope), TopLevel.Builtins.Array);
	}

	/**
	 * Get the prototype for the named class.
	 * <p>
	 * For example, <code>getClassPrototype(s, "Date")</code> will first
	 * walk up the parent chain to find the outermost scope, then will
	 * search that scope for the Date constructor, and then will
	 * return Date.prototype. If any of the lookups fail, or
	 * the prototype is not a JavaScript object, then null will
	 * be returned.
	 *
	 * @param scope     an object in the scope chain
	 * @param className the name of the constructor
	 * @return the prototype for the named class, or null if it
	 * cannot be found.
	 */
	public static Scriptable getClassPrototype(Context cx, Scriptable scope, String className) {
		scope = getTopLevelScope(scope);
		Object ctor = getProperty(cx, scope, className);
		Object proto;
		if (ctor instanceof BaseFunction) {
			proto = ((BaseFunction) ctor).getPrototypeProperty(cx);
		} else if (ctor instanceof Scriptable ctorObj) {
			proto = ctorObj.get(cx, "prototype", ctorObj);
		} else {
			return null;
		}
		if (proto instanceof Scriptable) {
			return (Scriptable) proto;
		}
		return null;
	}

	/**
	 * Get the global scope.
	 *
	 * <p>Walks the parent scope chain to find an object with a null
	 * parent scope (the global object).
	 *
	 * @param obj a JavaScript object
	 * @return the corresponding global scope
	 */
	public static Scriptable getTopLevelScope(Scriptable obj) {
		for (; ; ) {
			Scriptable parent = obj.getParentScope();
			if (parent == null) {
				return obj;
			}
			obj = parent;
		}
	}

	public boolean isExtensible() {
		return isExtensible;
	}

	public void preventExtensions() {
		isExtensible = false;
	}

	/**
	 * Gets a named property from an object or any object in its prototype chain.
	 * <p>
	 * Searches the prototype chain for a property named <code>name</code>.
	 * <p>
	 *
	 * @param obj  a JavaScript object
	 * @param name a property name
	 * @return the value of a property with name <code>name</code> found in
	 * <code>obj</code> or any object in its prototype chain, or
	 * <code>Scriptable.NOT_FOUND</code> if not found
	 * @since 1.5R2
	 */
	public static Object getProperty(Context cx, Scriptable obj, String name) {
		Scriptable start = obj;
		Object result;
		do {
			result = obj.get(cx, name, start);
			if (result != NOT_FOUND) {
				break;
			}
			obj = obj.getPrototype(cx);
		} while (obj != null);
		return result;
	}

	/**
	 * This is a version of getProperty that works with Symbols.
	 */
	public static Object getProperty(Context cx, Scriptable obj, Symbol key) {
		Scriptable start = obj;
		Object result;
		do {
			result = ensureSymbolScriptable(obj).get(cx, key, start);
			if (result != NOT_FOUND) {
				break;
			}
			obj = obj.getPrototype(cx);
		} while (obj != null);
		return result;
	}

	/**
	 * Gets an indexed property from an object or any object in its prototype
	 * chain and coerces it to the requested Java type.
	 * <p>
	 * Searches the prototype chain for a property with integral index
	 * <code>index</code>. Note that if you wish to look for properties with numerical
	 * but non-integral indicies, you should use getProperty(Scriptable,String) with
	 * the string value of the index.
	 * <p>
	 *
	 * @param s     a JavaScript object
	 * @param index an integral index
	 * @param type  the required Java type of the result
	 * @return the value of a property with name <code>name</code> found in
	 * <code>obj</code> or any object in its prototype chain, or
	 * null if not found. Note that it does not return
	 * {@link Scriptable#NOT_FOUND} as it can ordinarily not be
	 * converted to most of the types.
	 * @since 1.7R3
	 */
	public static <T> T getTypedProperty(Context cx, Scriptable s, int index, Class<T> type) {
		Object val = getProperty(cx, s, index);
		if (val == NOT_FOUND) {
			val = null;
		}
		return type.cast(Context.jsToJava(cx, val, type));
	}

	/**
	 * Gets an indexed property from an object or any object in its prototype chain.
	 * <p>
	 * Searches the prototype chain for a property with integral index
	 * <code>index</code>. Note that if you wish to look for properties with numerical
	 * but non-integral indicies, you should use getProperty(Scriptable,String) with
	 * the string value of the index.
	 * <p>
	 *
	 * @param obj   a JavaScript object
	 * @param index an integral index
	 * @return the value of a property with index <code>index</code> found in
	 * <code>obj</code> or any object in its prototype chain, or
	 * <code>Scriptable.NOT_FOUND</code> if not found
	 * @since 1.5R2
	 */
	public static Object getProperty(Context cx, Scriptable obj, int index) {
		Scriptable start = obj;
		Object result;
		do {
			result = obj.get(cx, index, start);
			if (result != NOT_FOUND) {
				break;
			}
			obj = obj.getPrototype(cx);
		} while (obj != null);
		return result;
	}

	/**
	 * Gets a named property from an object or any object in its prototype chain
	 * and coerces it to the requested Java type.
	 * <p>
	 * Searches the prototype chain for a property named <code>name</code>.
	 * <p>
	 *
	 * @param s    a JavaScript object
	 * @param name a property name
	 * @param type the required Java type of the result
	 * @return the value of a property with name <code>name</code> found in
	 * <code>obj</code> or any object in its prototype chain, or
	 * null if not found. Note that it does not return
	 * {@link Scriptable#NOT_FOUND} as it can ordinarily not be
	 * converted to most of the types.
	 * @since 1.7R3
	 */
	public static <T> T getTypedProperty(Context cx, Scriptable s, String name, Class<T> type) {
		Object val = getProperty(cx, s, name);
		if (val == NOT_FOUND) {
			val = null;
		}
		return type.cast(Context.jsToJava(cx, val, type));
	}

	/**
	 * Returns whether a named property is defined in an object or any object
	 * in its prototype chain.
	 * <p>
	 * Searches the prototype chain for a property named <code>name</code>.
	 * <p>
	 *
	 * @param obj  a JavaScript object
	 * @param name a property name
	 * @return the true if property was found
	 * @since 1.5R2
	 */
	public static boolean hasProperty(Context cx, Scriptable obj, String name) {
		return null != getBase(cx, obj, name);
	}

	/**
	 * If hasProperty(obj, name) would return true, then if the property that
	 * was found is compatible with the new property, this method just returns.
	 * If the property is not compatible, then an exception is thrown.
	 * <p>
	 * A property redefinition is incompatible if the first definition was a
	 * const declaration or if this one is.  They are compatible only if neither
	 * was const.
	 */
	public static void redefineProperty(Context cx, Scriptable obj, String name, boolean isConst) {
		Scriptable base = getBase(cx, obj, name);
		if (base == null) {
			return;
		}
		if (base instanceof ConstProperties cp) {

			if (cp.isConst(cx, name)) {
				throw ScriptRuntime.typeError1("msg.const.redecl", name);
			}
		}
		if (isConst) {
			throw ScriptRuntime.typeError1("msg.var.redecl", name);
		}
	}

	/**
	 * Returns whether an indexed property is defined in an object or any object
	 * in its prototype chain.
	 * <p>
	 * Searches the prototype chain for a property with index <code>index</code>.
	 * <p>
	 *
	 * @param obj   a JavaScript object
	 * @param index a property index
	 * @return the true if property was found
	 * @since 1.5R2
	 */
	public static boolean hasProperty(Context cx, Scriptable obj, int index) {
		return null != getBase(cx, obj, index);
	}

	/**
	 * A version of hasProperty for properties with Symbol keys.
	 */
	public static boolean hasProperty(Context cx, Scriptable obj, Symbol key) {
		return null != getBase(cx, obj, key);
	}

	/**
	 * Puts a named property in an object or in an object in its prototype chain.
	 * <p>
	 * Searches for the named property in the prototype chain. If it is found,
	 * the value of the property in <code>obj</code> is changed through a call
	 * to {@link Scriptable#put(Context, String, Scriptable, Object)} on the
	 * prototype passing <code>obj</code> as the <code>start</code> argument.
	 * This allows the prototype to veto the property setting in case the
	 * prototype defines the property with [[ReadOnly]] attribute. If the
	 * property is not found, it is added in <code>obj</code>.
	 *
	 * @param obj   a JavaScript object
	 * @param name  a property name
	 * @param value any JavaScript value accepted by Scriptable.put
	 * @since 1.5R2
	 */
	public static void putProperty(Context cx, Scriptable obj, String name, Object value) {
		Scriptable base = getBase(cx, obj, name);
		if (base == null) {
			base = obj;
		}
		base.put(cx, name, obj, value);
	}

	/**
	 * This is a version of putProperty for Symbol keys.
	 */
	public static void putProperty(Context cx, Scriptable obj, Symbol key, Object value) {
		Scriptable base = getBase(cx, obj, key);
		if (base == null) {
			base = obj;
		}
		ensureSymbolScriptable(base).put(cx, key, obj, value);
	}

	/**
	 * Puts a named property in an object or in an object in its prototype chain.
	 * <p>
	 * Searches for the named property in the prototype chain. If it is found,
	 * the value of the property in <code>obj</code> is changed through a call
	 * to {@link Scriptable#put(Context, String, Scriptable, Object)} on the
	 * prototype passing <code>obj</code> as the <code>start</code> argument.
	 * This allows the prototype to veto the property setting in case the
	 * prototype defines the property with [[ReadOnly]] attribute. If the
	 * property is not found, it is added in <code>obj</code>.
	 *
	 * @param obj   a JavaScript object
	 * @param name  a property name
	 * @param value any JavaScript value accepted by Scriptable.put
	 * @since 1.5R2
	 */
	public static void putConstProperty(Context cx, Scriptable obj, String name, Object value) {
		Scriptable base = getBase(cx, obj, name);
		if (base == null) {
			base = obj;
		}
		if (base instanceof ConstProperties) {
			((ConstProperties) base).putConst(cx, name, obj, value);
		}
	}

	/**
	 * Puts an indexed property in an object or in an object in its prototype chain.
	 * <p>
	 * Searches for the indexed property in the prototype chain. If it is found,
	 * the value of the property in <code>obj</code> is changed through a call
	 * to {@link Scriptable#put(Context, int, Scriptable, Object)} on the prototype
	 * passing <code>obj</code> as the <code>start</code> argument. This allows
	 * the prototype to veto the property setting in case the prototype defines
	 * the property with [[ReadOnly]] attribute. If the property is not found,
	 * it is added in <code>obj</code>.
	 *
	 * @param obj   a JavaScript object
	 * @param index a property index
	 * @param value any JavaScript value accepted by Scriptable.put
	 * @since 1.5R2
	 */
	public static void putProperty(Context cx, Scriptable obj, int index, Object value) {
		Scriptable base = getBase(cx, obj, index);
		if (base == null) {
			base = obj;
		}
		base.put(cx, index, obj, value);
	}

	/**
	 * Removes the property from an object or its prototype chain.
	 * <p>
	 * Searches for a property with <code>name</code> in obj or
	 * its prototype chain. If it is found, the object's delete
	 * method is called.
	 *
	 * @param scope a JavaScript object
	 * @param name  a property name
	 * @return true if the property doesn't exist or was successfully removed
	 * @since 1.5R2
	 */
	public static boolean deleteProperty(Context cx, Scriptable scope, String name) {
		Scriptable base = getBase(cx, scope, name);
		if (base == null) {
			return true;
		}
		base.delete(cx, scope, name);
		return !base.has(cx, name, scope);
	}

	/**
	 * Removes the property from an object or its prototype chain.
	 * <p>
	 * Searches for a property with <code>index</code> in obj or
	 * its prototype chain. If it is found, the object's delete
	 * method is called.
	 *
	 * @param scope a JavaScript object
	 * @param index a property index
	 * @return true if the property doesn't exist or was successfully removed
	 * @since 1.5R2
	 */
	public static boolean deleteProperty(Context cx, Scriptable scope, int index) {
		Scriptable base = getBase(cx, scope, index);
		if (base == null) {
			return true;
		}
		base.delete(cx, scope, index);
		return !base.has(cx, index, scope);
	}

	/**
	 * Returns an array of all ids from an object and its prototypes.
	 * <p>
	 *
	 * @param obj a JavaScript object
	 * @return an array of all ids from all object in the prototype chain.
	 * If a given id occurs multiple times in the prototype chain,
	 * it will occur only once in this list.
	 * @since 1.5R2
	 */
	public static Object[] getPropertyIds(Context cx, Scriptable obj) {
		if (obj == null) {
			return ScriptRuntime.EMPTY_OBJECTS;
		}
		Object[] result = obj.getIds(cx);
		ObjToIntMap map = null;
		for (; ; ) {
			obj = obj.getPrototype(cx);
			if (obj == null) {
				break;
			}
			Object[] ids = obj.getIds(cx);
			if (ids.length == 0) {
				continue;
			}
			if (map == null) {
				if (result.length == 0) {
					result = ids;
					continue;
				}
				map = new ObjToIntMap(result.length + ids.length);
				for (int i = 0; i != result.length; ++i) {
					map.intern(result[i]);
				}
				result = null; // Allow to GC the result
			}
			for (int i = 0; i != ids.length; ++i) {
				map.intern(ids[i]);
			}
		}
		if (map != null) {
			result = map.getKeys();
		}
		return result;
	}

	/**
	 * Call a method of an object.
	 *
	 * @param cx         the Context object associated with the current thread.
	 * @param obj        the JavaScript object
	 * @param methodName the name of the function property
	 * @param args       the arguments for the call
	 */
	public static Object callMethod(Context cx, Scriptable obj, String methodName, Object[] args) {
		Object funObj = getProperty(cx, obj, methodName);
		if (!(funObj instanceof Function fun)) {
			throw ScriptRuntime.notFunctionError(obj, methodName);
		}
		// XXX: What should be the scope when calling funObj?
		// The following favor scope stored in the object on the assumption
		// that is more useful especially under dynamic scope setup.
		// An alternative is to check for dynamic scope flag
		// and use ScriptableObject.getTopLevelScope(fun) if the flag is not
		// set. But that require access to Context and messy code
		// so for now it is not checked.
		Scriptable scope = ScriptableObject.getTopLevelScope(obj);
		return fun.call(cx, scope, obj, args);
	}

	private static Scriptable getBase(Context cx, Scriptable obj, String name) {
		do {
			if (obj.has(cx, name, obj)) {
				break;
			}
			obj = obj.getPrototype(cx);
		} while (obj != null);
		return obj;
	}

	private static Scriptable getBase(Context cx, Scriptable obj, int index) {
		do {
			if (obj.has(cx, index, obj)) {
				break;
			}
			obj = obj.getPrototype(cx);
		} while (obj != null);
		return obj;
	}

	private static Scriptable getBase(Context cx, Scriptable obj, Symbol key) {
		do {
			if (ensureSymbolScriptable(obj).has(cx, key, obj)) {
				break;
			}
			obj = obj.getPrototype(cx);
		} while (obj != null);
		return obj;
	}

	/**
	 * Get arbitrary application-specific value associated with this object.
	 *
	 * @param key key object to select particular value.
	 * @see #associateValue(Object key, Object value)
	 */
	public final Object getAssociatedValue(Object key) {
		Map<Object, Object> h = associatedValues;
		if (h == null) {
			return null;
		}
		return h.get(key);
	}

	/**
	 * Get arbitrary application-specific value associated with the top scope
	 * of the given scope.
	 * The method first calls {@link #getTopLevelScope(Scriptable scope)}
	 * and then searches the prototype chain of the top scope for the first
	 * object containing the associated value with the given key.
	 *
	 * @param scope the starting scope.
	 * @param key   key object to select particular value.
	 * @see #getAssociatedValue(Object key)
	 */
	public static Object getTopScopeValue(Context cx, Scriptable scope, Object key) {
		scope = ScriptableObject.getTopLevelScope(scope);
		for (; ; ) {
			if (scope instanceof ScriptableObject so) {
				Object value = so.getAssociatedValue(key);
				if (value != null) {
					return value;
				}
			}
			scope = scope.getPrototype(cx);
			if (scope == null) {
				return null;
			}
		}
	}

	/**
	 * Associate arbitrary application-specific value with this object.
	 * Value can only be associated with the given object and key only once.
	 * The method ignores any subsequent attempts to change the already
	 * associated value.
	 * <p> The associated values are not serialized.
	 *
	 * @param key   key object to select particular value.
	 * @param value the value to associate
	 * @return the passed value if the method is called first time for the
	 * given key or old value for any subsequent calls.
	 * @see #getAssociatedValue(Object key)
	 */
	public synchronized final Object associateValue(Object key, Object value) {
		if (value == null) {
			throw new IllegalArgumentException();
		}
		Map<Object, Object> h = associatedValues;
		if (h == null) {
			h = new HashMap<>();
			associatedValues = h;
		}
		return Kit.initHash(h, key, value);
	}

	/**
	 * @param key
	 * @param index
	 * @param start
	 * @param value
	 * @return false if this != start and no slot was found.  true if this == start
	 * or this != start and a READONLY slot was found.
	 */
	private boolean putImpl(Context cx, Object key, int index, Scriptable start, Object value) {
		// This method is very hot (basically called on each assignment)
		// so we inline the extensible/sealed checks below.
		Slot slot;
		if (this != start) {
			slot = slotMap.query(cx, key, index);
			if (!isExtensible && (slot == null || (!(slot instanceof GetterSlot) && (slot.getAttributes() & READONLY) != 0)) && cx.isStrictMode()) {
				throw ScriptRuntime.typeError0("msg.not.extensible");
			}
			if (slot == null) {
				return false;
			}
		} else if (!isExtensible) {
			slot = slotMap.query(cx, key, index);
			if ((slot == null || (!(slot instanceof GetterSlot) && (slot.getAttributes() & READONLY) != 0)) && cx.isStrictMode()) {
				throw ScriptRuntime.typeError0("msg.not.extensible");
			}
			if (slot == null) {
				return true;
			}
		} else {
			slot = slotMap.get(cx, key, index, SlotAccess.MODIFY);
		}
		return slot.setValue(cx, value, this, start);
	}


	/**
	 * @param name
	 * @param index
	 * @param start
	 * @param value
	 * @param constFlag EMPTY means normal put.  UNINITIALIZED_CONST means
	 *                  defineConstProperty.  READONLY means const initialization expression.
	 * @return false if this != start and no slot was found.  true if this == start
	 * or this != start and a READONLY slot was found.
	 */
	private boolean putConstImpl(Context cx, String name, int index, Scriptable start, Object value, int constFlag) {
		assert (constFlag != EMPTY);
		if (!isExtensible) {
			if (cx.isStrictMode()) {
				throw ScriptRuntime.typeError0("msg.not.extensible");
			}
		}
		Slot slot;
		if (this != start) {
			slot = slotMap.query(cx, name, index);
			if (slot == null) {
				return false;
			}
		} else if (!isExtensible()) {
			slot = slotMap.query(cx, name, index);
			if (slot == null) {
				return true;
			}
		} else {
			// either const hoisted declaration or initialization
			slot = slotMap.get(cx, name, index, SlotAccess.MODIFY_CONST);
			int attr = slot.getAttributes();
			if ((attr & READONLY) == 0) {
				throw Context.reportRuntimeError1(cx, "msg.var.redecl", name);
			}
			if ((attr & UNINITIALIZED_CONST) != 0) {
				slot.value = value;
				// clear the bit on const initialization
				if (constFlag != UNINITIALIZED_CONST) {
					slot.setAttributes(attr & ~UNINITIALIZED_CONST);
				}
			}
			return true;
		}
		return slot.setValue(cx, value, this, start);
	}

	private Slot findAttributeSlot(Context cx, String name, int index, SlotAccess accessType) {
		Slot slot = slotMap.get(cx, name, index, accessType);
		if (slot == null) {
			String str = (name != null ? name : Integer.toString(index));
			throw Context.reportRuntimeError1(cx, "msg.prop.not.found", str);
		}
		return slot;
	}

	private Slot findAttributeSlot(Context cx, Symbol key, SlotAccess accessType) {
		Slot slot = slotMap.get(cx, key, 0, accessType);
		if (slot == null) {
			throw Context.reportRuntimeError1(cx, "msg.prop.not.found", key);
		}
		return slot;
	}

	Object[] getIds(Context cx, boolean getNonEnumerable, boolean getSymbols) {
		Object[] a = ScriptRuntime.EMPTY_OBJECTS;
		if (slotMap.isEmpty()) {
			return ScriptRuntime.EMPTY_OBJECTS;
		}

		int c = 0;
		final long stamp = slotMap.readLock();
		try {
			for (Slot slot : slotMap) {
				if ((getNonEnumerable || (slot.getAttributes() & DONTENUM) == 0) && (getSymbols || !(slot.name instanceof Symbol))) {
					if (c == 0) {
						a = new Object[slotMap.dirtySize()];
					}

					a[c++] = slot.name != null ? slot.name : Integer.valueOf(slot.indexOrHash);
				}
			}
		} finally {
			slotMap.unlockRead(stamp);
		}

		Object[] result;
		if (c == a.length) {
			result = a;
		} else {
			result = new Object[c];
			System.arraycopy(a, 0, result, 0, c);
		}

		// Move all the numeric IDs to the front in numeric order
		Arrays.sort(result, KEY_COMPARATOR);
		return result;
	}

	protected ScriptableObject getOwnPropertyDescriptor(Context cx, Object id) {
		Slot slot = getSlot(cx, id, SlotAccess.QUERY);
		if (slot == null) {
			return null;
		}
		Scriptable scope = getParentScope();
		return slot.getPropertyDescriptor(cx, (scope == null ? this : scope));
	}

	protected Slot getSlot(Context cx, Object id, SlotAccess accessType) {
		if (id instanceof Symbol) {
			return slotMap.get(cx, id, 0, accessType);
		}
		ScriptRuntime.StringIdOrIndex s = ScriptRuntime.toStringIdOrIndex(cx, id);
		if (s.stringId == null) {
			return slotMap.get(cx, null, s.index, accessType);
		}
		return slotMap.get(cx, s.stringId, 0, accessType);
	}

	// Partial implementation of java.util.Map. See NativeObject for
	// a subclass that implements java.util.Map.

	public int size() {
		return slotMap.size();
	}

	public boolean isEmpty() {
		return slotMap.isEmpty();
	}

	public Object get(Context cx, Object key) {
		Object value = null;
		if (key instanceof String) {
			value = get(cx, (String) key, this);
		} else if (key instanceof Symbol) {
			value = get(cx, (Symbol) key, this);
		} else if (key instanceof Number) {
			value = get(cx, ((Number) key).intValue(), this);
		}
		if (value == NOT_FOUND || value == Undefined.instance) {
			return null;
		} else if (value instanceof Wrapper) {
			return ((Wrapper) value).unwrap();
		} else {
			return value;
		}
	}

	private static final Comparator<Object> KEY_COMPARATOR = new KeyComparator();

	/**
	 * This comparator sorts property fields in spec-compliant order. Numeric ids first, in numeric
	 * order, followed by string ids, in insertion order. Since this class already keeps string keys
	 * in insertion-time order, we treat all as equal. The "Arrays.sort" method will then not
	 * change their order, but simply move all the numeric properties to the front, since this
	 * method is defined to be stable.
	 */
	public static final class KeyComparator implements Comparator<Object> {
		@Override
		public int compare(Object o1, Object o2) {
			if (o1 instanceof Integer) {
				if (o2 instanceof Integer) {
					int i1 = (Integer) o1;
					int i2 = (Integer) o2;
					if (i1 < i2) {
						return -1;
					}
					if (i1 > i2) {
						return 1;
					}
					return 0;
				}
				return -1;
			}
			if (o2 instanceof Integer) {
				return 1;
			}
			return 0;
		}
	}
}
