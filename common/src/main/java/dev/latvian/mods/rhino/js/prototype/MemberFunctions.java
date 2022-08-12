package dev.latvian.mods.rhino.js.prototype;

import dev.latvian.mods.rhino.ContextJS;
import dev.latvian.mods.rhino.js.UndefinedJS;

public interface MemberFunctions {
	default Object getValue(ContextJS cx, Object self, Object key) {
		return UndefinedJS.PROTOTYPE;
	}

	default boolean setValue(ContextJS cx, Object self, Object key, Object value) {
		return false;
	}

	default Object invoke(ContextJS cx, Object self, Object key, Object[] args) {
		return UndefinedJS.PROTOTYPE;
	}

	default boolean deleteValue(ContextJS cx, Object self, Object key) {
		return false;
	}
}
