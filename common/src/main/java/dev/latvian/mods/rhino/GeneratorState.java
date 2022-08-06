package dev.latvian.mods.rhino;

public class GeneratorState {
	public static final int GENERATOR_SEND = 0;
	public static final int GENERATOR_THROW = 1;
	public static final int GENERATOR_CLOSE = 2;

	public static class GeneratorClosedException extends RuntimeException {
	}

	GeneratorState(int operation, Object value) {
		this.operation = operation;
		this.value = value;
	}

	int operation;
	Object value;
	RuntimeException returnedException;
}
