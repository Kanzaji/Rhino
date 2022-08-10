package dev.latvian.mods.rhino.util;

import dev.latvian.mods.rhino.SharedContextData;
import dev.latvian.mods.rhino.classdata.FieldInfo;
import dev.latvian.mods.rhino.classdata.MethodInfo;
import dev.latvian.mods.rhino.classdata.PublicClassData;

import java.util.function.Function;

public interface Remapper {
	String remapClass(SharedContextData data, PublicClassData from);

	String unmapClass(SharedContextData data, String from);

	String remapField(SharedContextData data, PublicClassData from, FieldInfo field);

	String remapMethod(SharedContextData data, PublicClassData from, MethodInfo method);

	default String getMappedClass(SharedContextData data, PublicClassData from) {
		String s = remapClass(data, from);
		return s.isEmpty() ? from.toString() : s;
	}

	default String getUnmappedClass(SharedContextData data, String from) {
		String s = unmapClass(data, from);
		return s.isEmpty() ? from : s;
	}

	default String getMappedField(SharedContextData data, PublicClassData from, FieldInfo field) {
		if (!field.remappedName.isEmpty()) {
			return field.remappedName;
		}

		String s = remapField(data, from, field);

		if (!s.isEmpty()) {
			return s;
		}

		for (var p : from.getParents()) {
			String ss = remapField(data, p, field);

			if (!ss.isEmpty()) {
				return ss;
			}
		}

		return field.member.getName();
	}

	default String getMappedMethod(SharedContextData data, PublicClassData from, MethodInfo method) {
		if (!method.remappedName.isEmpty()) {
			return method.remappedName;
		}

		String s = remapMethod(data, from, method);

		if (!s.isEmpty()) {
			return s;
		}

		for (var p : from.getParents()) {
			String ss = remapMethod(data, p, method);

			if (!ss.isEmpty()) {
				return ss;
			}
		}

		return method.member.getName();
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
