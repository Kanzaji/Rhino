/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package dev.latvian.mods.rhino;

/**
 * This class implements the "arguments" object.
 * <p>
 * See ECMA 10.1.8
 *
 * @author Norris Boyd
 * @see NativeCall
 */
final class Arguments extends IdScriptableObject {
	private static final String FTAG = "Arguments";

	public Arguments(Context cx, NativeCall activation) {
		this.activation = activation;

		Scriptable parent = activation.getParentScope();
		setParentScope(parent);
		setPrototype(cx, getObjectPrototype(cx, parent));

		args = activation.originalArgs;
		lengthObj = args.length;

		calleeObj = activation.function;
		callerObj = NOT_FOUND;

		defineProperty(cx, SymbolKey.ITERATOR, iteratorMethod, DONTENUM);
	}

	@Override
	public String getClassName() {
		return FTAG;
	}

	private Object arg(int index) {
		if (index < 0 || args.length <= index) {
			return NOT_FOUND;
		}
		return args[index];
	}

	// the following helper methods assume that 0 < index < args.length

	private void putIntoActivation(Context cx, int index, Object value) {
		String argName = activation.function.getParamOrVarName(index);
		activation.put(cx, argName, activation, value);
	}

	private Object getFromActivation(Context cx, int index) {
		String argName = activation.function.getParamOrVarName(index);
		return activation.get(cx, argName, activation);
	}

	private void replaceArg(Context cx, int index, Object value) {
		if (sharedWithActivation(cx, index)) {
			putIntoActivation(cx, index, value);
		}
		synchronized (this) {
			if (args == activation.originalArgs) {
				args = args.clone();
			}
			args[index] = value;
		}
	}

	private void removeArg(int index) {
		synchronized (this) {
			if (args[index] != NOT_FOUND) {
				if (args == activation.originalArgs) {
					args = args.clone();
				}
				args[index] = NOT_FOUND;
			}
		}
	}

	// end helpers

	@Override
	public boolean has(Context cx, int index, Scriptable start) {
		if (arg(index) != NOT_FOUND) {
			return true;
		}
		return super.has(cx, index, start);
	}

	@Override
	public Object get(Context cx, int index, Scriptable start) {
		final Object value = arg(index);
		if (value == NOT_FOUND) {
			return super.get(cx, index, start);
		}
		if (sharedWithActivation(cx, index)) {
			return getFromActivation(cx, index);
		}
		return value;
	}

	private boolean sharedWithActivation(Context cx, int index) {
		if (cx.isStrictMode()) {
			return false;
		}
		NativeFunction f = activation.function;
		int definedCount = f.getParamCount();
		if (index < definedCount) {
			// Check if argument is not hidden by later argument with the same
			// name as hidden arguments are not shared with activation
			if (index < definedCount - 1) {
				String argName = f.getParamOrVarName(index);
				for (int i = index + 1; i < definedCount; i++) {
					if (argName.equals(f.getParamOrVarName(i))) {
						return false;
					}
				}
			}
			return true;
		}
		return false;
	}

	@Override
	public void put(Context cx, int index, Scriptable start, Object value) {
		if (arg(index) == NOT_FOUND) {
			super.put(cx, index, start, value);
		} else {
			replaceArg(cx, index, value);
		}
	}

	@Override
	public void put(Context cx, String name, Scriptable start, Object value) {
		super.put(cx, name, start, value);
	}

	@Override
	public void delete(Context cx, int index) {
		if (0 <= index && index < args.length) {
			removeArg(index);
		}
		super.delete(cx, index);
	}

	// #string_id_map#

	private static final int Id_callee = 1;
	private static final int Id_length = 2;
	private static final int Id_caller = 3;

	private static final int MAX_INSTANCE_ID = Id_caller;

	@Override
	protected int getMaxInstanceId() {
		return MAX_INSTANCE_ID;
	}

	@Override
	protected int findInstanceIdInfo(String s) {
		int id = switch (s) {
			case "callee" -> Id_callee;
			case "length" -> Id_length;
			case "caller" -> Id_caller;
			default -> 0;
		};

		Context cx = Context.getContext();

		if (cx.isStrictMode()) {
			if (id == Id_callee || id == Id_caller) {
				return super.findInstanceIdInfo(s);
			}
		}

		if (id == 0) {
			return super.findInstanceIdInfo(s);
		}

		int attr = switch (id) {
			case Id_callee -> calleeAttr;
			case Id_caller -> callerAttr;
			case Id_length -> lengthAttr;
			default -> throw new IllegalStateException();
		};
		return instanceIdInfo(attr, id);
	}

	// #/string_id_map#

	@Override
	protected String getInstanceIdName(int id) {
		return switch (id) {
			case Id_callee -> "callee";
			case Id_length -> "length";
			case Id_caller -> "caller";
			default -> null;
		};
	}

	@Override
	protected Object getInstanceIdValue(Context cx, int id) {
		switch (id) {
			case Id_callee:
				return calleeObj;
			case Id_length:
				return lengthObj;
			case Id_caller: {
				Object value = callerObj;
				if (value == UniqueTag.NULL_VALUE) {
					value = null;
				} else if (value == null) {
					NativeCall caller = activation.parentActivationCall;
					if (caller != null) {
						value = caller.get(cx, "arguments", caller);
					}
				}
				return value;
			}
		}
		return super.getInstanceIdValue(cx, id);
	}

	@Override
	protected void setInstanceIdValue(Context cx, int id, Object value) {
		switch (id) {
			case Id_callee -> calleeObj = value;
			case Id_length -> lengthObj = value;
			case Id_caller -> callerObj = (value != null) ? value : UniqueTag.NULL_VALUE;
			default -> super.setInstanceIdValue(cx, id, value);
		}

	}

	@Override
	protected void setInstanceIdAttributes(int id, int attr) {
		switch (id) {
			case Id_callee -> calleeAttr = attr;
			case Id_length -> lengthAttr = attr;
			case Id_caller -> callerAttr = attr;
			default -> super.setInstanceIdAttributes(id, attr);
		}
	}

	@Override
	Object[] getIds(Context cx, boolean getNonEnumerable, boolean getSymbols) {
		Object[] ids = super.getIds(cx, getNonEnumerable, getSymbols);
		if (args.length != 0) {
			boolean[] present = new boolean[args.length];
			int extraCount = args.length;
			for (int i = 0; i != ids.length; ++i) {
				Object id = ids[i];
				if (id instanceof Integer) {
					int index = (Integer) id;
					if (0 <= index && index < args.length) {
						if (!present[index]) {
							present[index] = true;
							extraCount--;
						}
					}
				}
			}
			if (!getNonEnumerable) { // avoid adding args which were redefined to non-enumerable
				for (int i = 0; i < present.length; i++) {
					if (!present[i] && super.has(cx, i, this)) {
						present[i] = true;
						extraCount--;
					}
				}
			}
			if (extraCount != 0) {
				Object[] tmp = new Object[extraCount + ids.length];
				System.arraycopy(ids, 0, tmp, extraCount, ids.length);
				ids = tmp;
				int offset = 0;
				for (int i = 0; i != args.length; ++i) {
					if (!present[i]) {
						ids[offset] = i;
						++offset;
					}
				}
				if (offset != extraCount) {
					throw Kit.codeBug();
				}
			}
		}
		return ids;
	}

	@Override
	protected ScriptableObject getOwnPropertyDescriptor(Context cx, Object id) {
		if (ScriptRuntime.isSymbol(id) || id instanceof Scriptable) {
			return super.getOwnPropertyDescriptor(cx, id);
		}

		double d = ScriptRuntime.toNumber(id);
		int index = (int) d;
		if (d != index) {
			return super.getOwnPropertyDescriptor(cx, id);
		}
		Object value = arg(index);
		if (value == NOT_FOUND) {
			return super.getOwnPropertyDescriptor(cx, id);
		}
		if (sharedWithActivation(cx, index)) {
			value = getFromActivation(cx, index);
		}
		if (super.has(cx, index, this)) { // the descriptor has been redefined
			ScriptableObject desc = super.getOwnPropertyDescriptor(cx, id);
			desc.put(cx, "value", desc, value);
			return desc;
		}
		Scriptable scope = getParentScope();
		if (scope == null) {
			scope = this;
		}
		return buildDataDescriptor(cx, scope, value, EMPTY);
	}

	@Override
	protected void defineOwnProperty(Context cx, Object id, Scriptable desc, boolean checkValid) {
		super.defineOwnProperty(cx, id, desc, checkValid);
		if (ScriptRuntime.isSymbol(id)) {
			return;
		}

		double d = ScriptRuntime.toNumber(id);
		int index = (int) d;
		if (d != index) {
			return;
		}

		Object value = arg(index);
		if (value == NOT_FOUND) {
			return;
		}

		if (isAccessorDescriptor(cx, desc)) {
			removeArg(index);
			return;
		}

		Object newValue = getProperty(cx, desc, "value");
		if (newValue == NOT_FOUND) {
			return;
		}

		replaceArg(cx, index, newValue);

		if (isFalse(getProperty(cx, desc, "writable"))) {
			removeArg(index);
		}
	}

	// ECMAScript2015
	// 9.4.4.6 CreateUnmappedArgumentsObject(argumentsList)
	//   8. Perform DefinePropertyOrThrow(obj, "caller", PropertyDescriptor {[[Get]]: %ThrowTypeError%,
	//      [[Set]]: %ThrowTypeError%, [[Enumerable]]: false, [[Configurable]]: false}).
	//   9. Perform DefinePropertyOrThrow(obj, "callee", PropertyDescriptor {[[Get]]: %ThrowTypeError%,
	//      [[Set]]: %ThrowTypeError%, [[Enumerable]]: false, [[Configurable]]: false}).
	void defineAttributesForStrictMode(Context cx) {
		if (!cx.isStrictMode()) {
			return;
		}
		setGetterOrSetter(cx, "caller", 0, new ThrowTypeError("caller"), true);
		setGetterOrSetter(cx, "caller", 0, new ThrowTypeError("caller"), false);
		setGetterOrSetter(cx, "callee", 0, new ThrowTypeError("callee"), true);
		setGetterOrSetter(cx, "callee", 0, new ThrowTypeError("callee"), false);
		setAttributes(cx, "caller", DONTENUM | PERMANENT);
		setAttributes(cx, "callee", DONTENUM | PERMANENT);
		callerObj = null;
		calleeObj = null;
	}

	private static final BaseFunction iteratorMethod = new BaseFunction() {
		@Override
		public Object call(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
			// TODO : call %ArrayProto_values%
			// 9.4.4.6 CreateUnmappedArgumentsObject(argumentsList)
			//  1. Perform DefinePropertyOrThrow(obj, @@iterator, PropertyDescriptor {[[Value]]:%ArrayProto_values%,
			//     [[Writable]]: true, [[Enumerable]]: false, [[Configurable]]: true}).
			return new NativeArrayIterator(cx, scope, thisObj, NativeArrayIterator.ArrayIteratorType.VALUES);
		}
	};

	private static class ThrowTypeError extends BaseFunction {
		private final String propertyName;

		ThrowTypeError(String propertyName) {
			this.propertyName = propertyName;
		}

		@Override
		public Object call(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
			throw ScriptRuntime.typeError1("msg.arguments.not.access.strict", propertyName);
		}
	}

	// Fields to hold caller, callee and length properties,
	// where NOT_FOUND value tags deleted properties.
	// In addition if callerObj == NULL_VALUE, it tags null for scripts, as
	// initial callerObj == null means access to caller arguments available
	// only in JS <= 1.3 scripts
	private Object callerObj;
	private Object calleeObj;
	private Object lengthObj;

	private int callerAttr = DONTENUM;
	private int calleeAttr = DONTENUM;
	private int lengthAttr = DONTENUM;

	private final NativeCall activation;

	// Initially args holds activation.getOriginalArgs(), but any modification
	// of its elements triggers creation of a copy. If its element holds NOT_FOUND,
	// it indicates deleted index, in which case super class is queried.
	private Object[] args;
}
