package dev.latvian.mods.rhino;

import dev.latvian.mods.rhino.classdata.ClassDataCache;
import dev.latvian.mods.rhino.util.CustomJavaToJsWrapper;
import dev.latvian.mods.rhino.util.CustomJavaToJsWrapperProvider;
import dev.latvian.mods.rhino.util.CustomJavaToJsWrapperProviderHolder;
import dev.latvian.mods.rhino.util.DefaultRemapper;
import dev.latvian.mods.rhino.util.Remapper;
import dev.latvian.mods.rhino.util.wrap.TypeWrappers;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

public class SharedContextData {
	public final ContextFactory factory;
	private TypeWrappers typeWrappers;
	private Remapper remapper;
	private ClassDataCache classDataCache;
	final List<CustomJavaToJsWrapperProviderHolder<?>> customScriptableWrappers;
	final Map<Class<?>, CustomJavaToJsWrapperProvider> customScriptableWrapperCache;
	private final Map<String, Object> extraProperties;

	SharedContextData(ContextFactory cxf) {
		factory = cxf;
		typeWrappers = null;
		remapper = DefaultRemapper.INSTANCE;
		classDataCache = null;
		customScriptableWrappers = new ArrayList<>();
		customScriptableWrapperCache = new HashMap<>();
		extraProperties = new HashMap<>();
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

	public ClassDataCache getClassDataCache() {
		if (classDataCache == null) {
			classDataCache = new ClassDataCache(this);
		}

		return classDataCache;
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
