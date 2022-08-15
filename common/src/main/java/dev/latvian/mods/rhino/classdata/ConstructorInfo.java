package dev.latvian.mods.rhino.classdata;

import dev.latvian.mods.rhino.Context;
import dev.latvian.mods.rhino.ContextJS;
import dev.latvian.mods.rhino.NativeJavaList;
import dev.latvian.mods.rhino.Scriptable;
import dev.latvian.mods.rhino.js.prototype.CastType;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;

public class ConstructorInfo implements ExecutableInfo {
	public static final ConstructorInfo[] EMPTY_ARRAY = new ConstructorInfo[0];

	private final Constructor<?> javaConstructor;
	public final MethodSignature signature;

	public ConstructorInfo(Constructor<?> javaConstructor) {
		this.javaConstructor = javaConstructor;
		this.signature = MethodSignature.ofExecutable(javaConstructor);
	}

	@Override
	public MethodSignature getSignature() {
		return signature;
	}

	@Override
	public Object invoke(ContextJS cx, @Nullable Object self, Object key, Object[] args, CastType returnType) {
		Class<?>[] argTypes = signature.types;
		Scriptable scope = cx.getTopLevelScope();

		if (javaConstructor.isVarArgs()) {
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
			return returnType.cast(cx, javaConstructor.newInstance(args));
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}
}