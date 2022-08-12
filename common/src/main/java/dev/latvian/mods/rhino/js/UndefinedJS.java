package dev.latvian.mods.rhino.js;

import dev.latvian.mods.rhino.js.prototype.PrototypeJS;

public class UndefinedJS {
	public static final PrototypeJS PROTOTYPE = PrototypeJS.DEFAULT.create("undefined")
			.asBoolean(value -> false)
			.asNumber(value -> 0.0);

	private UndefinedJS() {
	}
}
