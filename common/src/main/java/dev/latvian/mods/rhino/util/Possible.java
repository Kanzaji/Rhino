package dev.latvian.mods.rhino.util;

import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.function.Function;

@SuppressWarnings("unchecked")
public record Possible<T>(@Nullable T value) {
	public static final Possible<?> EMPTY = new Possible<>(null);
	public static final Possible<?> NULL = new Possible<>(null);

	public static <T> Possible<T> of(@Nullable T o) {
		return o == null ? (Possible<T>) NULL : new Possible<>(o);
	}


	public static <T> Possible<T> absent() {
		return (Possible<T>) EMPTY;
	}

	public boolean isSet() {
		return this != EMPTY;
	}

	public boolean isEmpty() {
		return this == EMPTY;
	}

	public T get() {
		return Objects.requireNonNull(value);
	}

	@Override
	public String toString() {
		return this == EMPTY ? "EMPTY" : String.valueOf(value);
	}

	public <C> Possible<C> cast(Class<C> type) {
		return (Possible<C>) this;
	}

	public <C> Possible<C> map(Function<T, C> mapper) {
		return isSet() ? of(value == null ? null : mapper.apply(value)) : absent();
	}

	public <C> Possible<C> mapNullable(Function<T, C> mapper) {
		return isSet() ? of(mapper.apply(value)) : absent();
	}
}
