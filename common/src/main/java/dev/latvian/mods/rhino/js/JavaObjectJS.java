package dev.latvian.mods.rhino.js;

import dev.latvian.mods.rhino.ContextJS;
import dev.latvian.mods.rhino.js.prototype.PrototypeJS;

public class JavaObjectJS {
	public static PrototypeJS PROTOTYPE = PrototypeJS.DEFAULT.create("JavaObject")
			.function("getClass", JavaObjectJS::getClassJS);

	private static Object getClassJS(ContextJS cx, Object self) {
		return self.getClass();
	}
}
