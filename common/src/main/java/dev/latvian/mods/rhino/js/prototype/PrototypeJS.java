package dev.latvian.mods.rhino.js.prototype;

import dev.latvian.mods.rhino.ContextJS;
import dev.latvian.mods.rhino.Wrapper;
import dev.latvian.mods.rhino.js.ArrayJS;
import dev.latvian.mods.rhino.js.BooleanJS;
import dev.latvian.mods.rhino.js.JavaClassJS;
import dev.latvian.mods.rhino.js.JavaObjectJS;
import dev.latvian.mods.rhino.js.NumberJS;
import dev.latvian.mods.rhino.js.ObjectJS;
import dev.latvian.mods.rhino.js.StringJS;
import dev.latvian.mods.rhino.js.TypeJS;
import dev.latvian.mods.rhino.js.UndefinedJS;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class PrototypeJS implements WithPrototype, MemberFunctions {
	public static final PrototypeJS DEFAULT = new PrototypeJS(TypeJS.UNDEFINED, "Default")
			.function("toString", PrototypeJS::toStringJS)
			.function("valueOf", PrototypeJS::valueOfJS);

	private static String toStringJS(ContextJS cx, Object self) {
		return String.valueOf(self);
	}

	private static Object valueOfJS(ContextJS cx, Object self) {
		return Wrapper.unwrapped(self);
	}

	public static PrototypeJS of(ContextJS cx, Object object) {
		if (object instanceof WithPrototype p) {
			return p.getPrototype(cx);
		} else if (object instanceof CharSequence || object instanceof Character) {
			return StringJS.PROTOTYPE;
		} else if (object instanceof Number) {
			return NumberJS.PROTOTYPE;
		} else if (object instanceof Boolean) {
			return BooleanJS.PROTOTYPE;
		} else if (object instanceof Class<?>) {
			return JavaClassJS.PROTOTYPE;
		} else if (object instanceof Map<?, ?>) {
			return ObjectJS.PROTOTYPE;
		} else if (object instanceof Iterable<?> || object != null && object.getClass().isArray()) {
			return ArrayJS.PROTOTYPE;
		}

		return JavaObjectJS.PROTOTYPE;
	}

	public final TypeJS type;
	public final String name;
	private PrototypeJS parent;
	private StaticFunctionCallback constructor;
	private Map<String, PropertyCallback> properties;
	private Map<String, FunctionCallback> functions;
	private Map<String, StaticPropertyCallback> staticProperties;
	private Map<String, StaticFunctionCallback> staticFunctions;
	private Set<Object> keys;
	private AsString asString;
	private AsNumber asNumber;
	private AsBoolean asBoolean;
	private ListSupplier keyList;
	private ListSupplier valueList;
	private ListSupplier entryList;
	private MemberFunctions memberFunctions;

	private PrototypeJS(TypeJS type, String name) {
		this.type = type;
		this.name = name;
		this.asString = AsString.DEFAULT;
		this.asNumber = AsNumber.DEFAULT;
		this.asBoolean = AsBoolean.DEFAULT;
		this.keyList = ListSupplier.DEFAULT_KEYS;
		this.valueList = ListSupplier.DEFAULT_VALUES;
		this.entryList = ListSupplier.DEFAULT_ENTRIES;
	}

	@Override
	public PrototypeJS getPrototype(ContextJS cx) {
		return this;
	}

	@Override
	public String toString() {
		return name;
	}

	public PrototypeJS create(TypeJS type, String name) {
		var p = new PrototypeJS(type, name);
		p.parent = this;
		p.asString = asString;
		p.asNumber = asNumber;
		p.asBoolean = asBoolean;
		return p;
	}

	public PrototypeJS create(String name) {
		return create(type, name);
	}

	// Builder methods //

	public PrototypeJS constructor(StaticFunctionCallback callback) {
		this.constructor = callback;
		return this;
	}

	public PrototypeJS property(String name, PropertyCallback callback) {
		if (properties == null) {
			properties = new HashMap<>();
		}

		keys = null;
		properties.put(name, callback);
		return this;
	}

	public PrototypeJS function(String name, FunctionCallback callback) {
		if (functions == null) {
			functions = new HashMap<>();
		}

		keys = null;
		functions.put(name, callback);
		return this;
	}

	public PrototypeJS function(String name, FunctionCallbackNoArgs callback) {
		return function(name, (FunctionCallback) callback);
	}

	public PrototypeJS staticProperty(String name, StaticPropertyCallback callback) {
		if (staticProperties == null) {
			staticProperties = new HashMap<>();
		}

		staticProperties.put(name, callback);
		return this;
	}

	public PrototypeJS staticPropertyValue(String name, Object value) {
		return staticProperty(name, new StaticPropertyCallback.Fixed(value));
	}

	public PrototypeJS staticFunction(String name, StaticFunctionCallback callback) {
		if (staticFunctions == null) {
			staticFunctions = new HashMap<>();
		}

		staticFunctions.put(name, callback);
		return this;
	}

	public PrototypeJS staticFunction(String name, StaticFunctionCallbackNoArgs callback) {
		return staticFunction(name, (StaticFunctionCallback) callback);
	}

	public PrototypeJS asString(AsString asString) {
		this.asString = asString;
		return this;
	}

	public PrototypeJS asNumber(AsNumber asNumber) {
		this.asNumber = asNumber;
		return this;
	}

	public PrototypeJS asBoolean(AsBoolean asBoolean) {
		this.asBoolean = asBoolean;
		return this;
	}

	public PrototypeJS keyList(ListSupplier supplier) {
		this.keyList = supplier;
		return this;
	}

	public PrototypeJS valueList(ListSupplier supplier) {
		this.valueList = supplier;
		return this;
	}

	public PrototypeJS entryList(ListSupplier supplier) {
		this.entryList = supplier;
		return this;
	}

	public PrototypeJS memberFunctions(MemberFunctions memberFunctions) {
		this.memberFunctions = memberFunctions;
		return this;
	}

	// Getters //

	@Nullable
	public StaticFunctionCallback getConstructor() {
		return constructor;
	}

	public Set<Object> getPrototypeKeys() {
		if (keys == null) {
			keys = new HashSet<>((properties == null ? 0 : properties.size()) + (functions == null ? 0 : functions.size()));

			if (properties != null) {
				keys.addAll(properties.keySet());
			}

			if (functions != null) {
				keys.addAll(functions.keySet());
			}

			keys = Set.copyOf(keys);
		}

		return keys;
	}

	public Set<Object> getAllPrototypeKeys() {
		var keys = new HashSet<>();
		PrototypeJS p = this;

		while (p != null) {
			keys.addAll(p.getPrototypeKeys());
			p = p.parent;
		}

		return keys;
	}

	public String getAsString(Object value) {
		return asString.asString(value);
	}

	public double getAsNumber(Object value) {
		return asNumber.asNumber(value);
	}

	public Boolean getAsBoolean(Object value) {
		return asBoolean.asBoolean(value);
	}

	public List<?> getKeyList(ContextJS cx, Object self) {
		return keyList.get(cx, this, self);
	}

	public List<?> getValueList(ContextJS cx, Object self) {
		return valueList.get(cx, this, self);
	}

	public List<?> getEntryList(ContextJS cx, Object self) {
		return entryList.get(cx, this, self);
	}

	@Override
	public Object getValue(ContextJS cx, Object self, Object key) {
		if (self instanceof MemberFunctions mf) {
			var result = mf.getValue(cx, self, key);

			if (result != UndefinedJS.PROTOTYPE) {
				return result;
			}
		}

		Object result = UndefinedJS.PROTOTYPE;
		PrototypeJS p = this;

		while (result == UndefinedJS.PROTOTYPE && p != null) {
			if (p.memberFunctions != null) {
				result = p.memberFunctions.getValue(cx, self, key);
			}

			if (result == UndefinedJS.PROTOTYPE && p.properties != null) {
				var o = p.properties.get(name);

				if (o != null) {
					result = o.get(cx, key);
				}
			}

			if (result == UndefinedJS.PROTOTYPE && p.staticProperties != null) {
				var o = p.staticProperties.get(name);

				if (o != null) {
					result = o.get(cx);
				}
			}

			p = p.parent;
		}

		return result;
	}

	@Override
	public boolean setValue(ContextJS cx, Object self, Object key, Object value) {
		if (self instanceof MemberFunctions mf) {
			var result = mf.setValue(cx, self, key, value);

			if (result) {
				return true;
			}
		}

		boolean result = false;
		PrototypeJS p = this;

		while (!result && p != null) {
			if (p.memberFunctions != null) {
				result = p.memberFunctions.setValue(cx, self, key, value);
			}

			p = p.parent;
		}

		return result;
	}

	@Override
	public Object invoke(ContextJS cx, Object self, Object key, Object[] args) {
		if (self instanceof MemberFunctions mf) {
			var result = mf.invoke(cx, self, key, args);

			if (result != UndefinedJS.PROTOTYPE) {
				return result;
			}
		}

		Object result = UndefinedJS.PROTOTYPE;
		PrototypeJS p = this;

		while (result == UndefinedJS.PROTOTYPE && p != null) {
			if (p.memberFunctions != null) {
				result = p.memberFunctions.invoke(cx, self, key, args);
			}

			if (result == UndefinedJS.PROTOTYPE && p.functions != null) {
				var o = p.functions.get(name);

				if (o != null) {
					result = o.invoke(cx, self, args);
				}
			}

			if (result == UndefinedJS.PROTOTYPE && p.staticFunctions != null) {
				var o = p.staticFunctions.get(name);

				if (o != null) {
					result = o.invoke(cx, args);
				}
			}

			p = p.parent;
		}

		return result;
	}

	@Override
	public boolean deleteValue(ContextJS cx, Object self, Object key) {
		if (self instanceof MemberFunctions mf) {
			var result = mf.deleteValue(cx, self, key);

			if (result) {
				return true;
			}
		}

		boolean result = false;
		PrototypeJS p = this;

		while (!result && p != null) {
			if (p.memberFunctions != null) {
				result = p.memberFunctions.deleteValue(cx, self, key);
			}

			p = p.parent;
		}

		return result;
	}
}
