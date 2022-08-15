package dev.latvian.mods.rhino.js.prototype;

import dev.latvian.mods.rhino.ContextJS;
import dev.latvian.mods.rhino.js.UndefinedJS;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public enum ListMemberFunctions implements MemberFunctions {
	INSTANCE;

	@SuppressWarnings("unchecked")
	private static List<Object> self(@Nullable Object self) {
		return self instanceof List l ? l : List.of();
	}

	@Override
	public Object getValue(ContextJS cx, @Nullable Object self, Object key, CastType returnType) {
		if (key instanceof Number n) {
			return self(self).get(n.intValue());
		}

		return UndefinedJS.PROTOTYPE;
	}

	@Override
	public boolean hasValue(ContextJS cx, @Nullable Object self, Object key) {
		if (key instanceof Number n) {
			int i = n.intValue();
			return i >= 0 && i < self(self).size();
		}

		return false;
	}

	@Override
	public boolean setValue(ContextJS cx, @Nullable Object self, Object key, Object value, CastType valueType) {
		if (key instanceof Number n) {
			int i = n.intValue();
			var list = self(self);

			if (i >= 0 && i < list.size()) {
				list.set(i, valueType.cast(cx, value));
				return true;
			}
		}

		return false;
	}

	@Override
	public Object deleteValue(ContextJS cx, @Nullable Object self, Object key, CastType returnType) {
		if (key instanceof Number n) {
			int i = n.intValue();

			if (i >= 0) {
				var list = self(self);

				if (i < list.size()) {
					return returnType.cast(cx, list.remove(i));
				}
			}
		}

		return UndefinedJS.PROTOTYPE;
	}
}
