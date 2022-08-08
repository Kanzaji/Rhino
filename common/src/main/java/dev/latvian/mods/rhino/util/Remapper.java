package dev.latvian.mods.rhino.util;

import dev.latvian.mods.rhino.SharedContextData;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.function.Function;

public interface Remapper {
	String remapClass(SharedContextData data, Class<?> from, String className);

	String unmapClass(SharedContextData data, String from);

	String remapField(SharedContextData data, Class<?> from, Field field, String fieldName);

	String remapMethod(SharedContextData data, Class<?> from, Method method, String methodString);

	default String getMappedClass(SharedContextData data, Class<?> from) {
		String n = from.getName();
		String s = remapClass(data, from, n);
		return s.isEmpty() ? n : s;
	}

	default String getUnmappedClass(SharedContextData data, String from) {
		String s = unmapClass(data, from);
		return s.isEmpty() ? from : s;
	}

	default String getMappedField(SharedContextData data, Class<?> from, Field field) {
		return getMappedField(data, from, field, field.getName());
	}

	default String getMappedField(SharedContextData data, Class<?> from, Field field, String fieldName) {
		if (from == null || from == Object.class) {
			return field.getName();
		}

		String s = remapField(data, from, field, fieldName);

		if (!s.isEmpty()) {
			return s;
		}

		String ss = getMappedField(data, from.getSuperclass(), field, fieldName);

		if (!ss.isEmpty()) {
			return ss;
		}

		for (Class<?> c : from.getInterfaces()) {
			String si = getMappedField(data, c, field, fieldName);

			if (!si.isEmpty()) {
				return si;
			}
		}

		return field.getName();
	}

	default String getMappedMethod(SharedContextData data, Class<?> from, Method method) {
		StringBuilder sb = new StringBuilder(method.getName());
		sb.append('(');

		if (method.getParameterCount() > 0) {
			for (Class<?> param : method.getParameterTypes()) {
				sb.append(Remapper.getTypeName(param.getTypeName()));
			}
		}

		sb.append(')');
		return getMappedMethod(data, from, method, sb.toString());
	}

	default String getMappedMethod(SharedContextData data, Class<?> from, Method method, String methodString) {
		if (from == null || from == Object.class) {
			return method.getName();
		}

		String s = remapMethod(data, from, method, methodString);

		if (!s.isEmpty()) {
			return s;
		}

		String ss = getMappedMethod(data, from.getSuperclass(), method, methodString);

		if (!ss.isEmpty()) {
			return ss;
		}

		for (Class<?> c : from.getInterfaces()) {
			String si = getMappedMethod(data, c, method, methodString);

			if (!si.isEmpty()) {
				return si;
			}
		}

		return method.getName();
	}

	static String getTypeName(String type, Function<String, String> remap) {
		int array = 0;

		while (type.endsWith("[]")) {
			array++;
			type = type.substring(0, type.length() - 2);
		}

		String t = switch (type) {
			case "boolean" -> "Z";
			case "byte" -> "B";
			case "short" -> "S";
			case "int" -> "I";
			case "long" -> "J";
			case "float" -> "F";
			case "double" -> "D";
			case "char" -> "C";
			case "void" -> "V";
			default -> "L" + remap.apply(type.replace('/', '.')).replace('.', '/') + ";";
		};

		return array == 0 ? t : ("[".repeat(array) + t);
	}

	static String getTypeName(String type) {
		return getTypeName(type, Function.identity());
	}
}
