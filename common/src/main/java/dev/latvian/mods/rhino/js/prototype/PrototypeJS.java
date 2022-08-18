package dev.latvian.mods.rhino.js.prototype;

import dev.latvian.mods.rhino.ContextJS;
import dev.latvian.mods.rhino.Wrapper;
import dev.latvian.mods.rhino.classdata.ExecutableGroup;
import dev.latvian.mods.rhino.classdata.RemappedClassData;
import dev.latvian.mods.rhino.js.TypeJS;
import dev.latvian.mods.rhino.js.UndefinedJS;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

	public final TypeJS type;
	public final String name;
	private PrototypeJS parent;
	private MemberFunctions constructor;
	private MemberFunctions selfMembers;
	private Map<String, MemberFunctions> members;
	private Map<String, MemberFunctions> staticMembers;
	private AsString asString;
	private AsNumber asNumber;
	private AsBoolean asBoolean;
	private ListSupplier keyList;
	private ListSupplier valueList;
	private ListSupplier entryList;
	private boolean immutable;

	private PrototypeJS(TypeJS type, String name) {
		this.type = type;
		this.name = name;
		this.asString = AsString.DEFAULT;
		this.asNumber = AsNumber.DEFAULT;
		this.asBoolean = AsBoolean.DEFAULT;
		this.keyList = ListSupplier.DEFAULT_KEYS;
		this.valueList = ListSupplier.DEFAULT_VALUES;
		this.entryList = ListSupplier.DEFAULT_ENTRIES;
		this.immutable = false;
	}

	private PrototypeJS(TypeJS type, String name, PrototypeJS p) {
		this(type, name);
		parent = p;
		asString = p.asString;
		asNumber = p.asNumber;
		asBoolean = p.asBoolean;
		keyList = p.keyList;
		valueList = p.valueList;
		entryList = p.entryList;
	}

	@Override
	public PrototypeJS getPrototype() {
		return this;
	}

	@Override
	public String toString() {
		return name;
	}

	public PrototypeJS create(TypeJS type, String name) {
		return new PrototypeJS(type, name, this);
	}

	public PrototypeJS create(String name) {
		return create(type, name);
	}

	private void checkImmutable() {
		if (immutable) {
			throw new IllegalStateException("Prototype for '" + name + "' can no longer be modified!");
		}
	}

	// Builder methods //

	public PrototypeJS constructorMember(MemberFunctions constructor) {
		checkImmutable();
		this.constructor = constructor;
		return this;
	}

	public PrototypeJS constructor(StaticFunctionCallback callback) {
		return constructorMember(callback);
	}

	public PrototypeJS member(String name, MemberFunctions member) {
		checkImmutable();

		if (members == null) {
			members = new HashMap<>();
		}

		members.put(name, member);
		return this;
	}

	public PrototypeJS staticMember(String name, MemberFunctions member) {
		checkImmutable();

		if (staticMembers == null) {
			staticMembers = new HashMap<>();
		}

		staticMembers.put(name, member);
		return this;
	}

	public PrototypeJS property(String name, PropertyCallback callback) {
		return member(name, callback);
	}

	public PrototypeJS function(String name, FunctionCallback callback) {
		return member(name, callback);
	}

	public PrototypeJS function(String name, FunctionCallbackNoArgs callback) {
		return member(name, callback);
	}

	public PrototypeJS staticProperty(String name, StaticPropertyCallback callback) {
		return staticMember(name, callback);
	}

	public PrototypeJS staticPropertyValue(String name, Object value) {
		return staticMember(name, new StaticPropertyCallback.Fixed(value));
	}

	public PrototypeJS staticFunction(String name, StaticFunctionCallback callback) {
		return staticMember(name, callback);
	}

	public PrototypeJS staticFunction(String name, StaticFunctionCallbackNoArgs callback) {
		return staticMember(name, callback);
	}

	public PrototypeJS asString(AsString asString) {
		checkImmutable();
		this.asString = asString;
		return this;
	}

	public PrototypeJS asNumber(AsNumber asNumber) {
		checkImmutable();
		this.asNumber = asNumber;
		return this;
	}

	public PrototypeJS asBoolean(AsBoolean asBoolean) {
		checkImmutable();
		this.asBoolean = asBoolean;
		return this;
	}

	public PrototypeJS keyList(ListSupplier supplier) {
		checkImmutable();
		this.keyList = supplier;
		return this;
	}

	public PrototypeJS valueList(ListSupplier supplier) {
		checkImmutable();
		this.valueList = supplier;
		return this;
	}

	public PrototypeJS entryList(ListSupplier supplier) {
		checkImmutable();
		this.entryList = supplier;
		return this;
	}

	public PrototypeJS selfMembers(MemberFunctions selfMembers) {
		checkImmutable();
		this.selfMembers = selfMembers;
		return this;
	}

	// Getters //

	@Nullable
	public MemberFunctions getConstructor() {
		return constructor;
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
	public Object getValue(ContextJS cx, @Nullable Object self, Object key, CastType returnType) {
		if (self instanceof MemberFunctions mf) {
			var result = mf.getValue(cx, self, key, returnType);

			if (result != UndefinedJS.PROTOTYPE) {
				return result;
			}
		}

		PrototypeJS p = this;

		while (p != null) {
			if (p.selfMembers != null) {
				var result = p.selfMembers.getValue(cx, self, key, returnType);

				if (result != UndefinedJS.PROTOTYPE) {
					return result;
				}
			}

			if (key instanceof String) {
				if (p.members != null && self != null) {
					var o = p.members.get(key);

					if (o != null) {
						var result = o.getValue(cx, self, key, returnType);

						if (result != UndefinedJS.PROTOTYPE) {
							return result;
						}
					}
				}

				if (p.staticMembers != null) {
					var o = p.staticMembers.get(key);

					if (o != null) {
						var result = o.getValue(cx, null, key, returnType);

						if (result != UndefinedJS.PROTOTYPE) {
							return result;
						}
					}
				}
			}

			p = p.parent;
		}

		return UndefinedJS.PROTOTYPE;
	}

	@Override
	public boolean hasValue(ContextJS cx, @Nullable Object self, Object key) {
		if (self instanceof MemberFunctions mf && mf.hasValue(cx, self, key)) {
			return true;
		}

		PrototypeJS p = this;

		while (p != null) {
			if (p.selfMembers != null && p.selfMembers.hasValue(cx, self, key)) {
				return true;
			}

			if (key instanceof String) {
				if (p.members != null && self != null && p.members.containsKey(key)) {
					return true;
				}

				if (p.staticMembers != null && p.staticMembers.containsKey(key)) {
					return true;
				}
			}

			p = p.parent;
		}

		return false;
	}

	@Override
	public boolean setValue(ContextJS cx, @Nullable Object self, Object key, Object value, CastType valueType) {
		if (self == null) {
			return false;
		} else if (self instanceof MemberFunctions mf) {
			var result = mf.setValue(cx, self, key, value, valueType);

			if (result) {
				return true;
			}
		}

		PrototypeJS p = this;

		while (p != null) {
			if (p.selfMembers != null && p.selfMembers.setValue(cx, self, key, value, valueType)) {
				return true;
			}

			p = p.parent;
		}

		return false;
	}

	@Override
	public Object invoke(ContextJS cx, @Nullable Object self, Object key, Object[] args, CastType returnType) {
		if (self instanceof MemberFunctions mf) {
			var result = mf.invoke(cx, self, key, args, returnType);

			if (result != UndefinedJS.PROTOTYPE) {
				return result;
			}
		}

		PrototypeJS p = this;

		while (p != null) {
			if (p.selfMembers != null && self != null) {
				var result = p.selfMembers.invoke(cx, self, key, args, returnType);

				if (result != UndefinedJS.PROTOTYPE) {
					return result;
				}
			}

			if (key instanceof String) {
				if (p.members != null && self != null) {
					var o = p.members.get(key);

					if (o != null) {
						var result = o.invoke(cx, self, key, args, returnType);

						if (result != UndefinedJS.PROTOTYPE) {
							return result;
						}
					}
				}

				if (p.staticMembers != null) {
					var o = p.staticMembers.get(key);

					if (o != null) {
						var result = o.invoke(cx, null, key, args, returnType);

						if (result != UndefinedJS.PROTOTYPE) {
							return result;
						}
					}
				}
			}

			p = p.parent;
		}

		return UndefinedJS.PROTOTYPE;
	}

	@Override
	public Object deleteValue(ContextJS cx, @Nullable Object self, Object key, CastType returnType) {
		if (self instanceof MemberFunctions mf) {
			var result = mf.getValue(cx, self, key, returnType);

			if (result != UndefinedJS.PROTOTYPE) {
				return result;
			}
		}

		PrototypeJS p = this;

		while (p != null) {
			if (p.selfMembers != null) {
				var result = p.selfMembers.deleteValue(cx, self, key, returnType);

				if (result != UndefinedJS.PROTOTYPE) {
					return result;
				}
			}

			if (key instanceof String) {
				if (p.members != null && self != null) {
					var o = p.members.get(key);

					if (o != null) {
						var result = o.deleteValue(cx, self, key, returnType);

						if (result != UndefinedJS.PROTOTYPE) {
							return result;
						}
					}
				}

				if (p.staticMembers != null) {
					var o = p.staticMembers.get(key);

					if (o != null) {
						var result = o.deleteValue(cx, null, key, returnType);

						if (result != UndefinedJS.PROTOTYPE) {
							return result;
						}
					}
				}
			}

			p = p.parent;
		}

		return UndefinedJS.PROTOTYPE;
	}

	public void fill(RemappedClassData classData) {
		checkImmutable();
		classData.members.forEach(this::member);
		classData.staticMembers.forEach(this::staticMember);

		var ctors = classData.publicClassData.getConstructors();

		if (ctors.length > 0) {
			constructorMember(ctors.length == 1 ? ctors[0] : new ExecutableGroup(ctors));
		}
	}

	public void optimize() {
		if (immutable) {
			return;
		}

		immutable = true;

		if (members != null) {
			if (members.isEmpty()) {
				members = null;
			} else {
				members = optimizeMap(members);
			}
		}

		if (staticMembers != null) {
			if (staticMembers.isEmpty()) {
				staticMembers = null;
			} else {
				staticMembers = optimizeMap(staticMembers);
			}
		}
	}

	private static <K, V> Map<K, V> optimizeMap(Map<K, V> m) {
		if (m.isEmpty()) {
			return Map.of();
		} else if (m.size() == 1) {
			var entry = m.entrySet().iterator().next();
			return Map.of(entry.getKey(), entry.getValue());
		} else {
			return m;
		}
	}
}
