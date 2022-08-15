package dev.latvian.mods.rhino.js;

import dev.latvian.mods.rhino.util.NativeArrayWrapper;
import org.jetbrains.annotations.Nullable;

public enum TypeJS {
	UNDEFINED,
	OBJECT,
	FUNCTION,
	SCRIPTABLE, // TO be removed after new scope impl
	ARRAY,
	STRING,
	NUMBER,
	BOOLEAN;

	public static Object wrap(@Nullable Object object) {
		if (object == null) {
			return null;
		} else if (object.getClass().isArray()) {
			return NativeArrayWrapper.of(object);
		} else {
			return object;
		}
	}
}
