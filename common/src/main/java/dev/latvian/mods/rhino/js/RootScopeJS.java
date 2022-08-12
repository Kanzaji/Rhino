package dev.latvian.mods.rhino.js;

import dev.latvian.mods.rhino.js.prototype.PrototypeJS;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public class RootScopeJS extends ScopeJS {
	// Keep ClassCache and other shared data here
	public final Map<String, PrototypeJS> prototypes = new HashMap<>();

	public void addPrototype(PrototypeJS prototype) {
		prototypes.put(prototype.name, prototype);
	}

	public static RootScopeJS create() {
		var scope = new RootScopeJS();
		scope.addPrototype(UndefinedJS.PROTOTYPE);
		scope.addPrototype(StringJS.PROTOTYPE);
		scope.addPrototype(NumberJS.PROTOTYPE);
		scope.addPrototype(BooleanJS.PROTOTYPE);
		scope.addPrototype(ObjectJS.PROTOTYPE);
		scope.addPrototype(ArrayJS.PROTOTYPE);
		scope.addPrototype(JavaObjectJS.PROTOTYPE);
		scope.addPrototype(JavaClassJS.PROTOTYPE);
		// Function
		// Date
		// Math
		// RegExp
		// Set
		// Promise
		// NaN
		return scope;
	}

	@Override
	@Nullable
	public ScopeJS getParent() {
		return null;
	}

	@Override
	public void setParent(@Nullable ScopeJS parent) {
	}

	@Override
	public RootScopeJS getRoot() {
		return this;
	}
}
