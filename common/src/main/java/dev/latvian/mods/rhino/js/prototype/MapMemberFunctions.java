package dev.latvian.mods.rhino.js.prototype;

import dev.latvian.mods.rhino.ContextJS;
import dev.latvian.mods.rhino.js.UndefinedJS;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public enum MapMemberFunctions implements MemberFunctions {
	INSTANCE;

	@SuppressWarnings("unchecked")
	private static Map<Object, Object> self(@Nullable Object self) {
		return self instanceof Map l ? l : Map.of();
	}

	@Override
	public Object getValue(ContextJS cx, @Nullable Object self, Object key, CastType returnType) {
		return self(self).getOrDefault(key, UndefinedJS.PROTOTYPE);
	}

	@Override
	public boolean hasValue(ContextJS cx, @Nullable Object self, Object key) {
		return self(self).containsKey(key);
	}

	@Override
	public boolean setValue(ContextJS cx, @Nullable Object self, Object key, Object value, CastType valueType) {
		self(self).put(key, valueType.cast(cx, value));
		return true;
	}

	@Override
	public Object deleteValue(ContextJS cx, @Nullable Object self, Object key, CastType returnType) {
		var map = self(self);

		if (map.containsKey(key)) {
			var r = map.remove(key);
			return r == null ? null : returnType.cast(cx, r);
		}

		return UndefinedJS.PROTOTYPE;
	}
}
