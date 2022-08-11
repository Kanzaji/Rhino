package dev.latvian.mods.rhino.js;

import dev.latvian.mods.rhino.ContextJS;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public class PrototypeJS {
	@FunctionalInterface
	public interface ConstructorCallback {
		ObjectJS construct(ContextJS cx, Object[] args) throws Exception;
	}

	@FunctionalInterface
	public interface PropertyCallback {
		ObjectJS get(ContextJS cx, ObjectJS self) throws Exception;
	}

	@FunctionalInterface
	public interface FunctionCallback {
		ObjectJS invoke(ContextJS cx, ObjectJS self, Object[] args) throws Exception;
	}

	@FunctionalInterface
	public interface FunctionCallbackNoArgs extends FunctionCallback {
		ObjectJS invoke(ContextJS cx, ObjectJS self) throws Exception;

		@Override
		default ObjectJS invoke(ContextJS cx, ObjectJS self, Object[] args) throws Exception {
			return invoke(cx, self);
		}
	}

	public static final PrototypeJS EMPTY = new PrototypeJS("null") {
		@Override
		public PrototypeJS createSub(String name) {
			return create(name);
		}

		@Override
		public PrototypeJS property(String name, PropertyCallback callback) {
			return this;
		}

		@Override
		public PrototypeJS function(String name, FunctionCallback callback) {
			return this;
		}
	};

	private static class CombinedPrototypeJS extends PrototypeJS {
		private final PrototypeJS parent;

		private CombinedPrototypeJS(String name, PrototypeJS parent) {
			super(name);
			this.parent = parent;
		}

		@Override
		public PropertyCallback getProperty(String name) {
			var v = super.getProperty(name);
			return v == null ? parent.getProperty(name) : v;
		}

		@Override
		public FunctionCallback getFunction(String name) {
			var v = super.getFunction(name);
			return v == null ? parent.getFunction(name) : v;
		}
	}

	public static PrototypeJS create(String name) {
		return new PrototypeJS(name);
	}

	public final String name;
	private ConstructorCallback constructor;
	private Map<String, PropertyCallback> properties;
	private Map<String, FunctionCallback> functions;

	private PrototypeJS(String name) {
		this.name = name;
	}

	@Override
	public String toString() {
		return name;
	}

	public PrototypeJS createSub(String name) {
		return new CombinedPrototypeJS(name, this);
	}

	public PrototypeJS constructor(ConstructorCallback callback) {
		this.constructor = callback;
		return this;
	}

	public PrototypeJS property(String name, PropertyCallback callback) {
		if (properties == null) {
			properties = new HashMap<>();
		}

		properties.put(name, callback);
		return this;
	}

	public PrototypeJS function(String name, FunctionCallback callback) {
		if (functions == null) {
			functions = new HashMap<>();
		}

		functions.put(name, callback);
		return this;
	}

	public PrototypeJS function(String name, FunctionCallbackNoArgs callback) {
		return function(name, (FunctionCallback) callback);
	}

	@Nullable
	public ConstructorCallback getConstructor() {
		return constructor;
	}

	@Nullable
	public PropertyCallback getProperty(String name) {
		return properties == null ? null : properties.get(name);
	}

	@Nullable
	public FunctionCallback getFunction(String name) {
		return functions == null ? null : functions.get(name);
	}
}
