package dev.latvian.mods.rhino.js.prototype;

import dev.latvian.mods.rhino.ContextJS;
import org.jetbrains.annotations.Nullable;

@FunctionalInterface
public interface StaticFunctionCallback extends MemberFunctions {
	Object invoke(ContextJS cx, Object[] args);

	@Override
	default Object invoke(ContextJS cx, @Nullable Object self, Object key, Object[] args, CastType returnType) {
		return returnType.cast(cx, invoke(cx, args));
	}
}