package dev.latvian.mods.rhino.util;

import dev.latvian.mods.rhino.Context;
import dev.latvian.mods.rhino.Scriptable;

/**
 * Implement this on a class to override == != === and !== checks in JavaScript
 */
public interface SpecialEquality {
	default boolean specialEquals(Object o, boolean shallow) {
		return equals(o);
	}

	static boolean checkSpecialEquality(Context cx, Scriptable scope, Object o, Object o1, boolean shallow) {
		if (o == o1) {
			return true;
		} else if (o instanceof SpecialEquality s) {
			return s.specialEquals(o1, shallow);
		} else if (o1 != null && o instanceof Enum<?> e) {
			if (o1 instanceof Number n) {
				return e.ordinal() == n.intValue();
			} else {
				return EnumTypeWrapperFactory.getName(cx.getSharedData(scope), o.getClass(), e).equalsIgnoreCase(String.valueOf(o1));
			}
		}

		return false;
	}
}
