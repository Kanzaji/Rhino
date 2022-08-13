package dev.latvian.mods.rhino.js.prototype;

import dev.latvian.mods.rhino.ContextJS;
import dev.latvian.mods.rhino.js.UndefinedJS;
import org.jetbrains.annotations.Nullable;

public interface MemberFunctions {
	default Object getValue(ContextJS cx, @Nullable Object self, Object key) {
		return UndefinedJS.PROTOTYPE;
	}

	default boolean setValue(ContextJS cx, @Nullable Object self, Object key, Object value) {
		return false;
	}

	default Object invoke(ContextJS cx, @Nullable Object self, Object key, Object[] args) {
		return UndefinedJS.PROTOTYPE;
	}

	default boolean deleteValue(ContextJS cx, @Nullable Object self, Object key) {
		return false;
	}
}
