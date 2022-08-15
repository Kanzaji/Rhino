package dev.latvian.mods.rhino.js.prototype;

import dev.latvian.mods.rhino.ContextJS;
import dev.latvian.mods.rhino.js.UndefinedJS;
import org.jetbrains.annotations.Nullable;

public interface MemberFunctions {
	MemberFunctions[] EMPTY_ARRAY = new MemberFunctions[0];

	default boolean isStatic() {
		return true;
	}

	default Object getValue(ContextJS cx, @Nullable Object self, Object key, CastType returnType) {
		return UndefinedJS.PROTOTYPE;
	}

	default boolean hasValue(ContextJS cx, @Nullable Object self, Object key) {
		return getValue(cx, self, key, CastType.NONE) != UndefinedJS.PROTOTYPE;
	}

	default boolean setValue(ContextJS cx, @Nullable Object self, Object key, Object value, CastType valueType) {
		return false;
	}

	default Object invoke(ContextJS cx, @Nullable Object self, Object key, Object[] args, CastType returnType) {
		return UndefinedJS.PROTOTYPE;
	}

	default Object deleteValue(ContextJS cx, @Nullable Object self, Object key, CastType returnType) {
		return UndefinedJS.PROTOTYPE;
	}
}
