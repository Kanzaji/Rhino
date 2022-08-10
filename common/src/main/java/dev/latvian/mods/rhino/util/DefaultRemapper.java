package dev.latvian.mods.rhino.util;

import dev.latvian.mods.rhino.SharedContextData;
import dev.latvian.mods.rhino.classdata.FieldInfo;
import dev.latvian.mods.rhino.classdata.MethodInfo;
import dev.latvian.mods.rhino.classdata.PublicClassData;

public class DefaultRemapper implements Remapper {
	public static final DefaultRemapper INSTANCE = new DefaultRemapper();

	private DefaultRemapper() {
	}

	@Override
	public String remapClass(SharedContextData data, PublicClassData from) {
		return "";
	}

	@Override
	public String unmapClass(SharedContextData data, String from) {
		return "";
	}

	@Override
	public String remapField(SharedContextData data, PublicClassData from, FieldInfo field) {
		return "";
	}

	@Override
	public String remapMethod(SharedContextData data, PublicClassData from, MethodInfo method) {
		return "";
	}
}
