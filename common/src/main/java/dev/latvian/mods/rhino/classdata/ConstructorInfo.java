package dev.latvian.mods.rhino.classdata;

import dev.latvian.mods.rhino.Context;
import dev.latvian.mods.rhino.Scriptable;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Constructor;

public class ConstructorInfo extends ExecutableInfo {
	public static final ConstructorInfo[] EMPTY_ARRAY = new ConstructorInfo[0];

	ConstructorInfo(Constructor<?> constructor) {
		super(constructor);
	}

	@Override
	public boolean isStatic() {
		return true;
	}

	@Override
	public boolean isMethod() {
		return false;
	}

	@Override
	public Class<?> getType() {
		return getDeclaringClass();
	}

	public Object newInstance(Object[] args) throws Exception {
		return ((Constructor<?>) member).newInstance(args);
	}

	@Override
	public Object invoke(Context cx, Scriptable scope, @Nullable Object self, Object[] args, MethodSignature argsSig) throws Exception {
		return newInstance(args);
	}
}