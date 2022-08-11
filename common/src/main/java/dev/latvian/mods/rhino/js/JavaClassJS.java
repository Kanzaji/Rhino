package dev.latvian.mods.rhino.js;

import dev.latvian.mods.rhino.ContextJS;

public class JavaClassJS extends JavaObjectJS {
	public static PrototypeJS PROTOTYPE = OBJECT_PROTOTYPE.createSub("JavaClass")
			.property("name", JavaClassJS::name);

	public final Class<?> type;

	public JavaClassJS(Class<?> type) {
		super(type);
		this.type = type;
	}

	@Override
	public PrototypeJS getPrototype() {
		return PROTOTYPE;
	}

	private static ObjectJS name(ContextJS cx, ObjectJS self) {
		return StringJS.of(((JavaClassJS) self).getClassData(cx).toString());
	}
}
