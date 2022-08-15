package dev.latvian.mods.rhino.util;

import dev.latvian.mods.rhino.SharedContextData;
import dev.latvian.mods.rhino.classdata.PublicClassData;
import dev.latvian.mods.rhino.util.wrap.TypeWrapperFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class EnumTypeWrapperFactory<T> implements TypeWrapperFactory<T> {
	public static String getName(SharedContextData data, Class<?> enumType, Enum<?> e) {
		String name = e.name();

		if (e instanceof RemappedEnumConstant c) {
			String s = c.getRemappedEnumConstantName();

			if (!s.isEmpty()) {
				return s;
			}
		}

		try {
			var classData = PublicClassData.of(enumType);

			for (var field : classData.getFields()) {
				if (field.javaField.getName().equals(name)) {
					String s = data.getRemapper().remapField(classData, field);

					if (!s.isEmpty()) {
						return s;
					}
				}
			}
		} catch (Exception ex) {
		}

		return name;
	}

	public final Class<T> enumType;
	public final T[] indexValues;
	public final Map<String, T> nameValues;
	public final Map<T, String> valueNames;

	public EnumTypeWrapperFactory(SharedContextData data, Class<T> enumType) {
		this.enumType = enumType;
		this.indexValues = enumType.getEnumConstants();
		this.nameValues = new HashMap<>();
		this.valueNames = new HashMap<>();

		for (T t : indexValues) {
			String name = getName(data, enumType, (Enum<?>) t).toLowerCase();
			nameValues.put(name, t);
			valueNames.put(t, name);
		}
	}

	@Override
	public T wrap(Object o) {
		if (o instanceof CharSequence) {
			String s = o.toString().toLowerCase();

			if (s.isEmpty()) {
				return null;
			}

			T t = nameValues.get(s);

			if (t == null) {
				throw new IllegalArgumentException("'" + s + "' is not a valid enum constant! Valid values are: " + nameValues.keySet().stream().map(s1 -> "'" + s1 + "'").collect(Collectors.joining(", ")));
			}

			return t;
		} else if (o instanceof Number) {
			int index = ((Number) o).intValue();

			if (index < 0 || index >= indexValues.length) {
				throw new IllegalArgumentException(index + " is not a valid enum index! Valid values are: 0 - " + (indexValues.length - 1));
			}

			return indexValues[index];
		}

		return (T) o;
	}
}
