package dev.latvian.mods.rhino.classdata;

import dev.latvian.mods.rhino.Context;
import dev.latvian.mods.rhino.Scriptable;
import org.jetbrains.annotations.Nullable;

public record CallbackMethodMember<T>(CallbackMethodMember.Callback<T> callback) implements BaseMember {
	public interface Callback<T> {
		Object invoke(Context cx, Scriptable scope, T self, Object[] args) throws Exception;
	}

	@Override
	public Object invoke(Context cx, Scriptable scope, @Nullable Object self, Object[] args, MethodSignature argsSig) throws Exception {
		return callback.invoke(cx, scope, (T) self, args);
	}
}
