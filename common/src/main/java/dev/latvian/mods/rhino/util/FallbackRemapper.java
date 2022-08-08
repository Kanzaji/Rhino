package dev.latvian.mods.rhino.util;

import dev.latvian.mods.rhino.SharedContextData;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public record FallbackRemapper(Remapper main, Remapper fallback) implements Remapper {
	@Override
	public String remapClass(SharedContextData data, Class<?> from, String className) {
		String s = main.remapClass(data, from, className);
		return s.isEmpty() ? fallback.remapClass(data, from, className) : s;
	}

	@Override
	public String unmapClass(SharedContextData data, String from) {
		String s = main.unmapClass(data, from);
		return s.isEmpty() ? fallback.unmapClass(data, from) : s;
	}

	@Override
	public String remapField(SharedContextData data, Class<?> from, Field field, String fieldName) {
		String s = main.remapField(data, from, field, fieldName);
		return s.isEmpty() ? fallback.remapField(data, from, field, fieldName) : s;
	}

	@Override
	public String remapMethod(SharedContextData data, Class<?> from, Method method, String methodString) {
		String s = main.remapMethod(data, from, method, methodString);
		return s.isEmpty() ? fallback.remapMethod(data, from, method, methodString) : s;
	}
}
