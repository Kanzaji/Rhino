package dev.latvian.mods.rhino.classdata;

import dev.latvian.mods.rhino.ContextJS;
import org.jetbrains.annotations.Nullable;

public record SyntheticMethod<T>(Class<?> type, SyntheticMethod.Callback<T> callback, MethodSignature sig, boolean isStatic) implements BaseMember {
	public interface Callback<T> {
		Object invoke(ContextJS cx, T self, Object[] args) throws Exception;
	}

	public static <T> SyntheticMethod<T> make(Class<?> type, Callback<T> callback, Class<?>... sig) {
		return new SyntheticMethod<>(type, callback, MethodSignature.of(sig), false);
	}

	public static <T> SyntheticMethod<T> make(Callback<T> callback, Class<?>... sig) {
		return make(Void.TYPE, callback, sig);
	}

	public static <T> SyntheticMethod<T> makeStatic(Class<?> type, Callback<T> callback, Class<?>... sig) {
		return new SyntheticMethod<>(type, callback, MethodSignature.of(sig), true);
	}

	public static <T> SyntheticMethod<T> makeStatic(Callback<T> callback, Class<?>... sig) {
		return makeStatic(Void.TYPE, callback, sig);
	}

	@Override
	public Object invoke(ContextJS cx, @Nullable Object self, Object[] args) throws Exception {
		return callback.invoke(cx, (T) self, args);
	}

	@Override
	public MethodSignature getSignature() {
		return sig;
	}

	@Override
	public Class<?> getType() {
		return type;
	}

	@Override
	public boolean isStatic() {
		return isStatic;
	}
}
