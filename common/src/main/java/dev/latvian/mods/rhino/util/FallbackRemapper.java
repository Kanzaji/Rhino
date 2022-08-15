package dev.latvian.mods.rhino.util;

import dev.latvian.mods.rhino.classdata.FieldInfo;
import dev.latvian.mods.rhino.classdata.MethodInfo;
import dev.latvian.mods.rhino.classdata.PublicClassData;

public record FallbackRemapper(Remapper main, Remapper fallback) implements Remapper {
	@Override
	public String remapClass(PublicClassData from) {
		String s = main.remapClass(from);
		return s.isEmpty() ? fallback.remapClass(from) : s;
	}

	@Override
	public String unmapClass(String from) {
		String s = main.unmapClass(from);
		return s.isEmpty() ? fallback.unmapClass(from) : s;
	}

	@Override
	public String remapField(PublicClassData from, FieldInfo field) {
		String s = main.remapField(from, field);
		return s.isEmpty() ? fallback.remapField(from, field) : s;
	}

	@Override
	public String remapMethod(PublicClassData from, MethodInfo method) {
		String s = main.remapMethod(from, method);
		return s.isEmpty() ? fallback.remapMethod(from, method) : s;
	}
}
