package dev.latvian.mods.rhino.util;

import dev.latvian.mods.rhino.classdata.FieldInfo;
import dev.latvian.mods.rhino.classdata.MethodInfo;
import dev.latvian.mods.rhino.classdata.PublicClassData;

import java.util.function.Function;

public interface Remapper {
	String remapClass(PublicClassData from);

	String unmapClass(String from);

	String remapField(PublicClassData from, FieldInfo field);

	String remapMethod(PublicClassData from, MethodInfo method);

	default String getMappedClass(PublicClassData from) {
		String s = remapClass(from);
		return s.isEmpty() ? from.toString() : s;
	}

	default String getUnmappedClass(String from) {
		String s = unmapClass(from);
		return s.isEmpty() ? from : s;
	}

	default String getMappedField(PublicClassData from, FieldInfo field) {
		if (!field.remappedName.isEmpty()) {
			return field.remappedName;
		}

		String s = remapField(from, field);

		if (!s.isEmpty()) {
			return s;
		}

		for (var p : from.getParents()) {
			String ss = remapField(p, field);

			if (!ss.isEmpty()) {
				return ss;
			}
		}

		return field.javaField.getName();
	}

	default String getMappedMethod(PublicClassData from, MethodInfo method) {
		if (!method.remappedName.isEmpty()) {
			return method.remappedName;
		}

		String s = remapMethod(from, method);

		if (!s.isEmpty()) {
			return s;
		}

		for (var p : from.getParents()) {
			String ss = remapMethod(p, method);

			if (!ss.isEmpty()) {
				return ss;
			}
		}

		return method.javaMethod.getName();
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
