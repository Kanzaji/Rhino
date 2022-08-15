package dev.latvian.mods.rhino.js.prototype;

import dev.latvian.mods.rhino.ContextJS;
import org.jetbrains.annotations.Nullable;

@FunctionalInterface
public interface FunctionCallback extends MemberFunctions {
	Object invoke(ContextJS cx, Object self, Object[] args);

	@Override
	default boolean isStatic() {
		return false;
	}

	@Override
	default Object invoke(ContextJS cx, @Nullable Object self, Object key, Object[] args, CastType returnType) {
		return returnType.cast(cx, invoke(cx, self, args));
	}
}