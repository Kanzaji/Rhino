package dev.latvian.mods.rhino;

import java.lang.reflect.Field;

public class FieldAndMethods extends NativeJavaMethod {
	Field field;
	Object javaObject;

	FieldAndMethods(Context cx, Scriptable scope, MemberBox[] methods, Field field) {
		super(methods);
		this.field = field;
		setParentScope(scope);
		setPrototype(cx, getFunctionPrototype(cx, scope));
	}

	@Override
	public Object getDefaultValue(Context cx, Class<?> hint) {
		if (hint == ScriptRuntime.FunctionClass) {
			return this;
		}
		Object rval;
		Class<?> type;
		try {
			rval = field.get(javaObject);
			type = field.getType();
		} catch (IllegalAccessException accEx) {
			throw Context.reportRuntimeError1(cx, "msg.java.internal.private", field.getName());
		}
		rval = cx.getWrapFactory().wrap(cx, this, rval, type);
		if (rval instanceof Scriptable s) {
			rval = s.getDefaultValue(cx, hint);
		}
		return rval;
	}
}
