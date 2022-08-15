package dev.latvian.mods.rhino.js.prototype;

import dev.latvian.mods.rhino.ContextJS;
import org.jetbrains.annotations.Nullable;

@FunctionalInterface
public interface StaticPropertyCallback extends MemberFunctions {
	Object getStatic(ContextJS cx);

	@Override
	default Object getValue(ContextJS cx, @Nullable Object self, Object key, CastType returnType) {
		return returnType.cast(cx, getStatic(cx));
	}

	record Fixed(Object value) implements MemberFunctions {
		@Override
		public Object getValue(ContextJS cx, @Nullable Object self, Object key, CastType returnType) {
			return returnType.cast(cx, value);
		}
	}
}