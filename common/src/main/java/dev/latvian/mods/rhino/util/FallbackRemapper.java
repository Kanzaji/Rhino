package dev.latvian.mods.rhino.util;

import dev.latvian.mods.rhino.SharedContextData;
import dev.latvian.mods.rhino.classdata.FieldInfo;
import dev.latvian.mods.rhino.classdata.MethodInfo;
import dev.latvian.mods.rhino.classdata.PublicClassData;

public record FallbackRemapper(Remapper main, Remapper fallback) implements Remapper {
	@Override
	public String remapClass(SharedContextData data, PublicClassData from) {
		String s = main.remapClass(data, from);
		return s.isEmpty() ? fallback.remapClass(data, from) : s;
	}

	@Override
	public String unmapClass(SharedContextData data, String from) {
		String s = main.unmapClass(data, from);
		return s.isEmpty() ? fallback.unmapClass(data, from) : s;
	}

	@Override
	public String remapField(SharedContextData data, PublicClassData from, FieldInfo field) {
		String s = main.remapField(data, from, field);
		return s.isEmpty() ? fallback.remapField(data, from, field) : s;
	}

	@Override
	public String remapMethod(SharedContextData data, PublicClassData from, MethodInfo method) {
		String s = main.remapMethod(data, from, method);
		return s.isEmpty() ? fallback.remapMethod(data, from, method) : s;
	}
}
