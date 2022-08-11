package dev.latvian.mods.rhino.js;

import dev.latvian.mods.rhino.ContextJS;
import dev.latvian.mods.rhino.classdata.ClassData;

public class JavaObjectJS extends ObjectJS {
	public static PrototypeJS OBJECT_PROTOTYPE = PrototypeJS.create("JavaObject")
			.function("getClass", JavaObjectJS::getClassJS);

	public final Object javaObject;
	private ClassData classData;

	public JavaObjectJS(Object javaObject) {
		this.javaObject = javaObject;
		this.classData = null;
	}

	@Override
	public PrototypeJS getPrototype() {
		return OBJECT_PROTOTYPE;
	}

	public ClassData getClassData(ContextJS cx) {
		if (classData == null) {
			classData = cx.getSharedContextData().getClassDataCache().of(javaObject.getClass());
		}

		return classData;
	}

	private static ObjectJS getClassJS(ContextJS cx, ObjectJS self) {
		return ((JavaObjectJS) self).getClassData(cx).asJS(cx);
	}
}
