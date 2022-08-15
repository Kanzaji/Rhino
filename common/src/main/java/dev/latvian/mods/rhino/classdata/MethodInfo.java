package dev.latvian.mods.rhino.classdata;

import dev.latvian.mods.rhino.Context;
import dev.latvian.mods.rhino.ContextJS;
import dev.latvian.mods.rhino.FunctionObject;
import dev.latvian.mods.rhino.NativeJavaList;
import dev.latvian.mods.rhino.Scriptable;
import dev.latvian.mods.rhino.Undefined;
import dev.latvian.mods.rhino.js.prototype.CastType;
import dev.latvian.mods.rhino.util.HideFromJS;
import dev.latvian.mods.rhino.util.RemapForJS;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

public class MethodInfo implements ExecutableInfo {
	public static final MethodInfo[] EMPTY_ARRAY = new MethodInfo[0];

	public final Method javaMethod;
	public final MethodSignature signature;
	public final String remappedName;

	MethodInfo(Method javaMethod, MutableBoolean hidden, PublicClassData[] parentList, String[] remapPrefixes) {
		this.javaMethod = javaMethod;
		this.signature = MethodSignature.ofExecutable(javaMethod);
		String remappedName = "";

		var m = javaMethod;

		for (var c : parentList) {
			if (m != null) {
				if (m.isAnnotationPresent(HideFromJS.class)) {
					hidden.setTrue();
					break;
				}

				if (remappedName.isBlank()) {
					var ra = m.getAnnotation(RemapForJS.class);
					remappedName = ra == null ? "" : ra.value();
				}
			}

			try {
				m = c.type.getDeclaredMethod(javaMethod.getName(), signature.types);
			} catch (Exception ex) {
				m = null;
			}
		}

		if (hidden.isFalse() && remappedName.isBlank()) {
			for (String s : remapPrefixes) {
				if (javaMethod.getName().startsWith(s)) {
					remappedName = javaMethod.getName().substring(s.length());
					break;
				}
			}
		}

		this.remappedName = remappedName;
	}

	@Override
	public boolean isStatic() {
		return Modifier.isStatic(javaMethod.getModifiers());
	}

	@Override
	public MethodSignature getSignature() {
		return signature;
	}

	public Class<?> getType() {
		return javaMethod.getReturnType();
	}

	public boolean isVoid() {
		var c = getType();
		return c == void.class || c == Void.class;
	}

	public boolean isBoolean() {
		var c = getType();
		return c == boolean.class || c == Boolean.class;
	}

	@Override
	public Object getValue(ContextJS cx, @Nullable Object self, Object key, CastType returnType) {
		return new FunctionObject(cx.context, key.toString(), self == null ? this : new DelegatedMemberFunctions(this, self), cx.getScope());
	}

	@Override
	public Object invoke(ContextJS cx, @Nullable Object self, Object key, Object[] args, CastType returnType) {
		Class<?>[] argTypes = signature.types;
		Scriptable scope = cx.getTopLevelScope();

		if (javaMethod.isVarArgs()) {
			// marshall the explicit parameter
			Object[] newArgs = new Object[argTypes.length];
			for (int i = 0; i < argTypes.length - 1; i++) {
				newArgs[i] = Context.jsToJava(cx.context, scope, args[i], argTypes[i]);
			}

			Object varArgs;

			// Handle special situation where a single variable parameter
			// is given and it is a Java or ECMA array.
			if (args.length == argTypes.length && (args[args.length - 1] == null || args[args.length - 1] instanceof NativeJavaList)) {
				// convert the ECMA array into a native array
				varArgs = Context.jsToJava(cx.context, scope, args[args.length - 1], argTypes[argTypes.length - 1]);
			} else {
				// marshall the variable parameter
				Class<?> componentType = argTypes[argTypes.length - 1].getComponentType();
				varArgs = Array.newInstance(componentType, args.length - argTypes.length + 1);
				for (int i = 0; i < args.length - argTypes.length + 1; i++) {
					Object value = Context.jsToJava(cx.context, scope, args[argTypes.length - 1 + i], componentType);
					Array.set(varArgs, i, value);
				}
			}

			// add varargs
			newArgs[argTypes.length - 1] = varArgs;
			// replace the original args with the new one
			args = newArgs;
		} else {
			Object[] origArgs = args;
			for (int i = 0; i < args.length; i++) {
				Object arg = args[i];
				Object x = Context.jsToJava(cx.context, scope, arg, argTypes[i]);
				if (x != arg) {
					if (args == origArgs) {
						args = origArgs.clone();
					}
					args[i] = x;
				}
			}
		}

		try {
			var result = javaMethod.invoke(self, args);

			if (result == null && isVoid()) {
				return Undefined.instance;
			}

			return returnType.cast(cx, result);
		} catch (Exception ex) {
			throw Context.throwAsScriptRuntimeEx(ex);
		}
	}
}