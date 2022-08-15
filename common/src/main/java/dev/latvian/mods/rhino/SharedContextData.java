package dev.latvian.mods.rhino;

import dev.latvian.mods.rhino.classdata.PublicClassData;
import dev.latvian.mods.rhino.classdata.RemappedClassData;
import dev.latvian.mods.rhino.js.ArrayJS;
import dev.latvian.mods.rhino.js.BooleanJS;
import dev.latvian.mods.rhino.js.JavaClassJS;
import dev.latvian.mods.rhino.js.JavaObjectJS;
import dev.latvian.mods.rhino.js.NumberJS;
import dev.latvian.mods.rhino.js.ObjectJS;
import dev.latvian.mods.rhino.js.StringJS;
import dev.latvian.mods.rhino.js.TypeJS;
import dev.latvian.mods.rhino.js.UndefinedJS;
import dev.latvian.mods.rhino.js.prototype.PrototypeJS;
import dev.latvian.mods.rhino.js.prototype.WithPrototype;
import dev.latvian.mods.rhino.util.CustomJavaToJsWrapper;
import dev.latvian.mods.rhino.util.CustomJavaToJsWrapperProvider;
import dev.latvian.mods.rhino.util.CustomJavaToJsWrapperProviderHolder;
import dev.latvian.mods.rhino.util.DefaultRemapper;
import dev.latvian.mods.rhino.util.Remapper;
import dev.latvian.mods.rhino.util.wrap.TypeWrappers;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

public class SharedContextData implements WithPrototype {
	public static final String KEY = "SharedData";

	public static SharedContextData get(Context cx, Scriptable scope) {
		SharedContextData data = (SharedContextData) ScriptableObject.getTopScopeValue(cx, scope, KEY);

		if (data == null) {
			throw new RuntimeException("Can't find top level scope for SharedContextData.get!");
		}

		return data;
	}

	public final Scriptable topLevelScope;
	private final Map<String, Object> extraProperties;
	private transient Map<JavaAdapter.JavaAdapterSignature, Class<?>> classAdapterCache;
	private transient Map<Class<?>, Object> interfaceAdapterCache;
	private int generatedClassSerial;
	private Scriptable associatedScope;
	private TypeWrappers typeWrappers;
	private Remapper remapper;
	private final Map<Class<?>, PrototypeJS> prototypeCache;
	final List<CustomJavaToJsWrapperProviderHolder<?>> customScriptableWrappers;
	final Map<Class<?>, CustomJavaToJsWrapperProvider> customScriptableWrapperCache;

	SharedContextData(Scriptable scope) {
		topLevelScope = scope;
		typeWrappers = null;
		remapper = DefaultRemapper.INSTANCE;
		prototypeCache = new IdentityHashMap<>();
		customScriptableWrappers = new ArrayList<>();
		customScriptableWrapperCache = new IdentityHashMap<>();
		extraProperties = new HashMap<>();
	}

	@Override
	public PrototypeJS getPrototype() {
		return UndefinedJS.PROTOTYPE;
	}

	public void setExtraProperty(String key, @Nullable Object value) {
		if (value == null) {
			extraProperties.remove(key);
		} else {
			extraProperties.put(key, value);
		}
	}

	@Nullable
	public Object getExtraProperty(String key) {
		return extraProperties.get(key);
	}

	public Map<JavaAdapter.JavaAdapterSignature, Class<?>> getInterfaceAdapterCacheMap() {
		if (classAdapterCache == null) {
			classAdapterCache = new ConcurrentHashMap<>(16, 0.75f, 1);
		}
		return classAdapterCache;
	}

	public final synchronized int newClassSerialNumber() {
		return ++generatedClassSerial;
	}

	public Object getInterfaceAdapter(Class<?> cl) {
		return interfaceAdapterCache == null ? null : interfaceAdapterCache.get(cl);
	}

	public synchronized void cacheInterfaceAdapter(Class<?> cl, Object iadapter) {
		if (interfaceAdapterCache == null) {
			interfaceAdapterCache = new ConcurrentHashMap<>(16, 0.75f, 1);
		}
		interfaceAdapterCache.put(cl, iadapter);
	}

	public Scriptable getAssociatedScope() {
		return associatedScope;
	}

	public TypeWrappers getTypeWrappers() {
		if (typeWrappers == null) {
			typeWrappers = new TypeWrappers();
		}

		return typeWrappers;
	}

	@Nullable
	public TypeWrappers getTypeWrappersOrNull() {
		return typeWrappers;
	}

	public boolean hasTypeWrappers() {
		return typeWrappers != null;
	}

	public void setRemapper(Remapper r) {
		remapper = r;
	}

	public Remapper getRemapper() {
		return remapper;
	}

	public PrototypeJS getPrototype(ContextJS cx, Class<?> type) {
		if (type == Boolean.class || type == Boolean.TYPE) {
			return BooleanJS.PROTOTYPE;
		} else if (type == Class.class) {
			return JavaClassJS.PROTOTYPE;
		} else if (type == Character.class || type == Character.TYPE || CharSequence.class.isAssignableFrom(type)) {
			return StringJS.PROTOTYPE;
		} else if (Number.class.isAssignableFrom(type)) {
			return NumberJS.PROTOTYPE;
		} else if (Map.class.isAssignableFrom(type)) {
			return ObjectJS.PROTOTYPE;
		} else if (Iterable.class.isAssignableFrom(type) || type.isArray()) {
			return ArrayJS.PROTOTYPE;
		}

		PrototypeJS p = prototypeCache.get(type);

		if (p == null) {
			p = JavaObjectJS.PROTOTYPE.create(TypeJS.OBJECT, type.getName());
			p.fill(new RemappedClassData(PublicClassData.of(type), cx.getSharedData().getRemapper()));
			p.optimize();
			prototypeCache.put(type, p);
		}

		return p;
	}

	public PrototypeJS getPrototypeOf(ContextJS cx, Object object) {
		if (object == null) {
			return PrototypeJS.DEFAULT;
		} else if (object instanceof WithPrototype p) {
			return p.getPrototype();
		} else if (object == Class.class) {
			return JavaClassJS.PROTOTYPE;
		} else if (object instanceof Boolean) {
			return BooleanJS.PROTOTYPE;
		} else if (object instanceof Character || object instanceof CharSequence) {
			return StringJS.PROTOTYPE;
		} else if (object instanceof Number) {
			return NumberJS.PROTOTYPE;
		} else if (object instanceof Map) {
			return ObjectJS.PROTOTYPE;
		} else if (object instanceof Iterable || object.getClass().isArray()) {
			return ArrayJS.PROTOTYPE;
		} else if (object instanceof Class<?> t) {
			return getPrototype(cx, t);
		} else {
			return getPrototype(cx, object.getClass());
		}
	}

	@Nullable
	@SuppressWarnings({"unchecked", "rawtypes"})
	public CustomJavaToJsWrapper wrapCustomJavaToJs(Object javaObject) {
		if (customScriptableWrappers.isEmpty()) {
			return null;
		}

		var provider = customScriptableWrapperCache.get(javaObject.getClass());

		if (provider == null) {
			for (CustomJavaToJsWrapperProviderHolder wrapper : customScriptableWrappers) {
				provider = wrapper.create(javaObject);

				if (provider != null) {
					break;
				}
			}

			if (provider == null) {
				provider = CustomJavaToJsWrapperProvider.NONE;
			}

			customScriptableWrapperCache.put(javaObject.getClass(), provider);
		}

		return provider.create(javaObject);
	}

	public <T> void addCustomJavaToJsWrapper(Predicate<T> predicate, CustomJavaToJsWrapperProvider<T> provider) {
		customScriptableWrappers.add(new CustomJavaToJsWrapperProviderHolder<>(predicate, provider));
	}

	public <T> void addCustomJavaToJsWrapper(Class<T> type, CustomJavaToJsWrapperProvider<T> provider) {
		addCustomJavaToJsWrapper(new CustomJavaToJsWrapperProviderHolder.PredicateFromClass<>(type), provider);
	}
}
