package dev.latvian.mods.rhino.js;

import dev.latvian.mods.rhino.ContextJS;
import dev.latvian.mods.rhino.js.prototype.PrototypeJS;

public class BooleanJS {
	public static PrototypeJS PROTOTYPE = PrototypeJS.DEFAULT.create(TypeJS.BOOLEAN, "Boolean")
			.constructor(BooleanJS::construct)
			.asNumber(object -> (Boolean) object ? 1.0 : 0.0)
			.asBoolean(object -> (Boolean) object);

	private static Boolean construct(ContextJS cx, Object[] args) {
		if (args.length == 0 || args[0] == null || args[0] == UndefinedJS.PROTOTYPE) {
			return Boolean.FALSE;
		} else if (args[0] instanceof Boolean b) {
			return b;
		} else if (args[0] instanceof Number n) {
			return n.doubleValue() != 0.0;
		} else if (args[0] instanceof CharSequence s) {
			return !s.isEmpty();
		} else {
			return Boolean.TRUE;
		}
	}
}
