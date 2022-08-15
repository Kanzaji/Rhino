package dev.latvian.mods.rhino.js.prototype;

import dev.latvian.mods.rhino.ContextJS;
import org.jetbrains.annotations.Nullable;

@FunctionalInterface
public interface PropertyCallback extends MemberFunctions {
	Object get(ContextJS cx, Object self);

	@Override
	default boolean isStatic() {
		return false;
	}

	@Override
	default Object getValue(ContextJS cx, @Nullable Object self, Object key, CastType returnType) {
		return returnType.cast(cx, get(cx, self));
	}
}